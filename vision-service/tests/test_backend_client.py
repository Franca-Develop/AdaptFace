import os
import unittest
from unittest.mock import Mock, patch

import numpy as np

from backend_client import BackendClient, BackendError, strict_json


class StrictJsonTests(unittest.TestCase):
    def test_accepts_valid_json(self):
        self.assertEqual({"result": "GRANTED"}, strict_json('{"result":"GRANTED"}'))

    def test_rejects_duplicate_keys(self):
        with self.assertRaisesRegex(ValueError, "duplicada"):
            strict_json('{"result":"GRANTED","result":"DENIED"}')

    def test_rejects_non_finite_numbers(self):
        with self.assertRaisesRegex(ValueError, "inválido"):
            strict_json('{"distance":NaN}')


class BackendClientTests(unittest.TestCase):
    def test_allows_https_and_local_http(self):
        self.assertEqual("https://api.example.com", BackendClient("https://api.example.com").base_url)
        self.assertEqual("http://127.0.0.1:8080", BackendClient("http://127.0.0.1:8080/").base_url)

    def test_allows_explicit_internal_docker_host(self):
        with patch.dict(os.environ, {"ADAPTFACE_ALLOW_HTTP_HOSTS": "api"}):
            self.assertEqual("http://api:8080", BackendClient("http://api:8080").base_url)

    def test_rejects_remote_http_credentials_and_query(self):
        invalid = [
            "http://api.example.com",
            "https://user:password@api.example.com",
            "https://api.example.com?token=secret",
            "file:///tmp/backend",
        ]
        for value in invalid:
            with self.subTest(value=value), self.assertRaises(BackendError):
                BackendClient(value)

    def test_requires_machine_key_before_face_validation(self):
        client = BackendClient("http://localhost:8080", api_key="")
        with self.assertRaisesRegex(BackendError, "não configurado"):
            client.validar_rosto(np.ones(512, dtype=np.float32))

    def test_maps_granted_and_denied_results(self):
        client = BackendClient("http://localhost:8080", api_key="key")
        client._request = Mock(return_value={"result": "GRANTED", "name": "Maria", "reason": None})
        granted = client.validar_rosto(np.ones(512, dtype=np.float32))
        self.assertEqual(("GRANTED", "Maria", None), (granted.result, granted.name, granted.reason))

        client._request.return_value = {"result": "DENIED", "name": None, "reason": "Não reconhecido"}
        denied = client.validar_rosto(np.ones(512, dtype=np.float32))
        self.assertEqual("DENIED", denied.result)
        self.assertEqual("Não reconhecido", denied.reason)

    def test_rejects_incomplete_backend_result(self):
        client = BackendClient("http://localhost:8080", api_key="key")
        client._request = Mock(return_value={"result": "GRANTED", "name": "", "reason": None})
        with self.assertRaisesRegex(BackendError, "incompleta"):
            client.validar_rosto(np.ones(512, dtype=np.float32))

    def test_registration_requires_admin_token(self):
        client = BackendClient("http://localhost:8080")
        with self.assertRaisesRegex(BackendError, "administrativa") as context:
            client.cadastrar_pessoa({}, np.ones(512, dtype=np.float32), "")
        self.assertEqual(401, context.exception.status)


if __name__ == "__main__":
    unittest.main()
