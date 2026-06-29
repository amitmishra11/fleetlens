package com.fleetlens.memory.correlator;

import java.util.List;

/**
 * Simple least-squares slope over a series of (index, value) pairs, normalized
 * by the mean value so trends are comparable across different magnitudes
 * (heap MB vs raw lag counts).
 */
public final class TrendCalculator {

    private TrendCalculator() {}

    public static double linearTrend(List<Double> values) {
        int n = values.size();
        if (n < 2) {
            return 0.0;
        }

        double meanX = (n - 1) / 2.0;
        double meanY = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double numerator = 0.0;
        double denominator = 0.0;
        for (int i = 0; i < n; i++) {
            double dx = i - meanX;
            numerator += dx * (values.get(i) - meanY);
            denominator += dx * dx;
        }

        if (denominator == 0.0) {
            return 0.0;
        }

        double slope = numerator / denominator;
        if (meanY == 0.0) {
            return 0.0;
        }
        return slope / meanY;
    }
}
