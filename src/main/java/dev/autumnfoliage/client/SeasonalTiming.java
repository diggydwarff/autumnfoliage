package dev.autumnfoliage.client;

import dev.autumnfoliage.config.AutumnConfig;
import dev.autumnfoliage.config.RuntimeSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Optional real-world calendar envelope for the autumn effect.
 *
 * The current date comes from the client computer's local calendar. The date is cached and checked
 * from the client tick hook rather than during every block-color lookup, keeping this path cheap
 * enough for chunk rebuilds and Distant Horizons LOD generation.
 */
public final class SeasonalTiming {
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MM-dd", Locale.ROOT);
    private static final int CALENDAR_DAYS = 366; // Leap-year reference keeps Feb 29 valid.

    private static volatile LocalDate currentDate = LocalDate.now();
    private static volatile RuntimeSettings cachedSettings;
    private static final ConcurrentMap<Block, Integer> SPECIES_OFFSET_CACHE = new ConcurrentHashMap<>();
    private static volatile int cachedStartDay = dayOfYear("09-01");
    private static volatile int cachedEndDay = dayOfYear("11-30");

    private SeasonalTiming() {}

    /** Returns true when the OS-local calendar date changed since the previous check. */
    public static boolean updateCurrentDate() {
        LocalDate today = LocalDate.now();
        if (today.equals(currentDate)) {
            return false;
        }
        currentDate = today;
        return true;
    }

    /** Current OS-local date, mainly for the settings screen/status text. */
    public static LocalDate currentDate() {
        return currentDate;
    }

    /**
     * Seasonal multiplier for a block at a position. 1.0 means fully autumnal and 0.0 means the
     * calendar suppresses the autumn effect. If calendar timing is disabled this always returns 1.
     */
    public static double strength(BlockState state, BlockPos pos) {
        RuntimeSettings settings = AutumnConfig.runtime();
        if (!settings.calendarTimingEnabled()) {
            return 1.0;
        }

        ensureParsedDates(settings);

        int speciesOffset = speciesOffsetDays(state, settings.speciesTimingOffsets());
        int localOffset = localVariationDays(state, pos, settings.seasonalVariationDays(), settings.colorPatchSize());
        int totalOffset = speciesOffset + localOffset;

        int start = wrapDay(cachedStartDay + totalOffset);
        int end = wrapDay(cachedEndDay + totalOffset);
        int today = currentDayOfLeapYear(currentDate);

        int seasonLength = forwardDistance(start, end) + 1;
        int progress = forwardDistance(start, today);
        if (progress >= seasonLength) {
            return 0.0;
        }

        int blend = Math.max(0, settings.seasonBlendDays());
        blend = Math.min(blend, Math.max(0, (seasonLength - 1) / 2));
        if (blend == 0) {
            return 1.0;
        }

        double fadeIn = clamp01(progress / (double) blend);
        int remaining = seasonLength - 1 - progress;
        double fadeOut = clamp01(remaining / (double) blend);
        return smoothstep(Math.min(fadeIn, fadeOut));
    }

    /** Parses MM-dd using leap year 2000. Exposed for config/UI validation. */
    public static boolean isValidMonthDay(String value) {
        if (value == null || value.length() != 5) {
            return false;
        }
        try {
            MonthDay.parse(value, MONTH_DAY);
            return true;
        } catch (DateTimeParseException ignored) {
            return false;
        }
    }

    /** Converts an MM-dd string to a 1..366 day index using leap year 2000. */
    public static int dayOfYear(String value) {
        try {
            MonthDay monthDay = MonthDay.parse(value, MONTH_DAY);
            return monthDay.atYear(2000).getDayOfYear();
        } catch (DateTimeException ignored) {
            return 1;
        }
    }

    /** Converts a leap-year day index back to MM-dd for the settings UI. */
    public static String monthDayFromDayOfYear(int day) {
        int wrapped = wrapDay(day);
        return LocalDate.ofYearDay(2000, wrapped).format(MONTH_DAY);
    }

    public static String displayMonthDay(String value) {
        try {
            MonthDay monthDay = MonthDay.parse(value, MONTH_DAY);
            LocalDate date = monthDay.atYear(2000);
            return date.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)
                    + " " + date.getDayOfMonth();
        } catch (DateTimeException ignored) {
            return value;
        }
    }

    private static void ensureParsedDates(RuntimeSettings settings) {
        if (cachedSettings == settings) {
            return;
        }
        synchronized (SeasonalTiming.class) {
            if (cachedSettings == settings) {
                return;
            }
            cachedStartDay = dayOfYear(settings.autumnStartDate());
            cachedEndDay = dayOfYear(settings.autumnEndDate());
            SPECIES_OFFSET_CACHE.clear();
            cachedSettings = settings;
        }
    }

    private static int speciesOffsetDays(BlockState state, Map<String, Integer> offsets) {
        if (state == null || offsets.isEmpty()) {
            return 0;
        }
        return SPECIES_OFFSET_CACHE.computeIfAbsent(state.getBlock(), block -> {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null) {
                return 0;
            }
            String path = id.getPath().toLowerCase(Locale.ROOT);

            // Prefer the most specific match. This lets "dark_oak" override a generic "oak" entry.
            String best = null;
            int bestOffset = 0;
            for (Map.Entry<String, Integer> entry : offsets.entrySet()) {
                String keyword = entry.getKey();
                if (!keyword.isBlank() && path.contains(keyword) && (best == null || keyword.length() > best.length())) {
                    best = keyword;
                    bestOffset = entry.getValue();
                }
            }
            return bestOffset;
        });
    }

    private static int localVariationDays(BlockState state, BlockPos pos, int variationDays, int patchSize) {
        if (state == null || pos == null || variationDays <= 0) {
            return 0;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        int scale = Math.max(32, patchSize * 4);
        int x = Math.floorDiv(pos.getX(), scale);
        int z = Math.floorDiv(pos.getZ(), scale);
        int hash = 0x6D2B79F5;
        hash = 31 * hash + x;
        hash = 31 * hash + z;
        hash = 31 * hash + (id == null ? 0 : id.hashCode());
        hash ^= (hash >>> 16);

        int width = variationDays * 2 + 1;
        return Math.floorMod(hash, width) - variationDays;
    }

    private static int currentDayOfLeapYear(LocalDate date) {
        // Map the real month/day onto leap year 2000 so every configured MonthDay uses one stable
        // 366-day circular calendar regardless of the current year's leap status.
        return MonthDay.from(date).atYear(2000).getDayOfYear();
    }

    private static int forwardDistance(int from, int to) {
        return Math.floorMod(to - from, CALENDAR_DAYS);
    }

    private static int wrapDay(int day) {
        return Math.floorMod(day - 1, CALENDAR_DAYS) + 1;
    }

    private static double smoothstep(double value) {
        double t = clamp01(value);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
