import io
import struct
import wave
from typing import Optional

from livekit import rtc


class AudioChunker:
    """Gom audio frame và xuất ra từng chunk WAV có độ dài cố định."""

    def __init__(
        self,
        sample_rate: int,
        num_channels: int,
        chunk_seconds: float,
        overlap_seconds: float = 0,
    ):
        """Thiết lập thời lượng chunk và định dạng audio đầu ra."""
        if chunk_seconds <= 0:
            raise ValueError("chunk_seconds must be greater than 0")
        if overlap_seconds < 0 or overlap_seconds >= chunk_seconds:
            raise ValueError("overlap_seconds must be between 0 and chunk_seconds")

        self.sample_rate = sample_rate
        self.num_channels = num_channels
        self.chunk_samples = int(sample_rate * chunk_seconds)
        self.overlap_samples = int(sample_rate * overlap_seconds)
        self.bytes_per_sample = 2
        self.buffer = bytearray()

    def push(self, frame: rtc.AudioFrame) -> list[bytes]:
        """Thêm một frame và trả về các chunk WAV hoàn chỉnh hiện có."""
        self.buffer.extend(bytes(frame.data))
        chunk_size_bytes = self.chunk_samples * self.num_channels * self.bytes_per_sample
        step_size_bytes = (
            self.chunk_samples - self.overlap_samples
        ) * self.num_channels * self.bytes_per_sample

        chunks: list[bytes] = []
        while len(self.buffer) >= chunk_size_bytes:
            raw_chunk = bytes(self.buffer[:chunk_size_bytes])
            # Giu lai phan overlap o cuoi chunk cho lan nhan dien tiep theo.
            del self.buffer[:step_size_bytes]
            chunks.append(self._to_wav_bytes(raw_chunk))
        return chunks

    def flush(self) -> Optional[bytes]:
        """Trả về chunk cuối cùng còn dư khi track dừng."""
        overlap_size_bytes = (
            self.overlap_samples * self.num_channels * self.bytes_per_sample
        )
        if not self.buffer or len(self.buffer) <= overlap_size_bytes:
            self.buffer.clear()
            return None
        raw_chunk = bytes(self.buffer)
        self.buffer.clear()
        return self._to_wav_bytes(raw_chunk)

    def _to_wav_bytes(self, pcm_bytes: bytes) -> bytes:
        """Bọc PCM thô vào container WAV để gửi sang Whisper."""
        output = io.BytesIO()
        with wave.open(output, "wb") as wav_file:
            wav_file.setnchannels(self.num_channels)
            wav_file.setsampwidth(self.bytes_per_sample)
            wav_file.setframerate(self.sample_rate)
            wav_file.writeframes(pcm_bytes)
        return output.getvalue()

