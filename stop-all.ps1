# Dorm system - stop backend (8080) and frontend (Vite: 5173 ~ 5179)
# Usage: powershell -ExecutionPolicy Bypass -File D:\my-projects\dorm\stop-all.ps1
# 前端 Vite 在 5173 被占用时会自动顺延，因此这里一并清理 5173-5179。

$backendPort = 8080
$frontendPorts = 5173..5179
$allPorts = @($backendPort) + @($frontendPorts)

function Stop-Listener([int]$port, [bool]$nodeOnly) {
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if (-not $conns) { return $false }
    foreach ($procId in ($conns.OwningProcess | Select-Object -Unique)) {
        $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
        $name = if ($proc) { $proc.ProcessName } else { 'unknown' }
        if ($nodeOnly -and $name -ne 'node') {
            Write-Host "Port $port : in use by $name (pid $procId), not our dev server - skipped." -ForegroundColor Yellow
            continue
        }
        try {
            Stop-Process -Id $procId -Force -ErrorAction Stop
            Write-Host "Port $port : stopped $name pid $procId" -ForegroundColor Green
        } catch {
            Write-Host "Port $port : failed to stop pid $procId -> $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    return $true
}

$touched = @()
if (Stop-Listener $backendPort $false) { $touched += $backendPort }
foreach ($port in $frontendPorts) { if (Stop-Listener $port $true) { $touched += $port } }

if ($touched.Count -eq 0) {
    Write-Host "Nothing to stop: 8080 and 5173-5179 are all free." -ForegroundColor Yellow
}

# 等待端口真正释放，再逐个确认（杀进程是异步的）
$deadline = (Get-Date).AddSeconds(8)
do {
    Start-Sleep -Milliseconds 500
    $busy = @(Get-NetTCPConnection -LocalPort $allPorts -State Listen -ErrorAction SilentlyContinue)
} while ($busy.Count -gt 0 -and (Get-Date) -lt $deadline)

$stillInUse = @()
foreach ($port in $allPorts) {
    $still = @(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
    if ($still.Count -gt 0) {
        $stillInUse += $port
        Write-Host "Port $port : STILL in use (pid $(($still.OwningProcess | Select-Object -Unique) -join ','))" -ForegroundColor Red
    }
}
if ($stillInUse.Count -eq 0) {
    Write-Host "All dev ports released (8080, 5173-5179)." -ForegroundColor Green
}

Write-Host ""
Write-Host "MySQL80 service is left running. Stop it with: net stop MySQL80" -ForegroundColor DarkGray