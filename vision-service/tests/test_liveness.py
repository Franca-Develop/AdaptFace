import unittest

import numpy as np

import config
from recognition_flow import PassiveLiveness, quality_guidance


class PassiveLivenessTests(unittest.TestCase):
    def setUp(self):
        self.embedding = np.zeros(512, dtype=np.float32)
        self.embedding[0] = 1.0
        self.frame_number = 0

    def frame(self):
        self.frame_number += 1
        return np.full((32, 32, 3), self.frame_number, dtype=np.uint8)

    def test_completes_without_guided_movement(self):
        session = PassiveLiveness(0.0)
        decision = None
        for index in range(config.LIVENESS_CONFIRMATION_FRAMES):
            decision = session.update(self.embedding, self.frame(), 3.0 + index * 0.1)
        self.assertTrue(decision.complete)
        self.assertEqual(100, decision.progress)

    def test_moving_face_keeps_confirmation_active(self):
        session = PassiveLiveness(0.0)
        for index in range(config.LIVENESS_CONFIRMATION_FRAMES):
            decision = session.update(self.embedding, self.frame(), 3.0 + index * 0.1)
        self.assertTrue(decision.complete)

    def test_repeated_frame_does_not_advance(self):
        session = PassiveLiveness(0.0)
        image = self.frame()
        session.update(self.embedding, image, 0.5)
        repeated = session.update(self.embedding, image, 1.0)
        self.assertFalse(repeated.complete)
        self.assertIn("imagem atual", repeated.detail)
        self.assertEqual(1, session.confirmed_frames)

    def test_rejects_identity_change(self):
        session = PassiveLiveness(0.0)
        session.update(self.embedding, self.frame(), 0.5)
        other = np.zeros(512, dtype=np.float32)
        other[1] = 1.0
        decision = session.update(other, self.frame(), 1.0)
        self.assertTrue(decision.rejected)
        self.assertIn("rosto mudou", decision.detail)

    def test_rejects_expired_session(self):
        session = PassiveLiveness(0.0)
        decision = session.update(self.embedding, self.frame(), config.LIVENESS_SESSION_TIMEOUT + 0.1)
        self.assertTrue(decision.rejected)

    def test_guidance_allows_normal_movement_inside_extended_area(self):
        frame = np.full((720, 1280, 3), 180, dtype=np.uint8)
        bbox = np.array([300, 180, 500, 480], dtype=np.float32)
        self.assertIsNone(quality_guidance(frame, bbox))


if __name__ == "__main__":
    unittest.main()
