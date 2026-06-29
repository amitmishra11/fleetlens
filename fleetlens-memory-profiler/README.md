# fleetlens-memory-profiler

JVM heap / Kafka consumer lag correlation sidecar module. Polls target service
JVMs over JMX and Kafka consumer group offsets, persists samples to the shared
`memory_snapshots` table, and correlates heap growth with rising lag into
`HeapLagCorrelationEvent`s.

This is a library module — it is wired into `fleetlens-api-gateway`, not run
standalone.

## JMX setup

Target JVMs must be started with JMX remote management enabled, e.g.:

```
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9010
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false
```

Heap dump triggering additionally requires the `com.sun.management:type=HotSpotDiagnostic`
MBean to be exposed over that JMX connection. Not all JVM/JMX configurations
expose it; heap dump attempts are best-effort and failures are logged, not
propagated.

## Persistence tradeoffs

- Heap and lag readings share a single `memory_snapshots` row shape (columns
  nullable depending on which poller wrote the row), per the shared schema.
- Correlation events (`HeapLagCorrelationEvent`) and triggered heap dump
  records are **not persisted to Postgres** in this build. They are kept in a
  bounded in-memory list (last 500 entries each, see `CorrelationLog` and
  `HeapDumpLog`). This is simpler than adding a dedicated table/migration but
  means history is lost on restart and isn't visible across multiple gateway
  instances. If that becomes a problem, add a `heap_lag_correlations` table
  and back these two stores with JPA repositories instead.

## Configuration

Services are configured under `fleetlens.services` (see root `application.yml`
in `fleetlens-api-gateway`), each with `id`, `jmx-host`, `jmx-port`, and
`kafka-consumer-groups`. Poll interval and heap pressure threshold are
configurable via `fleetlens.memory.poll-interval-ms` (default 30000) and
`fleetlens.memory.heap-threshold-percent` (default 85).
