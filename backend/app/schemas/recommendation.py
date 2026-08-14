from pydantic import BaseModel, ConfigDict, Field, model_validator


class RecommendRecipesRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    selected_fridge_item_ids: list[int] = Field(alias="selectedFridgeItemIds", default_factory=list)
    excluded_fridge_item_ids: list[int] = Field(default_factory=list, alias="excludedFridgeItemIds")
    prioritize_expiring: bool = Field(default=True, alias="prioritizeExpiring")
    limit: int = Field(default=20, ge=1, le=100)

    @model_validator(mode="after")
    def check_no_overlap(self) -> "RecommendRecipesRequest":
        overlap = set(self.selected_fridge_item_ids) & set(self.excluded_fridge_item_ids)
        if overlap:
            raise ValueError("selectedFridgeItemIds와 excludedFridgeItemIds가 겹칠 수 없습니다.")
        return self
