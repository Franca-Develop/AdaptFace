FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    ADAPTFACE_VISION_ROOT=/app \
    VISION_HOST=0.0.0.0 \
    DEEPFACE_BACKEND_ENGINE=onnx \
    DEEPFACE_HOME=/app/models

RUN apt-get update \
    && apt-get install --no-install-recommends -y libglib2.0-0 libgl1 \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --uid 10001 --create-home adaptface

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY --chown=adaptface:adaptface . ./
RUN mkdir -p /app/models && chown -R adaptface:adaptface /app

USER adaptface
EXPOSE 8000

CMD ["python", "server.py"]
