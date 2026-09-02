param(
    [ValidateSet("dev", "prod")]
    [string]$Profile = "dev"
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot 'runtime-config-display.ps1')

try {

    # ============================================================
    # ADMIN API - Maven spring-boot:run
    # DEV  : 8082
    # PROD : 8082
    # ============================================================

    $Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

    $env:SPRING_PROFILES_ACTIVE = $Profile
    $env:APP_STORAGE_ROOT = $Root
    $env:APP_UPLOAD_PATH = Join-Path $Root 'data\attachments'
    $env:APP_QR_STORAGE_PATH = Join-Path $Root 'data\qr-codes'
    $env:ADMIN_SERVER_PORT = "8082"

    if ($Profile -eq "prod") {
        $env:APP_QR_BASE_URL = "http://192.168.1.55/sig/"
        $env:APP_FRONTEND_PUBLIC_BASE_URL = "http://192.168.1.55/sig/"
        $env:CORS_ALLOWED_ORIGINS = "http://192.168.1.55,http://192.168.1.55:4500,http://localhost:4500"
    }
    else {
        if (-not $env:CORS_ALLOWED_ORIGINS) {
            $env:CORS_ALLOWED_ORIGINS = "http://localhost:4200,http://localhost:4500"
        }
    }

    New-Item -ItemType Directory -Force -Path $env:APP_UPLOAD_PATH | Out-Null
    New-Item -ItemType Directory -Force -Path $env:APP_QR_STORAGE_PATH | Out-Null

    $mvn = Get-Command mvn -ErrorAction SilentlyContinue
    if (-not $mvn) {
        throw "Maven n'est pas disponible dans le PATH."
    }

    $mavenCmd = "mvn -pl admin-api spring-boot:run"

    Clear-Host

    Write-RuntimeHeader "TRANSTU - ADMIN API ($Profile / Maven)"

    Write-RuntimeSection "Execution"
    Write-EnvLine "Mode" "mvn spring-boot:run" -ValueColor Green
    Write-EnvLine "Commande" $mavenCmd
    Write-EnvLine "Maven" $mvn.Source
    Write-EnvLine "Racine projet" $Root

    Write-RuntimeSection "Spring / Serveur"
    Write-EnvLine "SPRING_PROFILES_ACTIVE" $env:SPRING_PROFILES_ACTIVE -ValueColor Green
    Write-EnvLine "ADMIN_SERVER_PORT" $env:ADMIN_SERVER_PORT -ValueColor Green
    Write-EnvLine "URL API" "http://localhost:$($env:ADMIN_SERVER_PORT)" -ValueColor Green

    Write-RuntimeSection "Stockage / CORS"
    Write-EnvLine "APP_STORAGE_ROOT" $env:APP_STORAGE_ROOT
    Write-EnvLine "APP_UPLOAD_PATH" $env:APP_UPLOAD_PATH
    Write-EnvLine "APP_QR_STORAGE_PATH" $env:APP_QR_STORAGE_PATH
    Write-EnvLine "APP_QR_BASE_URL" (Get-EnvDisplayValue -Name 'APP_QR_BASE_URL' -DefaultDisplay '(defaut profil)')
    Write-EnvLine "APP_FRONTEND_PUBLIC_BASE_URL" (Get-EnvDisplayValue -Name 'APP_FRONTEND_PUBLIC_BASE_URL' -DefaultDisplay '(defaut profil)')
    Write-EnvLine "CORS_ALLOWED_ORIGINS" (Get-EnvDisplayValue -Name 'CORS_ALLOWED_ORIGINS')

    Write-SpringDatabaseBlock

    Write-RuntimeFooter

    Write-Host "Demarrage de admin-api..." -ForegroundColor Yellow
    Write-Host ""

    Set-Location $Root
    & mvn -pl admin-api spring-boot:run

    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "ERREUR DE DEMARRAGE ADMIN API - Code Maven : $LASTEXITCODE" -ForegroundColor Red
        Write-Host ""
    }
    else {
        Write-Host ""
        Write-Host "Admin API arretee." -ForegroundColor Yellow
    }
}
catch {
    Write-Host ""
    Write-Host "ERREUR : $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

Write-Host ""
Read-Host "Appuyez sur ENTREE pour fermer"
