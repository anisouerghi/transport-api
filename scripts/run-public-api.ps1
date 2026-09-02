param(
    [ValidateSet("dev", "prod")]
    [string]$Profile = "dev"
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot 'runtime-config-display.ps1')

try {

    # ============================================================
    # PUBLIC API - Maven spring-boot:run
    # DEV  : 8081
    # PROD : 8081
    # ============================================================

    $secretsFile = Import-LocalSecrets -ScriptRoot $PSScriptRoot
    $Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

    $env:SPRING_PROFILES_ACTIVE = $Profile
    $env:APP_STORAGE_ROOT = $Root
    $env:APP_UPLOAD_PATH = Join-Path $Root 'data\attachments'
    $env:APP_QR_STORAGE_PATH = Join-Path $Root 'data\qr-codes'
    $env:PUBLIC_SERVER_PORT = "8081"

    if ($Profile -eq "prod") {
        $env:APP_QR_BASE_URL = "http://192.168.1.55/sig/"
        $env:APP_FRONTEND_PUBLIC_BASE_URL = "http://192.168.1.55/sig/"
        $env:CORS_ALLOWED_ORIGINS = "http://192.168.1.55,http://192.168.1.55:4200,http://localhost:4200"
    }
    else {
        $env:APP_QR_BASE_URL = "http://localhost:4200"
        $env:APP_FRONTEND_PUBLIC_BASE_URL = "http://localhost:4200"
        if (-not $env:CORS_ALLOWED_ORIGINS) {
            $env:CORS_ALLOWED_ORIGINS = "http://localhost:4200,http://localhost:4500"
        }
    }

    if (-not $env:GOOGLE_CLIENT_ID) {
        $env:GOOGLE_CLIENT_ID = "805628985152-kkg4l131p8jmpi7bek764icsp5ikmso7.apps.googleusercontent.com"
    }
    if (-not $env:GOOGLE_REDIRECT_URI) {
        $env:GOOGLE_REDIRECT_URI = "http://localhost:8081/login/oauth2/code/google"
    }
    if (-not $env:GOOGLE_FRONTEND_CALLBACK_URL) {
        $env:GOOGLE_FRONTEND_CALLBACK_URL = "http://localhost:4200/connexion/google/callback"
    }

    New-Item -ItemType Directory -Force -Path $env:APP_UPLOAD_PATH | Out-Null
    New-Item -ItemType Directory -Force -Path $env:APP_QR_STORAGE_PATH | Out-Null

    $mvn = Get-Command mvn -ErrorAction SilentlyContinue
    if (-not $mvn) {
        throw "Maven n'est pas disponible dans le PATH."
    }

    $mavenCmd = "mvn -pl public-api spring-boot:run"

    Clear-Host

    Write-RuntimeHeader "TRANSTU - PUBLIC API ($Profile / Maven)"

    Write-RuntimeSection "Execution"
    Write-EnvLine "Mode" "mvn spring-boot:run" -ValueColor Green
    Write-EnvLine "Commande" $mavenCmd
    Write-EnvLine "Maven" $mvn.Source
    Write-EnvLine "Racine projet" $Root
    Write-EnvLine "Secrets locaux" ($(if ($secretsFile) { $secretsFile } else { '(aucun - secrets.local.ps1 absent)' }))

    Write-RuntimeSection "Spring / Serveur"
    Write-EnvLine "SPRING_PROFILES_ACTIVE" $env:SPRING_PROFILES_ACTIVE -ValueColor Green
    Write-EnvLine "PUBLIC_SERVER_PORT" $env:PUBLIC_SERVER_PORT -ValueColor Green
    Write-EnvLine "URL API" "http://localhost:$($env:PUBLIC_SERVER_PORT)" -ValueColor Green

    Write-RuntimeSection "Stockage / Frontend / CORS"
    Write-EnvLine "APP_STORAGE_ROOT" $env:APP_STORAGE_ROOT
    Write-EnvLine "APP_UPLOAD_PATH" $env:APP_UPLOAD_PATH
    Write-EnvLine "APP_QR_STORAGE_PATH" $env:APP_QR_STORAGE_PATH
    Write-EnvLine "APP_QR_BASE_URL" (Get-EnvDisplayValue -Name 'APP_QR_BASE_URL')
    Write-EnvLine "APP_FRONTEND_PUBLIC_BASE_URL" (Get-EnvDisplayValue -Name 'APP_FRONTEND_PUBLIC_BASE_URL')
    Write-EnvLine "CORS_ALLOWED_ORIGINS" (Get-EnvDisplayValue -Name 'CORS_ALLOWED_ORIGINS')

    Write-GoogleOAuthBlock
    Write-SpringDatabaseBlock

    Write-RuntimeFooter

    Write-Host "Demarrage de public-api..." -ForegroundColor Yellow
    Write-Host ""

    Set-Location $Root
    & mvn -pl public-api spring-boot:run

    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "ERREUR DE DEMARRAGE PUBLIC API - Code Maven : $LASTEXITCODE" -ForegroundColor Red
        Write-Host ""
    }
    else {
        Write-Host ""
        Write-Host "Public API arretee." -ForegroundColor Yellow
    }
}
catch {
    Write-Host ""
    Write-Host "ERREUR : $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

Write-Host ""
Read-Host "Appuyez sur ENTREE pour fermer"
