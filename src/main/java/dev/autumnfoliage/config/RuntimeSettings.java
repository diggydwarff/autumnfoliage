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
                List.of(),
                true,
                1.0,
                0.9,
                0.46,
                0.72,
                0.10,
                10,
                Set.of(),
                Set.of(),
                List.of("spruce", "pine", "fir", "cedar", "redwood", "sequoia", "cypress", "juniper", "hemlock", "conifer", "evergreen"),
                List.of("jungle", "palm", "coconut", "banana", "tropical")
        );
    }
}
