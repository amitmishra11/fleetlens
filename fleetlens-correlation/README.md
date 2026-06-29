# fleetlens-correlation

The core platform correlation engine for FleetLens. Listens for `ModuleEvent`s published by the
four instrumentation modules (schema drift, trace replay, memory profiler, config auditor),
joins related signals into `Incident`s using a sliding time-window per service, and exposes the
unified incident and timeline REST API.

This is a Spring Boot **library** module — it is not independently bootable. The
`fleetlens-api-gateway` module wires it into the running application context.

## Packages

- `engine` — `CorrelationEngine`, the `@EventListener`-based join logic
- `incident` — `Incident` / `ModuleEventEntity` JPA entities, repositories, `IncidentBuilder`
- `timeline` — `TimelineAssembler` and the `ServiceTimeline` / `TimelineEntry` view models
- `api` — `IncidentController`, `TimelineController`

## REST endpoints

```
GET   /api/v1/incidents
GET   /api/v1/incidents/{incidentId}
PATCH /api/v1/incidents/{incidentId}/resolve
GET   /api/v1/timeline/{serviceId}?from=&to=
GET   /api/v1/timeline/global?from=&to=
```

## Configuration

```yaml
fleetlens:
  correlation:
    window-minutes: 5
```

See `DESIGN.md` for the correlation algorithm.
