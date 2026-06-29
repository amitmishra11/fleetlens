package com.fleetlens.trace.replay;

import com.fleetlens.trace.diff.TraceDiff;

public record ReplayResult(String replayId, TraceDiff diff, int actualStatusCode) {
}
