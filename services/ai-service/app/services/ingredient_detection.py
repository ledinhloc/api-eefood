from __future__ import annotations

from collections import OrderedDict
from io import BytesIO
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

from app.core.config import settings
from app.models.schemas import BoundingBox, DetectionItem, IngredientDetectionResponse


class IngredientDetectionError(RuntimeError):
    """Raised when the detection pipeline cannot run."""


class IngredientDetectionService:
    # Cache model, classes va output names de khong phai load lai moi request.
    _net = None
    _classes: list[str] | None = None
    _out_names: list[str] | None = None
    _annotation_font = None

    def __init__(self) -> None:
        # Lay cau hinh model va threshold tu file config.
        self.model_path: Path = settings.model_path
        self.classes_path: Path = settings.classes_path
        self.input_size = settings.input_size
        self.confidence_threshold = settings.confidence_threshold
        self.nms_threshold = settings.nms_threshold
        self.scale = settings.scale
        self.mean = [0, 0, 0]
        self.background_label_id = -1
        self.backend = 0
        self.target = 0
        self.postprocessing = "yolov8"

    def detect(self, image_bytes: bytes) -> IngredientDetectionResponse:
        # Nhan bytes anh tu API va chuyen sang dinh dang OpenCV BGR.
        frame = self._read_image(image_bytes)
        frame_height, frame_width = frame.shape[:2]

        # Load model va danh sach ten class.
        net = self._get_net()
        classes = self._get_classes()
        out_names = self._get_out_names()

        # Tao blob dau vao dung theo tham so cua model YOLOv8 ONNX hien tai.
        blob = cv2.dnn.blobFromImage(
            frame,
            size=(self.input_size, self.input_size),
            swapRB=True,
            ddepth=cv2.CV_8U,
            crop=False,
        )
        net.setInput(blob, scalefactor=self.scale, mean=self.mean)

        first_layer = net.getLayer(0)
        if hasattr(first_layer, "outputNameToIndex") and first_layer.outputNameToIndex("im_info") != -1:
            net.setInput(
                np.array([[self.input_size, self.input_size, 1.6]], dtype=np.float32),
                "im_info",
            )

        # Chay forward de lay output tu model.
        outputs = net.forward(out_names)

        # Giai ma output, loc confidence va ap dung NMS.
        detections = self._postprocess(
            outputs=outputs,
            frame_width=frame_width,
            frame_height=frame_height,
            classes=classes,
            out_names=out_names,
            net=net,
        )

        # Rut gon danh sach nhan duy nhat va tra response cuoi cung.
        labels = list(OrderedDict.fromkeys(item.label for item in detections))
        return IngredientDetectionResponse(
            labels=labels,
            detections=detections,
            imageWidth=frame_width,
            imageHeight=frame_height,
        )

    def detect_annotated_image(self, image_bytes: bytes) -> bytes:
        # Doc anh goc va chay detection de lay nhan, confidence va bounding box.
        frame = self._read_image(image_bytes)
        result = self.detect(image_bytes)

        # Chuyen sang Pillow de ve duoc nhan Unicode tieng Viet.
        annotated_image = Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))
        draw = ImageDraw.Draw(annotated_image)
        font = self._get_annotation_font()

        for detection in result.detections:
            box = detection.box

            # Gioi han toa do trong kich thuoc anh, tranh ve box ra ngoai frame.
            x1 = max(0, box.x)
            y1 = max(0, box.y)
            x2 = min(result.imageWidth - 1, box.x + box.width)
            y2 = min(result.imageHeight - 1, box.y + box.height)
            if x2 <= x1 or y2 <= y1:
                continue

            color = self._label_color(detection.label)
            label = f"{detection.label} {detection.confidence:.1%}"

            # Tinh kich thuoc label de tao nen mau vua du cho chu.
            text_box = draw.textbbox((0, 0), label, font=font)
            text_width = text_box[2] - text_box[0]
            text_height = text_box[3] - text_box[1]
            label_top = max(0, y1 - text_height - 10)
            label_right = min(result.imageWidth - 1, x1 + text_width + 10)

            # Ve bounding box, nen label va ten nhan kem phan tram tin cay.
            draw.rectangle((x1, y1, x2, y2), outline=color, width=3)
            draw.rectangle((x1, label_top, label_right, y1), fill=color)
            draw.text(
                (x1 + 5, label_top + 3),
                label,
                font=font,
                fill=(255, 255, 255),
            )

        # Chuyen anh ve OpenCV BGR va encode thanh JPEG de tra qua HTTP.
        frame = cv2.cvtColor(np.array(annotated_image), cv2.COLOR_RGB2BGR)
        encoded, buffer = cv2.imencode(".jpg", frame, [cv2.IMWRITE_JPEG_QUALITY, 92])
        if not encoded:
            raise IngredientDetectionError("Cannot encode annotated image.")
        return buffer.tobytes()

# Hàm này tạo màu cho bounding box dựa trên tên nhãn
    @staticmethod
    def _label_color(label: str) -> tuple[int, int, int]:
        seed = sum((index + 1) * ord(char) for index, char in enumerate(label))
        return (
            64 + seed % 160,
            64 + (seed // 7) % 160,
            64 + (seed // 13) % 160,
        )

# tìm và load font Unicode để vẽ được tiếng Việt 
    @classmethod
    def _get_annotation_font(cls):
        if cls._annotation_font is not None:
            return cls._annotation_font

        candidates = [
            settings.annotation_font_path,
            Path("C:/Windows/Fonts/arial.ttf"),
            Path("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
            Path("/usr/share/fonts/dejavu/DejaVuSans.ttf"),
        ]
        for font_path in candidates:
            if font_path is not None and font_path.exists():
                cls._annotation_font = ImageFont.truetype(str(font_path), size=20)
                return cls._annotation_font

        raise IngredientDetectionError(
            "Unicode font not found. Set AI_SERVICE_ANNOTATION_FONT_PATH."
        )

    def _read_image(self, image_bytes: bytes) -> np.ndarray:
        try:
            pil_image = Image.open(BytesIO(image_bytes))
        except Exception as exc:
            raise IngredientDetectionError("Cannot decode uploaded image.") from exc

        # PIL doc anh de dang hon, sau do doi sang numpy/BGR de OpenCV xu ly.
        rgb_image = pil_image.convert("RGB")
        image_array = np.array(rgb_image)
        return cv2.cvtColor(image_array, cv2.COLOR_RGB2BGR)

    @classmethod
    def _get_net(cls):
        if cls._net is None:
            if not settings.model_path.exists():
                raise IngredientDetectionError(
                    f"Model file not found: {settings.model_path}"
                )
            # Load model ONNX mot lan va giu lai trong bo nho de tai su dung.
            cls._net = cv2.dnn.readNet(str(settings.model_path))
            cls._net.setPreferableBackend(0)
            cls._net.setPreferableTarget(0)
        return cls._net

    @classmethod
    def _get_classes(cls) -> list[str]:
        if cls._classes is None:
            if not settings.classes_path.exists():
                raise IngredientDetectionError(
                    f"Classes file not found: {settings.classes_path}"
                )
            # Moi dong trong file txt tuong ung voi mot nhan class cua model.
            cls._classes = [
                line.strip()
                for line in settings.classes_path.read_text(encoding="utf-8").splitlines()
                if line.strip()
            ]
        return cls._classes

    @classmethod
    def _get_out_names(cls) -> list[str]:
        if cls._out_names is None:
            # Lay ten cac output layer de dung cho forward(...).
            net = cls._get_net()
            cls._out_names = list(net.getUnconnectedOutLayersNames())
        return cls._out_names

    def _postprocess(
        self,
        outputs: list[np.ndarray] | tuple[np.ndarray, ...] | np.ndarray,
        frame_width: int,
        frame_height: int,
        classes: list[str],
        out_names: list[str],
        net,
    ) -> list[DetectionItem]:
        # Postprocess dua tren logic Streamlit goc: giai ma box, score va class.
        layer_names = net.getLayerNames()
        last_layer_id = net.getLayerId(layer_names[-1])
        last_layer = net.getLayer(last_layer_id)

        class_ids: list[int] = []
        confidences: list[float] = []
        boxes: list[list[int]] = []

        # Chuan hoa output ve dang de lap qua tung tensor.
        processed_outputs = outputs if isinstance(outputs, (list, tuple)) else [outputs]

        # Tinh ti le scale box theo kich thuoc anh goc.
        if last_layer.type == "Region":
            box_scale_w = frame_width
            box_scale_h = frame_height
        else:
            box_scale_w = frame_width / float(self.input_size)
            box_scale_h = frame_height / float(self.input_size)

        # Duyet tung output va tung detection de lay class, score va bounding box.
        if last_layer.type == "Region" or self.postprocessing == "yolov8":
            for out in processed_outputs:
                current = out
                if current is None:
                    continue

                # YOLOv8 ONNX thuong can transpose ve [num_boxes, features].
                if current.ndim == 3:
                    current = current[0].transpose(1, 0)
                elif current.ndim == 2 and current.shape[0] < current.shape[1]:
                    current = current.transpose(1, 0)

                for detection in current:
                    if detection.shape[0] < 5:
                        continue

                    # Lay vector score, chon class co score cao nhat va loc theo threshold.
                    scores = detection[4:]
                    if self.background_label_id >= 0:
                        scores = np.delete(scores, self.background_label_id)

                    class_id = int(np.argmax(scores))
                    confidence = float(scores[class_id])
                    if confidence <= self.confidence_threshold:
                        continue

                    # Chuyen toa do center-width-height thanh left-top-width-height.
                    center_x = int(detection[0] * box_scale_w)
                    center_y = int(detection[1] * box_scale_h)
                    width = int(detection[2] * box_scale_w)
                    height = int(detection[3] * box_scale_h)
                    left = int(center_x - width / 2)
                    top = int(center_y - height / 2)

                    class_ids.append(class_id)
                    confidences.append(confidence)
                    boxes.append([left, top, width, height])
        else:
            raise IngredientDetectionError(f"Unknown output layer type: {last_layer.type}")

        selected_indices = self._apply_nms_per_class(
            class_ids=class_ids,
            confidences=confidences,
            boxes=boxes,
            out_names=out_names,
            last_layer_type=last_layer.type,
        )

        # Chuyen ket qua da loc thanh schema response.
        detections: list[DetectionItem] = []
        for index in selected_indices:
            left, top, width, height = boxes[index]
            class_id = class_ids[index]
            label = classes[class_id] if class_id < len(classes) else f"class_{class_id}"
            detections.append(
                DetectionItem(
                    label=label,
                    confidence=round(confidences[index], 4),
                    box=BoundingBox(x=left, y=top, width=width, height=height),
                )
            )

        return detections

    def _apply_nms_per_class(
        self,
        class_ids: list[int],
        confidences: list[float],
        boxes: list[list[int]],
        out_names: list[str],
        last_layer_type: str,
    ) -> list[int]:
        if not class_ids:
            return []

        # Giu hanh vi NMS gan voi logic goc trong file Streamlit.
        need_nms = len(out_names) > 1 or (
            (last_layer_type == "Region" or self.postprocessing == "yolov8")
            and self.backend != cv2.dnn.DNN_BACKEND_OPENCV
        )
        if not need_nms:
            return list(range(len(class_ids)))

        # NMS theo tung class de giam box trung lap nhung van giu nhan hop le.
        class_ids_array = np.array(class_ids)
        boxes_array = np.array(boxes)
        confidences_array = np.array(confidences)

        indices: list[int] = []
        for class_id in set(class_ids_array.tolist()):
            # Tach box theo tung class roi moi chay NMS.
            class_indices = np.where(class_ids_array == class_id)[0]
            class_confidences = confidences_array[class_indices]
            class_boxes = boxes_array[class_indices].tolist()
            nms_indices = cv2.dnn.NMSBoxes(
                class_boxes,
                class_confidences.tolist(),
                self.confidence_threshold,
                self.nms_threshold,
            )
            if nms_indices is None or len(nms_indices) == 0:
                continue

            # Map chi so sau NMS ve chi so goc cua danh sach detection.
            flattened = np.array(nms_indices).reshape(-1)
            for idx in flattened:
                indices.append(int(class_indices[int(idx)]))

        return indices
