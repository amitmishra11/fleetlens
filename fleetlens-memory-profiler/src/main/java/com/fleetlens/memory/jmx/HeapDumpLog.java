package com.fleetlens.memory.jmx;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Bounded in-memory log of triggered heap dumps. Not persisted across restarts —
 * acceptable for this scope since heap dumps are best-effort diagnostics, not
 * the system of record (the .hprof file on disk is).
 */
@Component
public class HeapDumpLog {

    private static final int MAX_ENTRIES = 500;

    private final Deque<HeapDumpRecord> records = new ArrayDeque<>(MAX_ENTRIES);

    public synchronized void record(HeapDumpRecord record) {
        if (records.size() >= MAX_ENTRIES) {
            records.removeFirst();
        }
        records.addLast(record);
    }

    public synchronized List<HeapDumpRecord> recent() {
        return List.copyOf(Collections.unmodifiableCollection(records));
    }
}
