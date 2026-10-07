# Dorm backend starter - forces JDK 21 (Spring Boot 3.4 LTS baseline)
# Usage: powershell -ExecutionPolicy Bypass -File D:\my-projects\dorm\start-backend.ps1

$env:JAVA_HOME = Join-Path $PSScriptRoot "tools\jdk21\jdk-21.0.7+6"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
$mvn = Join-Path $PSScriptRoot "tools\maven\apache-maven-3.9.9\bin\mvn.cmd"
$backendDir = Join-Path $PSScriptRoot "backend"

$p = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($p) {
    Write-Host "ERROR: port 8080 is already in use (pid $($p.OwningProcess)). Run stop-all.ps1 first." -ForegroundColor Red
    exit 1
}

Write-Host "JAVA_HOME = $env:JAVA_HOME" -ForegroundColor Cyan
Write-Host "Starting Spring Boot on http://localhost:8080 ..." -ForegroundColor Cyan
Set-Location $backendDir
& $mvn spring-boot:run
