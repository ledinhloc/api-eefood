from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


BASE_DIR = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="AI_SERVICE_", extra="ignore")

    service_name: str = "ai-service"
    host: str = "0.0.0.0"
    port: int = 8099
    model_path: Path = BASE_DIR / "assets" / "models" / "yolov8n_traicay.onnx"
    classes_path: Path = BASE_DIR / "assets" / "classes" / "fruit_detection_classes_yolo.txt"
    input_size: int = 640
    confidence_threshold: float = 0.5
    nms_threshold: float = 0.4
    scale: float = 0.00392


settings = Settings()
