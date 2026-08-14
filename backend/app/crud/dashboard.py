from datetime import datetime

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.fridge_item import FridgeItem
from app.models.notification import Notification


def count_fridge_items(db: Session) -> int:
    return db.scalar(select(func.count()).select_from(FridgeItem)) or 0


def count_starred_fridge_items(db: Session) -> int:
    return db.scalar(
        select(func.count()).select_from(FridgeItem).where(FridgeItem.starred.is_(True))
    ) or 0


def count_expiring(db: Session, *, now: datetime, threshold: datetime) -> int:
    return db.scalar(
        select(func.count()).select_from(FridgeItem).where(
            FridgeItem.deadline.is_not(None),
            FridgeItem.deadline >= now,
            FridgeItem.deadline <= threshold,
        )
    ) or 0


def count_expired(db: Session, *, now: datetime) -> int:
    return db.scalar(
        select(func.count()).select_from(FridgeItem).where(
            FridgeItem.deadline.is_not(None),
            FridgeItem.deadline < now,
        )
    ) or 0


def count_upcoming_notifications(db: Session, *, now: datetime) -> int:
    return db.scalar(
        select(func.count()).select_from(Notification).where(
            Notification.sent.is_(False),
            Notification.notify_time >= now,
        )
    ) or 0
