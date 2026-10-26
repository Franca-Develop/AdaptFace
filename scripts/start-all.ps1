$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env"

if (-not (Test-Path $envFile)) {
    function New-Secret([int]$bytes = 32) {
        $buffer = [byte[]]::new($bytes)
        [System.Security.Cryptography.RandomNumberGenerator]::Fill($buffer)
        [Convert]::ToBase64String($buffer)
    }

    function Get-ContainerValue([string]$container, [string]$name) {
        $existing = docker inspect $container --format '{{range .Config.Env}}{{println .}}{{end}}' 2>$null
        (($existing | Where-Object { $_ -like "$name=*" }) -replace "^$name=", "") | Select-Object -First 1
    }

    $mysqlPassword = Get-ContainerValue "adaptface-main-mysql-1" "MYSQL_PASSWORD"
    $rootPassword = Get-ContainerValue "adaptface-main-mysql-1" "MYSQL_ROOT_PASSWORD"
    $jwtSecret = Get-ContainerValue "adaptface-main-api-1" "JWT_SECRET"
    $machineKey = Get-ContainerValue "adaptface-main-api-1" "MACHINE_API_KEY"
    $bootstrapKey = Get-ContainerValue "adaptface-main-api-1" "BOOTSTRAP_KEY"

    @(
        "MYSQL_APP_PASSWORD=$(if ($mysqlPassword) { $mysqlPassword } else { New-Secret })"
        "MYSQL_ROOT_PASSWORD=$(if ($rootPassword) { $rootPassword } else { New-Secret })"
        "JWT_SECRET=$(if ($jwtSecret) { $jwtSecret } else { New-Secret 48 })"
        "MACHINE_API_KEY=$(if ($machineKey) { $machineKey } else { New-Secret })"
        "BOOTSTRAP_KEY=$(if ($bootstrapKey) { $bootstrapKey } else { New-Secret })"
        "API_PORT=8080"
        "FRONTEND_PORT=3000"
        "CORS_ALLOWED_ORIGINS=http://localhost:3000,http://127.0.0.1:3000"
    ) | Set-Content -LiteralPath $envFile -Encoding utf8
}

docker compose --project-directory $root up -d --build
if ($LASTEXITCODE -ne 0) {
    throw "Não foi possível iniciar o AdaptFace."
}

Start-Process "http://localhost:3000"
