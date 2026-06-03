import asyncio
import logging
import os
import signal
from datetime import datetime
from typing import Optional

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

QueueItem = tuple[bytes, datetime]


class SubtitleWorker:
    def __init__(self) -> None:
        self.config = load_config()
        self.whisper_client = WhisperClient(self.config)
        self.backend_client = BackendClient(self.config)
        self.livekit_client = LiveKitAudioClient(self.config, self.handle_chunk)
        # chứa các audio chunk cần xử lý
        self.chunk_queue: asyncio.Queue[Optional[QueueItem]] = asyncio.Queue(maxsize=8)
        self.processor_task: Optional[asyncio.Task] = None

    # Audio chunk được đưa vào queue.
    async def handle_chunk(self, wav_bytes: bytes) -> None:
        await self.chunk_queue.put((wav_bytes, datetime.now().replace(tzinfo=None)))

    # lấy từng chunk từ queue ra xử lý
    async def _process_chunks(self) -> None:
        while True:
            item = await self.chunk_queue.get()
            try:
                if item is None:
                    return
                wav_bytes, created_at = item
                await self._process_chunk(wav_bytes, created_at)
            finally:
                self.chunk_queue.task_done()
    # Xử lý một audio chunk
    async def _process_chunk(self, wav_bytes: bytes, created_at: datetime) -> None:
        if AudioChunker.is_blank_audio(wav_bytes):
            logger.info("Bo qua blank audio, khong goi Whisper")
            return
        # Gọi Whisper để nhận diện giọng nói.
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
        # Nếu có nội dung, log transcript rồi gửi lên backend
        await self.backend_client.publish_transcript(text=text, created_at=created_at)

    async def run(self) -> None:
        """Chạy worker cho tới khi LiveKit ngắt kết nối hoặc nhận tín hiệu dừng."""
        self._register_signal_handlers()
        self.processor_task = asyncio.create_task(self._process_chunks())
        try:
            # 
            await self.livekit_client.run()
        finally:
            await self.shutdown()

    # SIGINT và SIGTERM là tín hiệu từ hệ điều hành gửi tới chương trình để yêu cầu dừng.
    def _register_signal_handlers(self) -> None:
        loop = asyncio.get_running_loop()
        for sig in (signal.SIGINT, signal.SIGTERM):
            try:
                loop.add_signal_handler(sig, self.livekit_client.request_stop)
            except NotImplementedError:
                pass

    async def shutdown(self) -> None:
        await self.livekit_client.shutdown()
        await self.chunk_queue.join()
        if self.processor_task is not None:
            await self.chunk_queue.put(None)
            await self.processor_task
        self.whisper_client.close()
        self.backend_client.close()


async def async_main() -> None:
    worker = SubtitleWorker()
    await worker.run()


def main() -> None:
    asyncio.run(async_main())

if __name__ == "__main__":
    main()
