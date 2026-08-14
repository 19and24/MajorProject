from datetime import datetime

from fastapi import APIRouter, Depends, Query, Response, status
from sqlalchemy.orm import Session

from app.core.responses import success
from app.dependencies import get_db
from app.schemas.notification import NotificationCreate, NotificationUpdate
from app.services import notification as notification_service

router = APIRouter(prefix="/notifications", tags=["notifications"])


@router.get("")
def list_notifications(
    fridge_item_id: int | None = Query(None, alias="fridgeItemId"),
    sent: bool | None = None,
    from_: datetime | None = Query(None, alias="from"),
    to: datetime | None = None,
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=200),
    db: Session = Depends(get_db),
):
    data = notification_service.list_notifications(
        db, fridge_item_id=fridge_item_id, sent=sent, from_time=from_, to_time=to,
        page=page, size=size,
    )
    return success(data)


@router.get("/{notification_id}")
def get_notification(notification_id: int, db: Session = Depends(get_db)):
    return success(notification_service.get_notification(db, notification_id))


@router.post("", status_code=status.HTTP_201_CREATED)
def create_notification(body: NotificationCreate, db: Session = Depends(get_db)):
    return success(notification_service.create_notification(db, body))


@router.patch("/{notification_id}")
def update_notification(
    notification_id: int, body: NotificationUpdate, db: Session = Depends(get_db)
):
    return success(notification_service.update_notification(db, notification_id, body))


@router.delete("/{notification_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_notification(notification_id: int, db: Session = Depends(get_db)):
    notification_service.delete_notification(db, notification_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)
