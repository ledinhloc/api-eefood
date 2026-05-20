import asyncio
import logging
import os
import signal
from datetime import datetime

from audio_chunker import AudioChunker
from backend_client import BackendClient
from config import load_config
from livekit_client import LiveKitAudioClient
from whisper_client import WhisperClient


logging.basicConfig(
    level=os.getenv("LOG_LEVEL", "INFO").upper(),
    format="%(asctime)s %(levelname)s %(message)s",
)
logger = logging.getLogger("subtitle-worker")


class SubtitleWorker:
    """Điều phối luồng lấy audio từ LiveKit, gọi Whisper và gửi kết quả về backend."""

    def __init__(self) -> None:
        """Khởi tạo cấu hình và các client cần cho worker demo."""
        self.config = load_config()
        self.whisper_client = WhisperClient(self.config)
        self.backend_client = BackendClient(self.config)
        self.livekit_client = LiveKitAudioClient(self.config, self.handle_chunk)

    async def handle_chunk(self, wav_bytes: bytes) -> None:
        """Transcribe một chunk và gửi lên backend nếu có text."""
        if AudioChunker.is_blank_audio(wav_bytes):
            logger.info("Bo qua blank audio, khong goi Whisper")
            return

        text = (await self.whisper_client.transcribe_chunk(wav_bytes)).strip()
        if not text or text.upper() == "[BLANK_AUDIO]":
            logger.info("Khong nhan dien duoc text tu chunk audio nay")
            return

        logger.info(
            "Text nhan dien duoc - livestream=%s, ngon_ngu=%s, noi_dung=%s",
            self.config.live_stream_id,
            self.config.language or "auto",
            text,
        )
        await self.backend_client.publish_transcript(
            text=text,
            created_at=datetime.now().replace(tzinfo=None),
        )

    async def run(self) -> None:
        """Chạy worker cho tới khi LiveKit ngắt kết nối hoặc nhận tín hiệu dừng."""
        self._register_signal_handlers()
        try:
            await self.livekit_client.run()
        finally:
            await self.shutdown()

    def _register_signal_handlers(self) -> None:
        """Đăng ký SIGINT và SIGTERM để worker dừng gọn gàng."""
        loop = asyncio.get_running_loop()
        for sig in (signal.SIGINT, signal.SIGTERM):
            try:
                loop.add_signal_handler(sig, self.livekit_client.request_stop)
            except NotImplementedError:
                pass

    async def shutdown(self) -> None:
        """Đóng tài nguyên mạng và dừng các task nền."""
        await self.livekit_client.shutdown()
        self.whisper_client.close()
        self.backend_client.close()


async def async_main() -> None:
    """Chạy worker bên trong asyncio event loop."""
    worker = SubtitleWorker()
    await worker.run()


def main() -> None:
    """Cung cấp entrypoint đồng bộ để chạy local."""
    asyncio.run(async_main())


if __name__ == "__main__":
    main()
