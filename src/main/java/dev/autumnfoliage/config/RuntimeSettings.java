package dev.autumnfoliage.config;

import java.util.List;
import java.util.Set;

public record RuntimeSettings(
        boolean enabled,
        Axis axis,
        List<AutumnRange> ranges,
        boolean forceTintUntintedModels,
        double leafStrength,
        double saplingStrength,
        double grassStrength,
        double vineAndShrubStrength,
        double evergreenStrength,
        int colorPatchSize,
        Set<String> forceInclude,
        Set<String> forceExclude,
        List<String> evergreenKeywords,
        List<String> tropicalKeywords
) {
    public static RuntimeSettings defaults() {
        return new RuntimeSettings(
                true,
                Axis.Z,
                List.of(
                        new AutumnRange(-18000, -9000, 1500),
                        new AutumnRange(7000, 16000, 1500)
                ),
                true,
                1.0,
                0.85,
                0.42,
                0.65,
                0.12,
                10,
                Set.of(),
                Set.of(),
                List.of("spruce", "pine", "fir", "cedar", "redwood", "sequoia", "cypress", "juniper", "hemlock", "conifer", "evergreen"),
                List.of("jungle", "palm", "coconut", "banana", "tropical")
        );
    }
}
