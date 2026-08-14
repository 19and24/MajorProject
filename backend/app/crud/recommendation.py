from sqlalchemy import func, select
from sqlalchemy.orm import Session, joinedload

from app.models.fridge_item import FridgeItem
from app.models.ingredient import Ingredient
from app.models.recipe import Recipe
from app.models.recipe_ingredient import RecipeIngredient


def get_fridge_items(
    db: Session,
    *,
    starred: bool | None,
    category: str | None,
    include_expired: bool,
) -> list[FridgeItem]:
    query = (
        select(FridgeItem)
        .join(FridgeItem.ingredient)
        .options(joinedload(FridgeItem.ingredient))
    )
    if starred is not None:
        query = query.where(FridgeItem.starred == starred)
    if category:
        query = query.where(Ingredient.category == category)
    query = query.order_by(FridgeItem.deadline.asc().nulls_last(), FridgeItem.id.asc())
    items = list(db.scalars(query).unique().all())
    if not include_expired:
        return items
    return items


def get_fridge_items_by_ids(db: Session, ids: list[int]) -> list[FridgeItem]:
    if not ids:
        return []
    query = (
        select(FridgeItem)
        .options(joinedload(FridgeItem.ingredient))
        .where(FridgeItem.id.in_(ids))
    )
    return list(db.scalars(query).unique().all())


def get_candidate_recipes(db: Session, ingredient_ids: list[int], *, limit: int) -> list[Recipe]:
    """Recipes that use at least one of the given ingredients, ranked by how many they use."""
    if not ingredient_ids:
        return []
    match_count = func.count(RecipeIngredient.ingredient_id).label("match_count")
    query = (
        select(Recipe, match_count)
        .join(RecipeIngredient, RecipeIngredient.recipe_id == Recipe.id)
        .where(RecipeIngredient.ingredient_id.in_(ingredient_ids))
        .group_by(Recipe.id)
        .order_by(match_count.desc(), Recipe.id.asc())
        .limit(limit)
    )
    return [row[0] for row in db.execute(query).all()]


def get_ingredients_for_recipes(db: Session, recipe_ids: list[int]) -> dict[int, list[RecipeIngredient]]:
    if not recipe_ids:
        return {}
    query = (
        select(RecipeIngredient)
        .options(joinedload(RecipeIngredient.ingredient))
        .where(RecipeIngredient.recipe_id.in_(recipe_ids))
    )
    result: dict[int, list[RecipeIngredient]] = {}
    for link in db.scalars(query).unique().all():
        result.setdefault(link.recipe_id, []).append(link)
    return result
