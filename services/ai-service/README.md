# AI Service

Python microservice for AI image capabilities in EEFOOD.

## Current Capability

This service detects food ingredients from an uploaded image using `FastAPI`, `OpenCV`, and an `ONNX` object detection model.

The current ingredient model can detect:

- bưởi
- sầu riêng
- táo
- thanh long
- xoài
- cà chua
- cà rốt
- chuối
- dưa hấu
- dưa leo
- trứng gà

## Structure

- `app/main.py`: FastAPI entrypoint
- `app/api/routes/vision.py`: HTTP endpoints
- `app/services/ingredient_detection.py`: model loading and inference
- `app/models/schemas.py`: request/response schemas
- `app/core/config.py`: environment-based settings
- `assets/models/`: ONNX model files
- `assets/classes/`: class label files

## Model Files

The service expects these files by default:

- `assets/models/ingredient.onnx`
- `assets/classes/ingredient_classes.txt`

The class file must contain one label per line, in the same order as the model output classes.

## Run Locally

```bash
cd services/ai-service
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8099
```

PowerShell:

```powershell
cd services/ai-service
.\.venv\Scripts\Activate.ps1
uvicorn app.main:app --reload --host 0.0.0.0 --port 8099
```

## API

### Health

`GET /health`

### Detect Ingredient Data

`POST /api/v1/vision/ingredients/detections`

Body:

- `file`: uploaded image file

Returns JSON detection data, including unique labels, bounding boxes, confidence scores, and original image size.

Response example:

```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "labels": ["táo", "chuối"],
    "detections": [
      {
        "label": "táo",
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
}
```

### Detect Ingredients And Return Annotated Image File

`POST /api/v1/vision/ingredients/detections/annotated-image`

Body:

- `file`: uploaded image file

Returns `image/jpeg` directly. The image contains bounding boxes, ingredient labels, and confidence percentages.

Use this endpoint when the client only needs to display or download the annotated image.

### Detect Ingredients And Return Labels With Annotated Image

`POST /api/v1/vision/ingredients/detections/with-annotated-image`

Body:

- `file`: uploaded image file

Returns unique detected labels and the annotated image encoded as base64.

Use this endpoint when the client only needs class labels and the annotated image in a single request. The base64 image is longer than the original JPEG bytes, so prefer `/annotated-image` if labels are not needed.

Response example:

```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "labels": ["táo", "chuối"],
    "annotatedImageBase64": "/9j/4AAQSkZJRg..."
  }
}
```

## Configuration

Set a Unicode font path if the environment cannot find a font that supports Vietnamese labels:

```env
AI_SERVICE_ANNOTATION_FONT_PATH=/path/to/font.ttf
```

Optional environment variables:

- `AI_SERVICE_MODEL_PATH`: ONNX model path
- `AI_SERVICE_CLASSES_PATH`: class label file path
- `AI_SERVICE_CONFIDENCE_THRESHOLD`: minimum confidence score for detections
- `AI_SERVICE_NMS_THRESHOLD`: non-maximum suppression threshold
- `AI_SERVICE_INPUT_SIZE`: model input image size

## Legacy Endpoints

These endpoints still work for backward compatibility, but the new endpoint names above should be used for new clients:

- `POST /api/v1/vision/ingredients/detect`
- `POST /api/v1/vision/ingredients/detect/annotated`
