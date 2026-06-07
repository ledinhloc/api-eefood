import asyncio
import logging
from datetime import datetime
from typing import Optional

from subtitle_worker.clients.backend import BackendClient
from subtitle_worker.clients.livekit import LiveKitAudioClient
from subtitle_worker.clients.whisper import WhisperClient
from subtitle_worker.config import WorkerConfig


logger = logging.getLogger("subtitle-worker")

QueueItem = tuple[bytes, datetime]


class SubtitleWorker:
    def __init__(self, config: WorkerConfig) -> None:
        self.config = config
        self.whisper_client = WhisperClient(self.config)
        self.backend_client = BackendClient(self.config)
        self.livekit_client = LiveKitAudioClient(self.config, self.handle_chunk)
        # Chua cac audio chunk can xu ly.
        self.chunk_queue: asyncio.Queue[Optional[QueueItem]] = asyncio.Queue(maxsize=8)
        self.processor_task: Optional[asyncio.Task] = None

    # Audio chunk duoc dua vao queue.
    async def handle_chunk(self, wav_bytes: bytes) -> None:
        await self.chunk_queue.put((wav_bytes, datetime.now().replace(tzinfo=None)))

    # Lay tung chunk tu queue ra xu ly.
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

    # Xu ly mot audio chunk.
    async def _process_chunk(self, wav_bytes: bytes, created_at: datetime) -> None:
        # logger.info(
        #     "Dang gui audio sang Whisper - livestream=%s, ngon_ngu=%s",
        #     self.config.live_stream_id,
        #     self.config.language or "auto",
        # )
        # Goi Whisper de nhan dien giong noi.
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
        # Neu co noi dung, log transcript roi gui len backend.
        await self.backend_client.publish_transcript(text=text, created_at=created_at)

    async def run(self) -> None:
        # Chay worker cho toi khi LiveKit ngat ket noi hoac nhan lenh dung.
        self.processor_task = asyncio.create_task(self._process_chunks())
        try:
            await self.livekit_client.run()
        finally:
            await self.shutdown()

    async def shutdown(self) -> None:
        # Tat LiveKit, xu ly het queue con lai, roi dong cac client.
        await self.livekit_client.shutdown()
        await self.chunk_queue.join()
        if self.processor_task is not None:
            await self.chunk_queue.put(None)
            await self.processor_task
        self.whisper_client.close()
        self.backend_client.close()
