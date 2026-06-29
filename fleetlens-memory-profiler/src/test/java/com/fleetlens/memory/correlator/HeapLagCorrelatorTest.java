package com.fleetlens.memory.correlator;

import com.fleetlens.common.event.HeapLagCorrelationEvent;
import com.fleetlens.memory.kafka.LagPollCompleteEvent;
import com.fleetlens.memory.store.MemorySnapshot;
import com.fleetlens.memory.store.MemorySnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HeapLagCorrelatorTest {

    private MemorySnapshotRepository repo;
    private ApplicationEventPublisher eventPublisher;
    private CorrelationLog correlationLog;
    private HeapLagCorrelator correlator;

    @BeforeEach
    void setUp() {
        repo = mock(MemorySnapshotRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        correlationLog = new CorrelationLog();
        correlator = new HeapLagCorrelator(repo, eventPublisher, correlationLog);
    }

    @Test
    void firesWhenBothTrendsRisingAboveThreshold() throws Exception {
        when(repo.findHeapSince(anyString(), any())).thenReturn(snapshotsWithHeap(100, 150, 200, 260, 320));
        when(repo.findLagSince(anyString(), any())).thenReturn(snapshotsWithLag(10, 20, 35, 55, 80));

        correlator.correlate(new LagPollCompleteEvent("order-service", "order-group", 80));

        ArgumentCaptor<HeapLagCorrelationEvent> captor = ArgumentCaptor.forClass(HeapLagCorrelationEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        HeapLagCorrelationEvent fired = captor.getValue();
        assertEquals("order-service", fired.getServiceId());
        assertEquals(1, correlationLog.since(Instant.now().minusSeconds(60)).size());
    }

    @Test
    void doesNotFireWhenHeapFlatAndLagRising() {
        when(repo.findHeapSince(anyString(), any())).thenReturn(snapshotsWithHeap(200, 200, 201, 199, 200));
        when(repo.findLagSince(anyString(), any())).thenReturn(snapshotsWithLag(10, 20, 35, 55, 80));

        correlator.correlate(new LagPollCompleteEvent("order-service", "order-group", 80));

        verify(eventPublisher, never()).publishEvent(any());
        assertFalse(correlationLog.since(Instant.now().minusSeconds(60)).size() > 0);
    }

    @Test
    void doesNotFireWhenBothTrendsFalling() {
        when(repo.findHeapSince(anyString(), any())).thenReturn(snapshotsWithHeap(320, 260, 200, 150, 100));
        when(repo.findLagSince(anyString(), any())).thenReturn(snapshotsWithLag(80, 55, 35, 20, 10));

        correlator.correlate(new LagPollCompleteEvent("order-service", "order-group", 10));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void doesNotFireWhenInsufficientSamples() {
        when(repo.findHeapSince(anyString(), any())).thenReturn(snapshotsWithHeap(100, 200));
        when(repo.findLagSince(anyString(), any())).thenReturn(snapshotsWithLag(10, 20));

        correlator.correlate(new LagPollCompleteEvent("order-service", "order-group", 20));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void linearTrendIsPositiveForRisingSeries() {
        double trend = TrendCalculator.linearTrend(List.of(100.0, 150.0, 200.0, 260.0, 320.0));
        assertEquals(true, trend > 0.05);
    }

    @Test
    void linearTrendIsZeroOrNegativeForFlatSeries() {
        double trend = TrendCalculator.linearTrend(List.of(200.0, 200.0, 201.0, 199.0, 200.0));
        assertEquals(true, Math.abs(trend) < 0.05);
    }

    private List<MemorySnapshot> snapshotsWithHeap(double... values) {
        List<MemorySnapshot> result = new java.util.ArrayList<>();
        for (double v : values) {
            MemorySnapshot snapshot = MemorySnapshot.heap("order-service", v, 1000.0, 5.0);
            result.add(snapshot);
        }
        return result;
    }

    private List<MemorySnapshot> snapshotsWithLag(long... values) {
        List<MemorySnapshot> result = new java.util.ArrayList<>();
        for (long v : values) {
            MemorySnapshot snapshot = MemorySnapshot.lag("order-group", "order-service", v);
            result.add(snapshot);
        }
        return result;
    }
}
