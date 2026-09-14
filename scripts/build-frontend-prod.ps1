param(
    [switch]$Deploy,
    [string]$DeployPath = "",
    [string]$HostIp = "192.168.1.55"
)

$ErrorActionPreference = "Stop"

$FrontendRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\transport-signalement-frontend')).Path
$DistDir = Join-Path $FrontendRoot 'dist\transport-signalement-frontend\browser'

Write-Host ""
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "  TRANSTU - BUILD FRONTEND PROD (/sig/)" -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Projet  : $FrontendRoot" -ForegroundColor Gray
Write-Host "Commande: npm run build:sig (--base-href /sig/)" -ForegroundColor Gray
Write-Host ""

if (-not (Test-Path (Join-Path $FrontendRoot 'node_modules'))) {
    Write-Host "Installation npm..." -ForegroundColor Yellow
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
    npm run build:sig
    if ($LASTEXITCODE -ne 0) { throw "Build Angular echoue (code $LASTEXITCODE)." }
}
finally {
    Pop-Location
}

# Verifier que le bundle OTP est present
$authChunk = Get-ChildItem -Path $DistDir -Filter 'chunk-*.js' -ErrorAction SilentlyContinue |
    Where-Object { (Select-String -Path $_.FullName -Pattern 'OTP_REQUIRED' -Quiet) }
if (-not $authChunk) {
    Write-Host ""
    Write-Host "AVERTISSEMENT : OTP_REQUIRED introuvable dans le build." -ForegroundColor Red
    Write-Host "Le deploiement risque de ne pas afficher l'ecran OTP." -ForegroundColor Red
}
else {
    Write-Host ""
    Write-Host "Build OK (OTP detecte dans $($authChunk.Name))." -ForegroundColor Green
}

Write-Host ""
Write-Host "Artefact : $DistDir" -ForegroundColor Green
Write-Host "URL prod : http://${HostIp}/sig/connexion" -ForegroundColor Green
Write-Host ""

# Mettre a jour config.json embarque si besoin (API sur le meme host)
$configPath = Join-Path $DistDir 'assets\config\config.json'
if (Test-Path -LiteralPath $configPath) {
    $json = Get-Content -Raw -LiteralPath $configPath | ConvertFrom-Json
    $expectedApi = "http://${HostIp}:8081"
    $changed = $false
    if ($json.apiBaseUrl -ne $expectedApi) {
        $json.apiBaseUrl = $expectedApi
        $changed = $true
        Write-Host "config.json mis a jour : apiBaseUrl=$expectedApi" -ForegroundColor Yellow
    }
    # Site key publique PROD (jamais la secret) — injectee via env au build/deploy
    if ($env:CLOUDFLARE_SITE_KEY -and $json.cloudflareSiteKey -ne $env:CLOUDFLARE_SITE_KEY) {
        $json.cloudflareSiteKey = $env:CLOUDFLARE_SITE_KEY
        $changed = $true
        Write-Host "config.json mis a jour : cloudflareSiteKey (depuis CLOUDFLARE_SITE_KEY)" -ForegroundColor Yellow
    }
    if ($null -eq $json.cloudflareEnabled) {
        $json.cloudflareEnabled = $true
        $changed = $true
    }
    if ($env:CLOUDFLARE_ENABLED -eq 'false') {
        $json.cloudflareEnabled = $false
        $changed = $true
    }
    elseif ($env:CLOUDFLARE_ENABLED -eq 'true') {
        $json.cloudflareEnabled = $true
        $changed = $true
    }
    if ($changed) {
        $json | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath $configPath -Encoding UTF8
    }
    if (-not $json.cloudflareSiteKey) {
        Write-Host "AVERTISSEMENT : cloudflareSiteKey vide en PROD — definir CLOUDFLARE_SITE_KEY avant build ou editer assets/config/config.json sur le serveur." -ForegroundColor Yellow
    }
}

if ($Deploy) {
    if ([string]::IsNullOrWhiteSpace($DeployPath)) {
        $candidates = @(
            'C:\xampp\htdocs\sig',
            'C:\Apache24\htdocs\sig',
            'C:\inetpub\wwwroot\sig'
        )
        foreach ($c in $candidates) {
            if (Test-Path (Split-Path $c -Parent)) {
                $DeployPath = $c
                break
            }
        }
    }

    if ([string]::IsNullOrWhiteSpace($DeployPath)) {
        Write-Host "DeployPath non trouve. Copiez manuellement :" -ForegroundColor Yellow
        Write-Host "  $DistDir" -ForegroundColor Gray
        Write-Host "  -> repertoire Apache /sig/" -ForegroundColor Gray
    }
    else {
        New-Item -ItemType Directory -Force -Path $DeployPath | Out-Null
        Write-Host "Deploiement vers $DeployPath ..." -ForegroundColor Yellow
        Copy-Item -Path (Join-Path $DistDir '*') -Destination $DeployPath -Recurse -Force
        Write-Host "Deploye. Ouvrir http://${HostIp}/sig/connexion (Ctrl+F5)" -ForegroundColor Green
    }
}
else {
    Write-Host "Deploiement manuel :" -ForegroundColor Yellow
    Write-Host "  Copier le contenu de dist\...\browser\ vers Apache /sig/" -ForegroundColor Gray
    Write-Host "  Ou relancer avec -Deploy [-DeployPath 'C:\chemin\sig']" -ForegroundColor Gray
}

Write-Host ""
Read-Host "Appuyez sur ENTREE pour fermer"
