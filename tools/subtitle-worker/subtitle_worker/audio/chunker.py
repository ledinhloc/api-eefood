import io
import struct
import wave
from typing import Optional

from livekit import rtc


class AudioChunker:
    """Gom audio frame và xuất ra từng chunk WAV có độ dài cố định."""

    def __init__(self, sample_rate: int, num_channels: int, chunk_seconds: int):
        """Thiết lập thời lượng chunk và định dạng audio đầu ra."""
        self.sample_rate = sample_rate
        self.num_channels = num_channels
        self.chunk_samples = sample_rate * chunk_seconds
        self.bytes_per_sample = 2
        self.buffer = bytearray()

    def push(self, frame: rtc.AudioFrame) -> list[bytes]:
        """Thêm một frame và trả về các chunk WAV hoàn chỉnh hiện có."""
        self.buffer.extend(bytes(frame.data))
        chunk_size_bytes = self.chunk_samples * self.num_channels * self.bytes_per_sample

        chunks: list[bytes] = []
        while len(self.buffer) >= chunk_size_bytes:
            raw_chunk = bytes(self.buffer[:chunk_size_bytes])
            del self.buffer[:chunk_size_bytes]
            chunks.append(self._to_wav_bytes(raw_chunk))
        return chunks

    def flush(self) -> Optional[bytes]:
        """Trả về chunk cuối cùng còn dư khi track dừng."""
        if not self.buffer:
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

    @staticmethod
    def is_blank_audio(wav_bytes: bytes, silence_threshold: int = 150) -> bool:
        """Kiểm tra chunk WAV có gần như im lặng hay không để bỏ qua Whisper."""
        with wave.open(io.BytesIO(wav_bytes), "rb") as wav_file:
            frames = wav_file.readframes(wav_file.getnframes())
            sample_width = wav_file.getsampwidth()

        if not frames:
            return True

        if sample_width != 2:
            return False

        total_amplitude = 0
        sample_count = 0
        for (sample,) in struct.iter_unpack("<h", frames):
            total_amplitude += abs(sample)
            sample_count += 1

        if sample_count == 0:
            return True

        average_amplitude = total_amplitude / sample_count
        return average_amplitude < silence_threshold
