param(
    [string]$JarPath = ""
)

$ErrorActionPreference = "Stop"

function Resolve-ModuleJar {
    param(
        [Parameter(Mandatory = $true)][string]$ModuleName,
        [Parameter(Mandatory = $true)][string]$ProjectRoot
    )

    $targetDir = Join-Path $ProjectRoot "$ModuleName\target"
    $fixedName = Join-Path $targetDir "$ModuleName.jar"

    if (Test-Path -LiteralPath $fixedName) {
        return $fixedName
    }

    $candidate = Get-ChildItem -Path $targetDir -Filter "$ModuleName-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Extension -eq '.jar' -and $_.Name -notlike '*.original' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ($candidate) {
        return $candidate.FullName
    }

    return $null
}

try {

    # ============================================================
    # PUBLIC API - PRODUCTION (JAR)
    # Test prod : 192.168.1.55:8081
    # ============================================================

    $Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
    $HostIp = "192.168.1.55"

    # ------------------------------------------------------------
    # Profil Spring
    # ------------------------------------------------------------

    $env:SPRING_PROFILES_ACTIVE = "prod"

    # ------------------------------------------------------------
    # Serveur
    # ------------------------------------------------------------

    $env:PUBLIC_SERVER_PORT = "8081"

    # ------------------------------------------------------------
    # Stockage partage (absolu - identique public-api + admin-api)
    # ------------------------------------------------------------

    $env:APP_UPLOAD_PATH = "/var/data/transport/attachments"
    $env:APP_QR_STORAGE_PATH = "/var/data/transport/qr-codes"

    # ------------------------------------------------------------
    # QR / Frontend / CORS
    # ------------------------------------------------------------

    # QR + liens e-mail → front voyageur (sous-repertoire /sig/), PAS l'API :8081
    # Le code ajoute /#/report/{uuid} et /#/report-followup/{uuid} (Hash Routing)
    $env:APP_QR_BASE_URL = "http://${HostIp}/sig/"
    $env:APP_FRONTEND_PUBLIC_BASE_URL = "http://${HostIp}/sig/"
    $env:CORS_ALLOWED_ORIGINS = "http://${HostIp},http://${HostIp}:4200,http://${HostIp}:4500,http://localhost:4200,http://localhost:4500"

    # ------------------------------------------------------------
    # Base de données / JWT (surcharge uniquement si deja definies)
    # Sinon : defauts Spring (application-prod.properties / application.properties)
    # ------------------------------------------------------------

    $databaseUrlDisplay = if ($env:DATABASE_URL) { $env:DATABASE_URL } else { '(defaut Spring - localhost:3306/transport_reporting)' }
    $databaseUserDisplay = if ($env:DATABASE_USERNAME) { $env:DATABASE_USERNAME } else { '(defaut Spring - root)' }
    $databasePasswordDisplay = if ($env:DATABASE_PASSWORD) { '********' } else { '(defaut Spring - vide)' }
    $jwtSecretDisplay = if ($env:JWT_SECRET) { '********' } else { '(defaut Spring - application.properties)' }
    $jwtExpirationDisplay = if ($env:JWT_EXPIRATION_MS) { $env:JWT_EXPIRATION_MS } else { '(defaut Spring - 86400000)' }

    # ------------------------------------------------------------
    # JAR
    # ------------------------------------------------------------

    if ([string]::IsNullOrWhiteSpace($JarPath)) {
        $JarPath = Resolve-ModuleJar -ModuleName 'public-api' -ProjectRoot $Root
        if (-not $JarPath) {
            throw @"
JAR introuvable dans public-api\target\
Attendu : public-api.jar ou public-api-*.jar (Spring Boot repackage)

Construire d'abord :
  mvn -pl public-api -am clean package -DskipTests

Ou depuis Maven portable :
  C:\Users\ThInKpAd11\Desktop\Vm\Back\apache-maven-3.9.6\bin\mvn.cmd -pl public-api -am clean package -DskipTests
"@
        }
    }
    else {
        $JarPath = (Resolve-Path $JarPath).Path
    }

    # ------------------------------------------------------------
    # Répertoires stockage
    # ------------------------------------------------------------

    foreach ($dir in @($env:APP_UPLOAD_PATH, $env:APP_QR_STORAGE_PATH)) {
        try {
            New-Item -ItemType Directory -Force -Path $dir | Out-Null
        }
        catch {
            Write-Host "Avertissement : impossible de creer $dir - $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }

    # ------------------------------------------------------------
    # Affichage
    # ------------------------------------------------------------

    Clear-Host

    Write-Host ""
    Write-Host "==================================================" -ForegroundColor Cyan
    Write-Host "        TRANSTU - PUBLIC API (PRODUCTION)" -ForegroundColor Cyan
    Write-Host "==================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Profil                  : $env:SPRING_PROFILES_ACTIVE"
    Write-Host "Port                    : $env:PUBLIC_SERVER_PORT"
    Write-Host "JAR                     : $JarPath"
    Write-Host ""
    Write-Host "APP_UPLOAD_PATH         : $env:APP_UPLOAD_PATH"
    Write-Host "APP_QR_STORAGE_PATH     : $env:APP_QR_STORAGE_PATH"
    Write-Host "APP_QR_BASE_URL         : $env:APP_QR_BASE_URL"
    Write-Host "APP_FRONTEND_PUBLIC_BASE_URL : $env:APP_FRONTEND_PUBLIC_BASE_URL"
    Write-Host "CORS_ALLOWED_ORIGINS    : $env:CORS_ALLOWED_ORIGINS"
    Write-Host ""
    Write-Host "DATABASE_URL            : $databaseUrlDisplay"
    Write-Host "DATABASE_USERNAME       : $databaseUserDisplay"
    Write-Host "DATABASE_PASSWORD       : $databasePasswordDisplay"
    Write-Host "JWT_SECRET              : $jwtSecretDisplay"
    Write-Host "JWT_EXPIRATION_MS       : $jwtExpirationDisplay"
    Write-Host ""
    Write-Host "API : http://${HostIp}:$env:PUBLIC_SERVER_PORT" -ForegroundColor Green
    Write-Host ""
    Write-Host "==================================================" -ForegroundColor Cyan
    Write-Host ""

    # ------------------------------------------------------------
    # Vérifications
    # ------------------------------------------------------------

    $java = Get-Command java -ErrorAction SilentlyContinue
    if (-not $java) {
        throw "Java n'est pas disponible dans le PATH."
    }

    if (-not (Test-Path -LiteralPath $JarPath)) {
        throw "JAR introuvable : $JarPath"
    }

    Write-Host "Java : $($java.Source)" -ForegroundColor DarkGray
    Write-Host ""
    Write-Host "Demarrage de public-api.jar (profil prod)..." -ForegroundColor Yellow
    Write-Host ""

    Set-Location $Root

    & java -jar $JarPath

    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "==================================================" -ForegroundColor Red
        Write-Host "    ERREUR DE DEMARRAGE PUBLIC API (PROD)" -ForegroundColor Red
        Write-Host "==================================================" -ForegroundColor Red
        Write-Host ""
        Write-Host "Code retour Java : $LASTEXITCODE" -ForegroundColor Red
        Write-Host ""
    }
    else {
        Write-Host ""
        Write-Host "Public API arretee." -ForegroundColor Yellow
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
