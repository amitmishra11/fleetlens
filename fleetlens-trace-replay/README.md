# fleetlens-trace-replay

Captures distributed request traces (root request + downstream calls), serialises them
into portable replay bundles, and lets you re-issue the original request against a target
service with all downstream dependencies mocked via WireMock — diffing the actual response
against the originally captured one.

## Capture

In this build there is no live OTel collector wired up yet, so capture works two ways:

1. **Embeddable exporter** — `com.fleetlens.trace.capture.FleetLensSpanExporter` implements
   `io.opentelemetry.sdk.trace.export.SpanExporter` and can be registered inside a target
   service's OpenTelemetry SDK configuration. It buffers spans by trace ID and, once a
   trace's root span has ended, builds a `ReplayBundle` and persists it via
   `TraceReplayRepository`. This is the intended production integration path.
2. **Pre-built bundle ingestion (demo/testing)** — `POST /api/v1/traces/capture/{serviceId}`
   accepts an optional request body that is itself a fully-formed `ReplayBundle` JSON
   document and persists it directly, without requiring a live agent.

## Replay bundle format

See `com.fleetlens.trace.capture.ReplayBundle` / `RootSpanInfo` / `DownstreamCall`. Bundles
are stored as serialised JSON bytes in the `trace_replays.replay_file` column
(`com.fleetlens.trace.capture.TraceSerializer` handles the byte <-> bundle conversion).

## Replay

`com.fleetlens.trace.replay.ReplayEngine#replay(ReplayBundle, ReplayOptions)`:

1. Starts a `MockDispatcher` (WireMock) stubbing every downstream call's HTTP method/path
   to return its captured response body.
2. Re-issues the root request (method, path, body) against `ReplayOptions.targetBaseUrl()`
   using `java.net.http.HttpClient`.
3. Diffs the actual response body against the originally captured response body via
   `TraceDiffEngine`.
4. Publishes a `TraceDiffEvent` and persists the diff onto the `TraceReplay` entity's
   `last_diff` column.
5. Stops WireMock in a `finally` block regardless of outcome.

## REST API

Base path: `/api/v1/traces`

| Method | Path | Description |
|---|---|---|
| POST | `/capture/{serviceId}` | Persist a pre-built or empty replay bundle for a service |
| GET | `` | List all captured bundles (summary view) |
| GET | `/{replayId}` | Fetch a full bundle |
| POST | `/{replayId}/replay` | Run a replay (`mockPort` + `targetBaseUrl` via body or query params) |
| GET | `/{replayId}/diff` | Fetch the diff from the last replay |
| DELETE | `/{replayId}` | Delete a bundle |

## Notes

- This module is a library JAR, not an independently bootable Spring Boot app —
  `fleetlens-api-gateway` wires it in.
- `wiremock-jre8` is pinned directly (3.0.1) since it isn't covered by any BOM managed in
  the parent POM.
- `TraceDiffEngine` performs a generic structural JSON diff (added / removed / changed
  field paths) and works on any Jackson-serialisable object, not just HTTP response bodies.
