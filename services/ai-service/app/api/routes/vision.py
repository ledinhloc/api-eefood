import logging

from fastapi import APIRouter, Depends, File, UploadFile
from fastapi.responses import JSONResponse, Response

from app.models.schemas import ResponseData
from app.services.ingredient_detection import (
    IngredientDetectionError,
    IngredientDetectionService,
)


router = APIRouter(prefix="/api/v1/vision", tags=["vision"])
logger = logging.getLogger(__name__)


def get_detection_service() -> IngredientDetectionService:
    return IngredientDetectionService()


@router.post("/ingredients/detect", response_model=ResponseData, response_model_exclude_none=True)
async def detect_ingredients(
    file: UploadFile = File(...),
    detection_service: IngredientDetectionService = Depends(get_detection_service),
) -> ResponseData | JSONResponse:
    if not file.content_type or not file.content_type.startswith("image/"):
        return JSONResponse(
            status_code=400,
            content={"status": 400, "message": "Uploaded file must be an image."},
        )

    image_bytes = await file.read()
    if not image_bytes:
        return JSONResponse(
            status_code=400,
            content={"status": 400, "message": "Uploaded image is empty."},
        )

    try:
        result = detection_service.detect(image_bytes)
        return ResponseData(status=200, message="Success", data=result)
    except IngredientDetectionError as exc:
        return JSONResponse(
            status_code=503,
            content={"status": 503, "message": str(exc)},
        )
    except Exception as exc:
        logger.exception("Unexpected error while detecting ingredients.")
        return JSONResponse(
            status_code=500,
            content={"status": 500, "message": "Unexpected detection error."},
        )


@router.post(
    "/ingredients/detect/annotated",
    response_model=None,
    response_class=Response,
    responses={200: {"content": {"image/jpeg": {}}}},
)
async def detect_ingredients_annotated(
    file: UploadFile = File(...),
    detection_service: IngredientDetectionService = Depends(get_detection_service),
) -> Response | JSONResponse:
    if not file.content_type or not file.content_type.startswith("image/"):
        return JSONResponse(
            status_code=400,
            content={"status": 400, "message": "Uploaded file must be an image."},
        )

    image_bytes = await file.read()
    if not image_bytes:
        return JSONResponse(
            status_code=400,
            content={"status": 400, "message": "Uploaded image is empty."},
        )

    try:
        annotated_image = detection_service.detect_annotated_image(image_bytes)
        return Response(
            content=annotated_image,
            media_type="image/jpeg",
            headers={"Content-Disposition": 'inline; filename="ingredient-detection.jpg"'},
        )
    except IngredientDetectionError as exc:
        return JSONResponse(
            status_code=503,
            content={"status": 503, "message": str(exc)},
        )
    except Exception:
        logger.exception("Unexpected error while creating annotated detection image.")
        return JSONResponse(
            status_code=500,
            content={"status": 500, "message": "Unexpected detection error."},
        )
