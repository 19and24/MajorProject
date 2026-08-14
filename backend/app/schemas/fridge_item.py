from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel, ConfigDict, Field


class FridgeItemCreate(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    ingredient_id: int = Field(alias="ingredientId", gt=0)
    quantity: Decimal = Field(gt=0)
    unit: str = Field(min_length=1, max_length=30)
    deadline: datetime | None = None
    starred: bool = False


class FridgeItemUpdate(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    ingredient_id: int | None = Field(default=None, alias="ingredientId", gt=0)
    quantity: Decimal | None = Field(default=None, gt=0)
    unit: str | None = Field(default=None, min_length=1, max_length=30)
    deadline: datetime | None = None
    starred: bool | None = None


class FridgeItemStarUpdate(BaseModel):
    starred: bool


class FridgeItemConsume(BaseModel):
    quantity: Decimal = Field(gt=0)
