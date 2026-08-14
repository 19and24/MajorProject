from datetime import datetime, timedelta
from typing import Any

from sqlalchemy import asc, desc, func, select
from sqlalchemy.orm import Session, joinedload

from app.models.fridge_item import FridgeItem
from app.models.ingredient import Ingredient


SORT_COLUMNS = {
    "deadline": FridgeItem.deadline,
    "createdAt": FridgeItem.created_at,
}


def get(db: Session, item_id: int) -> FridgeItem | None:
    return db.scalar(
        select(FridgeItem)
        .options(joinedload(FridgeItem.ingredient))
        .where(FridgeItem.id == item_id)
    )


def get_many(
    db: Session,
    *,
    keyword: str | None,
    category: str | None,
    starred: bool | None,
    expired: bool | None,
    deadline_before: datetime | None,
    page: int,
    size: int,
    sort_field: str,
    descending: bool,
    now: datetime,
) -> tuple[list[FridgeItem], int]:
    query = (
        select(FridgeItem)
        .join(FridgeItem.ingredient)
        .options(joinedload(FridgeItem.ingredient))
    )
    if keyword:
        query = query.where(Ingredient.name.contains(keyword))
    if category:
        query = query.where(Ingredient.category == category)
    if starred is not None:
        query = query.where(FridgeItem.starred == starred)
    if expired is True:
        query = query.where(FridgeItem.deadline < now)
    elif expired is False:
        query = query.where((FridgeItem.deadline >= now) | FridgeItem.deadline.is_(None))
    if deadline_before:
        query = query.where(FridgeItem.deadline <= deadline_before)

    direction = desc if descending else asc
    query = query.order_by(direction(SORT_COLUMNS[sort_field]), FridgeItem.id.asc())
    total = db.scalar(select(func.count()).select_from(query.order_by(None).subquery())) or 0
    items = list(db.scalars(query.offset((page - 1) * size).limit(size)).unique().all())
    return items, total


def get_expiring(
    db: Session,
    *,
    days: int,
    include_expired: bool,
    now: datetime,
) -> list[FridgeItem]:
    threshold = now + timedelta(days=days)
    query = (
        select(FridgeItem)
        .options(joinedload(FridgeItem.ingredient))
        .where(FridgeItem.deadline.is_not(None))
        .where(FridgeItem.deadline <= threshold)
    )
    if not include_expired:
        query = query.where(FridgeItem.deadline >= now)
    query = query.order_by(FridgeItem.deadline.asc(), FridgeItem.id.asc())
    return list(db.scalars(query).unique().all())


def create(db: Session, values: dict[str, Any]) -> FridgeItem:
    item = FridgeItem(**values)
    db.add(item)
    db.commit()
    return get(db, item.id)  # type: ignore[return-value]


def update(db: Session, item: FridgeItem, changes: dict[str, Any]) -> FridgeItem:
    for field, value in changes.items():
        setattr(item, field, value)
    db.commit()
    return get(db, item.id)  # type: ignore[return-value]


def delete(db: Session, item: FridgeItem) -> None:
    db.delete(item)
    db.commit()
