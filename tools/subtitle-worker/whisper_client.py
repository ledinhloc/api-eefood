import asyncio
import logging
import os
import tempfile

from faster_whisper import WhisperModel

from config import WorkerConfig


logger = logging.getLogger("subtitle-worker")


class WhisperClient:
    """Chạy faster-whisper cục bộ và trả về transcript text."""

    def __init__(self, config: WorkerConfig):
        """Khởi tạo model Whisper một lần để dùng lại cho các chunk tiếp theo."""
        self.config = config
        self.model = WhisperModel(
            config.whisper_model_size,
            device=config.whisper_device,
            compute_type=config.whisper_compute_type,
        )

    async def transcribe_chunk(self, wav_bytes: bytes) -> str:
        """Chạy Whisper dạng blocking nhưng không khóa event loop."""
        return await asyncio.to_thread(self._transcribe_chunk_blocking, wav_bytes)

    def _transcribe_chunk_blocking(self, wav_bytes: bytes) -> str:
        """Chạy model Whisper local cho một chunk WAV và lấy transcript text."""
        temp_file_path = None
        try:
            with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as temp_file:
                temp_file.write(wav_bytes)
                temp_file_path = temp_file.name

            segments, _ = self.model.transcribe(
                temp_file_path,
                language=self.config.language,
            )
            return " ".join(segment.text.strip() for segment in segments if segment.text).strip()
        finally:
            if temp_file_path and os.path.exists(temp_file_path):
                os.remove(temp_file_path)

    def close(self) -> None:
        """Giữ nguyên interface đóng client để app hiện tại không phải sửa thêm."""
