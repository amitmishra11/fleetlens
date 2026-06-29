export type Severity = 'INFO' | 'WARN' | 'CRITICAL';

export type ModuleType = 'SCHEMA_DRIFT' | 'TRACE_REPLAY' | 'MEMORY' | 'CONFIG';

export interface Incident {
  id: string;
  serviceId: string;
  title: string;
  severity: Severity;
  eventIds: string[];
  eventCount: number;
  correlationScore: number | null;
  openedAt: string;
  resolvedAt: string | null;
  createdAt: string;
}

export interface IncidentPage {
  content: Incident[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface IncidentQueryParams {
  page?: number;
  size?: number;
}

export interface TimelineEntry {
  occurredAt: string;
  module: ModuleType;
  severity: Severity;
  summary: string;
  incidentId?: string | null;
  serviceId: string;
}

export interface ServiceTimeline {
  serviceId: string;
  from: string;
  to: string;
  entries: TimelineEntry[];
  incidents: Incident[];
}

export interface ServiceDefinition {
  id: string;
  env: string;
  baseUrl?: string;
  managed: boolean;
}

export interface RegisterServiceRequest {
  id: string;
  baseUrl: string;
  env?: string;
  jmxHost?: string;
  jmxPort?: number;
  kafkaConsumerGroups?: string[];
}

export interface ConfigSnapshot {
  serviceId: string;
  env: string;
  configJson: Record<string, unknown>;
  capturedAt: string;
}

export type ChangeType = 'ADDED' | 'REMOVED' | 'MODIFIED';

export interface ConfigChange {
  key: string;
  oldValue: unknown;
  newValue: unknown;
  changeType: ChangeType;
}

export interface ConfigMatrixRow {
  key: string;
  values: Record<string, string | number | boolean | null>;
  hasDrift: boolean;
}

export interface ConfigMatrix {
  environments: string[];
  keys: ConfigMatrixRow[];
}

export interface SchemaVersion {
  id: string;
  topic: string;
  version: number;
  schemaJson: string;
  diffFromPrev: string | null;
  breaking: boolean;
  detectedAt: string;
}

export interface SchemaTopic {
  topic: string;
  latestVersion: number;
  hasBreakingHistory: boolean;
}

export type FindingType = 'FIELD_REMOVED' | 'FIELD_ADDED' | 'TYPE_CHANGED';

export interface SchemaDriftFinding {
  field: string;
  findingType: FindingType;
  isBreaking: boolean;
}

export interface SchemaDiff {
  topic: string;
  fromVersion: number;
  toVersion: number;
  findings: SchemaDriftFinding[];
  hasBreaking: boolean;
}

export interface MemorySnapshot {
  id: string;
  serviceId: string;
  heapUsedMb: number | null;
  heapMaxMb: number | null;
  gcPauseMs: number | null;
  kafkaLag: number | null;
  consumerGroup: string | null;
  sampledAt: string;
}

export interface MemoryTimelineResponse {
  serviceId: string;
  from: string;
  to: string;
  heapSamples: MemorySnapshot[];
  lagSamples: MemorySnapshot[];
}

/** Heap + lag samples merged onto a shared, chart-friendly timeline. */
export interface MergedMemoryPoint {
  sampledAt: string;
  heapUsedMb: number | null;
  heapMaxMb: number | null;
  kafkaLag: number | null;
}

export interface CorrelationEvent {
  id: string;
  serviceId: string;
  occurredAt: string;
  severity: Severity;
  heapTrend: number;
  lagTrend: number;
  summary: string;
}

export interface HeapDump {
  serviceId: string;
  filePath: string;
  triggeredAt: string;
}

export interface RootSpanInfo {
  spanId?: string | null;
  name?: string | null;
  httpMethod?: string | null;
  httpPath?: string | null;
  requestBody?: unknown;
  responseBody?: unknown;
  responseStatus?: number | null;
}

export interface DownstreamCall {
  spanId?: string;
  calledService?: string;
  httpMethod?: string;
  httpPath?: string;
  requestCapture?: unknown;
  responseCapture?: unknown;
}

export interface ReplayBundle {
  replayId: string;
  traceId: string;
  serviceId: string;
  rootSpan: RootSpanInfo;
  downstreamCalls: DownstreamCall[];
  recordedAt: string;
}

export interface TraceBundleSummary {
  replayId: string;
  traceId: string;
  serviceId: string;
  recordedAt: string;
}

export type TraceFieldChangeType = 'ADDED' | 'REMOVED' | 'CHANGED';

export interface TraceFieldDiff {
  path: string;
  changeType: TraceFieldChangeType;
  oldValue: unknown;
  newValue: unknown;
}

export interface TraceDiff {
  fieldDiffs: TraceFieldDiff[];
  diffCount: number;
}

export interface ReplayResult {
  replayId: string;
  diff: TraceDiff;
  actualStatusCode: number;
}

export type TimeRangePreset = '1h' | '6h' | '24h';
