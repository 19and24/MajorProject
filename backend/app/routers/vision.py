from fastapi import APIRouter, File, UploadFile

from app.core.responses import api_error, success
from app.schemas.vision import VisionScanResponse
from app.services import vision_scan as vision_service

router = APIRouter(tags=["vision"])

_MAX_IMAGE_BYTES = 10 * 1024 * 1024
_ALLOWED_MIME_TYPES = {"image/jpeg", "image/png", "image/webp", "image/heic", "image/heif"}


@router.post("/vision/scan")
async def scan_photo(image: UploadFile = File(...)):
    mime_type = image.content_type or "image/jpeg"
    if mime_type not in _ALLOWED_MIME_TYPES:
        raise api_error(415, "지원하지 않는 이미지 형식입니다.", "UNSUPPORTED_IMAGE_TYPE")

    data = await image.read()
    if not data:
        raise api_error(400, "이미지 파일이 비어 있습니다.", "EMPTY_IMAGE")
    if len(data) > _MAX_IMAGE_BYTES:
        raise api_error(413, "이미지 파일이 너무 큽니다. (최대 10MB)", "IMAGE_TOO_LARGE")

    items = await vision_service.scan_image(data, mime_type)
    return success(VisionScanResponse(items=items).model_dump())
