from datetime import datetime, timedelta

from sqlalchemy.orm import Session

from app.core.responses import api_error, pagination
from app.crud import fridge_item as fridge_item_crud
from app.crud import notification as notification_crud
from app.models.notification import Notification
from app.schemas.notification import NotificationCreate, NotificationUpdate


def serialize_list_item(notification: Notification) -> dict:
    fridge_item = notification.fridge_item
    return {
        "id": notification.id,
        "fridgeItem": {
            "id": fridge_item.id,
            "ingredientName": fridge_item.ingredient.name,
        },
        "title": notification.title,
        "content": notification.content,
        "notifyTime": notification.notify_time,
        "sent": notification.sent,
    }


def serialize_detail(notification: Notification) -> dict:
    fridge_item = notification.fridge_item
    return {
        "id": notification.id,
        "fridgeItem": {
            "id": fridge_item.id,
            "ingredient": {
                "id": fridge_item.ingredient.id,
                "name": fridge_item.ingredient.name,
            },
            "deadline": fridge_item.deadline,
        },
        "title": notification.title,
        "content": notification.content,
        "notifyTime": notification.notify_time,
        "sent": notification.sent,
    }


def serialize_created(notification: Notification) -> dict:
    return {
        "id": notification.id,
        "fridgeItemId": notification.fridge_item_id,
        "title": notification.title,
        "content": notification.content,
        "notifyTime": notification.notify_time,
        "sent": notification.sent,
    }


def require_notification(db: Session, notification_id: int) -> Notification:
    notification = notification_crud.get(db, notification_id)
    if notification is None:
        raise api_error(404, "알림을 찾을 수 없습니다.", "NOTIFICATION_NOT_FOUND")
    return notification


def require_fridge_item(db: Session, fridge_item_id: int):
    item = fridge_item_crud.get(db, fridge_item_id)
    if item is None:
        raise api_error(404, "냉장고 재고를 찾을 수 없습니다.", "FRIDGE_ITEM_NOT_FOUND")
    return item


def list_notifications(db: Session, *, fridge_item_id: int | None, sent: bool | None,
                        from_time: datetime | None, to_time: datetime | None,
                        page: int, size: int) -> dict:
    items, total = notification_crud.get_many(
        db, fridge_item_id=fridge_item_id, sent=sent, from_time=from_time,
        to_time=to_time, page=page, size=size,
    )
    return pagination([serialize_list_item(item) for item in items], page, size, total)


def get_notification(db: Session, notification_id: int) -> dict:
    return serialize_detail(require_notification(db, notification_id))


def create_notification(db: Session, body: NotificationCreate) -> dict:
    require_fridge_item(db, body.fridge_item_id)
    if body.notify_time <= datetime.now():
        raise api_error(422, "알림 시간이 올바르지 않습니다.", "INVALID_NOTIFY_TIME")
    notification = notification_crud.create(db, body.model_dump())
    return serialize_created(notification)


def update_notification(db: Session, notification_id: int, body: NotificationUpdate) -> dict:
    notification = require_notification(db, notification_id)
    changes = body.model_dump(exclude_unset=True)
    if changes.get("notify_time") is not None and changes["notify_time"] <= datetime.now():
        raise api_error(422, "알림 시간이 올바르지 않습니다.", "INVALID_NOTIFY_TIME")
    updated = notification_crud.update(db, notification, changes)
    return serialize_created(updated)


def delete_notification(db: Session, notification_id: int) -> None:
    notification_crud.delete(db, require_notification(db, notification_id))


def create_default_notifications(db: Session, fridge_item_id: int, *,
                                  days_before: list[int], notify_hour: int) -> dict:
    item = require_fridge_item(db, fridge_item_id)
    if item.deadline is None:
        raise api_error(422, "기본 알림 생성에 필요한 유통기한이 없습니다.", "DEADLINE_REQUIRED")

    now = datetime.now()
    to_create: list[dict] = []

    for days in days_before:
        notify_time = (item.deadline - timedelta(days=days)).replace(
            hour=notify_hour, minute=0, second=0, microsecond=0
        )
        if notify_time <= now:
            continue
        already_queued = any(entry["notify_time"] == notify_time for entry in to_create)
        if already_queued or notification_crud.exists_for_fridge_item_at(
            db, fridge_item_id, notify_time
        ):
            continue
        to_create.append({
            "fridge_item_id": fridge_item_id,
            "title": f"{item.ingredient.name} 유통기한 임박",
            "content": f"{item.ingredient.name}의 유통기한이 {days}일 남았습니다.",
            "notify_time": notify_time,
            "days_before": days,
        })

    created = []
    if to_create:
        values_list = [
            {k: v for k, v in entry.items() if k != "days_before"} for entry in to_create
        ]
        created = notification_crud.create_many(db, values_list)

    created_summary = [
        {
            "id": notification.id,
            "notifyTime": notification.notify_time,
            "daysBefore": entry["days_before"],
        }
        for notification, entry in zip(created, to_create)
    ]
    return {"createdNotifications": created_summary}
