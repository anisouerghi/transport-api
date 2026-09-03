$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot 'runtime-config-display.ps1')

$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

Write-Host ""
Write-Host "Build public-api (fat JAR)..." -ForegroundColor Cyan
Write-Host ""

Invoke-ModulePackage -ModuleName 'public-api' -ProjectRoot $Root

$jar = Get-ChildItem -Path (Join-Path $Root 'public-api\target') -Filter 'public-api-*.jar' |
    Where-Object { $_.Name -notlike '*.original' } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if ($jar) {
    Write-Host "JAR genere : $($jar.FullName)" -ForegroundColor Green
    Write-Host "Taille     : $([math]::Round($jar.Length / 1MB, 1)) Mo" -ForegroundColor Green
    Write-Host ""
    Write-Host "Lancer : .\scripts\run-public-prod.ps1" -ForegroundColor Yellow
}
else {
    throw "Build termine mais JAR introuvable dans public-api\target\"
}

Write-Host ""
Read-Host "Appuyez sur ENTREE pour fermer"
