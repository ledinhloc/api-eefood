import asyncio
import logging
import time
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
        # queue chờ xử lý
        self.chunk_queue: asyncio.Queue[Optional[QueueItem]] = asyncio.Queue(
            maxsize=self.config.chunk_queue_size
        )
        self.processor_task: Optional[asyncio.Task] = None

    # Audio chunk duoc dua vao queue.
    async def handle_chunk(self, wav_bytes: bytes) -> None:
        item = (wav_bytes, datetime.now().replace(tzinfo=None))
        if self.chunk_queue.full():
            try:
                self.chunk_queue.get_nowait()
                self.chunk_queue.task_done()
                logger.warning(
                    "Dropped queued audio chunk to keep subtitle current - livestream=%s",
                    self.config.live_stream_id,
                )
            except asyncio.QueueEmpty:
                pass
        self.chunk_queue.put_nowait(item)

    # Lay tung chunk tu queue ra xu ly.
    async def _process_chunks(self) -> None:
        while True:
            item = await self.chunk_queue.get()
            try:
                if item is None:
                    return
                wav_bytes, created_at = item
                if self._is_stale(created_at):
                    logger.warning(
                        "Skipped stale audio chunk before transcription - livestream=%s age=%.2fs",
                        self.config.live_stream_id,
                        self._age_seconds(created_at),
                    )
                    continue
                try:
                    await self._process_chunk(wav_bytes, created_at)
                except Exception:
                    logger.exception(
                        "Failed to process audio chunk - livestream=%s",
                        self.config.live_stream_id,
                    )
            finally:
                self.chunk_queue.task_done()

    # Xu ly mot audio chunk.
    async def _process_chunk(self, wav_bytes: bytes, created_at: datetime) -> None:
        # logger.info(
        #     "Dang gui audio sang Whisper - livestream=%s, ngon_ngu=%s",
        #     self.config.live_stream_id,
        #     self.config.spoken_language or "auto",
        # )
        # Goi Whisper de nhan dien giong noi.
        started_at = time.perf_counter()
        text = (await self.whisper_client.transcribe_chunk(wav_bytes)).strip()
        processing_seconds = time.perf_counter() - started_at
        if self._is_stale(created_at):
            logger.warning(
                "Dropped late Whisper result - livestream=%s processing=%.2fs age=%.2fs",
                self.config.live_stream_id,
                processing_seconds,
                self._age_seconds(created_at),
            )
            return
        if not text or text.upper() == "[BLANK_AUDIO]":
            logger.info("Khong nhan dien duoc text tu chunk audio nay")
            return

        logger.info(
            "Whisper result - mode=%s livestream=%s spoken=%s target=%s processing=%.2fs text=%s",
            (
                "SUBTITLE"
                if self.config.spoken_language == self.config.target_language
                else "TRANSLATE"
            ),
            self.config.live_stream_id,
            self.config.spoken_language or "auto",
            self.config.target_language or self.config.spoken_language or "auto",
            processing_seconds,
            text,
        )
        # Neu co noi dung, log transcript roi gui len backend.
        await self.backend_client.publish_transcript(text=text, created_at=created_at)

    def _age_seconds(self, created_at: datetime) -> float:
        return max(
            0.0,
            (datetime.now().replace(tzinfo=None) - created_at).total_seconds(),
        )

    def _is_stale(self, created_at: datetime) -> bool:
        max_latency = self.config.max_transcript_latency_seconds
        return max_latency > 0 and self._age_seconds(created_at) > max_latency

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
