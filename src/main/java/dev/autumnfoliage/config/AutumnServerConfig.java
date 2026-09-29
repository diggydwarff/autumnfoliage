package dev.autumnfoliage.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional server-side zone policy. NeoForge's SERVER config type syncs this config to clients
 * that also have the mod. A tiny optional payload is used only to tell the client that these
 * synced values should override its local fallback zones.
 */
public final class AutumnServerConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.EnumValue<Axis> AXIS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> RANGES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("zones");
        ENABLED = builder
                .comment(
                        "Server-authoritative autumn zone switch.",
                        "Only used by clients that have Autumn Foliage installed.")
                .define("enabled", true);
        AXIS = builder
                .comment("World axis used for coordinate bands. Z is the usual north/south setup.")
                .defineEnum("axis", Axis.Z);
        RANGES = builder
                .comment(
                        "Autumn-active coordinate ranges. Any number of ranges is supported.",
                        "Format: min,max,fadeDistance",
                        "An EMPTY list means the entire world is autumn-active.",
                        "When ranges are present, full autumn applies inside each range and fades smoothly outside its edges.",
                        "Overlapping ranges use the strongest effect rather than stacking.")
                .defineListAllowEmpty(
                        "ranges",
                        List.of(),
                        () -> "-18000,-9000,1500",
                        AutumnServerConfig::isRangeString
                );
        builder.pop();

        SPEC = builder.build();
    }

    private AutumnServerConfig() {}

    public static ZoneSettings snapshot() {
        List<AutumnRange> parsed = new ArrayList<>();
        for (String raw : RANGES.get()) {
            AutumnConfig.parseRange(raw).ifPresent(parsed::add);
        }
        return new ZoneSettings(ENABLED.get(), AXIS.get(), List.copyOf(parsed));
    }

    private static boolean isRangeString(Object value) {
        return value instanceof String s && AutumnConfig.parseRange(s).isPresent();
    }
}
