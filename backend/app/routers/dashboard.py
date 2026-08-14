from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.core.responses import success
from app.dependencies import get_db
from app.services import dashboard as dashboard_service

router = APIRouter(prefix="/dashboard", tags=["dashboard"])


@router.get("")
def get_dashboard(db: Session = Depends(get_db)):
    return success(dashboard_service.get_dashboard(db))
