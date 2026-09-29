package dev.autumnfoliage.config;

public record AutumnRange(int min, int max, int fadeDistance) {
    public AutumnRange {
        if (min > max) {
            int t = min;
            min = max;
            max = t;
        }
        fadeDistance = Math.max(0, fadeDistance);
    }

    public double strengthAt(int coordinate) {
        if (coordinate >= min && coordinate <= max) {
            return 1.0;
        }
        if (fadeDistance <= 0) {
            return 0.0;
        }

        if (coordinate < min) {
            int distance = min - coordinate;
            if (distance >= fadeDistance) {
                return 0.0;
            }
            return smoothstep(1.0 - distance / (double) fadeDistance);
        }

        int distance = coordinate - max;
        if (distance >= fadeDistance) {
            return 0.0;
        }
        return smoothstep(1.0 - distance / (double) fadeDistance);
    }

    private static double smoothstep(double value) {
        double x = Math.max(0.0, Math.min(1.0, value));
        return x * x * (3.0 - 2.0 * x);
    }
}
