import logging
import os
from contextlib import asynccontextmanager
from typing import Any

import uvicorn
from fastapi import FastAPI, HTTPException

from subtitle_worker.clients.backend import BackendClient
from subtitle_worker.config import load_config
from subtitle_worker.manager import SubtitleWorkerManager


logging.basicConfig(
    level=os.getenv("LOG_LEVEL", "INFO").upper(),
    format="%(asctime)s %(levelname)s %(message)s",
)
logging.getLogger("faster_whisper").setLevel(logging.WARNING)
logger = logging.getLogger("subtitle-worker")

config = load_config()
manager = SubtitleWorkerManager(config)


@asynccontextmanager
async def lifespan(_: FastAPI):
    try:
        await start_existing_livestreams()
        yield
    finally:
        await manager.stop_all()


app = FastAPI(
    title="Subtitle Worker",
    version="0.1.0",
    lifespan=lifespan,
)


async def start_existing_livestreams() -> None:
    # Khi worker restart, RAM state mat; lay lai cac live dang LIVE tu backend.
    backend_client = BackendClient(config)
    try:
        active_livestreams = await backend_client.get_active_livestreams()
    except Exception as exc:
        logger.warning("Cannot fetch active livestreams on startup: %s", exc)
        return
    finally:
        backend_client.close()

    if not active_livestreams:
        logger.info("No active livestreams found on startup")
        return

    logger.info("Found %s active livestream(s) on startup", len(active_livestreams))
    for payload in active_livestreams:
        try:
            result = await manager.start_livestream(payload)
            logger.info(
                "Startup subtitle worker result: livestream=%s status=%s",
                result.get("liveStreamId"),
                result.get("status"),
            )
        except Exception as exc:
            logger.warning("Cannot start subtitle worker from active payload %s: %s", payload, exc)


@app.post("/start")
async def start_livestream(payload: dict[str, Any]) -> dict[str, Any]:
    # Backend goi endpoint nay sau khi tao LiveKit room.
    try:
        return await manager.start_livestream(payload)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.post("/stop")
@app.post("/end")
async def stop_livestream(payload: dict[str, Any]) -> dict[str, Any]:
    # Backend goi endpoint nay khi live ket thuc.
    try:
        return await manager.stop_livestream(payload)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.get("/health")
async def health() -> dict[str, Any]:
    # Dung de xem worker con song va dang nghe nhung live nao.
    return {
        "status": "ok",
        "activeLivestreams": manager.list_livestreams(),
    }


def main() -> None:
    logger.info(
        "Subtitle worker control server listening on http://%s:%s",
        config.worker_control_host,
        config.worker_control_port,
    )
    uvicorn.run(
        app,
        host=config.worker_control_host,
        port=config.worker_control_port,
    )


if __name__ == "__main__":
    main()
