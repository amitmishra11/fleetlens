package com.fleetlens.memory.api;

import com.fleetlens.common.event.HeapLagCorrelationEvent;
import com.fleetlens.common.util.TimeUtils;
import com.fleetlens.memory.correlator.CorrelationLog;
import com.fleetlens.memory.jmx.HeapDumpLog;
import com.fleetlens.memory.jmx.HeapDumpRecord;
import com.fleetlens.memory.store.MemorySnapshot;
import com.fleetlens.memory.store.MemorySnapshotRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/memory")
public class MemoryProfilerController {

    private final MemorySnapshotRepository snapshotRepo;
    private final CorrelationLog correlationLog;
    private final HeapDumpLog heapDumpLog;

    public MemoryProfilerController(MemorySnapshotRepository snapshotRepo, CorrelationLog correlationLog,
                                     HeapDumpLog heapDumpLog) {
        this.snapshotRepo = snapshotRepo;
        this.correlationLog = correlationLog;
        this.heapDumpLog = heapDumpLog;
    }

    @GetMapping("/services/{serviceId}/timeline")
    public Map<String, Object> timeline(@PathVariable String serviceId,
                                         @RequestParam(required = false) String from,
                                         @RequestParam(required = false) String to) {
        Instant toInstant = to == null ? Instant.now() : TimeUtils.parseIsoOrNow(to);
        Instant fromInstant = from == null ? toInstant.minusSeconds(3600) : TimeUtils.parseIsoOrNow(from);

        List<MemorySnapshot> heapSamples = snapshotRepo.findHeapBetween(serviceId, fromInstant, toInstant);
        List<MemorySnapshot> lagSamples = snapshotRepo.findLagBetween(serviceId, fromInstant, toInstant);

        return Map.of(
                "serviceId", serviceId,
                "from", TimeUtils.toIso(fromInstant),
                "to", TimeUtils.toIso(toInstant),
                "heapSamples", heapSamples,
                "lagSamples", lagSamples
        );
    }

    @GetMapping("/services/{serviceId}/latest")
    public Map<String, Object> latest(@PathVariable String serviceId) {
        return snapshotRepo.findLatestForService(serviceId)
                .<Map<String, Object>>map(s -> Map.of("snapshot", s))
                .orElse(Map.of("snapshot", Map.of()));
    }

    @GetMapping("/correlations")
    public List<HeapLagCorrelationEvent> correlations(@RequestParam(required = false) String since) {
        Instant sinceInstant = since == null ? Instant.now().minusSeconds(86400) : TimeUtils.parseIsoOrNow(since);
        return correlationLog.since(sinceInstant);
    }

    @GetMapping("/heapdumps")
    public List<HeapDumpRecord> heapDumps() {
        return heapDumpLog.recent();
    }
}
