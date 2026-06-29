#Requires -Version 5.1
<#
One-command local startup for FleetLens - no Docker required.

What it does:
  1. Locates a JDK 21 and builds all backend modules (mvn package).
  2. Launches fleetlens-embedded-kafka: a real, in-process Kafka broker (KRaft mode,
     no Zookeeper) so the rest of the stack has an ordinary Kafka bootstrap address
     to talk to. Its actual host:port is written to .fleetlens-run\kafka-bootstrap.txt
     once it's up, since the test broker picks its own free port.
  3. Launches fleetlens-demo-service (the monitored "order-service") with JMX enabled,
     pointed at that broker.
  4. Launches fleetlens-api-gateway (uses a file-based H2 database - no Postgres
     install needed) and waits for it to report healthy.
  5. Installs dashboard deps if needed and starts the Vite dev server.

Logs go to .fleetlens-run\*.log. Process IDs are recorded in .fleetlens-run\pids.json
so stop.ps1 can clean everything up.
#>

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$runDir = Join-Path $root ".fleetlens-run"
New-Item -ItemType Directory -Force -Path $runDir | Out-Null

function Write-Step($message) {
    Write-Host ""
    Write-Host "==> $message" -ForegroundColor Cyan
}

function Wait-Until {
    param(
        [Parameter(Mandatory)] [scriptblock]$Condition,
        [int]$TimeoutSeconds = 90,
        [int]$IntervalSeconds = 2,
        [string]$Description = "condition"
    )
    $elapsed = 0
    while (-not (& $Condition)) {
        if ($elapsed -ge $TimeoutSeconds) {
            throw "Timed out after ${TimeoutSeconds}s waiting for: $Description"
        }
        Start-Sleep -Seconds $IntervalSeconds
        $elapsed += $IntervalSeconds
    }
}

# 1. JDK 21
Write-Step "Locating a JDK 21"
$javaHome = $env:JAVA_HOME
function Test-IsJdk21($path) {
    if (-not $path) { return $false }
    $javaExe = Join-Path $path "bin\java.exe"
    if (-not (Test-Path $javaExe)) { return $false }
    $verOutput = & $javaExe -version 2>&1 | Out-String
    return $verOutput -match '"21\.'
}

if (-not (Test-IsJdk21 $javaHome)) {
    $candidates = @()
    foreach ($base in @("C:\Program Files\Java", "C:\Program Files\Eclipse Adoptium", "C:\Program Files\Microsoft")) {
        if (Test-Path $base) {
            $candidates += Get-ChildItem -Path $base -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -match "21" } |
                ForEach-Object { $_.FullName }
        }
    }
    $found = $candidates | Where-Object { Test-IsJdk21 $_ } | Select-Object -First 1
    if (-not $found) {
        Write-Host "Could not find a JDK 21 installation. Install one and set JAVA_HOME, then re-run." -ForegroundColor Red
        exit 1
    }
    $javaHome = $found
}
Write-Host "Using JAVA_HOME=$javaHome" -ForegroundColor Green
$env:JAVA_HOME = $javaHome
$env:PATH = "$javaHome\bin;$env:PATH"

# 2. Build
Write-Step "Building backend (mvn clean package -DskipTests)"
Push-Location $root
try {
    if (Test-Path ".\mvnw.cmd") {
        & .\mvnw.cmd -q -DskipTests clean package
    } else {
        & mvn -q -DskipTests clean package
    }
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }
} finally {
    Pop-Location
}
Write-Host "Backend build succeeded." -ForegroundColor Green

# 3. Embedded Kafka broker
Write-Step "Starting the embedded Kafka broker"
$bootstrapFile = Join-Path $runDir "kafka-bootstrap.txt"
Remove-Item -Force -ErrorAction SilentlyContinue $bootstrapFile
$kafkaJar = Join-Path $root "fleetlens-embedded-kafka\target\fleetlens-embedded-kafka.jar"
$kafkaLog = Join-Path $runDir "embedded-kafka.log"
$kafkaProcess = Start-Process -FilePath "java" -ArgumentList @("-jar", $kafkaJar, $bootstrapFile) `
    -RedirectStandardOutput $kafkaLog -RedirectStandardError "$kafkaLog.err" -PassThru -WindowStyle Hidden

Wait-Until -Condition { Test-Path $bootstrapFile } -TimeoutSeconds 60 -IntervalSeconds 2 -Description "embedded Kafka broker to start"
$bootstrapServers = (Get-Content $bootstrapFile -Raw).Trim()
Write-Host "Embedded Kafka broker is up at $bootstrapServers" -ForegroundColor Green

# 4. Demo service
Write-Step "Starting fleetlens-demo-service (order-service, JMX on :9010)"
$demoJar = Join-Path $root "fleetlens-demo-service\target\fleetlens-demo-service.jar"
$demoLog = Join-Path $runDir "demo-service.log"
$demoArgs = @(
    "-Dcom.sun.management.jmxremote.port=9010",
    "-Dcom.sun.management.jmxremote.rmi.port=9010",
    "-Dcom.sun.management.jmxremote.authenticate=false",
    "-Dcom.sun.management.jmxremote.ssl=false",
    "-Djava.rmi.server.hostname=localhost",
    "-DKAFKA_BOOTSTRAP_SERVERS=$bootstrapServers",
    "-jar", $demoJar
)
$demoProcess = Start-Process -FilePath "java" -ArgumentList $demoArgs `
    -RedirectStandardOutput $demoLog -RedirectStandardError "$demoLog.err" -PassThru -WindowStyle Hidden

# 5. API gateway
Write-Step "Starting fleetlens-api-gateway (port 8080, local H2 database)"
$gatewayJar = Join-Path $root "fleetlens-api-gateway\target\fleetlens-api-gateway-0.1.0-SNAPSHOT.jar"
$gatewayLog = Join-Path $runDir "api-gateway.log"
$gatewayArgs = @("-DKAFKA_BOOTSTRAP_SERVERS=$bootstrapServers", "-jar", $gatewayJar)
$gatewayProcess = Start-Process -FilePath "java" -ArgumentList $gatewayArgs `
    -RedirectStandardOutput $gatewayLog -RedirectStandardError "$gatewayLog.err" -PassThru -WindowStyle Hidden

@{
    embeddedKafkaPid = $kafkaProcess.Id
    demoServicePid   = $demoProcess.Id
    apiGatewayPid    = $gatewayProcess.Id
} | ConvertTo-Json | Set-Content (Join-Path $runDir "pids.json")

Write-Step "Waiting for api-gateway to report healthy"
try {
    Wait-Until -Condition {
        try {
            $resp = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 3
            return $resp.StatusCode -eq 200
        } catch {
            return $false
        }
    } -TimeoutSeconds 120 -IntervalSeconds 3 -Description "api-gateway health"
    Write-Host "api-gateway is healthy." -ForegroundColor Green
} catch {
    Write-Host "api-gateway did not become healthy in time. Check $gatewayLog for details." -ForegroundColor Red
}

# 6. Dashboard
Write-Step "Starting the dashboard"
$dashboardDir = Join-Path $root "fleetlens-dashboard"
Push-Location $dashboardDir
try {
    if (-not (Test-Path (Join-Path $dashboardDir "node_modules"))) {
        Write-Host "Installing dashboard dependencies (npm install)..."
        npm install
        if ($LASTEXITCODE -ne 0) { throw "npm install failed" }
    }
    Write-Host ""
    Write-Host "FleetLens is up - no Docker required:" -ForegroundColor Green
    Write-Host "  Dashboard:    http://localhost:5173 (starting now)"
    Write-Host "  API:          http://localhost:8080/api/v1"
    Write-Host "  Health:       http://localhost:8080/actuator/health"
    Write-Host "  Demo service: http://localhost:8090/actuator/env"
    Write-Host "  Kafka:        $bootstrapServers (embedded, in-process)"
    Write-Host ""
    Write-Host "Logs: $runDir\*.log"
    Write-Host "Run .\stop.ps1 to shut everything down."
    Write-Host ""
    npm run dev
} finally {
    Pop-Location
}
