from sqlalchemy.orm import Session

from app.core.category_emojis import CATEGORY_EMOJIS, DEFAULT_EMOJI
from app.core.responses import api_error, pagination
from app.crud import ingredient as ingredient_crud
from app.models.ingredient import Ingredient


def serialize(item: Ingredient) -> dict:
    return {"id": item.id, "name": item.name, "category": item.category, "emoji": item.emoji}


def parse_sort(sort: str) -> tuple[str, bool]:
    field, separator, direction = sort.partition(",")
    if not separator or field not in ingredient_crud.SORT_COLUMNS or direction not in {"asc", "desc"}:
        raise api_error(400, "\uc815\ub82c \uc870\uac74\uc774 \uc62c\ubc14\ub974\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4.", "INVALID_SORT")
    return field, direction == "desc"


def get_ingredient(db: Session, ingredient_id: int) -> dict:
    item = ingredient_crud.get(db, ingredient_id)
    if item is None:
        raise api_error(404, "\ud45c\uc900 \uc2dd\uc7ac\ub8cc\ub97c \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.", "INGREDIENT_NOT_FOUND")
    return serialize(item)


def list_ingredients(db: Session, *, keyword: str | None, category: str | None,
                     page: int, size: int, sort: str) -> dict:
    sort_field, descending = parse_sort(sort)
    items, total = ingredient_crud.get_many(
        db, keyword=keyword, category=category, page=page, size=size,
        sort_field=sort_field, descending=descending,
    )
    return pagination([serialize(item) for item in items], page, size, total)


def list_categories(db: Session) -> list[dict]:
    return [
        {"name": name, "emoji": CATEGORY_EMOJIS.get(name, DEFAULT_EMOJI)}
        for name in ingredient_crud.get_categories(db)
    ]
