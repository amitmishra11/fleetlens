package com.fleetlens.config.api;

import java.util.List;
import java.util.Map;

public record ConfigMatrixResponse(List<String> environments, List<MatrixRow> keys) {

    public record MatrixRow(String key, Map<String, Object> values, boolean hasDrift) {
    }
}
