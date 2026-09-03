param(
    [string]$JarPath = "",
    [switch]$Build
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot 'runtime-config-display.ps1')

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
    # ADMIN API - PRODUCTION (JAR)
    # Test prod : 192.168.1.55:8082
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

    $env:ADMIN_SERVER_PORT = "8082"

    # ------------------------------------------------------------
    # Stockage partage (absolu - identique public-api + admin-api)
    # ------------------------------------------------------------

    $env:APP_UPLOAD_PATH = "/var/data/transport/attachments"
    $env:APP_QR_STORAGE_PATH = "/var/data/transport/qr-codes"

    # ------------------------------------------------------------
    # QR / Frontend / CORS
    # ------------------------------------------------------------

    # QR + liens e-mail → front voyageur (sous-repertoire /sig/), PAS l'API :8081
    # Le code ajoute report/{uuid} et report-followup/{uuid} a APP_*_BASE_URL
    $env:APP_QR_BASE_URL = "http://${HostIp}/sig/"
    $env:APP_FRONTEND_PUBLIC_BASE_URL = "http://${HostIp}/sig/"
    $env:CORS_ALLOWED_ORIGINS = "http://${HostIp},http://${HostIp}:4200,http://${HostIp}:4500,http://localhost:4200,http://localhost:4500"

    # ------------------------------------------------------------
    # Build JAR (optionnel)
    # ------------------------------------------------------------

    if ($Build) {
        Invoke-ModulePackage -ModuleName 'admin-api' -ProjectRoot $Root
    }

    # ------------------------------------------------------------
    # JAR
    # ------------------------------------------------------------

    if ([string]::IsNullOrWhiteSpace($JarPath)) {
        $JarPath = Resolve-ModuleJar -ModuleName 'admin-api' -ProjectRoot $Root
        if (-not $JarPath) {
            throw @"
JAR introuvable dans admin-api\target\
Attendu : admin-api.jar ou admin-api-*.jar (Spring Boot repackage)

Construire d'abord (choisir une option) :

  1) Build + run en une commande :
     .\scripts\run-admin-prod.ps1 -Build

  2) Build seulement :
     mvn -pl admin-api -am clean package -DskipTests

  3) Puis relancer sans rebuild :
     .\scripts\run-admin-prod.ps1
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
    # Affichage configuration effective
    # ------------------------------------------------------------

    Clear-Host

    Write-RuntimeHeader "TRANSTU - ADMIN API (PRODUCTION / JAR)"

    Write-RuntimeSection "Execution"
    Write-EnvLine "Mode" "java -jar" -ValueColor Green
    Write-EnvLine "Build avant run" ($(if ($Build) { 'OUI (mvn clean package)' } else { 'NON - JAR existant dans target/' }))
    Write-EnvLine "Commande" "java -jar `"$JarPath`""
    Write-EnvLine "JAR" $JarPath

    Write-RuntimeSection "Spring / Serveur"
    Write-EnvLine "SPRING_PROFILES_ACTIVE" $env:SPRING_PROFILES_ACTIVE -ValueColor Green
    Write-EnvLine "ADMIN_SERVER_PORT" $env:ADMIN_SERVER_PORT -ValueColor Green
    Write-EnvLine "URL API" "http://${HostIp}:$($env:ADMIN_SERVER_PORT)" -ValueColor Green

    Write-RuntimeSection "Stockage / Frontend / CORS"
    Write-EnvLine "APP_UPLOAD_PATH" $env:APP_UPLOAD_PATH
    Write-EnvLine "APP_QR_STORAGE_PATH" $env:APP_QR_STORAGE_PATH
    Write-EnvLine "APP_QR_BASE_URL" $env:APP_QR_BASE_URL
    Write-EnvLine "APP_FRONTEND_PUBLIC_BASE_URL" $env:APP_FRONTEND_PUBLIC_BASE_URL
    Write-EnvLine "CORS_ALLOWED_ORIGINS" $env:CORS_ALLOWED_ORIGINS

    Write-SpringDatabaseBlock

    Write-RuntimeFooter

    # ------------------------------------------------------------
    # Verifications
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
    Write-Host "Demarrage de admin-api.jar (profil prod)..." -ForegroundColor Yellow
    Write-Host ""

    Set-Location $Root

    & java -jar $JarPath

    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "==================================================" -ForegroundColor Red
        Write-Host "     ERREUR DE DEMARRAGE ADMIN API (PROD)" -ForegroundColor Red
        Write-Host "==================================================" -ForegroundColor Red
        Write-Host ""
        Write-Host "Code retour Java : $LASTEXITCODE" -ForegroundColor Red
        Write-Host ""
    }
    else {
        Write-Host ""
        Write-Host "Admin API arretee." -ForegroundColor Yellow
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
