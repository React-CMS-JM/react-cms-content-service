# Generates src/main/resources/jwt-secret.jwk from JWT_SECRET (.env or env var).
# Must match auth-service JWT_SECRET so content can verify auth tokens.
# Usage (from service root):  .\scripts\generate-jwt-jwk.ps1
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

if (-not $env:JWT_SECRET -and (Test-Path ".env")) {
    Get-Content ".env" | ForEach-Object {
        if ($_ -match '^\s*JWT_SECRET=(.*)$') {
            $env:JWT_SECRET = $Matches[1].Trim().Trim('"').Trim("'")
        }
    }
}

if (-not $env:JWT_SECRET) {
    Write-Error "JWT_SECRET is not set. Put it in .env or the environment (same value as auth-service)."
}

$bytes = [System.Text.Encoding]::UTF8.GetBytes($env:JWT_SECRET)
$b64 = [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
$jwk = (@{ kty = "oct"; k = $b64; alg = "HS256" } | ConvertTo-Json -Compress)
$out = Join-Path $root "src\main\resources\jwt-secret.jwk"
# UTF-8 without BOM — a BOM breaks SmallRye JWT JSON parsing and causes 401 on verify.
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($out, $jwk, $utf8NoBom)
Write-Host "Wrote $out (gitignored, no BOM). Restart quarkus:dev if it is already running."
