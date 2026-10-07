# Dorm system - start everything (MySQL + backend + frontend) in two new windows
# Usage: powershell -ExecutionPolicy Bypass -File D:\my-projects\dorm\start-all.ps1

$root = $PSScriptRoot

# 1) MySQL service
$svc = Get-Service MySQL80 -ErrorAction SilentlyContinue
if (-not $svc) {
    Write-Host "WARN: service MySQL80 not found. Install/start MySQL manually." -ForegroundColor Yellow
} elseif ($svc.Status -ne "Running") {
    Write-Host "Starting MySQL80 service ..." -ForegroundColor Cyan
    net start MySQL80 | Out-Null
    Write-Host "MySQL80 started." -ForegroundColor Green
} else {
    Write-Host "MySQL80 already running." -ForegroundColor Green
}

# 2) backend (new window)
Write-Host "Launching backend window ..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList @("-NoExit", "-ExecutionPolicy", "Bypass", "-File", (Join-Path $root "start-backend.ps1"))

# 等待后端监听 8080 再启动前端。否则 Vite 秒级就绪并自动打开浏览器，
# 本地仍有效的 token 会直接进入系统并并发请求，而这些请求全部打到尚未启动的后端
# （Vite 代理报 ECONNREFUSED，页面提示 network error）。
Write-Host "Waiting for backend on port 8080 (Spring Boot needs ~20-40s) ..." -ForegroundColor Cyan
$backendReady = $false
for ($i = 1; $i -le 90; $i++) {
    if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) { $backendReady = $true; break }
    Start-Sleep -Seconds 1
    if ($i % 10 -eq 0) { Write-Host ("  ... waiting {0}s" -f $i) -ForegroundColor DarkGray }
}
if ($backendReady) {
    Write-Host "Backend is listening on 8080." -ForegroundColor Green
} else {
    Write-Host "WARN: port 8080 not listening after 90s. Check the backend window for errors;" -ForegroundColor Yellow
    Write-Host "      the frontend will still start, but API calls may fail until it is up." -ForegroundColor Yellow
}

# 3) frontend (new window)
Write-Host "Launching frontend window ..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList @("-NoExit", "-ExecutionPolicy", "Bypass", "-File", (Join-Path $root "start-frontend.ps1"))

Write-Host ""
Write-Host "==============================================" -ForegroundColor Green
Write-Host " Frontend : http://localhost:5173 (auto-shifts to 5174+ if busy; browser opens automatically)"
Write-Host " Swagger  : http://localhost:8080/doc.html"
Write-Host " Admin    : admin / admin123"
Write-Host " Employee : zhangwei / employee123"
Write-Host "==============================================" -ForegroundColor Green
Write-Host "Backend was already up when the frontend started; browser opens to the real address."
Write-Host "To stop everything: powershell -ExecutionPolicy Bypass -File D:\my-projects\dorm\stop-all.ps1"
