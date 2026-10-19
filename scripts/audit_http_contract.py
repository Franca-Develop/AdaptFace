import os
import re
import time

import numpy as np

from backend_client import BackendClient, BackendError


def main():
    url = os.environ["AUDIT_HTTP_URL"]
    schema_url = os.environ["AUDIT_MYSQL_URL"]
    if not re.fullmatch(r"http://127\.0\.0\.1:\d+", url) or url.endswith(":8080") \
            or not re.fullmatch(r"jdbc:mysql://127\.0\.0\.1:3306/adaptface_audit_[a-f0-9]+(?:\?.*)?", schema_url):
        raise RuntimeError("Use somente o servidor de auditoria isolado")
    client = BackendClient(url, os.environ["AUDIT_MACHINE_KEY"], "AUDIT-HTTP", timeout=3)
    for attempt in range(30):
        try:
            status = client.situacao_configuracao()
            break
        except BackendError:
            if attempt == 29:
                raise
            time.sleep(0.5)
    assert status["requiresSetup"] is True
    token = client.configurar_administrador({
        "institutionName": "Auditoria HTTP sintética", "name": "Administrador sintético HTTP",
        "cpf": "70000000001", "email": "http-admin@example.test", "password": "synthetic-password",
        "department": "Auditoria",
    }, os.environ["AUDIT_BOOTSTRAP_KEY"])
    institution_id = client.instituicoes(token)[0]["id"]
    vector = np.zeros(512, dtype=np.float32)
    vector[0] = 1
    registered = client.cadastrar_pessoa({
        "institutionId": institution_id, "name": "Identidade sintética HTTP", "cpf": "70000000002",
        "email": "http-student@example.test", "password": "synthetic-password", "userType": "STUDENT",
        "registrationNumber": 70001, "course": "Auditoria", "period": "Teste",
    }, vector, token)
    granted = client.validar_rosto(vector)
    assert granted.result == "GRANTED" and granted.name == "Identidade sintética HTTP"
    unknown = np.zeros(512, dtype=np.float32)
    unknown[1] = 1
    assert client.validar_rosto(unknown).result == "DENIED"
    client._request(f"/enrollments/{registered['enrollmentId']}/status", {"active": False},
                    {"Authorization": f"Bearer {token}"}, method="PATCH")
    denied = client.validar_rosto(vector)
    assert denied.result == "DENIED" and denied.reason == "Matrícula inativa"
    history = client._request("/access/logs", None, {"Authorization": f"Bearer {token}"}, method="GET")
    assert history["page"]["totalElements"] == 3
    assert [item["result"] for item in history["content"]] == ["DENIED", "DENIED", "GRANTED"]
    assert history["content"][1]["enrollmentId"] is None
    assert "faceEmbedding" not in str(history) and "password" not in str(history)
    print("Python -> HTTP -> Spring -> MySQL: cadastro, GRANTED, desconhecido, inativo e historico OK")


if __name__ == "__main__":
    main()
