import os
import threading
from dataclasses import dataclass
from pathlib import Path

import cv2
import numpy as np
import onnxruntime as ort

import config


@dataclass
class DetectedFace:
    bbox: np.ndarray
    embedding: np.ndarray
    kps: np.ndarray | None = None


class FaceRecognizer:
    _lock = threading.Lock()

    def __init__(self):
        os.environ["DEEPFACE_BACKEND_ENGINE"] = "onnx"
        os.environ["DEEPFACE_HOME"] = config.MODELS_ROOT
        os.environ.setdefault("DEEPFACE_LOG_LEVEL", "40")
        from deepface import DeepFace
        from deepface.modules.exceptions import FaceNotDetected

        self._deepface = DeepFace
        self._not_detected = FaceNotDetected
        cv2.setNumThreads(1)
        with self._lock:
            model = DeepFace.build_model(config.MODEL_NAME)
            if not getattr(model, "_adaptface_bounded", False):
                options = ort.SessionOptions()
                options.log_severity_level = 3
                options.intra_op_num_threads = config.INFERENCE_THREADS
                options.inter_op_num_threads = 1
                options.add_session_config_entry("session.intra_op.allow_spinning", "0")
                options.add_session_config_entry("session.inter_op.allow_spinning", "0")
                weights = Path(config.MODELS_ROOT) / ".deepface" / "weights" / "arcface_weights.onnx"
                model.model = ort.InferenceSession(str(weights), sess_options=options,
                                                   providers=["CPUExecutionProvider"])
                model._adaptface_bounded = True
            DeepFace.build_model(config.DETECTOR_BACKEND, task="face_detector")

    def gerar_embedding(self, image_bgr):
        if not isinstance(image_bgr, np.ndarray) or image_bgr.ndim != 3 or image_bgr.shape[2] != 3 \
                or image_bgr.dtype != np.uint8 or image_bgr.size == 0:
            raise ValueError("Imagem BGR inválida para reconhecimento facial")
        with self._lock:
            try:
                results = self._deepface.represent(
                    img_path=image_bgr, model_name=config.MODEL_NAME,
                    detector_backend=config.DETECTOR_BACKEND, enforce_detection=True,
                    align=True, normalization="ArcFace", l2_normalize=True,
                )
            except self._not_detected:
                return []
        faces = []
        for result in results:
            area = result["facial_area"]
            embedding = np.asarray(result["embedding"], dtype=np.float32)
            if embedding.shape != (512,) or not np.isfinite(embedding).all() or np.linalg.norm(embedding) == 0:
                raise ValueError("Credencial facial inválida retornada pelo DeepFace")
            eyes = [area[key] for key in ("left_eye", "right_eye") if area.get(key) is not None]
            faces.append(DetectedFace(
                np.asarray([area["x"], area["y"], area["x"] + area["w"], area["y"] + area["h"]], dtype=np.float32),
                embedding, np.asarray(eyes, dtype=np.float32) if eyes else None,
            ))
        return faces

    @staticmethod
    def normalizar_embedding(face):
        embedding = np.asarray(face.embedding, dtype=np.float32)
        norm = np.linalg.norm(embedding)
        if not np.isfinite(embedding).all() or not np.isfinite(norm) or norm == 0:
            raise ValueError("Credencial facial inválida")
        return (embedding / norm).copy()

    @staticmethod
    def _largura_rosto(face):
        x1, _, x2, _ = face.bbox
        return float(x2 - x1)

    @staticmethod
    def _area_rosto(face):
        x1, y1, x2, y2 = face.bbox
        return float((x2 - x1) * (y2 - y1))

    def selecionar_rosto_principal(self, faces, largura_minima=config.MIN_FACE_WIDTH_PX):
        candidatos = [face for face in faces if self._largura_rosto(face) >= largura_minima]
        return max(candidatos, key=self._area_rosto) if candidatos else None
