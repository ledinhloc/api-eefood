import os
from dataclasses import dataclass, replace
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
    chunk_seconds: float
    chunk_overlap_seconds: float
    sample_rate: int
    num_channels: int
    spoken_language: Optional[str]
    target_language: Optional[str]
    request_timeout_seconds: int
    worker_control_host: str
    worker_control_port: int

    @property
    def transcript_url(self) -> str:
        """Tạo URL backend để gửi transcript text."""
        return f"{self.reaction_base_url.rstrip('/')}/api/v1/livestreams/subtitles/transcripts"

    @property
    def active_livestreams_url(self) -> str:
        """Tao URL backend de lay danh sach livestream dang LIVE."""
        return f"{self.reaction_base_url.rstrip('/')}/api/v1/livestreams/active"

    def with_livestream(
        self,
        *,
        room_name: str,
        live_stream_id: int,
        spoken_language: Optional[str],
        target_language: Optional[str],
    ) -> "WorkerConfig":
        # Tao config rieng cho tung live tu payload /start.
        resolved_spoken_language = spoken_language or self.spoken_language
        resolved_target_language = target_language or resolved_spoken_language
        return replace(
            self,
            room_name=room_name,
            live_stream_id=live_stream_id,
            spoken_language=resolved_spoken_language,
            target_language=resolved_target_language,
            bot_identity=(
                f"{self.bot_identity}-{live_stream_id}-{resolved_target_language}"
                if resolved_target_language
                else f"{self.bot_identity}-{live_stream_id}"
            ),
        )


def load_config() -> WorkerConfig:
    """Khởi tạo cấu hình worker từ biến môi trường."""
    return WorkerConfig(
        livekit_ws_url=required_env("LIVEKIT_WS_URL"),
        livekit_token=os.getenv("LIVEKIT_TOKEN"),
        livekit_api_key=os.getenv("LIVEKIT_API_KEY"),
        livekit_api_secret=os.getenv("LIVEKIT_API_SECRET"),
        # Hai gia tri nay duoc backend truyen qua /start.
        room_name="",
        live_stream_id=0,
        reaction_base_url=required_env("REACTION_BASE_URL"),
        whisper_model_size=os.getenv("WHISPER_MODEL_SIZE", "base"),
        whisper_device=os.getenv("WHISPER_DEVICE", "cpu"),
        whisper_compute_type=os.getenv("WHISPER_COMPUTE_TYPE", "int8"),
        bot_identity=os.getenv("BOT_IDENTITY", "subtitle-worker"),
        bot_name=os.getenv("BOT_NAME", "Subtitle Worker"),
        streamer_identity_prefix=os.getenv("STREAMER_IDENTITY_PREFIX", "streamer_"),
        chunk_seconds=float(os.getenv("CHUNK_SECONDS", "1.5")),
        chunk_overlap_seconds=float(os.getenv("CHUNK_OVERLAP_SECONDS", "0.25")),
        sample_rate=int(os.getenv("AUDIO_SAMPLE_RATE", "16000")),
        num_channels=int(os.getenv("AUDIO_NUM_CHANNELS", "1")),
        spoken_language=os.getenv("SPOKEN_LANGUAGE"),
        target_language=None,
        request_timeout_seconds=int(os.getenv("REQUEST_TIMEOUT_SECONDS", "30")),
        worker_control_host=os.getenv("WORKER_CONTROL_HOST", "127.0.0.1"),
        worker_control_port=int(os.getenv("WORKER_CONTROL_PORT", "9000")),
    )
