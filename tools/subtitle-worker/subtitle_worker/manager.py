import asyncio
import logging
from dataclasses import dataclass
from typing import Any, Optional

from subtitle_worker.config import WorkerConfig
from subtitle_worker.worker import SubtitleWorker


logger = logging.getLogger("subtitle-worker")


@dataclass
class RunningLivestream:
    """Luu worker, task va thong tin cua mot livestream dang duoc xu ly."""

    worker: SubtitleWorker
    task: asyncio.Task
    room_name: str
    spoken_language: Optional[str]
    target_language: Optional[str]


class SubtitleWorkerManager:
    """Quan ly cac subtitle worker dang chay theo tung livestream."""

    def __init__(self, config: WorkerConfig) -> None:
        """Khoi tao manager voi cau hinh chung cua subtitle worker."""
        self.config = config
        # Luu trong RAM: (liveStreamId, targetLanguage) -> worker dang nghe live do.
        self.active_workers: dict[tuple[int, str], RunningLivestream] = {}

    async def start_livestream(self, payload: dict[str, Any]) -> dict[str, Any]:
        """Tao va chay worker nen cho livestream neu live do chua duoc xu ly."""
        live_config = self._build_livestream_config(payload)
        live_stream_id = live_config.live_stream_id
        if (
            live_config.spoken_language != live_config.target_language
            and not (
                live_config.spoken_language == "vi"
                and live_config.target_language == "en"
            )
        ):
            raise ValueError(
                "Unsupported subtitle translation: "
                f"{live_config.spoken_language} -> {live_config.target_language}"
            )
        target_language = live_config.target_language or live_config.spoken_language or "auto"
        worker_key = (live_stream_id, target_language)

        # Khong tao trung worker khi backend gui lai cung mot lenh start.
        if worker_key in self.active_workers:
            running = self.active_workers[worker_key]
            return {
                "status": "already_running",
                "liveStreamId": live_stream_id,
                "roomName": running.room_name,
                "targetLanguage": running.target_language,
            }

        worker = SubtitleWorker(live_config)
        # Tao task nen de manager van nhan duoc /start cho live khac.
        task = asyncio.create_task(worker.run())
        self.active_workers[worker_key] = RunningLivestream(
            worker=worker,
            task=task,
            room_name=live_config.room_name,
            spoken_language=live_config.spoken_language,
            target_language=live_config.target_language,
        )
        #khi task chạy worker kết thúc, tự gọi hàm dọn dẹp worker khỏi danh sách active.
        task.add_done_callback(
            lambda done_task, key=worker_key: self._remove_finished_worker(
                key,
                done_task,
            )
        )

        logger.info(
            "Started subtitle worker: mode=%s livestream=%s room=%s spoken=%s target=%s",
            (
                "SUBTITLE"
                if live_config.spoken_language == live_config.target_language
                else "TRANSLATE"
            ),
            live_stream_id,
            live_config.room_name,
            live_config.spoken_language or "auto",
            live_config.target_language or "auto",
        )
        return {
            "status": "started",
            "liveStreamId": live_stream_id,
            "roomName": live_config.room_name,
            "targetLanguage": live_config.target_language,
        }

    async def stop_livestream(self, payload: dict[str, Any]) -> dict[str, Any]:
        """Dung worker cua mot livestream va cho task ket thuc hoan toan."""
        live_stream_id = self._read_live_stream_id(payload)
        target_language = payload.get("targetLanguage") or payload.get("target_language")
        if target_language:
            worker_keys = [(live_stream_id, str(target_language).lower())]
        else:
            worker_keys = [
                key for key in self.active_workers
                if key[0] == live_stream_id
            ]
        running_workers = [
            self.active_workers.pop(key)
            for key in worker_keys
            if key in self.active_workers
        ]

        if not running_workers:
            return {
                "status": "not_running",
                "liveStreamId": live_stream_id,
                "targetLanguage": target_language,
            }

        # Bao LiveKit client dung mem; worker.run() se tu shutdown.
        for running in running_workers:
            running.worker.livekit_client.request_stop()
        await asyncio.gather(
            *(running.task for running in running_workers),
            return_exceptions=True,
        )
        logger.info(
            "Stopped subtitle worker: livestream=%s target=%s",
            live_stream_id,
            target_language or "all",
        )
        return {
            "status": "stopped",
            "liveStreamId": live_stream_id,
            "targetLanguage": target_language,
        }

    async def stop_all(self) -> None:
        """Dung tat ca worker dang chay khi ung dung subtitle worker tat."""
        # Dung tat ca live khi process worker bi tat.
        live_stream_ids = {key[0] for key in self.active_workers}
        for live_stream_id in live_stream_ids:
            await self.stop_livestream({"liveStreamId": live_stream_id})

    def list_livestreams(self) -> list[dict[str, Any]]:
        """Tra ve danh sach livestream ma manager dang lang nghe."""
        return [
            {
                "liveStreamId": worker_key[0],
                "roomName": running.room_name,
                "spokenLanguage": running.spoken_language,
                "targetLanguage": running.target_language,
            }
            for worker_key, running in self.active_workers.items()
        ]

    def _build_livestream_config(self, payload: dict[str, Any]) -> WorkerConfig:
        live_stream_id = self._read_live_stream_id(payload)
        room_name = payload.get("roomName") or payload.get("room_name")
        spoken_language = payload.get("spokenLanguage") or payload.get("language")
        target_language = payload.get("targetLanguage") or payload.get("target_language")

        if not room_name:
            raise ValueError("Missing roomName")

        return self.config.with_livestream(
            room_name=str(room_name),
            live_stream_id=live_stream_id,
            spoken_language=str(spoken_language).lower() if spoken_language else None,
            target_language=str(target_language).lower() if target_language else None,
        )

    def _read_live_stream_id(self, payload: dict[str, Any]) -> int:
        live_stream_id = payload.get("liveStreamId") or payload.get("id")
        if not live_stream_id:
            raise ValueError("Missing liveStreamId")
        return int(live_stream_id)

    def _remove_finished_worker(
        self,
        worker_key: tuple[int, str],
        task: asyncio.Task,
    ) -> None:
        """Xoa worker da ket thuc khoi bo nho va ghi log neu task bi loi."""
        # Neu worker tu ket thuc do room disconnect, xoa khoi RAM state.
        running = self.active_workers.get(worker_key)
        if running is not None and running.task is task:
            self.active_workers.pop(worker_key, None)

        try:
            task.result()
        except asyncio.CancelledError:
            pass
        except Exception:
            logger.exception(
                "Subtitle worker failed: livestream=%s target=%s",
                worker_key[0],
                worker_key[1],
            )
