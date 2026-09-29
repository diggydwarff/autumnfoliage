package dev.autumnfoliage.config;

/** Appearance values that a server may optionally make authoritative. */
public record AppearanceSettings(
        double leafStrength,
        double foliageVibrancy,
        double foliageBrightness,
        double saplingStrength,
        double grassStrength,
        double vineAndShrubStrength,
        double evergreenStrength,
        int colorPatchSize
) {
    public static AppearanceSettings defaults() {
        return new AppearanceSettings(1.0, 1.35, 1.20, 0.9, 0.46, 0.72, 0.10, 10);
    }
}
