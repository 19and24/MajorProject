from pydantic import BaseModel


class VisionScanItem(BaseModel):
    name: str
    category: str
    emoji: str
    confidence: int
    expiry_days: int


class VisionScanResponse(BaseModel):
    items: list[VisionScanItem]
