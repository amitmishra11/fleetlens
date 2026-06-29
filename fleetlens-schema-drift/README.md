# fleetlens-schema-drift

Detects schema drift on Kafka topics by periodically sampling messages, inferring a field-level
schema, and diffing it against the previously stored version. Breaking changes (field removal,
field type change) raise a `SchemaDriftEvent` on the shared application event bus for the
correlation engine to pick up.

This is a library module — it does not run standalone. It is wired into `fleetlens-api-gateway`.

## Limitation: JSON-sampling-only schema inference

The original plan describes two inference modes: Avro (via Confluent Schema Registry client)
and JSON (via message sampling). This build implements **JSON mode only**.

Reason: the Confluent `kafka-schema-registry-client` artifact is published to Confluent's own
Maven repository, which is not configured in this project's `pom.xml`. Pulling it in would
require adding an external repository to the build, which was explicitly out of scope for this
module. As a result:

- `SchemaInferenceEngine` only exposes `inferFromJsonSamples(topic, messages)`.
- Topics that are Avro-encoded (binary, not valid JSON) will simply yield zero parseable
  samples and be skipped — no drift will be detected for them.
- If/when Avro support is added, it should live alongside `inferFromJsonSamples` as
  `inferFromAvro(topic)`, backed by a `SchemaRegistryClient` bean configured separately, without
  changing the `InferredSchema` / `DriftAnalyser` contracts.

## Breaking-change classification

A drift finding is breaking if:
- a field present in the previous version is missing in the new version (`FIELD_REMOVED`), or
- a field's observed JSON node type set changes at all (`TYPE_CHANGED`).

Field additions (`FIELD_ADDED`) are always non-breaking.

Note: the plan mentions "widening" type changes (e.g. int → long) could be treated as
non-breaking. This build does **not** attempt that distinction — Jackson's `JsonNodeType`
doesn't differentiate numeric subtypes, and reliably classifying widening vs. narrowing would
require parsing actual numeric ranges from samples. We chose the simpler, more conservative
rule: any type change is breaking.

## Key components

- `detector.SchemaInferenceEngine` — builds an `InferredSchema` from sampled JSON messages.
- `detector.DriftAnalyser` — diffs two `InferredSchema` versions into a `DriftReport`.
- `consumer.KafkaTopicSampler` — scheduled job that lists topics via `AdminClient`, samples
  messages via a short-lived `KafkaConsumer`, and triggers inference + drift analysis.
- `store.SchemaVersion` / `SchemaVersionRepository` — JPA persistence for schema history
  (table `schema_versions`).
- `alert.DriftAlertPublisher` — publishes `SchemaDriftEvent` for breaking changes.
- `api.SchemaDriftController` — REST endpoints under `/api/v1/schema`.

## Configuration

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092

fleetlens:
  schema:
    poll-interval-ms: 120000
    sample-count: 50
```
