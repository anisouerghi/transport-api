param(
    [int]$Port = 4200
)

$ErrorActionPreference = "Stop"

$FrontendRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\transport-signalement-frontend')).Path
$ApiRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

Write-Host ""
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "  TRANSTU - FRONTEND VOYAGEUR (DEV)" -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Frontend : http://localhost:$Port/connexion" -ForegroundColor Green
Write-Host "API      : http://localhost:8081 (lancer run-public-api.ps1)" -ForegroundColor Green
Write-Host "OTP      : active si APP_AUTH_OTP_ENABLED=true sur public-api" -ForegroundColor Green
Write-Host ""
Write-Host "Ordre de demarrage DEV :" -ForegroundColor Yellow
Write-Host "  1) .\scripts\run-public-api.ps1" -ForegroundColor Gray
Write-Host "  2) .\scripts\run-frontend-dev.ps1" -ForegroundColor Gray
Write-Host ""

if (-not (Test-Path (Join-Path $FrontendRoot 'node_modules'))) {
    Write-Host "Installation npm (premiere fois)..." -ForegroundColor Yellow
    Push-Location $FrontendRoot
    try {
        npm install
        if ($LASTEXITCODE -ne 0) { throw "npm install a echoue (code $LASTEXITCODE)." }
    }
    finally {
        Pop-Location
    }
}

Push-Location $FrontendRoot
try {
    & npm start -- --port $Port
}
finally {
    Pop-Location
}

Write-Host ""
Read-Host "Appuyez sur ENTREE pour fermer"
