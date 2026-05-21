import os
from dataclasses import dataclass
from typing import Optional

from dotenv import load_dotenv


load_dotenv()


def required_env(name: str) -> str:
    """Đọc biến môi trường bắt buộc và báo lỗi ngay nếu thiếu."""
    value = os.getenv(name)
    if not value:
        raise RuntimeError(f"Missing required environment variable: {name}")
    return value


@dataclass
class WorkerConfig:
    livekit_ws_url: str
    livekit_token: Optional[str]
    livekit_api_key: Optional[str]
    livekit_api_secret: Optional[str]
    room_name: str
    live_stream_id: int
    reaction_base_url: str
    whisper_model_size: str
    whisper_device: str
    whisper_compute_type: str
    bot_identity: str
    bot_name: str
    streamer_identity_prefix: str
    chunk_seconds: int
    sample_rate: int
    num_channels: int
    language: Optional[str]
    request_timeout_seconds: int

    @property
    def transcript_url(self) -> str:
        """Tạo URL backend để gửi transcript text."""
        return f"{self.reaction_base_url.rstrip('/')}/api/v1/livestreams/subtitles/transcripts"


def load_config() -> WorkerConfig:
    """Khởi tạo cấu hình worker từ biến môi trường."""
    return WorkerConfig(
        livekit_ws_url=required_env("LIVEKIT_WS_URL"),
        livekit_token=os.getenv("LIVEKIT_TOKEN"),
        livekit_api_key=os.getenv("LIVEKIT_API_KEY"),
        livekit_api_secret=os.getenv("LIVEKIT_API_SECRET"),
        room_name=required_env("ROOM_NAME"),
        live_stream_id=int(required_env("LIVE_STREAM_ID")),
        reaction_base_url=required_env("REACTION_BASE_URL"),
        whisper_model_size=os.getenv("WHISPER_MODEL_SIZE", "base"),
        whisper_device=os.getenv("WHISPER_DEVICE", "cpu"),
        whisper_compute_type=os.getenv("WHISPER_COMPUTE_TYPE", "int8"),
        bot_identity=os.getenv("BOT_IDENTITY", "subtitle-worker"),
        bot_name=os.getenv("BOT_NAME", "Subtitle Worker"),
        streamer_identity_prefix=os.getenv("STREAMER_IDENTITY_PREFIX", "streamer_"),
        chunk_seconds=int(os.getenv("CHUNK_SECONDS", "2")),
        sample_rate=int(os.getenv("AUDIO_SAMPLE_RATE", "16000")),
        num_channels=int(os.getenv("AUDIO_NUM_CHANNELS", "1")),
        language=os.getenv("SPOKEN_LANGUAGE"),
        request_timeout_seconds=int(os.getenv("REQUEST_TIMEOUT_SECONDS", "30")),
    )
