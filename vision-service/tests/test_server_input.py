import base64
import unittest
import uuid

import cv2
import numpy as np

from server import decode_image, session_id


class ServerInputTests(unittest.TestCase):
    def test_decodes_valid_data_url(self):
        source = np.full((16, 20, 3), 120, dtype=np.uint8)
        encoded = cv2.imencode(".jpg", source)[1].tobytes()
        image = decode_image("data:image/jpeg;base64," + base64.b64encode(encoded).decode("ascii"))
        self.assertEqual((16, 20, 3), image.shape)
        self.assertEqual(np.uint8, image.dtype)

    def test_rejects_malformed_or_empty_image(self):
        for value in (None, "", "sem-virgula", "data:image/jpeg;base64,", "data:image/jpeg;base64,@@@"):
            with self.subTest(value=value), self.assertRaises((ValueError, TypeError)):
                decode_image(value)

    def test_normalizes_valid_session_uuid(self):
        value = uuid.uuid4()
        self.assertEqual(str(value), session_id(str(value)))

    def test_rejects_invalid_session_identifier(self):
        for value in (None, "", "sessao", "../../arquivo"):
            with self.subTest(value=value), self.assertRaisesRegex(ValueError, "Sessão"):
                session_id(value)


if __name__ == "__main__":
    unittest.main()
