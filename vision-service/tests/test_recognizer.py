import unittest

import numpy as np

from recognizer import DetectedFace, FaceRecognizer


def face(x1, y1, x2, y2, embedding=None):
    return DetectedFace(
        np.asarray([x1, y1, x2, y2], dtype=np.float32),
        np.asarray(embedding if embedding is not None else [1.0, 0.0], dtype=np.float32),
    )


class FaceRecognizerMathTests(unittest.TestCase):
    def test_normalizes_embedding_without_mutating_source(self):
        detected = face(0, 0, 200, 200, [3.0, 4.0])
        normalized = FaceRecognizer.normalizar_embedding(detected)
        np.testing.assert_allclose([0.6, 0.8], normalized)
        np.testing.assert_allclose([3.0, 4.0], detected.embedding)

    def test_rejects_empty_non_finite_or_zero_embedding(self):
        for embedding in ([0.0, 0.0], [np.nan, 1.0], [np.inf, 1.0]):
            with self.subTest(embedding=embedding), self.assertRaises(ValueError):
                FaceRecognizer.normalizar_embedding(face(0, 0, 200, 200, embedding))

    def test_selects_largest_face_above_minimum_width(self):
        recognizer = FaceRecognizer.__new__(FaceRecognizer)
        small = face(0, 0, 80, 100)
        medium = face(0, 0, 160, 200)
        large = face(0, 0, 220, 300)
        self.assertIs(large, recognizer.selecionar_rosto_principal([small, medium, large], 100))

    def test_returns_none_without_eligible_face(self):
        recognizer = FaceRecognizer.__new__(FaceRecognizer)
        self.assertIsNone(recognizer.selecionar_rosto_principal([face(0, 0, 50, 50)], 100))
        self.assertIsNone(recognizer.selecionar_rosto_principal([], 100))

    def test_calculates_face_geometry(self):
        detected = face(10, 20, 110, 220)
        self.assertEqual(100.0, FaceRecognizer._largura_rosto(detected))
        self.assertEqual(20000.0, FaceRecognizer._area_rosto(detected))


if __name__ == "__main__":
    unittest.main()
