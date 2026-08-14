from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.core.responses import success
from app.dependencies import get_db
from app.schemas.recommendation import RecommendRecipesRequest
from app.services import recommendation as recommendation_service

router = APIRouter(prefix="/recommendations", tags=["recommendations"])


@router.get("/fridge-items")
def get_selectable_fridge_items(
    starred: bool | None = None,
    category: str | None = None,
    includeExpired: bool = True,
    db: Session = Depends(get_db),
):
    data = recommendation_service.list_selectable_fridge_items(
        db, starred=starred, category=category, include_expired=includeExpired
    )
    return success(data)


@router.post("/recipes")
def recommend_recipes(body: RecommendRecipesRequest, db: Session = Depends(get_db)):
    return success(recommendation_service.recommend_recipes(db, body))


@router.post("/recipes/filter")
def filter_recommended_recipes(body: RecommendRecipesRequest, db: Session = Depends(get_db)):
    return success(recommendation_service.recommend_recipes(db, body))
