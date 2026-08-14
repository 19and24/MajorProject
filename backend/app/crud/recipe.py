from sqlalchemy import func, select
from sqlalchemy.orm import Session, joinedload

from app.models.recipe import Recipe
from app.models.recipe_ingredient import RecipeIngredient


def get(db: Session, recipe_id: int) -> Recipe | None:
    return db.get(Recipe, recipe_id)


def get_with_ingredients(db: Session, recipe_id: int) -> Recipe | None:
    return db.scalar(
        select(Recipe)
        .options(
            joinedload(Recipe.recipe_ingredients).joinedload(RecipeIngredient.ingredient)
        )
        .where(Recipe.id == recipe_id)
    )


def get_many(
    db: Session,
    *,
    keyword: str | None,
    method: str | None,
    type_: str | None,
    ingredient_id: int | None,
    portion: str | None,
    cooktime: str | None,
    page: int,
    size: int,
) -> tuple[list[Recipe], int]:
    query = select(Recipe)
    if keyword:
        query = query.where(Recipe.title.contains(keyword))
    if method:
        query = query.where(Recipe.method == method)
    if type_:
        query = query.where(Recipe.type == type_)
    if portion:
        query = query.where(Recipe.portion == portion)
    if cooktime:
        query = query.where(Recipe.cooktime == cooktime)
    if ingredient_id is not None:
        query = query.join(RecipeIngredient, RecipeIngredient.recipe_id == Recipe.id).where(
            RecipeIngredient.ingredient_id == ingredient_id
        )

    query = query.order_by(Recipe.id.asc())
    total = db.scalar(select(func.count()).select_from(query.order_by(None).subquery())) or 0
    items = list(db.scalars(query.offset((page - 1) * size).limit(size)).unique().all())
    return items, total


def get_ingredients(db: Session, recipe_id: int) -> list[RecipeIngredient]:
    query = (
        select(RecipeIngredient)
        .options(joinedload(RecipeIngredient.ingredient))
        .where(RecipeIngredient.recipe_id == recipe_id)
    )
    return list(db.scalars(query).unique().all())
