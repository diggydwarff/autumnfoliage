package dev.autumnfoliage.config;

import java.util.List;

public record ZoneSettings(
        boolean enabled,
        Axis axis,
        List<AutumnRange> ranges
) {
    public static ZoneSettings allWorld() {
        return new ZoneSettings(true, Axis.Z, List.of());
    }
}
