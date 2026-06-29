package com.fleetlens.trace.capture;

import java.time.Instant;
import java.util.List;

public record ReplayBundle(
        String replayId,
        String traceId,
        String serviceId,
        RootSpanInfo rootSpan,
        List<DownstreamCall> downstreamCalls,
        Instant recordedAt
) {
}
