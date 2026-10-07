# Dorm frontend dev server (Vite, preferred port 5173)
# Usage: powershell -ExecutionPolicy Bypass -File D:\my-projects\dorm\start-frontend.ps1
# 5173 被占用时 Vite 会自动顺延到 5174/5175…，并自动打开浏览器到实际地址。

$frontendDir = Join-Path $PSScriptRoot "frontend"
Set-Location $frontendDir

if (-not (Test-Path (Join-Path $frontendDir "node_modules"))) {
    Write-Host "node_modules not found, running npm install ..." -ForegroundColor Yellow
    npm install
}

Write-Host "Starting Vite dev server (preferred port 5173, will auto-pick a free port if busy) ..." -ForegroundColor Cyan
Write-Host "The browser will open automatically at the real address; it is also shown as 'Local:' below." -ForegroundColor DarkGray
npm run dev