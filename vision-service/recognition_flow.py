import hashlib
from dataclasses import dataclass

import numpy as np

import config


@dataclass(frozen=True)
class LivenessDecision:
    complete: bool
    rejected: bool
    message: str
    detail: str
    progress: int


class PassiveLiveness:
    """Confirma presença contínua sem exigir movimentos guiados."""

    def __init__(self, now):
        self.started_at = now
        self.reference_embedding = None
        self.frame_hashes = set()
        self.confirmed_frames = 0

    def update(self, embedding, image, now):
        if now - self.started_at > config.LIVENESS_SESSION_TIMEOUT:
            return self._reject("Tempo da verificação expirado")

        embedding = np.asarray(embedding, dtype=np.float32)
        if self.reference_embedding is None:
            self.reference_embedding = embedding.copy()
        elif float(np.dot(self.reference_embedding, embedding)) < config.LIVENESS_IDENTITY_SIMILARITY:
            return self._reject("O rosto mudou durante a verificação")

        frame_hash = hashlib.blake2b(image.tobytes(), digest_size=16).digest()
        if frame_hash in self.frame_hashes:
            return self._pending("Aguardando uma imagem atual da câmera")
        self.frame_hashes.add(frame_hash)
        self.confirmed_frames += 1

        elapsed = now - self.started_at
        frames_ready = self.confirmed_frames >= config.LIVENESS_CONFIRMATION_FRAMES
        time_ready = elapsed >= config.LIVENESS_MIN_DURATION
        if frames_ready and time_ready:
            return LivenessDecision(True, False, "Rosto lido", "Confirmando identidade", 100)
        return self._pending("Continue olhando para a câmera")

    def _pending(self, detail):
        frame_progress = self.confirmed_frames / config.LIVENESS_CONFIRMATION_FRAMES
        progress = min(95, round(frame_progress * 100))
        return LivenessDecision(False, False, "Lendo seu rosto", detail, progress)

    @staticmethod
    def _reject(detail):
        return LivenessDecision(False, True, "Prova de vida recusada", detail, 0)


def quality_guidance(frame, bbox):
    height, width = frame.shape[:2]
    x1, y1, x2, y2 = [float(value) for value in bbox[:4]]
    face_width, face_height = x2 - x1, y2 - y1
    rw, rh = min(config.ROI_WIDTH, width), min(config.ROI_HEIGHT, height)
    cx, cy = (x1 + x2) / 2, (y1 + y2) / 2
    if face_width < min(140.0, rw * 0.34):
        return "Aproxime-se um pouco", "Mantenha o rosto dentro do guia"
    if face_width > rw * 1.05 or face_height > rh * 1.10:
        return "Afaste-se um pouco", "Deixe o rosto inteiro dentro do guia"
    position = ((cx - width / 2) / (rw / 2)) ** 2 + ((cy - height / 2) / (rh / 2)) ** 2
    if position > 1.45:
        return "Rosto fora da área", "Mantenha o rosto visível no guia"
    crop = frame[max(0, int(y1)):min(height, int(y2)), max(0, int(x1)):min(width, int(x2))]
    if crop.size and float(crop.mean()) < 55.0:
        return "Melhore a iluminação", "Seu rosto precisa estar bem iluminado"
    return None
