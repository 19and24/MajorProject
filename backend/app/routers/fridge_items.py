from datetime import datetime

from fastapi import APIRouter, Depends, Query, Response, status
from sqlalchemy.orm import Session

from app.core.responses import success
from app.dependencies import get_db
from app.schemas.fridge_item import (
    FridgeItemConsume,
    FridgeItemCreate,
    FridgeItemStarUpdate,
    FridgeItemUpdate,
)
from app.schemas.notification import DefaultNotificationsCreate
from app.services import fridge_item as fridge_item_service
from app.services import notification as notification_service

router = APIRouter(prefix="/fridge-items", tags=["fridge-items"])


@router.get("")
def list_fridge_items(
    keyword: str | None = None,
    category: str | None = None,
    starred: bool | None = None,
    expired: bool | None = None,
    deadline_before: datetime | None = Query(None, alias="deadlineBefore"),
    sort: str = "deadline,asc",
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=200),
    db: Session = Depends(get_db),
):
    data = fridge_item_service.list_items(
        db, keyword=keyword, category=category, starred=starred, expired=expired,
        deadline_before=deadline_before, sort=sort, page=page, size=size,
    )
    return success(data)


@router.get("/expiring")
def list_expiring_fridge_items(
    days: int = Query(3, ge=0),
    include_expired: bool = Query(False, alias="includeExpired"),
    db: Session = Depends(get_db),
):
    data = fridge_item_service.list_expiring(db, days=days, include_expired=include_expired)
    return success(data)


@router.get("/{item_id}")
def get_fridge_item(item_id: int, db: Session = Depends(get_db)):
    return success(fridge_item_service.get_item(db, item_id))


@router.post("", status_code=status.HTTP_201_CREATED)
def create_fridge_item(body: FridgeItemCreate, db: Session = Depends(get_db)):
    return success(fridge_item_service.create_item(db, body))


@router.patch("/{item_id}")
def update_fridge_item(
    item_id: int, body: FridgeItemUpdate, db: Session = Depends(get_db)
):
    return success(fridge_item_service.update_item(db, item_id, body))


@router.delete("/{item_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_fridge_item(item_id: int, db: Session = Depends(get_db)):
    fridge_item_service.delete_item(db, item_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.patch("/{item_id}/star")
def update_fridge_item_star(
    item_id: int, body: FridgeItemStarUpdate, db: Session = Depends(get_db)
):
    return success(fridge_item_service.set_starred(db, item_id, body.starred))


@router.post("/{item_id}/consume")
def consume_fridge_item(
    item_id: int, body: FridgeItemConsume, db: Session = Depends(get_db)
):
    return success(fridge_item_service.consume_item(db, item_id, body.quantity))


@router.post("/{item_id}/notifications/default")
def create_default_notifications(
    item_id: int, body: DefaultNotificationsCreate, db: Session = Depends(get_db)
):
    data = notification_service.create_default_notifications(
        db, item_id, days_before=body.days_before, notify_hour=body.notify_hour
    )
    return success(data)
