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

    # Secrets locaux (gitignore) : copier secrets.local.ps1.example -> secrets.local.ps1
    $secretsFile = Import-LocalSecrets -ScriptRoot $PSScriptRoot

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
    # Le code ajoute report/{uuid} et report-followup/{uuid} a APP_*_BASE_URL
    $env:APP_QR_BASE_URL = "http://${HostIp}/sig/"
    $env:APP_FRONTEND_PUBLIC_BASE_URL = "http://${HostIp}/sig/"
    $env:CORS_ALLOWED_ORIGINS = "http://${HostIp},http://${HostIp}:4200,http://${HostIp}:4500,http://localhost:4200,http://localhost:4500"

    # ------------------------------------------------------------
    # Google OAuth (secret via GOOGLE_CLIENT_SECRET - ne jamais committer)
    # ------------------------------------------------------------

    if (-not $env:GOOGLE_CLIENT_ID) {
        $env:GOOGLE_CLIENT_ID = "805628985152-kkg4l131p8jmpi7bek764icsp5ikmso7.apps.googleusercontent.com"
    }
    if (-not $env:GOOGLE_REDIRECT_URI) {
        $env:GOOGLE_REDIRECT_URI = "http://${HostIp}:8081/login/oauth2/code/google"
    }
    if (-not $env:GOOGLE_FRONTEND_CALLBACK_URL) {
        $env:GOOGLE_FRONTEND_CALLBACK_URL = "http://${HostIp}/sig/connexion/google/callback"
    }
    if (-not $env:GOOGLE_CLIENT_SECRET) {
        Write-Host ""
        Write-Host "!!! GOOGLE_CLIENT_SECRET manquant - connexion Google desactivee (HTTP 503) !!!" -ForegroundColor Red
        Write-Host "    Copiez secrets.local.ps1.example -> secrets.local.ps1" -ForegroundColor Yellow
        Write-Host ""
    }

    # OTP e-mail (connexion voyageur — activé par défaut)
    if (-not $env:APP_AUTH_OTP_ENABLED) { $env:APP_AUTH_OTP_ENABLED = "true" }
    if (-not $env:APP_AUTH_OTP_LENGTH) { $env:APP_AUTH_OTP_LENGTH = "6" }
    if (-not $env:APP_AUTH_OTP_EXPIRATION_MINUTES) { $env:APP_AUTH_OTP_EXPIRATION_MINUTES = "5" }
    if (-not $env:APP_AUTH_OTP_MAX_ATTEMPTS) { $env:APP_AUTH_OTP_MAX_ATTEMPTS = "5" }
    if (-not $env:APP_AUTH_OTP_RESEND_DELAY_SECONDS) { $env:APP_AUTH_OTP_RESEND_DELAY_SECONDS = "60" }

    # ------------------------------------------------------------
    # Build JAR (optionnel)
    # ------------------------------------------------------------

    if ($Build) {
        Invoke-ModulePackage -ModuleName 'public-api' -ProjectRoot $Root
    }

    # ------------------------------------------------------------
    # JAR
    # ------------------------------------------------------------

    if ([string]::IsNullOrWhiteSpace($JarPath)) {
        $JarPath = Resolve-ModuleJar -ModuleName 'public-api' -ProjectRoot $Root
        if (-not $JarPath) {
            throw @"
JAR introuvable dans public-api\target\
Attendu : public-api.jar ou public-api-*.jar (Spring Boot repackage)

Construire d'abord (choisir une option) :

  1) Build + run en une commande :
     .\scripts\run-public-prod.ps1 -Build

  2) Build seulement :
     mvn -pl public-api -am clean package -DskipTests

  3) Puis relancer sans rebuild :
     .\scripts\run-public-prod.ps1
"@
        }
    }
    else {
        $JarPath = (Resolve-Path $JarPath).Path
    }

    # ------------------------------------------------------------
    # Repertoires stockage
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

    Write-RuntimeHeader "TRANSTU - PUBLIC API (PRODUCTION / JAR)"

    Write-RuntimeSection "Execution"
    Write-EnvLine "Mode" "java -jar" -ValueColor Green
    Write-EnvLine "Build avant run" ($(if ($Build) { 'OUI (mvn clean package)' } else { 'NON - JAR existant dans target/' }))
    Write-EnvLine "Commande" "java -jar `"$JarPath`"" 
    Write-EnvLine "JAR" $JarPath
    Write-EnvLine "Secrets locaux" ($(if ($secretsFile) { $secretsFile } else { '(aucun - secrets.local.ps1 absent)' }))

    Write-RuntimeSection "Spring / Serveur"
    Write-EnvLine "SPRING_PROFILES_ACTIVE" $env:SPRING_PROFILES_ACTIVE -ValueColor Green
    Write-EnvLine "PUBLIC_SERVER_PORT" $env:PUBLIC_SERVER_PORT -ValueColor Green
    Write-EnvLine "URL API" "http://${HostIp}:$($env:PUBLIC_SERVER_PORT)" -ValueColor Green

    Write-RuntimeSection "Stockage / Frontend / CORS"
    Write-EnvLine "APP_UPLOAD_PATH" $env:APP_UPLOAD_PATH
    Write-EnvLine "APP_QR_STORAGE_PATH" $env:APP_QR_STORAGE_PATH
    Write-EnvLine "APP_QR_BASE_URL" $env:APP_QR_BASE_URL
    Write-EnvLine "APP_FRONTEND_PUBLIC_BASE_URL" $env:APP_FRONTEND_PUBLIC_BASE_URL
    Write-EnvLine "CORS_ALLOWED_ORIGINS" $env:CORS_ALLOWED_ORIGINS

    Write-GoogleOAuthBlock
    Write-OtpAuthBlock
    Write-SpringDatabaseBlock

    Write-RuntimeSection "Frontend voyageur (OTP)"
    Write-Host "  OTP cote UI necessite le frontend a jour (ecran /connexion)." -ForegroundColor Yellow
    Write-Host "  DEV  : .\scripts\run-frontend-dev.ps1  (+ run-public-api.ps1)" -ForegroundColor Gray
    Write-Host "  PROD : .\scripts\build-frontend-prod.ps1 -Deploy" -ForegroundColor Gray
    Write-Host "  URL  : http://${HostIp}/sig/connexion" -ForegroundColor Gray

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
