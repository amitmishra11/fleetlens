# FleetLens

[![CI](https://github.com/amitmishra11/fleetlens/actions/workflows/ci.yml/badge.svg)](https://github.com/amitmishra11/fleetlens/actions/workflows/ci.yml)

FleetLens is a unified runtime observability platform for Spring Boot + Kafka microservice fleets. It combines four instrumentation modules — schema drift detection, distributed trace replay, JVM heap / Kafka lag correlation, and live config auditing — into a single correlated incident timeline, surfaced through a React dashboard and a REST API.

## Testing & CI

- Backend: JUnit 5 unit tests per module (`mvn verify`), plus a Spring context-load test for the API gateway against an embedded Kafka broker.
- Frontend: Vitest unit tests for the timeline-merging logic, oxlint, and a full `tsc`/Vite production build.
- Both run on every push/PR via GitHub Actions (`.github/workflows/ci.yml`).

## Highlights

- **No Docker required** — the database is a local file-based H2 instance, and Kafka runs as a real in-process JVM broker (`fleetlens-embedded-kafka`, KRaft mode, no Zookeeper). The whole stack is plain Java + Node processes.
- **Zero-agent setup** for Config Auditor and Schema Drift — works against any existing Spring Boot service with Actuator enabled and any Kafka topic
- **Sidecar pattern** for Memory Profiler — runs alongside your service in the same pod, connects via JMX; no bytecode instrumentation
- **Correlation window is configurable** — default 5 minutes; tighten for high-frequency services, widen for batch jobs
- **All modules are independently deployable** — use only Schema Drift if that's all you need; they share no runtime coupling beyond the database and the event bus

## Quickstart (one command, no Docker)

```powershell
.\start.ps1      # Windows PowerShell
```
```bash
./start.sh       # git-bash / WSL / Linux / macOS
```

This builds everything, starts the embedded Kafka broker, launches `fleetlens-demo-service` (a monitored "order-service" with JMX enabled) and `fleetlens-api-gateway`, then starts the dashboard dev server. Open **http://localhost:5173**.

Stop everything with `.\stop.ps1` / `./stop.sh`.

Requires: JDK 21, Node.js, Maven (or rely on the script's JDK auto-detection under `C:\Program Files\Java`).

## Manual build & run

```bash
# Build all modules
mvn clean package -DskipTests

# Start the embedded Kafka broker (writes its actual address to .fleetlens-run/kafka-bootstrap.txt)
java -jar fleetlens-embedded-kafka/target/fleetlens-embedded-kafka.jar .fleetlens-run/kafka-bootstrap.txt

# In another terminal: start the demo target service, pointed at that broker
java -DKAFKA_BOOTSTRAP_SERVERS=$(cat .fleetlens-run/kafka-bootstrap.txt) \
     -Dcom.sun.management.jmxremote.port=9010 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.ssl=false \
     -jar fleetlens-demo-service/target/fleetlens-demo-service.jar

# In another terminal: start the API gateway
java -DKAFKA_BOOTSTRAP_SERVERS=$(cat .fleetlens-run/kafka-bootstrap.txt) \
     -jar fleetlens-api-gateway/target/fleetlens-api-gateway-0.1.0-SNAPSHOT.jar

# Run tests
mvn verify

# Start the dashboard
cd fleetlens-dashboard && npm install && npm run dev
```

The API gateway defaults to port 8080 (override with `-DSERVER_PORT=...` if that port is already in use on your machine) and uses a local H2 database file at `.fleetlens-data/fleetlens.mv.db` — delete that file to reset all stored incidents/snapshots.

## Optional: Docker-based Postgres/Kafka/Schema-Registry/Prometheus

`docker-compose.yml` and `schema.sql` are kept for anyone who wants to run against real Postgres/Kafka/Schema Registry/Prometheus instead (e.g. for a more production-like deployment). They are **not** used by `start.ps1`/`start.sh`. To use them, point `DB_URL`/`KAFKA_BOOTSTRAP_SERVERS` env vars at the Dockerised services instead of the embedded defaults.
