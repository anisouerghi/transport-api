param(
    [ValidateSet("dev", "prod")]
    [string]$Profile = "dev"
)

$ErrorActionPreference = "Stop"

try {

    # ============================================================
    # PUBLIC API
    # DEV  : 8081
    # PROD : 8081 par défaut
    # ============================================================

    $Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

    # ------------------------------------------------------------
    # Profil Spring
    # ------------------------------------------------------------

    $env:SPRING_PROFILES_ACTIVE = $Profile

    # ------------------------------------------------------------
    # Stockage partagé
    # ------------------------------------------------------------

    $env:APP_STORAGE_ROOT = $Root
    $env:APP_UPLOAD_PATH = Join-Path $Root 'data\attachments'
    $env:APP_QR_STORAGE_PATH = Join-Path $Root 'data\qr-codes'

    New-Item -ItemType Directory -Force -Path `
        $env:APP_UPLOAD_PATH | Out-Null

    New-Item -ItemType Directory -Force -Path `
        $env:APP_QR_STORAGE_PATH | Out-Null

    # ------------------------------------------------------------
    # Port Public API
    # ------------------------------------------------------------

    if ($Profile -eq "dev") {
        $env:PUBLIC_SERVER_PORT = "8081"
    }
    else {
        $env:PUBLIC_SERVER_PORT = "8081"
    }

    # ------------------------------------------------------------
    # Affichage
    # ------------------------------------------------------------

    Clear-Host

    Write-Host ""
    Write-Host "==================================================" -ForegroundColor Cyan
    Write-Host "              TRANSTU - PUBLIC API" -ForegroundColor Cyan
    Write-Host "==================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Profil              : $env:SPRING_PROFILES_ACTIVE"
    Write-Host "Port                : $env:PUBLIC_SERVER_PORT"
    Write-Host "APP_STORAGE_ROOT    : $env:APP_STORAGE_ROOT"
    Write-Host "APP_UPLOAD_PATH     : $env:APP_UPLOAD_PATH"
    Write-Host "APP_QR_STORAGE_PATH : $env:APP_QR_STORAGE_PATH"
    Write-Host ""
    Write-Host "API : http://localhost:$env:PUBLIC_SERVER_PORT" -ForegroundColor Green
    Write-Host ""
    Write-Host "==================================================" -ForegroundColor Cyan
    Write-Host ""

    # ------------------------------------------------------------
    # Racine du projet
    # ------------------------------------------------------------

    Set-Location $Root

    # ------------------------------------------------------------
    # Vérification Maven
    # ------------------------------------------------------------

    $mvn = Get-Command mvn -ErrorAction SilentlyContinue

    if (-not $mvn) {
        throw "Maven n'est pas disponible dans le PATH."
    }

    Write-Host "Maven : $($mvn.Source)" -ForegroundColor DarkGray
    Write-Host ""
    Write-Host "Démarrage de public-api..." -ForegroundColor Yellow
    Write-Host ""

    # ------------------------------------------------------------
    # IMPORTANT :
    # Pas de -am.
    #
    # public-api dépend déjà de common.
    # -am provoquerait le lancement du parent
    # transport-backend par spring-boot:run.
    # ------------------------------------------------------------

    & mvn -pl public-api spring-boot:run

    if ($LASTEXITCODE -ne 0) {

        Write-Host ""
        Write-Host "==================================================" -ForegroundColor Red
        Write-Host "       ERREUR DE DEMARRAGE PUBLIC API" -ForegroundColor Red
        Write-Host "==================================================" -ForegroundColor Red
        Write-Host ""
        Write-Host "Code retour Maven : $LASTEXITCODE" -ForegroundColor Red
        Write-Host ""

    }
    else {

        Write-Host ""
        Write-Host "Public API arrêtée." -ForegroundColor Yellow
    }

}
catch {

    Write-Host ""
    Write-Host "==================================================" -ForegroundColor Red
    Write-Host "                  ERREUR" -ForegroundColor Red
    Write-Host "==================================================" -ForegroundColor Red
    Write-Host ""
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ""
}

Write-Host ""
Read-Host "Appuyez sur ENTREE pour fermer"