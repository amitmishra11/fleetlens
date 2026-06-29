package com.fleetlens.memory.jmx;

import java.time.Instant;

public record HeapDumpRecord(String serviceId, String filePath, Instant triggeredAt) {
}
