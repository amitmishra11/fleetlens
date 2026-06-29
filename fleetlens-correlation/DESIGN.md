# Correlation Engine Design

## Problem

Each instrumentation module (schema drift, trace replay, memory profiler, config auditor)
independently detects anomalies for a given service and publishes a `ModuleEvent` subtype
(`SchemaDriftEvent`, `ConfigDriftEvent`, `MemoryPressureEvent`, `HeapLagCorrelationEvent`,
`TraceDiffEvent`) on the Spring application event bus. A real incident is often the result of
several of these signals firing together — e.g. a config change followed minutes later by
rising heap usage on the same service. The correlation engine's job is to recognise that these
separate signals belong to the same underlying incident.

## Algorithm: sliding window join on (serviceId, time)

`CorrelationEngine` listens for every event type via `@EventListener` methods, each delegating
to a single private `handle(ModuleEvent event)`:

1. Compute `windowStart = event.getOccurredAt() - correlationWindowMinutes` (default 5 minutes,
   configurable via `fleetlens.correlation.window-minutes`).
2. Query `IncidentRepository.findOpenForService(serviceId, windowStart)` — all incidents for that
   service that are still unresolved (`resolved_at IS NULL`) and were opened on or after
   `windowStart`, ordered most-recent-first.
3. **No open incident in window** → a brand new incident is the right model: `IncidentBuilder`
   persists a `ModuleEventEntity` for the triggering event and constructs a new `Incident`
   seeded with that single event. The engine saves it.
4. **One or more open incidents in window** → the signal is treated as belonging to the most
   recently opened one (`openIncidents.get(0)`). The triggering event is persisted as a
   `ModuleEventEntity`, then `Incident.addEvent(entity)` appends its id to the incident's
   `moduleEventIds`, after which the engine saves the (now-updated) incident.

This is a simple greedy join: every new signal looks backward `windowMinutes` from its own
occurrence time and attaches to whatever's still open for that service, rather than running a
batch join over historical data. It is O(1) per event (one indexed query) and requires no
external state beyond Postgres.

## Multiple module events landing in one incident

Because the window check is keyed only on `serviceId` + recency, it doesn't matter which module
fired which event — a `ConfigDriftEvent` from the config auditor and a `MemoryPressureEvent` from
the memory profiler for the same service, fired four minutes apart, land in the same incident.
Each contributes its own `ModuleEventEntity` row referenced from `Incident.moduleEventIds`
(stored as a comma-joined `UUID` list via `UuidListConverter` — see note below), so the full
fan-in is always recoverable for the incident detail view.

## Severity escalation

`Incident.addEvent` compares the incoming event's severity against the incident's current
severity using `IncidentSeverity.isHigherThan` (ordinal-based: `INFO < WARN < CRITICAL`) and only
ever escalates upward — a later `INFO`-level event arriving on a `CRITICAL` incident does not
downgrade it. This matches the intuition that an incident is as serious as its worst signal.

## Correlation score heuristic

`correlation_score` is a confidence proxy in `[0.0, 1.0]` for "how sure are we this is a real,
multi-signal incident" rather than a coincidence. The heuristic is intentionally simple:

```
score = min(1.0, 0.5 + 0.1 * eventCount)
```

A freshly created single-event incident starts at `0.5` (just one signal — moderate confidence).
Each additional correlated event nudges the score up by `0.1`, capped at `1.0`. This rewards
incidents with multiple independent corroborating signals without requiring a trained model.

## Why a String-backed UUID list instead of a native `uuid[]` column

The data model in the project spec describes `module_event_ids` as a native Postgres `UUID[]`
column. Hibernate's support for native array types varies across versions/dialects and typically
needs a vendor-specific type (e.g. `hibernate-types`) or `@JdbcTypeCode(SqlTypes.ARRAY)` wiring
that's easy to get subtly wrong across environments. Instead, `Incident.moduleEventIds` is mapped
as `List<UUID>` via a custom `AttributeConverter` (`UuidListConverter`) that stores it as a
comma-joined string in a single `text` column. This is fully portable, trivially testable, and
sufficient for the access patterns used here ("does this incident contain event X", "how many
events does this incident have") — at the cost of not being indexable/queryable as a SQL array.
If that becomes a requirement later, swapping in a real array column is a contained migration.
