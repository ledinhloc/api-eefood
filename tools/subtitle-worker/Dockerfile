FROM python:3.11-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

RUN apt-get update \
    && apt-get install --no-install-recommends -y libgomp1 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY subtitle_worker ./subtitle_worker

EXPOSE 9000

CMD ["uvicorn", "subtitle_worker.app:app", "--host", "0.0.0.0", "--port", "9000"]
