import base64
import json
import os
import sys
import threading
import time
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

VISION_ROOT = Path(os.environ.get("ADAPTFACE_VISION_ROOT", Path(__file__).parent)).resolve()
if not VISION_ROOT.is_dir():
    raise RuntimeError("ADAPTFACE_VISION_ROOT não aponta para o projeto de visão")

sys.path.insert(0, str(VISION_ROOT))

import cv2
import numpy as np

from backend_client import BackendClient, BackendError
import config
from recognition_flow import PassiveLiveness, quality_guidance
from recognizer import FaceRecognizer
from deepface.modules.exceptions import FaceNotDetected, SpoofDetected

MAX_BODY_SIZE = 6 * 1024 * 1024
_recognizer = None
_recognizer_lock = threading.Lock()
_sessions = {}
_sessions_lock = threading.Lock()


def recognizer():
    global _recognizer
    if _recognizer is None:
        with _recognizer_lock:
            if _recognizer is None:
                _recognizer = FaceRecognizer()
    return _recognizer


def decode_image(value):
    if not isinstance(value, str) or "," not in value:
        raise ValueError("Imagem inválida")
    raw = base64.b64decode(value.split(",", 1)[1], validate=True)
    if not raw or len(raw) > MAX_BODY_SIZE:
        raise ValueError("Imagem inválida")
    image = cv2.imdecode(np.frombuffer(raw, dtype=np.uint8), cv2.IMREAD_COLOR)
    if image is None or image.ndim != 3:
        raise ValueError("Imagem inválida")
    return image


def verify_liveness(image):
    engine = recognizer()
    with engine._lock:
        faces = engine._deepface.extract_faces(
            img_path=image,
            detector_backend=config.DETECTOR_BACKEND,
            enforce_detection=True,
            align=True,
            anti_spoofing=True,
        )
    if not faces or not all(
        face.get("is_real") is True and float(face.get("antispoof_score", 0.0)) >= 0.97
        for face in faces
    ):
        raise SpoofDetected("Apresentação não viva detectada")


def session_id(value):
    try:
        return str(uuid.UUID(value))
    except (ValueError, TypeError, AttributeError):
        raise ValueError("Sessão de prova de vida inválida")


def active_liveness(identifier, now):
    with _sessions_lock:
        expired = [key for key, item in _sessions.items()
                   if now - item.started_at > config.LIVENESS_SESSION_TIMEOUT]
        for key in expired:
            _sessions.pop(key, None)
        return _sessions.setdefault(identifier, PassiveLiveness(now))


def close_liveness(identifier):
    with _sessions_lock:
        _sessions.pop(identifier, None)


class Handler(BaseHTTPRequestHandler):
    server_version = "AdaptFaceVision/1.0"

    def do_OPTIONS(self):
        self.send_response(204)
        self._cors_headers()
        self.end_headers()

    def do_GET(self):
        self._json(200, {"status": "ok"}) if self.path == "/health" else self._json(404, {"message": "Rota não encontrada"})

    def do_POST(self):
        if self.path not in {"/analyze", "/embedding"}:
            self._json(404, {"message": "Rota não encontrada"})
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length <= 0 or length > MAX_BODY_SIZE:
                raise ValueError("Imagem inválida")
            payload = json.loads(self.rfile.read(length))
            image = decode_image(payload.get("image"))
            if self.path == "/analyze":
                identifier = session_id(payload.get("sessionId"))
                verify_liveness(image)
            faces = recognizer().gerar_embedding(image)
            if not faces:
                self._json(200, {"status": "looking", "message": "Posicione o rosto dentro do guia"})
                return
            if len(faces) > 1:
                self._json(200, {"status": "multiple", "message": "Uma pessoa de cada vez"})
                return
            face = recognizer().selecionar_rosto_principal(faces)
            if face is None:
                self._json(200, {"status": "looking", "message": "Aproxime-se um pouco"})
                return
            embedding = recognizer().normalizar_embedding(face)
            if self.path == "/embedding":
                self._json(200, {"status": "ready", "embedding": embedding.tolist(), "faceModel": "DEEPFACE_ARCFACE_OPENCV_V1"})
                return
            state = active_liveness(identifier, time.monotonic())
            guidance = quality_guidance(image, face.bbox)
            if guidance:
                self._json(200, {"status": "guidance", "message": guidance[0], "detail": guidance[1]})
                return
            decision = state.update(embedding, image, time.monotonic())
            if decision.rejected:
                close_liveness(identifier)
                self._json(200, {"status": "spoof", "message": decision.message, "detail": decision.detail})
                return
            if not decision.complete:
                self._json(200, {
                    "status": "scanning",
                    "message": decision.message, "detail": decision.detail,
                    "progress": decision.progress,
                })
                return
            close_liveness(identifier)
            result = BackendClient().validar_rosto(embedding)
            self._json(200, {
                "status": "granted" if result.result == "GRANTED" else "denied",
                "name": result.name,
                "message": result.reason or ("Acesso liberado" if result.result == "GRANTED" else "Rosto não cadastrado"),
            })
        except SpoofDetected:
            self._json(200, {"status": "spoof", "message": "Prova de vida recusada", "detail": "Não use fotos, vídeos ou telas. Olhe diretamente para a câmera."})
        except FaceNotDetected:
            self._json(200, {"status": "looking", "message": "Posicione o rosto dentro do guia"})
        except BackendError as error:
            self._json(503, {"status": "error", "message": str(error)})
        except (ValueError, TypeError, json.JSONDecodeError):
            self._json(400, {"status": "error", "message": "Não foi possível analisar esta imagem"})
        except Exception:
            self._json(500, {"status": "error", "message": "Serviço de reconhecimento indisponível"})

    def log_message(self, format, *args):
        return

    def _cors_headers(self):
        origin = self.headers.get("Origin", "")
        if origin in {"http://localhost:5173", "http://127.0.0.1:5173"}:
            self.send_header("Access-Control-Allow-Origin", origin)
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Vary", "Origin")

    def _json(self, status, payload):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self._cors_headers()
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


if __name__ == "__main__":
    ThreadingHTTPServer((os.environ.get("VISION_HOST", "127.0.0.1"), int(os.environ.get("VISION_PORT", "8000"))), Handler).serve_forever()
