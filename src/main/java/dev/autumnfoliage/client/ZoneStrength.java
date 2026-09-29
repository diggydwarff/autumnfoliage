package dev.autumnfoliage.client;

import dev.autumnfoliage.config.AutumnConfig;
import dev.autumnfoliage.config.AutumnRange;
import dev.autumnfoliage.config.RuntimeSettings;
import net.minecraft.core.BlockPos;

public final class ZoneStrength {
    private ZoneStrength() {}

    public static double at(BlockPos pos) {
        RuntimeSettings settings = AutumnConfig.runtime();
        if (!settings.enabled()) {
            return 0.0;
        }
        // An empty range list intentionally means "all world". This keeps the default
        // useful without requiring users to invent giant coordinate bounds.
        if (settings.ranges().isEmpty()) {
            return 1.0;
        }

        int coordinate = settings.axis().coordinate(pos);
        double strongest = 0.0;
        for (AutumnRange range : settings.ranges()) {
            strongest = Math.max(strongest, range.strengthAt(coordinate));
            if (strongest >= 1.0) {
                return 1.0;
            }
        }
        return strongest;
    }
}
