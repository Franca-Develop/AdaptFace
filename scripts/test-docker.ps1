$ErrorActionPreference = "Stop"

Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    docker compose config --quiet
    if ($LASTEXITCODE -ne 0) { throw "Configuração do Compose inválida." }

    docker compose build
    if ($LASTEXITCODE -ne 0) { throw "Falha no build das imagens." }

    docker compose up -d
    if ($LASTEXITCODE -ne 0) { throw "Falha ao iniciar os serviços." }

    $deadline = (Get-Date).AddMinutes(3)
    do {
        try {
            $frontend = Invoke-WebRequest -UseBasicParsing "http://localhost:3000/" -TimeoutSec 3
            $openApi = Invoke-WebRequest -UseBasicParsing "http://localhost:8080/v3/api-docs" -TimeoutSec 3
            $vision = Invoke-RestMethod "http://localhost:3000/vision/health" -TimeoutSec 3
            if ($frontend.StatusCode -eq 200 -and $openApi.StatusCode -eq 200 -and $vision.status -eq "ok") { break }
        } catch {
            Start-Sleep -Seconds 3
        }
    } while ((Get-Date) -lt $deadline)

    if ($frontend.StatusCode -ne 200 -or $openApi.StatusCode -ne 200 -or $vision.status -ne "ok") {
        throw "Os serviços não ficaram saudáveis dentro do prazo."
    }

    docker compose ps
    Write-Host "Docker aprovado: frontend=200, OpenAPI=200, vision=ok"
} finally {
    Pop-Location
}
