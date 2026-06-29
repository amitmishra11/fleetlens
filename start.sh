#!/usr/bin/env bash
# One-command local startup for FleetLens - no Docker required.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
run_dir="$root/.fleetlens-run"
mkdir -p "$run_dir"

step() { echo; echo "==> $1"; }

wait_until() {
    local desc="$1" timeout="$2" interval="$3"; shift 3
    local elapsed=0
    until "$@" >/dev/null 2>&1; do
        if [ "$elapsed" -ge "$timeout" ]; then
            echo "Timed out after ${timeout}s waiting for: $desc" >&2
            return 1
        fi
        sleep "$interval"
        elapsed=$((elapsed + interval))
    done
}

step "Locating a JDK 21"
if [ -z "${JAVA_HOME:-}" ] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -q '"21\.'; then
    found=""
    for base in "/c/Program Files/Java" "/c/Program Files/Eclipse Adoptium" "/c/Program Files/Microsoft"; do
        if [ -d "$base" ]; then
            for d in "$base"/*21*; do
                if [ -x "$d/bin/java.exe" ] || [ -x "$d/bin/java" ]; then
                    found="$d"
                    break 2
                fi
            done
        fi
    done
    if [ -z "$found" ]; then
        echo "Could not find a JDK 21 installation. Install one and set JAVA_HOME, then re-run." >&2
        exit 1
    fi
    export JAVA_HOME="$found"
fi
export PATH="$JAVA_HOME/bin:$PATH"
echo "Using JAVA_HOME=$JAVA_HOME"

step "Building backend (mvn clean package -DskipTests)"
(cd "$root" && mvn -q -DskipTests clean package)
echo "Backend build succeeded."

step "Starting the embedded Kafka broker"
bootstrap_file="$run_dir/kafka-bootstrap.txt"
rm -f "$bootstrap_file"
nohup java -jar "$root/fleetlens-embedded-kafka/target/fleetlens-embedded-kafka.jar" "$bootstrap_file" \
    > "$run_dir/embedded-kafka.log" 2>&1 &
echo $! > "$run_dir/embedded-kafka.pid"

wait_until "embedded Kafka broker to start" 60 2 test -f "$bootstrap_file"
bootstrap_servers="$(cat "$bootstrap_file")"
echo "Embedded Kafka broker is up at $bootstrap_servers"

step "Starting fleetlens-demo-service (order-service, JMX on :9010)"
nohup java \
    -Dcom.sun.management.jmxremote.port=9010 \
    -Dcom.sun.management.jmxremote.rmi.port=9010 \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Djava.rmi.server.hostname=localhost \
    -DKAFKA_BOOTSTRAP_SERVERS="$bootstrap_servers" \
    -jar "$root/fleetlens-demo-service/target/fleetlens-demo-service.jar" \
    > "$run_dir/demo-service.log" 2>&1 &
echo $! > "$run_dir/demo-service.pid"

step "Starting fleetlens-api-gateway (port 8080, local H2 database)"
nohup java -DKAFKA_BOOTSTRAP_SERVERS="$bootstrap_servers" \
    -jar "$root/fleetlens-api-gateway/target/fleetlens-api-gateway-0.1.0-SNAPSHOT.jar" \
    > "$run_dir/api-gateway.log" 2>&1 &
echo $! > "$run_dir/api-gateway.pid"

step "Waiting for api-gateway to report healthy"
if wait_until "api-gateway health" 120 3 curl -fsS http://localhost:8080/actuator/health; then
    echo "api-gateway is healthy."
else
    echo "api-gateway did not become healthy in time. Check $run_dir/api-gateway.log" >&2
fi

step "Starting the dashboard"
cd "$root/fleetlens-dashboard"
if [ ! -d node_modules ]; then
    echo "Installing dashboard dependencies (npm install)..."
    npm install
fi

cat <<EOF

FleetLens is up - no Docker required:
  Dashboard:    http://localhost:5173 (starting now)
  API:          http://localhost:8080/api/v1
  Health:       http://localhost:8080/actuator/health
  Demo service: http://localhost:8090/actuator/env
  Kafka:        $bootstrap_servers (embedded, in-process)

Logs: $run_dir/*.log
Run ./stop.sh to shut everything down.

EOF

npm run dev
