function Get-EnvDisplayValue {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [switch]$Secret,
        [string]$DefaultDisplay = '(non defini)'
    )

    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $DefaultDisplay
    }
    if ($Secret) {
        return '********'
    }
    return $value
}

function Write-EnvLine {
    param(
        [Parameter(Mandatory = $true)][string]$Label,
        [Parameter(Mandatory = $true)][string]$Value,
        [System.ConsoleColor]$ValueColor = [System.ConsoleColor]::Gray
    )

    $padded = ('{0,-36}' -f ($Label + ':'))
    Write-Host -NoNewline $padded
    Write-Host $Value -ForegroundColor $ValueColor
}

function Write-RuntimeSection {
    param([Parameter(Mandatory = $true)][string]$Title)

    Write-Host ""
    Write-Host $Title -ForegroundColor DarkCyan
    Write-Host ("-" * 54) -ForegroundColor DarkGray
}

function Write-RuntimeHeader {
    param([Parameter(Mandatory = $true)][string]$Title)

    Write-Host ""
    Write-Host "======================================================" -ForegroundColor Cyan
    Write-Host "  $Title" -ForegroundColor Cyan
    Write-Host "======================================================" -ForegroundColor Cyan
}

function Write-RuntimeFooter {
    Write-Host ""
    Write-Host "======================================================" -ForegroundColor Cyan
    Write-Host ""
}

function Write-SpringDatabaseBlock {
    Write-RuntimeSection "Base de donnees / JWT"
    Write-EnvLine "DATABASE_URL" (Get-EnvDisplayValue -Name 'DATABASE_URL' -DefaultDisplay '(defaut Spring)')
    Write-EnvLine "DATABASE_USERNAME" (Get-EnvDisplayValue -Name 'DATABASE_USERNAME' -DefaultDisplay '(defaut Spring)')
    Write-EnvLine "DATABASE_PASSWORD" (Get-EnvDisplayValue -Name 'DATABASE_PASSWORD' -Secret -DefaultDisplay '(defaut Spring - vide)')
    Write-EnvLine "JWT_SECRET" (Get-EnvDisplayValue -Name 'JWT_SECRET' -Secret -DefaultDisplay '(defaut Spring)')
    Write-EnvLine "JWT_EXPIRATION_MS" (Get-EnvDisplayValue -Name 'JWT_EXPIRATION_MS' -DefaultDisplay '(defaut Spring - 86400000)')
}

function Write-GoogleOAuthBlock {
    Write-RuntimeSection "Google OAuth (public-api)"
    $secret = [Environment]::GetEnvironmentVariable('GOOGLE_CLIENT_SECRET')
    Write-EnvLine "GOOGLE_CLIENT_ID" (Get-EnvDisplayValue -Name 'GOOGLE_CLIENT_ID')
    Write-EnvLine "GOOGLE_CLIENT_SECRET" (Get-EnvDisplayValue -Name 'GOOGLE_CLIENT_SECRET' -Secret)
    Write-EnvLine "GOOGLE_REDIRECT_URI" (Get-EnvDisplayValue -Name 'GOOGLE_REDIRECT_URI')
    Write-EnvLine "GOOGLE_FRONTEND_CALLBACK_URL" (Get-EnvDisplayValue -Name 'GOOGLE_FRONTEND_CALLBACK_URL')
    if ([string]::IsNullOrWhiteSpace($secret)) {
        Write-Host "  -> Google OAuth DESACTIVE (secret manquant) -> HTTP 503" -ForegroundColor Red
    }
    else {
        Write-Host "  -> Google OAuth ACTIVE" -ForegroundColor Green
    }
}

function Import-LocalSecrets {
    param([Parameter(Mandatory = $true)][string]$ScriptRoot)

    $localSecrets = Join-Path $ScriptRoot 'secrets.local.ps1'
    if (Test-Path -LiteralPath $localSecrets) {
        . $localSecrets
        return $localSecrets
    }
    return $null
}
