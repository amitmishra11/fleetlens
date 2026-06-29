#!/usr/bin/env bash
set -uo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
run_dir="$root/.fleetlens-run"

for name in embedded-kafka demo-service api-gateway; do
    pid_file="$run_dir/$name.pid"
    if [ -f "$pid_file" ]; then
        pid="$(cat "$pid_file")"
        if kill -0 "$pid" 2>/dev/null; then
            echo "Stopping $name (PID $pid)..."
            kill "$pid" 2>/dev/null
        fi
        rm -f "$pid_file"
    fi
done

rm -f "$run_dir/kafka-bootstrap.txt"
echo "Done. (The dashboard's 'npm run dev' terminal needs Ctrl+C if it's still running in its own window.)"
