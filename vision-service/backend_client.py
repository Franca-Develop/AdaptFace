import json
import ipaddress
import os
from http.client import HTTPException
from dataclasses import dataclass
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import Request, HTTPRedirectHandler, build_opener

import config


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, request, file_pointer, code, message, headers, new_url):
        return None


def urlopen(request, timeout):
    return build_opener(NoRedirect()).open(request, timeout=timeout)


def strict_json(raw):
    def unique_keys(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise ValueError("Chave JSON duplicada")
            result[key] = value
        return result

    def invalid_constant(value):
        raise ValueError("Número JSON inválido")

    return json.loads(raw, object_pairs_hook=unique_keys, parse_constant=invalid_constant)


class BackendError(RuntimeError):
    def __init__(self, message, status=None):
        super().__init__(message)
        self.status = status


@dataclass(frozen=True)
class AccessValidationResult:
    result: str
    name: str | None
    reason: str | None


class BackendClient:
    def __init__(
        self,
        base_url=config.BACKEND_URL,
        api_key=config.MACHINE_API_KEY,
        machine_model=config.MACHINE_MODEL,
        timeout=config.BACKEND_TIMEOUT,
    ):
        try:
            parsed = urlsplit(base_url)
            parsed.port
            host = parsed.hostname
            loopback = host == "localhost"
            if host and not loopback:
                try:
                    loopback = ipaddress.ip_address(host).is_loopback
                except ValueError:
                    pass
            trusted_http_hosts = {
                value.strip().lower()
                for value in os.getenv("ADAPTFACE_ALLOW_HTTP_HOSTS", "").split(",")
                if value.strip()
            }
            local_http = loopback or (isinstance(host, str) and host.lower() in trusted_http_hosts)
            if not host or parsed.username or parsed.password or parsed.query or parsed.fragment \
                    or parsed.scheme not in ("http", "https") or (parsed.scheme == "http" and not local_http):
                raise ValueError()
        except (ValueError, TypeError, AttributeError):
            raise BackendError("Use uma URL HTTPS válida, ou HTTP apenas para o backend local") from None
        self.base_url = base_url.rstrip("/")
        self.api_key = api_key
        self.machine_model = machine_model
        self.timeout = timeout

    def validar_rosto(self, embedding, entry=True) -> AccessValidationResult:
        if not self.api_key:
            raise BackendError("Terminal não configurado")
        response = self._request(
            "/access/validate",
            {
                "credentialType": "FACE",
                "faceEmbedding": embedding.tolist(),
                "faceModel": config.FACE_MODEL,
                "machineModel": self.machine_model,
                "entry": entry,
            },
            {"X-API-Key": self.api_key},
        )
        result, name, reason = response.get("result"), response.get("name"), response.get("reason")
        if result not in ("GRANTED", "DENIED"):
            raise BackendError("Resposta de validação inválida")
        if result == "GRANTED" and (not isinstance(name, str) or not name.strip()):
            raise BackendError("Resposta de identificação incompleta")
        if name is not None and (not isinstance(name, str) or len(name) > 255):
            raise BackendError("Resposta de identificação inválida")
        if reason is not None and (not isinstance(reason, str) or len(reason) > 255):
            raise BackendError("Motivo de identificação inválido")
        return AccessValidationResult(result, name, reason)

    def cadastrar_rosto(self, enrollment_id, embedding, token):
        if not token:
            raise BackendError("ADAPTFACE_JWT não configurado")
        return self._request(
            f"/enrollments/{enrollment_id}/credentials/face",
            {"faceEmbedding": embedding.tolist(), "faceModel": config.FACE_MODEL},
            {"Authorization": f"Bearer {token}"},
            method="PUT",
        )

    def situacao_configuracao(self):
        return self._request("/auth/setup-status", None, {}, method="GET", business_errors=True)

    def entrar(self, email, password):
        response = self._request("/auth/login", {"email": email, "password": password}, {}, business_errors=True)
        token = response.get("token")
        if not isinstance(token, str) or not token:
            raise BackendError("Resposta de autenticação inválida")
        return token

    def configurar_administrador(self, payload, key):
        response = self._request("/auth/bootstrap", payload, {"X-Bootstrap-Key": key}, business_errors=True)
        token = response.get("token")
        if not isinstance(token, str) or not token:
            raise BackendError("Resposta de autenticação inválida")
        return token

    def instituicoes(self, token):
        response = self._request("/institutions", None, {"Authorization": f"Bearer {token}"},
                                 method="GET", expecting_list=True, business_errors=True)
        if not all(isinstance(item, dict) and isinstance(item.get("id"), int)
                   and isinstance(item.get("name"), str) for item in response):
            raise BackendError("Lista de instituições inválida")
        return response

    def cadastrar_pessoa(self, user, embedding, token, rfid_tag=""):
        if not token:
            raise BackendError("Entre com uma conta administrativa", 401)
        return self._request("/registrations", {"user": user, "faceEmbedding": embedding.tolist(),
                             "faceModel": config.FACE_MODEL, "rfidTag": rfid_tag or None},
                             {"Authorization": f"Bearer {token}"},
                             business_errors=True)

    def _request(self, path, payload, headers, method="POST", expecting_list=False, business_errors=False):
        try:
            request = Request(
                f"{self.base_url}{path}",
                data=json.dumps(payload, allow_nan=False).encode("utf-8") if payload is not None else None,
                headers={"Content-Type": "application/json", **headers},
                method=method,
            )
            with urlopen(request, timeout=self.timeout) as response:
                raw = response.read(65537)
                if len(raw) > 65536:
                    raise BackendError("Resposta do serviço excede o limite permitido")
                data = strict_json(raw.decode("utf-8"))
                if not isinstance(data, list if expecting_list else dict):
                    raise BackendError("Resposta inválida do serviço")
                return data
        except HTTPError as error:
            message = "Não foi possível concluir a verificação"
            if business_errors:
                message = {401: "Email ou senha inválidos, ou sessão expirada. Entre novamente.",
                           403: "Esta conta não possui permissão administrativa.",
                           409: "Os dados informados já estão cadastrados."}.get(error.code,
                           "Não foi possível concluir o cadastro. Confira os dados e tente novamente.")
                if error.code == 400:
                    try:
                        details = strict_json(error.read(8192).decode("utf-8")).get("message")
                        if isinstance(details, str) and details.strip():
                            message = details[:350]
                    except (ValueError, AttributeError, OSError):
                        pass
            raise BackendError(message, error.code) from None
        except (URLError, TimeoutError, OSError, ValueError, TypeError, HTTPException):
            raise BackendError("Serviço de identificação indisponível") from None
