from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.core.responses import success
from app.dependencies import get_db
from app.services import ingredient as ingredient_service

router = APIRouter(tags=["ingredients"])


@router.get("/ingredients")
def list_ingredients(
    keyword: str | None = None,
    category: str | None = None,
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=200),
    sort: str = "name,asc",
    db: Session = Depends(get_db),
):
    data = ingredient_service.list_ingredients(
        db, keyword=keyword, category=category, page=page, size=size, sort=sort
    )
    return success(data)


@router.get("/ingredients/{ingredient_id}")
def get_ingredient(ingredient_id: int, db: Session = Depends(get_db)):
    return success(ingredient_service.get_ingredient(db, ingredient_id))


@router.get("/ingredient-categories")
def list_ingredient_categories(db: Session = Depends(get_db)):
    return success(ingredient_service.list_categories(db))
