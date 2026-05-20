import asyncio
import logging

import requests

from config import WorkerConfig


logger = logging.getLogger("subtitle-worker")


class WhisperClient:
    """Gửi audio chunk sang Whisper và chuẩn hóa text trả về."""

    def __init__(self, config: WorkerConfig):
        """Giữ một HTTP session dùng chung cho nhiều lần gọi Whisper."""
        self.config = config
        self.session = requests.Session()

    async def transcribe_chunk(self, wav_bytes: bytes) -> str:
        """Chạy request Whisper dạng blocking nhưng không khóa event loop."""
        return await asyncio.to_thread(self._transcribe_chunk_blocking, wav_bytes)

    def _transcribe_chunk_blocking(self, wav_bytes: bytes) -> str:
        """Upload một chunk WAV lên Whisper và lấy transcript text."""
        files = {
            "file": ("subtitle-chunk.wav", wav_bytes, "audio/wav"),
        }
        data = {}
        if self.config.language:
            data["language"] = self.config.language

        response = self.session.post(
            self.config.whisper_url,
            files=files,
            data=data,
            timeout=self.config.request_timeout_seconds,
        )
        response.raise_for_status()

        payload = response.json()
        if isinstance(payload, dict):
            return str(payload.get("text", "")).strip()
        return str(payload).strip()

    def close(self) -> None:
        """Đóng HTTP session dùng chung."""
        self.session.close()
