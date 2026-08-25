# Démarre admin-api sur :8082 — MÊMES chemins absolus que run-public-api.ps1.
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$env:APP_STORAGE_ROOT = $Root
$env:APP_UPLOAD_PATH = Join-Path $Root 'data\attachments'
$env:APP_QR_STORAGE_PATH = Join-Path $Root 'data\qr-codes'
New-Item -ItemType Directory -Force -Path $env:APP_UPLOAD_PATH, $env:APP_QR_STORAGE_PATH | Out-Null
Write-Host "APP_STORAGE_ROOT=$env:APP_STORAGE_ROOT"
Write-Host "APP_UPLOAD_PATH=$env:APP_UPLOAD_PATH"
Write-Host "APP_QR_STORAGE_PATH=$env:APP_QR_STORAGE_PATH"
Set-Location $Root
$mvn = Join-Path $Root '..\apache-maven-3.9.6\bin\mvn.cmd'
if (-not (Test-Path $mvn)) { $mvn = 'mvn' }
& $mvn -pl admin-api -am spring-boot:run
