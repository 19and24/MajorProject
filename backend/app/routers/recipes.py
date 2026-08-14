from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.core.responses import success
from app.dependencies import get_db
from app.services import recipe as recipe_service

router = APIRouter(prefix="/recipes", tags=["recipes"])


@router.get("")
def list_recipes(
    keyword: str | None = None,
    method: str | None = None,
    type: str | None = None,
    ingredientId: int | None = None,
    portion: str | None = None,
    cooktime: str | None = None,
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=200),
    db: Session = Depends(get_db),
):
    data = recipe_service.list_recipes(
        db, keyword=keyword, method=method, type_=type, ingredient_id=ingredientId,
        portion=portion, cooktime=cooktime, page=page, size=size,
    )
    return success(data)


@router.get("/{recipe_id}")
def get_recipe(recipe_id: int, db: Session = Depends(get_db)):
    return success(recipe_service.get_recipe(db, recipe_id))


@router.get("/{recipe_id}/ingredients")
def get_recipe_ingredients(recipe_id: int, db: Session = Depends(get_db)):
    return success(recipe_service.get_recipe_ingredients(db, recipe_id))


@router.get("/{recipe_id}/external-search")
def get_recipe_external_search(recipe_id: int, db: Session = Depends(get_db)):
    return success(recipe_service.get_external_search(db, recipe_id))
