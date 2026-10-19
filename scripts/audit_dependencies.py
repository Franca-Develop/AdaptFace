import importlib.metadata
import json
import sys
from datetime import datetime, timezone
from pathlib import Path
from urllib.request import Request, urlopen


def collect_packages(maven_file):
    packages = {}
    for distribution in importlib.metadata.distributions():
        name = distribution.metadata["Name"]
        if name:
            normalized = name.lower().replace("_", "-")
            packages[("PyPI", normalized)] = importlib.metadata.version(name)
    for line in Path(maven_file).read_text(encoding="utf-8").splitlines():
        parts = line.strip().split()[0].split(":") if line.strip() else []
        if len(parts) == 5 and parts[2] == "jar" and parts[4] in ("compile", "runtime"):
            packages[("Maven", parts[0] + ":" + parts[1])] = parts[3]
    return [{"package": {"ecosystem": ecosystem, "name": name}, "version": version}
            for (ecosystem, name), version in sorted(packages.items())]


def query(packages):
    findings = []
    for start in range(0, len(packages), 100):
        pending = packages[start:start + 100]
        while pending:
            request = Request("https://api.osv.dev/v1/querybatch",
                              json.dumps({"queries": pending}).encode(),
                              {"Content-Type": "application/json"}, method="POST")
            with urlopen(request, timeout=30) as response:
                results = json.load(response)["results"]
            if len(results) != len(pending):
                raise ValueError("Resposta OSV incompleta")
            next_page = []
            for package, result in zip(pending, results):
                if result.get("vulns"):
                    findings.append({"package": package["package"], "version": package["version"],
                                     "ids": sorted(item["id"] for item in result["vulns"])})
                if result.get("next_page_token"):
                    next_page.append({**package, "page_token": result["next_page_token"]})
            pending = next_page
    return findings


if __name__ == "__main__":
    packages = collect_packages(sys.argv[1])
    try:
        findings = query(packages)
    except Exception:
        print(json.dumps({"complete": False, "packages": len(packages),
                          "message": "Consulta OSV indisponível ou inválida"}))
        sys.exit(2)
    print(json.dumps({"complete": True, "checkedAt": datetime.now(timezone.utc).isoformat(),
                      "packages": len(packages), "findings": findings}, indent=2))
