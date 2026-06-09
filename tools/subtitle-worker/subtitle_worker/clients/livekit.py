import asyncio
import logging
from datetime import datetime, timedelta, timezone
from typing import Awaitable, Callable

import jwt
from livekit import rtc

from subtitle_worker.audio.chunker import AudioChunker
from subtitle_worker.config import WorkerConfig


logger = logging.getLogger("subtitle-worker")


class LiveKitAudioClient:
    """Kết nối LiveKit, bắt audio của streamer và phát ra các chunk WAV."""

    def __init__(self, config: WorkerConfig, on_chunk: Callable[[bytes], Awaitable[None]]):
        """Lưu cấu hình worker và callback async xử lý từng audio chunk."""
        self.config = config
        self.on_chunk = on_chunk
        self.room = rtc.Room()
        self.stop_event = asyncio.Event()
        self.audio_tasks: dict[str, asyncio.Task] = {}

    async def run(self) -> None:
        """Kết nối vào room và xử lý cho tới khi worker được dừng."""
        self._register_room_handlers()
        token = self._create_access_token()

        logger.info("Connecting to room '%s' as %s", self.config.room_name, self.config.bot_identity)
        await self.room.connect(self.config.livekit_ws_url, token)
        logger.info("Connected to room '%s'", self.room.name)

        for participant in self.room.remote_participants.values():
            self._subscribe_existing_tracks(participant)

        await self.stop_event.wait()

    def request_stop(self) -> None:
        """Phát tín hiệu để worker dừng một cách mềm mại."""
        self.stop_event.set()

    def _register_room_handlers(self) -> None:
        """Đăng ký callback LiveKit cho participant, track và sự kiện ngắt kết nối."""
        @self.room.on("participant_connected")
        def on_participant_connected(participant: rtc.RemoteParticipant) -> None:
            """Kiểm tra participant mới và subscribe nếu đó là streamer."""
            logger.info("Participant connected: %s", participant.identity)
            self._subscribe_existing_tracks(participant)

        @self.room.on("track_subscribed")
        def on_track_subscribed(
            track: rtc.Track,
            publication: rtc.RemoteTrackPublication,
            participant: rtc.RemoteParticipant,
        ) -> None:
            """Bắt đầu đọc audio track mới của streamer vừa subscribe."""
            if track.kind != rtc.TrackKind.KIND_AUDIO:
                return
            if not participant.identity.startswith(self.config.streamer_identity_prefix):
                return

            task_key = publication.sid
            if task_key in self.audio_tasks:
                return

            logger.info(
                "Subscribed to streamer audio track: participant=%s publication=%s",
                participant.identity,
                publication.sid,
            )
            task = asyncio.create_task(
                self._consume_audio_track(track, participant.identity, publication.sid)
            )
            self.audio_tasks[task_key] = task
            task.add_done_callback(lambda _, sid=task_key: self.audio_tasks.pop(sid, None))

        @self.room.on("track_unsubscribed")
        def on_track_unsubscribed(
            track: rtc.Track,
            publication: rtc.RemoteTrackPublication,
            participant: rtc.RemoteParticipant,
        ) -> None:
            """Dừng task đang đọc audio khi track bị gỡ bỏ."""
            del track
            task = self.audio_tasks.pop(publication.sid, None)
            if task:
                task.cancel()
            logger.info(
                "Unsubscribed from track: participant=%s publication=%s",
                participant.identity,
                publication.sid,
            )

        @self.room.on("disconnected")
        def on_disconnected(reason) -> None:
            """Dừng worker khi LiveKit bị ngắt kết nối."""
            logger.warning("Disconnected from room: %s", reason)
            self.request_stop()

    def _subscribe_existing_tracks(self, participant: rtc.RemoteParticipant) -> None:
        """Bắt đầu đọc các audio track mà streamer đã publish sẵn."""
        if not participant.identity.startswith(self.config.streamer_identity_prefix):
            return

        for publication in participant.track_publications.values():
            if publication.kind != rtc.TrackKind.KIND_AUDIO:
                continue
            if publication.track is None:
                continue
            if publication.sid in self.audio_tasks:
                continue

            logger.info(
                "Found existing streamer audio track: participant=%s publication=%s",
                participant.identity,
                publication.sid,
            )
            task = asyncio.create_task(
                self._consume_audio_track(publication.track, participant.identity, publication.sid)
            )
            self.audio_tasks[publication.sid] = task
            task.add_done_callback(
                lambda _, sid=publication.sid: self.audio_tasks.pop(sid, None)
            )

    async def _consume_audio_track(
        self,
        track: rtc.Track,
        participant_identity: str,
        publication_sid: str,
    ) -> None:
        """Đọc frame từ một audio track và chuyển tiếp các chunk đã hoàn chỉnh."""
        chunker = AudioChunker(
            sample_rate=self.config.sample_rate,
            num_channels=self.config.num_channels,
            chunk_seconds=self.config.chunk_seconds,
            overlap_seconds=self.config.chunk_overlap_seconds,
        )

        audio_stream = rtc.AudioStream(
            track,
            sample_rate=self.config.sample_rate,
            num_channels=self.config.num_channels,
        )

        try:
            async for event in audio_stream:
                for wav_bytes in chunker.push(event.frame):
                    logger.debug(
                        "Processing audio chunk: participant=%s publication=%s size=%s",
                        participant_identity,
                        publication_sid,
                        len(wav_bytes),
                    )
                    await self.on_chunk(wav_bytes)
        except asyncio.CancelledError:
            raise
        except Exception:
            logger.exception(
                "Error while consuming audio track: participant=%s publication=%s",
                participant_identity,
                publication_sid,
            )
        finally:
            final_chunk = chunker.flush()
            if final_chunk:
                try:
                    await self.on_chunk(final_chunk)
                except Exception:
                    logger.exception("Failed to process final audio chunk")
            await audio_stream.aclose()

    def _create_access_token(self) -> str:
        """Dùng token có sẵn hoặc tự tạo token subscriber cho worker."""
        if self.config.livekit_token:
            return self.config.livekit_token

        if not self.config.livekit_api_key or not self.config.livekit_api_secret:
            raise RuntimeError(
                "Set LIVEKIT_TOKEN or provide both LIVEKIT_API_KEY and LIVEKIT_API_SECRET"
            )

        now = datetime.now(timezone.utc)
        payload = {
            "iss": self.config.livekit_api_key,
            "sub": self.config.bot_identity,
            "name": self.config.bot_name,
            "nbf": int(now.timestamp()),
            "exp": int((now + timedelta(hours=6)).timestamp()),
            "video": {
                "roomJoin": True,
                "room": self.config.room_name,
                "canPublish": False,
                "canSubscribe": True,
                "canPublishData": False,
            },
        }
        return jwt.encode(
            payload,
            self.config.livekit_api_secret,
            algorithm="HS256",
        )

    async def shutdown(self) -> None:
        """Hủy các task đọc track và đóng kết nối LiveKit gọn gàng."""
        logger.info("Shutting down LiveKit audio client")
        for task in list(self.audio_tasks.values()):
            task.cancel()
        if self.audio_tasks:
            await asyncio.gather(*self.audio_tasks.values(), return_exceptions=True)

        if self.room.isconnected():
            await self.room.disconnect()
