package com.fleetlens.trace.capture;

import com.fleetlens.trace.store.TraceReplay;
import com.fleetlens.trace.store.TraceReplayRepository;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Pragmatic OpenTelemetry {@link SpanExporter} that buffers spans by trace ID
 * and, once a trace's root span has ended, builds a {@link ReplayBundle} and
 * persists it via {@link TraceReplayRepository}.
 *
 * <p>This is a sidecar/demo integration, not production-grade OTel tooling:
 * a trace is considered "complete" the moment its root span (no valid parent)
 * shows up in an export batch with an end timestamp set.
 */
public class FleetLensSpanExporter implements SpanExporter {

    private final Map<String, List<SpanData>> spanBuffer = new ConcurrentHashMap<>();
    private final TraceReplayRepository repository;
    private final String serviceId;

    public FleetLensSpanExporter(TraceReplayRepository repository, String serviceId) {
        this.repository = repository;
        this.serviceId = serviceId;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        for (SpanData span : spans) {
            spanBuffer.computeIfAbsent(span.getTraceId(), id -> new java.util.concurrent.CopyOnWriteArrayList<>())
                    .add(span);
        }
        flushCompleteTraces();
        return CompletableResultCode.ofSuccess();
    }

    void flushCompleteTraces() {
        spanBuffer.entrySet().removeIf(entry -> {
            List<SpanData> spans = entry.getValue();
            SpanData root = findRoot(spans);
            if (root == null || root.getEndEpochNanos() <= 0) {
                return false;
            }
            ReplayBundle bundle = buildBundle(entry.getKey(), root, spans);
            repository.save(TraceReplay.fromBundle(bundle));
            return true;
        });
    }

    private SpanData findRoot(List<SpanData> spans) {
        return spans.stream()
                .filter(s -> !s.getParentSpanContext().isValid())
                .findFirst()
                .orElse(null);
    }

    private ReplayBundle buildBundle(String traceId, SpanData root, List<SpanData> spans) {
        RootSpanInfo rootSpan = toRootSpanInfo(root);

        List<DownstreamCall> downstreamCalls = spans.stream()
                .filter(s -> s != root)
                .map(this::toDownstreamCall)
                .collect(Collectors.toList());

        return new ReplayBundle(
                UUID.randomUUID().toString(),
                traceId,
                serviceId,
                rootSpan,
                downstreamCalls,
                Instant.now()
        );
    }

    private RootSpanInfo toRootSpanInfo(SpanData root) {
        Map<String, Object> attrs = attributesAsMap(root);
        String method = stringAttr(attrs, "http.method");
        String path = stringAttr(attrs, "http.route", "http.target", "http.path");
        Object status = attrs.get("http.status_code");
        Integer statusCode = status instanceof Number n ? n.intValue() : null;

        return new RootSpanInfo(
                root.getSpanId(),
                root.getName(),
                method,
                path,
                attrs.get("http.request.body"),
                attrs.get("http.response.body"),
                statusCode
        );
    }

    private DownstreamCall toDownstreamCall(SpanData span) {
        Map<String, Object> attrs = attributesAsMap(span);
        String method = stringAttr(attrs, "http.method");
        String path = stringAttr(attrs, "http.route", "http.target", "http.path");

        return new DownstreamCall(
                span.getSpanId(),
                span.getName(),
                method,
                path,
                attrs.get("http.request.body"),
                attrs.get("http.response.body")
        );
    }

    private Map<String, Object> attributesAsMap(SpanData span) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        span.getAttributes().forEach((key, value) -> result.put(key.getKey(), value));
        return result;
    }

    private String stringAttr(Map<String, Object> attrs, String... keys) {
        for (String key : keys) {
            Object value = attrs.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    public CapturedSpan toCapturedSpan(SpanData span) {
        return new CapturedSpan(
                span.getTraceId(),
                span.getSpanId(),
                span.getParentSpanContext().isValid() ? span.getParentSpanContext().getSpanId() : null,
                span.getName(),
                span.getKind() == null ? SpanKind.INTERNAL.name() : span.getKind().name(),
                span.getStartEpochNanos(),
                span.getEndEpochNanos(),
                attributesAsMap(span),
                span.getStatus().getStatusCode().name()
        );
    }

    @Override
    public CompletableResultCode flush() {
        flushCompleteTraces();
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        spanBuffer.clear();
        return CompletableResultCode.ofSuccess();
    }
}
