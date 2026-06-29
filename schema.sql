CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Every signal emitted by any module lands here first
CREATE TABLE IF NOT EXISTS module_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id  VARCHAR(120) NOT NULL,
    module      VARCHAR(40)  NOT NULL,   -- SCHEMA_DRIFT | TRACE_REPLAY | MEMORY | CONFIG
    severity    VARCHAR(20)  NOT NULL,   -- INFO | WARN | CRITICAL
    summary     TEXT,
    payload     JSONB        NOT NULL,
    occurred_at TIMESTAMPTZ  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_module_events_service_time ON module_events (service_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_module_events_module_time ON module_events (module, occurred_at DESC);

-- Correlated incidents built from module_events
CREATE TABLE IF NOT EXISTS incidents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id      VARCHAR(120) NOT NULL,
    title           TEXT         NOT NULL,
    severity        VARCHAR(20)  NOT NULL,
    module_event_ids UUID[]      NOT NULL,
    correlation_score NUMERIC(4,3),
    opened_at       TIMESTAMPTZ  NOT NULL,
    resolved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_incidents_service_time ON incidents (service_id, opened_at DESC);

-- Schema versions per Kafka topic
CREATE TABLE IF NOT EXISTS schema_versions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic       VARCHAR(200) NOT NULL,
    version     INTEGER      NOT NULL,
    schema_json JSONB        NOT NULL,
    diff_from_prev JSONB,
    is_breaking BOOLEAN      NOT NULL DEFAULT false,
    detected_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_schema_versions_topic_version ON schema_versions (topic, version);

-- Config snapshots per service per environment
CREATE TABLE IF NOT EXISTS config_snapshots (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id  VARCHAR(120) NOT NULL,
    env         VARCHAR(40)  NOT NULL,
    config_json JSONB        NOT NULL,
    captured_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_config_snapshots_service_env_time ON config_snapshots (service_id, env, captured_at DESC);

-- Trace replay files
CREATE TABLE IF NOT EXISTS trace_replays (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id    VARCHAR(120) NOT NULL,
    trace_id      VARCHAR(64)  NOT NULL,
    replay_file   BYTEA        NOT NULL,
    last_diff     JSONB,
    recorded_at   TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Memory + lag snapshots
CREATE TABLE IF NOT EXISTS memory_snapshots (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id      VARCHAR(120) NOT NULL,
    heap_used_mb    NUMERIC(10,2),
    heap_max_mb     NUMERIC(10,2),
    gc_pause_ms     NUMERIC(10,2),
    kafka_lag       BIGINT,
    consumer_group  VARCHAR(200),
    sampled_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_memory_snapshots_service_time ON memory_snapshots (service_id, sampled_at DESC);
