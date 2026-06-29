package com.fleetlens.memory.jmx;

import com.fleetlens.common.event.MemoryPressureEvent;
import com.fleetlens.memory.config.MemoryProfilerProperties;
import com.fleetlens.memory.config.ServiceJmxTarget;
import com.fleetlens.memory.store.MemorySnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JmxHeapPoller {

    private static final Logger log = LoggerFactory.getLogger(JmxHeapPoller.class);

    private final Map<String, JMXConnector> connectors = new ConcurrentHashMap<>();
    private final MemoryProfilerProperties properties;
    private final MemorySnapshotRepository snapshotRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final HeapDumpLog heapDumpLog;

    @Value("${fleetlens.memory.heap-threshold-percent:85}")
    private double heapThresholdPercent;

    public JmxHeapPoller(MemoryProfilerProperties properties, MemorySnapshotRepository snapshotRepo,
                          ApplicationEventPublisher eventPublisher, HeapDumpLog heapDumpLog) {
        this.properties = properties;
        this.snapshotRepo = snapshotRepo;
        this.eventPublisher = eventPublisher;
        this.heapDumpLog = heapDumpLog;
    }

    @Scheduled(fixedDelayString = "${fleetlens.memory.poll-interval-ms:30000}")
    public void poll() {
        for (ServiceJmxTarget svc : properties.getServices()) {
            try {
                pollService(svc);
            } catch (Exception e) {
                log.error("JMX poll failed for {}", svc.getId(), e);
                connectors.remove(svc.getId());
            }
        }
    }

    private void pollService(ServiceJmxTarget svc) throws Exception {
        JMXConnector connector = getOrCreateConnector(svc);
        MBeanServerConnection mbs;
        try {
            mbs = connector.getMBeanServerConnection();
        } catch (Exception e) {
            connectors.remove(svc.getId());
            connector = getOrCreateConnector(svc);
            mbs = connector.getMBeanServerConnection();
        }

        MemoryMXBean memBean = ManagementFactory.newPlatformMXBeanProxy(
                mbs, ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class);
        MemoryUsage heap = memBean.getHeapMemoryUsage();

        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getPlatformMXBeans(mbs, GarbageCollectorMXBean.class);
        long totalGcMs = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();

        double usedMb = heap.getUsed() / 1024.0 / 1024.0;
        double maxMb = heap.getMax() / 1024.0 / 1024.0;

        snapshotRepo.saveHeap(svc.getId(), usedMb, maxMb, totalGcMs);

        if (heap.getMax() > 0) {
            double usedPct = heap.getUsed() * 100.0 / heap.getMax();
            if (usedPct > heapThresholdPercent) {
                eventPublisher.publishEvent(new MemoryPressureEvent(svc.getId(), usedPct));
                triggerHeapDump(mbs, svc);
            }
        }
    }

    private JMXConnector getOrCreateConnector(ServiceJmxTarget svc) {
        return connectors.computeIfAbsent(svc.getId(), id -> {
            try {
                String url = "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi".formatted(svc.getJmxHost(), svc.getJmxPort());
                JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(url));
                return connector;
            } catch (Exception e) {
                throw new IllegalStateException("Unable to connect to JMX for " + svc.getId(), e);
            }
        });
    }

    private void triggerHeapDump(MBeanServerConnection mbs, ServiceJmxTarget svc) {
        try {
            com.sun.management.HotSpotDiagnosticMXBean diagBean = ManagementFactory.newPlatformMXBeanProxy(
                    mbs, "com.sun.management:type=HotSpotDiagnostic", com.sun.management.HotSpotDiagnosticMXBean.class);
            String fileName = "fleetlens-%s-%d.hprof".formatted(svc.getId(), System.currentTimeMillis());
            String path = Path.of(System.getProperty("java.io.tmpdir"), fileName).toString();
            diagBean.dumpHeap(path, true);
            heapDumpLog.record(new HeapDumpRecord(svc.getId(), path, Instant.now()));
            log.warn("Heap dump triggered for {} -> {}", svc.getId(), path);
        } catch (Exception e) {
            log.error("Heap dump failed for {} (best-effort, com.sun.management may not be exposed over JMX)", svc.getId(), e);
        }
    }
}
