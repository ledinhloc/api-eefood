import asyncio
import logging
from dataclasses import dataclass
from typing import Any, Optional

from config import WorkerConfig
from subtitle_worker import SubtitleWorker


logger = logging.getLogger("subtitle-worker")


@dataclass
class RunningLivestream:
    worker: SubtitleWorker
    task: asyncio.Task
    room_name: str
    language: Optional[str]


class SubtitleWorkerManager:
    def __init__(self, config: WorkerConfig) -> None:
        self.config = config
        # Luu trong RAM: liveStreamId -> worker dang nghe live do.
        self.active_workers: dict[int, RunningLivestream] = {}

    async def start_livestream(self, payload: dict[str, Any]) -> dict[str, Any]:
        live_config = self._build_livestream_config(payload)
        live_stream_id = live_config.live_stream_id

        if live_stream_id in self.active_workers:
            running = self.active_workers[live_stream_id]
            return {
                "status": "already_running",
                "liveStreamId": live_stream_id,
                "roomName": running.room_name,
            }

        worker = SubtitleWorker(live_config)
        # Tao task nen de manager van nhan duoc /start cho live khac.
        task = asyncio.create_task(worker.run())
        self.active_workers[live_stream_id] = RunningLivestream(
            worker=worker,
            task=task,
            room_name=live_config.room_name,
            language=live_config.language,
        )
        task.add_done_callback(
            lambda done_task, stream_id=live_stream_id: self._remove_finished_worker(
                stream_id,
                done_task,
            )
        )

        logger.info(
            "Started subtitle worker: livestream=%s room=%s language=%s",
            live_stream_id,
            live_config.room_name,
            live_config.language or "auto",
        )
        return {
            "status": "started",
            "liveStreamId": live_stream_id,
            "roomName": live_config.room_name,
        }

    async def stop_livestream(self, payload: dict[str, Any]) -> dict[str, Any]:
        live_stream_id = self._read_live_stream_id(payload)
        running = self.active_workers.pop(live_stream_id, None)

        if running is None:
            return {
                "status": "not_running",
                "liveStreamId": live_stream_id,
            }

        # Bao LiveKit client dung mem; worker.run() se tu shutdown.
        running.worker.livekit_client.request_stop()
        await asyncio.gather(running.task, return_exceptions=True)
        logger.info("Stopped subtitle worker: livestream=%s", live_stream_id)
        return {
            "status": "stopped",
            "liveStreamId": live_stream_id,
        }

    async def stop_all(self) -> None:
        # Dung tat ca live khi process worker bi tat.
        live_stream_ids = list(self.active_workers)
        for live_stream_id in live_stream_ids:
            await self.stop_livestream({"liveStreamId": live_stream_id})

    def list_livestreams(self) -> list[dict[str, Any]]:
        return [
            {
                "liveStreamId": live_stream_id,
                "roomName": running.room_name,
                "language": running.language,
            }
            for live_stream_id, running in self.active_workers.items()
        ]

    def _build_livestream_config(self, payload: dict[str, Any]) -> WorkerConfig:
        live_stream_id = self._read_live_stream_id(payload)
        room_name = payload.get("roomName") or payload.get("room_name")
        language = payload.get("spokenLanguage") or payload.get("language")

        if not room_name:
            raise ValueError("Missing roomName")

        return self.config.with_livestream(
            room_name=str(room_name),
            live_stream_id=live_stream_id,
            language=str(language).lower() if language else None,
        )

    def _read_live_stream_id(self, payload: dict[str, Any]) -> int:
        live_stream_id = payload.get("liveStreamId") or payload.get("id")
        if not live_stream_id:
            raise ValueError("Missing liveStreamId")
        return int(live_stream_id)

    def _remove_finished_worker(self, live_stream_id: int, task: asyncio.Task) -> None:
        # Neu worker tu ket thuc do room disconnect, xoa khoi RAM state.
        running = self.active_workers.get(live_stream_id)
        if running is not None and running.task is task:
            self.active_workers.pop(live_stream_id, None)

        try:
            task.result()
        except asyncio.CancelledError:
            pass
        except Exception:
            logger.exception("Subtitle worker failed: livestream=%s", live_stream_id)
