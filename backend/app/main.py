from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.routers.dashboard import router as dashboard_router
from app.routers.fridge_items import router as fridge_items_router
from app.routers.ingredients import router as ingredients_router
from app.routers.notifications import router as notifications_router
from app.routers.recipes import router as recipes_router
from app.routers.recommendations import router as recommendations_router

app = FastAPI(title="Naengbu API")
app.include_router(ingredients_router, prefix="/api/v1")
app.include_router(fridge_items_router, prefix="/api/v1")
app.include_router(notifications_router, prefix="/api/v1")
app.include_router(recipes_router, prefix="/api/v1")
app.include_router(recommendations_router, prefix="/api/v1")
app.include_router(dashboard_router, prefix="/api/v1")


@app.exception_handler(HTTPException)
def http_error_handler(request: Request, exc: HTTPException):
    if isinstance(exc.detail, dict) and "errorCode" in exc.detail:
        return JSONResponse(status_code=exc.status_code, content=exc.detail)
    return JSONResponse(status_code=exc.status_code, content={
        "success": False, "data": None, "message": str(exc.detail),
        "errorCode": "HTTP_ERROR",
    })


@app.exception_handler(RequestValidationError)
def validation_error_handler(request: Request, exc: RequestValidationError):
    return JSONResponse(status_code=422, content={
        "success": False, "data": None,
        "message": "\uc785\ub825\uac12\uc774 \uc62c\ubc14\ub974\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4.",
        "errorCode": "VALIDATION_ERROR",
    })


@app.get("/")
def root():
    return {"message": "Hello Fridge"}
