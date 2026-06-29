package com.fleetlens.memory.correlator;

import com.fleetlens.common.event.HeapLagCorrelationEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Bounded in-memory log of fired HeapLagCorrelationEvents. Not persisted —
 * there is no dedicated correlations table in this build (see README for the
 * tradeoff). A restart loses history; acceptable for this scope since the
 * correlation engine module (fleetlens-correlation) is the system of record
 * for incidents built from these events.
 */
@Component
public class CorrelationLog {

    private static final int MAX_ENTRIES = 500;

    private final Deque<HeapLagCorrelationEvent> events = new ArrayDeque<>(MAX_ENTRIES);

    public synchronized void record(HeapLagCorrelationEvent event) {
        if (events.size() >= MAX_ENTRIES) {
            events.removeFirst();
        }
        events.addLast(event);
    }

    public synchronized List<HeapLagCorrelationEvent> since(java.time.Instant since) {
        return List.copyOf(Collections.unmodifiableCollection(events)).stream()
                .filter(e -> !e.getOccurredAt().isBefore(since))
                .toList();
    }
}
