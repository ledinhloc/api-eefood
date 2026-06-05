# Subtitle Worker

Worker nay nhan lenh tu `reaction-service`, join LiveKit room, nghe audio cua streamer, goi Whisper de tao transcript, roi gui subtitle ve backend.

## Flow

```text
UI start live
-> reaction-service tao LiveKit room
-> reaction-service goi subtitle-worker /start
-> subtitle-worker join room va nghe audio
-> Whisper nhan dien giong noi
-> subtitle-worker gui transcript ve reaction-service

UI end live
-> reaction-service goi subtitle-worker /stop
-> subtitle-worker dung worker cua live do
```

## Setup

Chay trong thu muc nay:

```powershell
cd D:\tlcn_v2\api-eefood\tools\subtitle-worker
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

Neu da tao `.venv` roi thi lan sau chi can:

```powershell
cd D:\tlcn_v2\api-eefood\tools\subtitle-worker
.\.venv\Scripts\Activate.ps1
```

## Environment

File `.env` can cac bien chinh:

```env
LIVEKIT_WS_URL=ws://127.0.0.1:7880
LIVEKIT_API_KEY=devkey
LIVEKIT_API_SECRET=secret
REACTION_BASE_URL=http://127.0.0.1:8095

AUDIO_NUM_CHANNELS=1
SPOKEN_LANGUAGE=vi

WORKER_CONTROL_HOST=127.0.0.1
WORKER_CONTROL_PORT=9000
```

## Run
Neu muon auto reload khi sua code:

```powershell
uvicorn subtitle_worker.app:app --reload --host 127.0.0.1 --port 9000
```

Co the chay bang Python:

```powershell
python -m subtitle_worker.app
```

## Endpoints

### Health

```http
GET /health
```

Response vi du:

```json
{
  "status": "ok",
  "activeLivestreams": []
}
```

### Start subtitle worker for a live

```http
POST /start
```

Body:

```json
{
  "liveStreamId": 43,
  "roomName": "live_2_1779379611589",
  "spokenLanguage": "vi"
}
```

Response:

```json
{
  "status": "started",
  "liveStreamId": 43,
  "roomName": "live_2_1779379611589"
}
```

Neu live do dang chay roi:

```json
{
  "status": "already_running",
  "liveStreamId": 43,
  "roomName": "live_2_1779379611589"
}
```

### Stop subtitle worker for a live

```http
POST /stop
```

Hoac:

```http
POST /end
```

Body:

```json
{
  "liveStreamId": 43
}
```

Response:

```json
{
  "status": "stopped",
  "liveStreamId": 43
}
```

Neu live do khong co worker dang chay:

```json
{
  "status": "not_running",
  "liveStreamId": 43
}
```

## Test bang curl

```powershell
curl http://127.0.0.1:9000/health
```

```powershell
curl -X POST http://127.0.0.1:9000/start `
  -H "Content-Type: application/json" `
  -d "{\"liveStreamId\":43,\"roomName\":\"live_2_1779379611589\",\"spokenLanguage\":\"vi\"}"
```

```powershell
curl -X POST http://127.0.0.1:9000/stop `
  -H "Content-Type: application/json" `
  -d "{\"liveStreamId\":43}"
```

## Reaction Service

`reaction-service` goi worker qua FeignClient:

```yaml
subtitle-worker:
  base-url: ${SUBTITLE_WORKER_BASE_URL:http://127.0.0.1:9000}
```

Khi start live, backend goi `/start`.

Khi end live, backend goi `/stop`.

## Notes

- Worker luu danh sach live dang chay trong RAM.
- Neu worker restart, danh sach live dang chay se mat.
- Worker chi bat dau nghe audio sau khi backend goi `/start`.
- Neu LiveKit room chua co audio track, worker se join room va cho streamer publish audio.
