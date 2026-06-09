# AI Service

Python microservice for AI capabilities in EEFOOD.

## Current capability

- Detect ingredients from an uploaded image with `FastAPI + OpenCV + ONNX`

## Structure

- `app/main.py`: FastAPI entrypoint
- `app/api/routes/vision.py`: HTTP endpoints
- `app/services/ingredient_detection.py`: model loading and inference
- `app/models/schemas.py`: request/response schemas
- `app/core/config.py`: environment-based settings
- `assets/models/`: ONNX model files
- `assets/classes/`: class label files

## Expected files

Place your model files here:

- `assets/models/yolov8n_traicay.onnx`
- `assets/classes/fruit_detection_classes_yolo.txt`

## Run locally

```bash
cd services/ai-service
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8099

cd services/ai-service
.\.venv\Scripts\Activate.ps1
uvicorn app.main:app --reload --host 0.0.0.0 --port 8099
```

## API

### Health

`GET /health`

### Detect ingredients

`POST /api/v1/vision/ingredients/detect`

Body:

- `file`: image upload

Response:

```json
{
  "labels": ["apple", "banana"],
  "detections": [
    {
      "label": "apple",
      "confidence": 0.91,
      "box": {
        "x": 120,
        "y": 48,
        "width": 220,
        "height": 210
      }
    }
  ],
  "imageWidth": 1280,
  "imageHeight": 720
}
```

### Detect ingredients and return annotated image

`POST /api/v1/vision/ingredients/detect/annotated`

Body:

- `file`: image upload

Response: `image/jpeg` with bounding boxes, labels, and confidence percentages.

```env
AI_SERVICE_ANNOTATION_FONT_PATH=/path/to/font.ttf
```
