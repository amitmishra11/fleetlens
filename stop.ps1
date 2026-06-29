$ErrorActionPreference = "SilentlyContinue"
$root = $PSScriptRoot
$runDir = Join-Path $root ".fleetlens-run"
$pidFile = Join-Path $runDir "pids.json"

if (Test-Path $pidFile) {
    $pids = Get-Content $pidFile | ConvertFrom-Json
    foreach ($name in @("embeddedKafkaPid", "demoServicePid", "apiGatewayPid")) {
        $procId = $pids.$name
        if ($procId) {
            Write-Host "Stopping $name (PID $procId)..."
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        }
    }
    Remove-Item $pidFile -Force
} else {
    Write-Host "No tracked PIDs found (.fleetlens-run\pids.json missing) - stopping any java processes that look like FleetLens instead."
    Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -match "fleetlens" } |
        ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
}

Remove-Item -Force -ErrorAction SilentlyContinue (Join-Path $runDir "kafka-bootstrap.txt")
Write-Host "Done. (The dashboard's 'npm run dev' terminal needs Ctrl+C if it's still running in its own window.)"
