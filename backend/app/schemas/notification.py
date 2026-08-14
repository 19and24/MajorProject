from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class NotificationCreate(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    fridge_item_id: int = Field(alias="fridgeItemId", gt=0)
    title: str = Field(min_length=1, max_length=100)
    content: str | None = Field(default=None)
    notify_time: datetime = Field(alias="notifyTime")


class NotificationUpdate(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    title: str | None = Field(default=None, min_length=1, max_length=100)
    content: str | None = Field(default=None)
    notify_time: datetime | None = Field(default=None, alias="notifyTime")


class DefaultNotificationsCreate(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    days_before: list[int] = Field(alias="daysBefore", min_length=1)
    notify_hour: int = Field(alias="notifyHour", ge=0, le=23)
