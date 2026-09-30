package dev.autumnfoliage.config;

import java.util.List;

/** Immutable copy of the editable client configuration used by the custom settings UI. */
public record ClientConfigSnapshot(
        boolean enabled,
        Axis axis,
        List<AutumnRange> ranges,
        boolean forceTintUntintedModels,
        double leafStrength,
        double foliageVibrancy,
        double foliageBrightness,
        double saplingStrength,
        double grassStrength,
        double vineAndShrubStrength,
        double evergreenStrength,
        boolean autumnalTropics,
        int colorPatchSize,
        boolean calendarTimingEnabled,
        String autumnStartDate,
        String autumnEndDate,
        int seasonBlendDays,
        int seasonalVariationDays,
        List<String> speciesTimingOffsets,
        List<String> forceInclude,
        List<String> forceExclude,
        List<String> evergreenKeywords,
        List<String> tropicalKeywords,
        List<String> tropicalBiomeKeywords
) {
    public ClientConfigSnapshot {
        ranges = List.copyOf(ranges);
        speciesTimingOffsets = List.copyOf(speciesTimingOffsets);
        forceInclude = List.copyOf(forceInclude);
        forceExclude = List.copyOf(forceExclude);
        evergreenKeywords = List.copyOf(evergreenKeywords);
        tropicalKeywords = List.copyOf(tropicalKeywords);
        tropicalBiomeKeywords = List.copyOf(tropicalBiomeKeywords);
    }
}
