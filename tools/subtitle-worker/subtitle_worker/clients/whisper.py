import asyncio
import io
import os

from faster_whisper import WhisperModel

from subtitle_worker.config import WorkerConfig


DEFAULT_BEAM_SIZE = 3
DEFAULT_CPU_THREADS = max(1, (os.cpu_count() or 4) - 1)


class WhisperClient:
    def __init__(self, config: WorkerConfig):
        self.config = config
        self.model = WhisperModel(
            config.whisper_model_size,
            device=config.whisper_device,
            compute_type=config.whisper_compute_type,
            cpu_threads=DEFAULT_CPU_THREADS,
        )

    async def transcribe_chunk(self, wav_bytes: bytes) -> str:
        return await asyncio.to_thread(self._transcribe_chunk_blocking, wav_bytes)

    def _transcribe_chunk_blocking(self, wav_bytes: bytes) -> str:
        segments, _ = self.model.transcribe(
            io.BytesIO(wav_bytes),
            language=self.config.language,
            beam_size=DEFAULT_BEAM_SIZE,
            condition_on_previous_text=False,
            vad_filter=True,
            temperature=0.0,
        )
        return " ".join(segment.text.strip() for segment in segments if segment.text).strip()

    def close(self) -> None:
        return None
