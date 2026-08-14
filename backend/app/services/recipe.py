from sqlalchemy.orm import Session

from app.core.responses import api_error, pagination
from app.crud import recipe as recipe_crud
from app.models.recipe import Recipe
from app.models.recipe_ingredient import RecipeIngredient
from app.services import external_search

DEFAULT_IMAGE_URL = "https://example.com/recipes/default.jpg"


def serialize_summary(recipe: Recipe) -> dict:
    return {
        "id": recipe.id,
        "title": recipe.title,
        "method": recipe.method,
        "ingredientActo": recipe.ingredient_acto,
        "type": recipe.type,
        "portion": recipe.portion,
        "cooktime": recipe.cooktime,
    }


def serialize_ingredient(link: RecipeIngredient) -> dict:
    return {
        "ingredient": {
            "id": link.ingredient.id,
            "name": link.ingredient.name,
            "category": link.ingredient.category,
            "emoji": link.ingredient.emoji,
        },
        "amount": link.amount,
        "optional": link.optional,
    }


def serialize_detail(recipe: Recipe) -> dict:
    return {
        "id": recipe.id,
        "title": recipe.title,
        "method": recipe.method,
        "ingredientActo": recipe.ingredient_acto,
        "type": recipe.type,
        "ingredientAll": recipe.ingredient_all,
        "portion": recipe.portion,
        "cooktime": recipe.cooktime,
        "ingredients": [serialize_ingredient(link) for link in recipe.recipe_ingredients],
    }


def require_recipe(db: Session, recipe_id: int) -> Recipe:
    recipe = recipe_crud.get(db, recipe_id)
    if recipe is None:
        raise api_error(404, "레시피를 찾을 수 없습니다.", "RECIPE_NOT_FOUND")
    return recipe


def list_recipes(db: Session, *, keyword: str | None, method: str | None, type_: str | None,
                  ingredient_id: int | None, portion: str | None, cooktime: str | None,
                  page: int, size: int) -> dict:
    items, total = recipe_crud.get_many(
        db, keyword=keyword, method=method, type_=type_, ingredient_id=ingredient_id,
        portion=portion, cooktime=cooktime, page=page, size=size,
    )
    return pagination([serialize_summary(item) for item in items], page, size, total)


def get_recipe(db: Session, recipe_id: int) -> dict:
    recipe = recipe_crud.get_with_ingredients(db, recipe_id)
    if recipe is None:
        raise api_error(404, "레시피를 찾을 수 없습니다.", "RECIPE_NOT_FOUND")
    return serialize_detail(recipe)


def get_recipe_ingredients(db: Session, recipe_id: int) -> list[dict]:
    require_recipe(db, recipe_id)
    links = recipe_crud.get_ingredients(db, recipe_id)
    return [serialize_ingredient(link) for link in links]


def get_external_search(db: Session, recipe_id: int) -> dict:
    recipe = require_recipe(db, recipe_id)
    return {
        "recipeId": recipe.id,
        "title": recipe.title,
        "provider": external_search.PROVIDER,
        "url": external_search.build_url(recipe.title),
    }
