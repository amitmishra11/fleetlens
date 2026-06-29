package com.fleetlens.trace.api;

import com.fleetlens.common.util.JsonUtils;
import com.fleetlens.trace.diff.TraceDiff;
import com.fleetlens.trace.capture.ReplayBundle;
import com.fleetlens.trace.capture.RootSpanInfo;
import com.fleetlens.trace.replay.ReplayEngine;
import com.fleetlens.trace.replay.ReplayOptions;
import com.fleetlens.trace.replay.ReplayResult;
import com.fleetlens.trace.store.TraceReplay;
import com.fleetlens.trace.store.TraceReplayRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/traces")
public class TraceReplayController {

    private final TraceReplayRepository repository;
    private final ReplayEngine replayEngine;

    public TraceReplayController(TraceReplayRepository repository, ReplayEngine replayEngine) {
        this.repository = repository;
        this.replayEngine = replayEngine;
    }

    /**
     * Accepts a fully-formed {@link ReplayBundle} for demo/testing capture
     * without a live OTel agent. In production, capture normally happens via
     * {@link com.fleetlens.trace.capture.FleetLensSpanExporter} running
     * inside the target service.
     */
    @PostMapping("/capture/{serviceId}")
    public ResponseEntity<TraceBundleSummary> capture(@PathVariable String serviceId,
                                                       @RequestBody(required = false) ReplayBundle body) {
        ReplayBundle bundle = body != null ? withServiceId(body, serviceId) : emptyBundle(serviceId);
        TraceReplay saved = repository.save(TraceReplay.fromBundle(bundle));
        return ResponseEntity.status(HttpStatus.CREATED).body(toSummary(saved));
    }

    @GetMapping
    public List<TraceBundleSummary> list() {
        return repository.findAll().stream().map(this::toSummary).toList();
    }

    @GetMapping("/{replayId}")
    public ResponseEntity<ReplayBundle> get(@PathVariable UUID replayId) {
        return repository.findById(replayId)
                .map(TraceReplay::toBundle)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{replayId}/replay")
    public ResponseEntity<ReplayResult> replay(@PathVariable UUID replayId,
                                                @RequestBody(required = false) ReplayRequest body,
                                                @RequestParam(required = false) Integer mockPort,
                                                @RequestParam(required = false) String targetBaseUrl) {
        return repository.findById(replayId).map(entity -> {
            int resolvedPort = body != null && body.mockPort() != null ? body.mockPort()
                    : mockPort != null ? mockPort : 0;
            String resolvedBaseUrl = body != null && body.targetBaseUrl() != null ? body.targetBaseUrl()
                    : targetBaseUrl;
            if (resolvedBaseUrl == null || resolvedBaseUrl.isBlank()) {
                throw new IllegalArgumentException("targetBaseUrl is required (as a query param or in the request body)");
            }

            ReplayBundle bundle = entity.toBundle();
            ReplayResult result = replayEngine.replay(bundle, new ReplayOptions(resolvedPort, resolvedBaseUrl));

            entity.setLastDiff(JsonUtils.toJson(result.diff()));
            repository.save(entity);

            return ResponseEntity.ok(result);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{replayId}/diff")
    public ResponseEntity<TraceDiff> diff(@PathVariable UUID replayId) {
        return repository.findById(replayId)
                .map(entity -> {
                    if (entity.getLastDiff() == null) {
                        return ResponseEntity.noContent().<TraceDiff>build();
                    }
                    try {
                        TraceDiff diff = JsonUtils.MAPPER.readValue(entity.getLastDiff(), TraceDiff.class);
                        return ResponseEntity.ok(diff);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to deserialise stored trace diff", e);
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{replayId}")
    public ResponseEntity<Void> delete(@PathVariable UUID replayId) {
        if (!repository.existsById(replayId)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(replayId);
        return ResponseEntity.noContent().build();
    }

    private TraceBundleSummary toSummary(TraceReplay entity) {
        return new TraceBundleSummary(entity.getId(), entity.getTraceId(), entity.getServiceId(), entity.getRecordedAt());
    }

    private ReplayBundle withServiceId(ReplayBundle bundle, String serviceId) {
        return new ReplayBundle(
                bundle.replayId() != null ? bundle.replayId() : UUID.randomUUID().toString(),
                bundle.traceId() != null ? bundle.traceId() : UUID.randomUUID().toString(),
                serviceId,
                bundle.rootSpan(),
                bundle.downstreamCalls() != null ? bundle.downstreamCalls() : List.of(),
                bundle.recordedAt() != null ? bundle.recordedAt() : Instant.now()
        );
    }

    private ReplayBundle emptyBundle(String serviceId) {
        return new ReplayBundle(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                serviceId,
                new RootSpanInfo(null, null, null, null, null, null, null),
                List.of(),
                Instant.now()
        );
    }
}
