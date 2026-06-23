import asyncio
from datetime import datetime
from typing import Any

import requests

from subtitle_worker.config import WorkerConfig


class BackendClient:
    """Gửi transcript text về backend Java để broadcast qua WebSocket."""

    def __init__(self, config: WorkerConfig):
        """Giữ một HTTP session dùng chung cho các API backend."""
        self.config = config
        self.session = requests.Session()

    async def publish_transcript(self, text: str, created_at: datetime) -> None:
        """Gửi một transcript lên backend mà không chặn event loop."""
        await asyncio.to_thread(self._publish_transcript_blocking, text, created_at)

    async def get_active_livestreams(self) -> list[dict[str, Any]]:
        """Lay danh sach livestream dang LIVE tu reaction-service."""
        return await asyncio.to_thread(self._get_active_livestreams_blocking)

    def _get_active_livestreams_blocking(self) -> list[dict[str, Any]]:
        """GET /api/v1/livestreams/active tu backend."""
        response = self.session.get(
            self.config.active_livestreams_url,
            timeout=self.config.request_timeout_seconds,
        )
        response.raise_for_status()

        body = response.json()
        data = body.get("data", body) if isinstance(body, dict) else body
        if data is None:
            return []
        if not isinstance(data, list):
            raise ValueError("Invalid active livestreams response")
        return [item for item in data if isinstance(item, dict)]

    def _publish_transcript_blocking(self, text: str, created_at: datetime) -> None:
        """POST payload transcript lên API backend."""
        payload = {
            "liveStreamId": self.config.live_stream_id,
            "spokenLanguage": self.config.spoken_language,
            "targetLanguage": self.config.target_language,
            "text": text,
            "createdAt": created_at.replace(microsecond=0).isoformat(),
        }
        response = self.session.post(
            self.config.transcript_url,
            json=payload,
            timeout=self.config.request_timeout_seconds,
        )
        response.raise_for_status()

    def close(self) -> None:
        """Đóng HTTP session dùng chung."""
        self.session.close()
