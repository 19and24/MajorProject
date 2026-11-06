from sqlalchemy import asc, case, desc, func, select
from sqlalchemy.orm import Session

from app.models.ingredient import Ingredient


SORT_COLUMNS = {
    "id": Ingredient.id,
    "name": Ingredient.name,
    "category": Ingredient.category,
}


def get(db: Session, ingredient_id: int) -> Ingredient | None:
    return db.get(Ingredient, ingredient_id)


def get_many(
    db: Session,
    *,
    keyword: str | None,
    category: str | None,
    page: int,
    size: int,
    sort_field: str,
    descending: bool,
) -> tuple[list[Ingredient], int]:
    query = select(Ingredient)
    if keyword:
        query = query.where(Ingredient.name.contains(keyword))
    if category:
        query = query.where(Ingredient.category == category)

    direction = desc if descending else asc
    order_columns = [direction(SORT_COLUMNS[sort_field]), Ingredient.id.asc()]
    if keyword:
        # Exact name matches should outrank partial matches regardless of the requested sort.
        order_columns.insert(0, case((Ingredient.name == keyword, 0), else_=1))
    query = query.order_by(*order_columns)
    total = db.scalar(select(func.count()).select_from(query.order_by(None).subquery())) or 0
    items = list(db.scalars(query.offset((page - 1) * size).limit(size)).all())
    return items, total


def get_categories(db: Session) -> list[str]:
    query = (
        select(Ingredient.category)
        .where(Ingredient.category.is_not(None))
        .distinct()
        .order_by(Ingredient.category)
    )
    return list(db.scalars(query).all())
