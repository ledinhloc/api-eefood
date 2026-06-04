from fastapi import FastAPI

from app.api.routes.vision import router as vision_router
from app.models.schemas import ResponseData


app = FastAPI(
    title="EEFOOD AI Service",
    version="0.1.0",
    description="AI capabilities for image and language workflows.",
)


@app.get("/health", response_model=ResponseData, response_model_exclude_none=True)
def health() -> ResponseData:
    return ResponseData(
        status=200,
        message="Success",
        data={"status": "ok"},
    )


app.include_router(vision_router)
