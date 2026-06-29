package com.fleetlens.trace.api;

public record ReplayRequest(Integer mockPort, String targetBaseUrl) {
}
