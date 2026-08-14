from datetime import datetime
from typing import Any

from sqlalchemy import asc, func, select
from sqlalchemy.orm import Session, joinedload

from app.models.notification import Notification


SORT_COLUMNS = {
    "notifyTime": Notification.notify_time,
}


def get(db: Session, notification_id: int) -> Notification | None:
    return db.scalar(
        select(Notification)
        .options(joinedload(Notification.fridge_item))
        .where(Notification.id == notification_id)
    )


def get_many(
    db: Session,
    *,
    fridge_item_id: int | None,
    sent: bool | None,
    from_time: datetime | None,
    to_time: datetime | None,
    page: int,
    size: int,
) -> tuple[list[Notification], int]:
    query = select(Notification).options(joinedload(Notification.fridge_item))
    if fridge_item_id is not None:
        query = query.where(Notification.fridge_item_id == fridge_item_id)
    if sent is not None:
        query = query.where(Notification.sent == sent)
    if from_time is not None:
        query = query.where(Notification.notify_time >= from_time)
    if to_time is not None:
        query = query.where(Notification.notify_time <= to_time)

    query = query.order_by(asc(Notification.notify_time), Notification.id.asc())
    total = db.scalar(select(func.count()).select_from(query.order_by(None).subquery())) or 0
    items = list(db.scalars(query.offset((page - 1) * size).limit(size)).unique().all())
    return items, total


def exists_for_fridge_item_at(db: Session, fridge_item_id: int, notify_time: datetime) -> bool:
    return db.scalar(
        select(Notification.id).where(
            Notification.fridge_item_id == fridge_item_id,
            Notification.notify_time == notify_time,
        )
    ) is not None


def create(db: Session, values: dict[str, Any]) -> Notification:
    notification = Notification(**values)
    db.add(notification)
    db.commit()
    return get(db, notification.id)  # type: ignore[return-value]


def create_many(db: Session, values_list: list[dict[str, Any]]) -> list[Notification]:
    notifications = [Notification(**values) for values in values_list]
    db.add_all(notifications)
    db.commit()
    return [get(db, n.id) for n in notifications]  # type: ignore[misc]


def update(db: Session, notification: Notification, changes: dict[str, Any]) -> Notification:
    for field, value in changes.items():
        setattr(notification, field, value)
    db.commit()
    return get(db, notification.id)  # type: ignore[return-value]


def delete(db: Session, notification: Notification) -> None:
    db.delete(notification)
    db.commit()
