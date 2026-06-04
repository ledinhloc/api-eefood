from pydantic import BaseModel, ConfigDict


class BoundingBox(BaseModel):
    x: int
    y: int
    width: int
    height: int


class DetectionItem(BaseModel):
    label: str
    confidence: float
    box: BoundingBox


class IngredientDetectionResponse(BaseModel):
    labels: list[str]
    detections: list[DetectionItem]
    imageWidth: int
    imageHeight: int


class ResponseData(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    status: int
    message: str
    data: IngredientDetectionResponse | dict[str, str] | None = None
