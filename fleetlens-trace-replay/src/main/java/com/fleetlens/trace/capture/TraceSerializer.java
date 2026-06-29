package com.fleetlens.trace.capture;

import com.fleetlens.common.util.JsonUtils;

import java.nio.charset.StandardCharsets;

/**
 * Serialises/deserialises a {@link ReplayBundle} to/from the byte[] stored in
 * the {@code trace_replays.replay_file} column.
 */
public final class TraceSerializer {

    private TraceSerializer() {}

    public static byte[] toBytes(ReplayBundle bundle) {
        return JsonUtils.toJson(bundle).getBytes(StandardCharsets.UTF_8);
    }

    public static ReplayBundle fromBytes(byte[] bytes) {
        try {
            String json = new String(bytes, StandardCharsets.UTF_8);
            return JsonUtils.MAPPER.readValue(json, ReplayBundle.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialise replay bundle", e);
        }
    }
}
