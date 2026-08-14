from datetime import datetime
from decimal import Decimal

from sqlalchemy.orm import Session

from app.core.responses import api_error, pagination
from app.crud import fridge_item as fridge_item_crud
from app.crud import ingredient as ingredient_crud
from app.models.fridge_item import FridgeItem
from app.schemas.fridge_item import FridgeItemCreate, FridgeItemUpdate


def serialize_decimal(value: Decimal):
    return int(value) if value == value.to_integral() else float(value)


def serialize(item: FridgeItem, *, now: datetime | None = None) -> dict:
    now = now or datetime.now()
    return {
        "id": item.id,
        "ingredient": {
            "id": item.ingredient.id,
            "name": item.ingredient.name,
            "category": item.ingredient.category,
            "emoji": item.ingredient.emoji,
        },
        "quantity": serialize_decimal(item.quantity),
        "unit": item.unit,
        "deadline": item.deadline,
        "starred": item.starred,
        "expired": item.deadline is not None and item.deadline < now,
        "daysUntilDeadline": (item.deadline.date() - now.date()).days if item.deadline else None,
        "createdAt": item.created_at,
        "updatedAt": item.updated_at,
    }


def require_item(db: Session, item_id: int) -> FridgeItem:
    item = fridge_item_crud.get(db, item_id)
    if item is None:
        raise api_error(404, "\ub0c9\uc7a5\uace0 \uc7ac\uace0\ub97c \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.", "FRIDGE_ITEM_NOT_FOUND")
    return item


def require_ingredient(db: Session, ingredient_id: int | None) -> None:
    if ingredient_id is None or ingredient_crud.get(db, ingredient_id) is None:
        raise api_error(404, "\ud45c\uc900 \uc2dd\uc7ac\ub8cc\ub97c \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.", "INGREDIENT_NOT_FOUND")


def parse_sort(sort: str) -> tuple[str, bool]:
    field, separator, direction = sort.partition(",")
    if not separator or field not in fridge_item_crud.SORT_COLUMNS or direction not in {"asc", "desc"}:
        raise api_error(400, "\uc815\ub82c \uc870\uac74\uc774 \uc62c\ubc14\ub974\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4.", "INVALID_SORT")
    return field, direction == "desc"


def list_items(db: Session, *, keyword: str | None, category: str | None,
               starred: bool | None, expired: bool | None,
               deadline_before: datetime | None, sort: str, page: int, size: int) -> dict:
    sort_field, descending = parse_sort(sort)
    now = datetime.now()
    items, total = fridge_item_crud.get_many(
        db, keyword=keyword, category=category, starred=starred, expired=expired,
        deadline_before=deadline_before, page=page, size=size, sort_field=sort_field,
        descending=descending, now=now,
    )
    return pagination([serialize(item, now=now) for item in items], page, size, total)


def get_item(db: Session, item_id: int) -> dict:
    return serialize(require_item(db, item_id))


def create_item(db: Session, body: FridgeItemCreate) -> dict:
    require_ingredient(db, body.ingredient_id)
    return serialize(fridge_item_crud.create(db, body.model_dump()))


def update_item(db: Session, item_id: int, body: FridgeItemUpdate) -> dict:
    item = require_item(db, item_id)
    changes = body.model_dump(exclude_unset=True)
    if "ingredient_id" in changes:
        require_ingredient(db, changes["ingredient_id"])
    if any(value is None for field, value in changes.items() if field != "deadline"):
        raise api_error(422, "\uc785\ub825\uac12\uc774 \uc62c\ubc14\ub974\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4.", "VALIDATION_ERROR")
    return serialize(fridge_item_crud.update(db, item, changes))


def delete_item(db: Session, item_id: int) -> None:
    fridge_item_crud.delete(db, require_item(db, item_id))


def set_starred(db: Session, item_id: int, starred: bool) -> dict:
    item = require_item(db, item_id)
    fridge_item_crud.update(db, item, {"starred": starred})
    return {"id": item.id, "starred": starred}


def serialize_expiring(item: FridgeItem, *, now: datetime) -> dict:
    return {
        "id": item.id,
        "ingredient": {
            "id": item.ingredient.id,
            "name": item.ingredient.name,
            "category": item.ingredient.category,
            "emoji": item.ingredient.emoji,
        },
        "quantity": serialize_decimal(item.quantity),
        "unit": item.unit,
        "deadline": item.deadline,
        "daysUntilDeadline": (item.deadline.date() - now.date()).days,
        "expired": item.deadline < now,
    }


def list_expiring(db: Session, *, days: int, include_expired: bool) -> list[dict]:
    now = datetime.now()
    items = fridge_item_crud.get_expiring(db, days=days, include_expired=include_expired, now=now)
    return [serialize_expiring(item, now=now) for item in items]


def consume_item(db: Session, item_id: int, quantity: Decimal) -> dict:
    item = require_item(db, item_id)
    if quantity <= 0:
        raise api_error(422, "사용량은 0보다 커야 합니다.", "INVALID_QUANTITY")
    if quantity > item.quantity:
        raise api_error(422, "현재 수량보다 많이 사용할 수 없습니다.", "INSUFFICIENT_QUANTITY")
    remaining = item.quantity - quantity
    fridge_item_crud.update(db, item, {"quantity": remaining})
    return {
        "id": item.id,
        "remainingQuantity": serialize_decimal(remaining),
        "unit": item.unit,
        "depleted": remaining == 0,
    }
