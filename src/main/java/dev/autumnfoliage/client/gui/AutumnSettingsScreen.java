package dev.autumnfoliage.client.gui;

import dev.autumnfoliage.client.ClientEvents;
import dev.autumnfoliage.client.SeasonalTiming;
import dev.autumnfoliage.config.AutumnConfig;
import dev.autumnfoliage.config.AutumnRange;
import dev.autumnfoliage.config.AutumnServerConfig;
import dev.autumnfoliage.config.Axis;
import dev.autumnfoliage.config.ClientConfigSnapshot;
import dev.autumnfoliage.config.RuntimeSettings;
import dev.autumnfoliage.network.ServerZoneOverride;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/**
 * Purpose-built Autumn Foliage settings screen.
 *
 * The generic NeoForge config editor remains excellent for raw config data, but this screen deliberately
 * presents the settings in player-facing groups and hides TOML/list implementation details.
 */
public final class AutumnSettingsScreen extends Screen {
    private static final int CONTROL_WIDTH = 132;
    private static final int TAB_GAP = 4;

    private final Screen parent;
    private final Draft draft;
    private final ClientConfigSnapshot defaults;
    private final boolean integratedOwner;
    private final List<RowText> rowText = new ArrayList<>();

    private Page page = Page.GENERAL;
    private int rangePage;
    private int panelLeft;
    private int panelRight;
    private int contentTop;
    private int rowHeight;
    private int footerY;
    private boolean compactLayout;
    private String pageMessage = "";

    public AutumnSettingsScreen(Screen parent) {
        super(Component.literal("Autumn Foliage Settings"));
        this.parent = parent;
        this.integratedOwner = Minecraft.getInstance().hasSingleplayerServer();
        this.draft = new Draft(AutumnConfig.clientSnapshot());
        this.defaults = AutumnConfig.defaultSnapshot();

        // A singleplayer world still has an integrated server and therefore receives the same
        // server-policy handshake as multiplayer. The world owner must not be treated like a remote
        // client: seed the draft from the effective world policy and allow it to be edited directly.
        if (integratedOwner && ServerZoneOverride.isActive()) {
            this.draft.applyEffectiveWorld(AutumnConfig.runtime());
        }
    }

    @Override
    protected void init() {
        rowText.clear();
        pageMessage = "";

        int panelWidth = Math.min(620, Math.max(250, this.width - 24));
        panelWidth = Math.min(panelWidth, Math.max(220, this.width - 8));
        panelLeft = (this.width - panelWidth) / 2;
        panelRight = panelLeft + panelWidth;

        compactLayout = this.height < 330;
        contentTop = compactLayout ? 70 : 80;
        rowHeight = compactLayout ? 23 : 36;
        footerY = this.height - 28;

        addTabs(panelWidth);

        switch (page) {
            case GENERAL -> addGeneralPage();
            case COLORS -> addColorsPage();
            case VEGETATION -> addVegetationPage();
            case SEASON -> addSeasonPage();
            case REGIONS -> addRegionsPage();
            case ADVANCED -> addAdvancedPage();
        }

        addFooter();
    }

    private void addTabs(int panelWidth) {
        int tabWidth = (panelWidth - TAB_GAP * (Page.values().length - 1)) / Page.values().length;
        int x = panelLeft;
        boolean shortTabs = panelWidth < 420;
        for (Page candidate : Page.values()) {
            Button button = Button.builder(Component.literal(shortTabs ? candidate.shortLabel : candidate.label), b -> {
                        page = candidate;
                        rangePage = 0;
                        rebuildWidgets();
                    })
                    .bounds(x, compactLayout ? 42 : 46, tabWidth, 20)
                    .build();
            button.active = candidate != page;
            addRenderableWidget(button);
            x += tabWidth + TAB_GAP;
        }
    }

    private void addFooter() {
        int y = footerY;
        int gap = 6;
        int small = Math.min(92, Math.max(72, (this.width - 24) / 3));
        int total = small * 3 + gap * 2;
        int x = (this.width - total) / 2;
        String resetLabel = small < 88 ? "Reset" : "Reset Page";
        String saveLabel = small < 88 ? "Save" : "Save & Close";

        Button reset = Button.builder(Component.literal(resetLabel), b -> resetCurrentPage())
                .bounds(x, y, small, 20)
                .build();
        reset.active = canResetCurrentPage();
        addRenderableWidget(reset);

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> closeWithoutSaving())
                .bounds(x + small + gap, y, small, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal(saveLabel), b -> saveAndClose())
                .bounds(x + (small + gap) * 2, y, small, 20)
                .build());
    }

    private void addGeneralPage() {
        int y = contentTop;
        boolean lockEnabled = serverLocksEnabled();
        boolean lockTropics = serverLocksTropics();
        RuntimeSettings runtime = AutumnConfig.runtime();

        y = addToggleRow(
                y,
                "Autumn Effect",
                "Master autumn on/off.",
                lockEnabled ? runtime.enabled() : draft.enabled,
                lockEnabled,
                value -> draft.enabled = value
        );

        y = addToggleRow(
                y,
                "Autumnal Tropics",
                "Include tropical foliage.",
                lockTropics ? runtime.autumnalTropics() : draft.autumnalTropics,
                lockTropics,
                value -> draft.autumnalTropics = value
        );

        addInfoRow(
                y,
                "Coverage",
                effectiveRanges().isEmpty()
                        ? "Entire world active. Add Regions to limit coverage."
                        : effectiveRanges().size() + " configured coordinate range" + (effectiveRanges().size() == 1 ? "" : "s") + "."
        );
    }

    private void addColorsPage() {
        int y = contentTop;
        boolean locked = serverLocksAppearance();
        RuntimeSettings runtime = AutumnConfig.runtime();

        y = addDoubleSliderRow(
                y,
                "Vibrancy",
                "Autumn color saturation.",
                locked ? runtime.foliageVibrancy() : draft.foliageVibrancy,
                0.50,
                1.50,
                locked,
                value -> draft.foliageVibrancy = value,
                AutumnSettingsScreen::percent
        );

        y = addDoubleSliderRow(
                y,
                "Brightness",
                "Foliage midtone brightness.",
                locked ? runtime.foliageBrightness() : draft.foliageBrightness,
                0.50,
                1.50,
                locked,
                value -> draft.foliageBrightness = value,
                AutumnSettingsScreen::percent
        );

        addIntSliderRow(
                y,
                "Color Patch Size",
                "Color patch size in blocks.",
                locked ? runtime.colorPatchSize() : draft.colorPatchSize,
                2,
                64,
                locked,
                value -> draft.colorPatchSize = value,
                value -> value + " blocks"
        );
    }

    private void addVegetationPage() {
        int y = contentTop;
        boolean locked = serverLocksAppearance();
        RuntimeSettings runtime = AutumnConfig.runtime();

        y = addDoubleSliderRow(y, "Leaves", "Leaf strength.",
                locked ? runtime.leafStrength() : draft.leafStrength, 0.0, 1.0, locked,
                value -> draft.leafStrength = value, AutumnSettingsScreen::percent);

        y = addDoubleSliderRow(y, "Saplings", "Sapling strength.",
                locked ? runtime.saplingStrength() : draft.saplingStrength, 0.0, 1.0, locked,
                value -> draft.saplingStrength = value, AutumnSettingsScreen::percent);

        y = addDoubleSliderRow(y, "Grass & Ferns", "Grass/fern strength.",
                locked ? runtime.grassStrength() : draft.grassStrength, 0.0, 1.0, locked,
                value -> draft.grassStrength = value, AutumnSettingsScreen::percent);

        y = addDoubleSliderRow(y, "Vines & Shrubs", "Vine/shrub strength.",
                locked ? runtime.vineAndShrubStrength() : draft.vineAndShrubStrength, 0.0, 1.0, locked,
                value -> draft.vineAndShrubStrength = value, AutumnSettingsScreen::percent);

        addDoubleSliderRow(y, "Evergreens", "Evergreen strength.",
                locked ? runtime.evergreenStrength() : draft.evergreenStrength, 0.0, 1.0, locked,
                value -> draft.evergreenStrength = value, AutumnSettingsScreen::percent);
    }

    private void addSeasonPage() {
        int y = contentTop;
        java.time.LocalDate today = SeasonalTiming.currentDate();
        String todayText = today.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)
                + " " + today.getDayOfMonth() + ", " + today.getYear();

        y = addToggleRow(
                y,
                "Calendar Season",
                "Use local date (today " + todayText + ").",
                draft.calendarTimingEnabled,
                false,
                value -> draft.calendarTimingEnabled = value
        );

        rowText.add(new RowText(y, "Season Dates", "Start/end of the real-world autumn window.", false));
        String dateLabel = SeasonalTiming.displayMonthDay(draft.autumnStartDate)
                + " → " + SeasonalTiming.displayMonthDay(draft.autumnEndDate);
        addRenderableWidget(Button.builder(Component.literal(dateLabel), b ->
                        Minecraft.getInstance().setScreen(new SeasonDateEditorScreen(this,
                                draft.autumnStartDate, draft.autumnEndDate, (start, end) -> {
                            draft.autumnStartDate = start;
                            draft.autumnEndDate = end;
                        })))
                .bounds(panelRight - CONTROL_WIDTH, controlY(y), CONTROL_WIDTH, 20)
                .build());
        y += rowHeight;

        y = addIntSliderRow(
                y,
                "Blend Time",
                "Fade-in/out duration at each end.",
                draft.seasonBlendDays,
                0,
                90,
                false,
                value -> draft.seasonBlendDays = value,
                value -> value + (value == 1 ? " day" : " days")
        );

        y = addIntSliderRow(
                y,
                "Local Variation",
                "Small timing differences between stands.",
                draft.seasonalVariationDays,
                0,
                14,
                false,
                value -> draft.seasonalVariationDays = value,
                value -> "±" + value + (value == 1 ? " day" : " days")
        );

        addListEditorRow(
                y,
                "Species Timing",
                "Small earlier/later offsets by tree name.",
                StringListEditorScreen.Kind.SPECIES_TIMING_OFFSETS,
                draft.speciesTimingOffsets
        );

    }

    private void addRegionsPage() {
        boolean locked = serverLocksZones();
        Axis shownAxis = locked ? AutumnConfig.runtime().axis() : draft.axis;
        List<AutumnRange> shownRanges = effectiveRanges();

        int y = contentTop;
        rowText.add(new RowText(y, "Coordinate Axis", "Coordinate axis for ranges.", locked));
        Button axis = Button.builder(Component.literal((locked ? "Server: " : "") + shownAxis.name()), b -> {
                    draft.axis = draft.axis == Axis.X ? Axis.Z : Axis.X;
                    b.setMessage(Component.literal(draft.axis.name()));
                })
                .bounds(panelRight - CONTROL_WIDTH, controlY(y), CONTROL_WIDTH, 20)
                .build();
        axis.active = !locked;
        addRenderableWidget(axis);
        y += rowHeight;

        if (shownRanges.isEmpty()) {
            pageMessage = locked
                    ? "Server coverage: whole world."
                    : "No ranges: the whole world is autumn-active.";
            if (!locked) {
                addRenderableWidget(Button.builder(Component.literal("Add First Range"), b -> openRangeEditor(-1, new AutumnRange(0, 1000, 500)))
                        .bounds(panelLeft, y + (compactLayout ? 15 : 18), panelRight - panelLeft, 20)
                        .build());
            }
            return;
        }

        pageMessage = locked
                ? "Server controls the active autumn ranges."
                : "Only the configured ranges are autumn-active.";

        int rowPitch = compactLayout ? 23 : 27;
        int rangeListTop = y + (compactLayout ? 15 : 18);
        int availableForRows = Math.max(rowPitch, footerY - rangeListTop - 26);
        int rowsPerPage = Math.max(1, Math.min(5, availableForRows / rowPitch));
        int pages = Math.max(1, (shownRanges.size() + rowsPerPage - 1) / rowsPerPage);
        rangePage = Math.max(0, Math.min(rangePage, pages - 1));
        int start = rangePage * rowsPerPage;
        int end = Math.min(shownRanges.size(), start + rowsPerPage);

        y = rangeListTop;
        for (int i = start; i < end; i++) {
            final int index = i;
            AutumnRange range = shownRanges.get(index);
            int rowY = y + (index - start) * rowPitch;
            String summary = "Range " + (index + 1) + ": " + formatNumber(range.min()) + " to " + formatNumber(range.max())
                    + "   |   fade " + formatNumber(range.fadeDistance());
            int removeWidth = compactLayout ? 52 : 60;
            Button edit = Button.builder(Component.literal(summary), b -> openRangeEditor(index, range))
                    .bounds(panelLeft, rowY, locked ? panelRight - panelLeft : panelRight - panelLeft - removeWidth - 4, 20)
                    .build();
            edit.active = !locked;
            addRenderableWidget(edit);

            if (!locked) {
                addRenderableWidget(Button.builder(Component.literal("Remove"), b -> {
                            draft.ranges.remove(index);
                            int newPages = Math.max(1, (draft.ranges.size() + rowsPerPage - 1) / rowsPerPage);
                            rangePage = Math.min(rangePage, newPages - 1);
                            rebuildWidgets();
                        })
                        .bounds(panelRight - removeWidth, rowY, removeWidth, 20)
                        .build());
            }
        }

        int controlsY = y + rowsPerPage * rowPitch + 2;
        if (pages > 1) {
            Button previous = Button.builder(Component.literal("<"), b -> {
                        rangePage--;
                        rebuildWidgets();
                    })
                    .bounds(panelLeft, controlsY, 28, 20)
                    .build();
            previous.active = rangePage > 0;
            addRenderableWidget(previous);

            Button next = Button.builder(Component.literal(">"), b -> {
                        rangePage++;
                        rebuildWidgets();
                    })
                    .bounds(panelLeft + 32, controlsY, 28, 20)
                    .build();
            next.active = rangePage + 1 < pages;
            addRenderableWidget(next);
            rowText.add(new RowText(controlsY, "Page " + (rangePage + 1) + " of " + pages, "", false, panelLeft + 70));
        }

        if (!locked) {
            addRenderableWidget(Button.builder(Component.literal("Add Range"), b -> openRangeEditor(-1, new AutumnRange(0, 1000, 500)))
                    .bounds(panelRight - 112, controlsY, 112, 20)
                    .build());
        }
    }

    private void addAdvancedPage() {
        int y = contentTop;

        y = addToggleRow(
                y,
                "Tint Untinted Vegetation",
                "Broader mod foliage support.",
                draft.forceTintUntintedModels,
                false,
                value -> draft.forceTintUntintedModels = value
        );

        y = addListEditorRow(y, "Force Include", "Always recolor block IDs.",
                StringListEditorScreen.Kind.FORCE_INCLUDE, draft.forceInclude);
        y = addListEditorRow(y, "Force Exclude", "Never recolor block IDs.",
                StringListEditorScreen.Kind.FORCE_EXCLUDE, draft.forceExclude);
        y = addListEditorRow(y, "Evergreen Keywords", "Detect evergreen names.",
                StringListEditorScreen.Kind.EVERGREEN_KEYWORDS, draft.evergreenKeywords);
        y = addListEditorRow(y, "Tropical Block Keywords", "Detect tropical block names.",
                StringListEditorScreen.Kind.TROPICAL_KEYWORDS, draft.tropicalKeywords);
        addListEditorRow(y, "Tropical Biome Keywords", "Detect tropical biome names.",
                StringListEditorScreen.Kind.TROPICAL_BIOME_KEYWORDS, draft.tropicalBiomeKeywords);
    }

    private int addToggleRow(int y, String label, String description, boolean value, boolean locked, java.util.function.Consumer<Boolean> setter) {
        rowText.add(new RowText(y, label, description, locked));
        final boolean[] current = {value};
        Button toggle = Button.builder(Component.literal((locked ? "Server: " : "") + onOff(value)), b -> {
                    current[0] = !current[0];
                    setter.accept(current[0]);
                    b.setMessage(Component.literal(onOff(current[0])));
                })
                .bounds(panelRight - CONTROL_WIDTH, controlY(y), CONTROL_WIDTH, 20)
                .build();
        toggle.active = !locked;
        addRenderableWidget(toggle);
        return y + rowHeight;
    }

    private int addDoubleSliderRow(int y, String label, String description, double value, double min, double max,
                                   boolean locked, DoubleConsumer setter, java.util.function.Function<Double, String> formatter) {
        rowText.add(new RowText(y, label, description, locked));
        ValueSlider slider = new ValueSlider(panelRight - CONTROL_WIDTH, controlY(y), CONTROL_WIDTH, 20,
                value, min, max, setter, formatter);
        slider.active = !locked;
        if (locked) {
            slider.setPrefix("Server: ");
        }
        addRenderableWidget(slider);
        return y + rowHeight;
    }

    private int addIntSliderRow(int y, String label, String description, int value, int min, int max,
                                boolean locked, IntConsumer setter, java.util.function.IntFunction<String> formatter) {
        rowText.add(new RowText(y, label, description, locked));
        IntValueSlider slider = new IntValueSlider(panelRight - CONTROL_WIDTH, controlY(y), CONTROL_WIDTH, 20,
                value, min, max, setter, formatter);
        slider.active = !locked;
        if (locked) {
            slider.setPrefix("Server: ");
        }
        addRenderableWidget(slider);
        return y + rowHeight;
    }

    private int addListEditorRow(int y, String label, String description, StringListEditorScreen.Kind kind, List<String> list) {
        rowText.add(new RowText(y, label, description, false));
        addRenderableWidget(Button.builder(Component.literal("Edit (" + list.size() + ")"), b -> {
                    Minecraft.getInstance().setScreen(new StringListEditorScreen(this, kind, list, updated -> {
                        list.clear();
                        list.addAll(updated);
                    }, defaultsFor(kind)));
                })
                .bounds(panelRight - CONTROL_WIDTH, controlY(y), CONTROL_WIDTH, 20)
                .build());
        return y + rowHeight;
    }

    private void addInfoRow(int y, String label, String description) {
        rowText.add(new RowText(y, label, description, false));
    }

    private void openRangeEditor(int index, AutumnRange range) {
        Minecraft.getInstance().setScreen(new RangeEditorScreen(this, draft.axis, range, updated -> {
            if (index < 0) {
                draft.ranges.add(updated);
                // Let the next init clamp this to the final page regardless of the responsive rows-per-page value.
                rangePage = draft.ranges.size();
            } else {
                draft.ranges.set(index, updated);
            }
        }));
    }

    private List<String> defaultsFor(StringListEditorScreen.Kind kind) {
        return switch (kind) {
            case FORCE_INCLUDE -> defaults.forceInclude();
            case FORCE_EXCLUDE -> defaults.forceExclude();
            case EVERGREEN_KEYWORDS -> defaults.evergreenKeywords();
            case TROPICAL_KEYWORDS -> defaults.tropicalKeywords();
            case TROPICAL_BIOME_KEYWORDS -> defaults.tropicalBiomeKeywords();
            case SPECIES_TIMING_OFFSETS -> defaults.speciesTimingOffsets();
        };
    }

    private List<AutumnRange> effectiveRanges() {
        return serverLocksZones() ? AutumnConfig.runtime().ranges() : draft.ranges;
    }

    private boolean serverLocksEnabled() {
        return !integratedOwner && ServerZoneOverride.isActive() && !AutumnServerConfig.allowClientEnabledOverride();
    }

    private boolean serverLocksZones() {
        return !integratedOwner && ServerZoneOverride.isActive() && !AutumnServerConfig.allowClientZoneOverride();
    }

    private boolean serverLocksTropics() {
        return !integratedOwner && ServerZoneOverride.isActive() && !AutumnServerConfig.allowClientTropicalOverride();
    }

    private boolean serverLocksAppearance() {
        return !integratedOwner && ServerZoneOverride.isActive() && !AutumnServerConfig.allowClientAppearanceOverride();
    }

    private void resetCurrentPage() {
        switch (page) {
            case GENERAL -> {
                if (!serverLocksEnabled()) draft.enabled = defaults.enabled();
                if (!serverLocksTropics()) draft.autumnalTropics = defaults.autumnalTropics();
            }
            case COLORS -> {
                if (!serverLocksAppearance()) {
                    draft.foliageVibrancy = defaults.foliageVibrancy();
                    draft.foliageBrightness = defaults.foliageBrightness();
                    draft.colorPatchSize = defaults.colorPatchSize();
                }
            }
            case VEGETATION -> {
                if (!serverLocksAppearance()) {
                    draft.leafStrength = defaults.leafStrength();
                    draft.saplingStrength = defaults.saplingStrength();
                    draft.grassStrength = defaults.grassStrength();
                    draft.vineAndShrubStrength = defaults.vineAndShrubStrength();
                    draft.evergreenStrength = defaults.evergreenStrength();
                }
            }
            case SEASON -> {
                draft.calendarTimingEnabled = defaults.calendarTimingEnabled();
                draft.autumnStartDate = defaults.autumnStartDate();
                draft.autumnEndDate = defaults.autumnEndDate();
                draft.seasonBlendDays = defaults.seasonBlendDays();
                draft.seasonalVariationDays = defaults.seasonalVariationDays();
                replace(draft.speciesTimingOffsets, defaults.speciesTimingOffsets());
            }
            case REGIONS -> {
                if (!serverLocksZones()) {
                    draft.axis = defaults.axis();
                    draft.ranges.clear();
                    draft.ranges.addAll(defaults.ranges());
                    rangePage = 0;
                }
            }
            case ADVANCED -> {
                draft.forceTintUntintedModels = defaults.forceTintUntintedModels();
                replace(draft.forceInclude, defaults.forceInclude());
                replace(draft.forceExclude, defaults.forceExclude());
                replace(draft.evergreenKeywords, defaults.evergreenKeywords());
                replace(draft.tropicalKeywords, defaults.tropicalKeywords());
                replace(draft.tropicalBiomeKeywords, defaults.tropicalBiomeKeywords());
            }
        }
        rebuildWidgets();
    }

    private boolean canResetCurrentPage() {
        return switch (page) {
            case GENERAL -> !serverLocksEnabled() || !serverLocksTropics();
            case COLORS, VEGETATION -> !serverLocksAppearance();
            case SEASON -> true;
            case REGIONS -> !serverLocksZones();
            case ADVANCED -> true;
        };
    }

    private void saveAndClose() {
        ClientConfigSnapshot snapshot = draft.snapshot();
        AutumnConfig.applyClientSnapshot(snapshot);

        // In singleplayer the player owns the integrated server. Mirror world-facing values into
        // the SERVER config so the effective policy changes immediately and is preserved for that
        // world (and for clients if the world is later opened to LAN).
        if (integratedOwner) {
            AutumnServerConfig.applyIntegratedOwnerSnapshot(snapshot);
        }

        ClientEvents.refreshRendering();
        Minecraft.getInstance().setScreen(parent);
    }

    private void closeWithoutSaving() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void onClose() {
        closeWithoutSaving();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Minecraft 1.21.8 extracts the screen background in a separate render stratum before
        // render() is called. Calling renderBackground() here would submit a second blur in the
        // same frame and crash with "Can only blur once per frame".

        int panelTop = Math.max(64, contentTop - 6);
        int panelBottom = Math.max(panelTop + 24, footerY - 6);
        graphics.fill(panelLeft - 8, panelTop, panelRight + 8, panelBottom, 0x50000000);

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, compactLayout ? 12 : 16, 0xFFFFFFFF);
        String status;
        int statusColor;
        if (integratedOwner) {
            status = "Singleplayer world - all world settings are editable.";
            statusColor = 0xFFB8E6B8;
        } else if (ServerZoneOverride.isActive()) {
            status = "Server policy active - locked values are marked Server.";
            statusColor = 0xFFF0C674;
        } else {
            status = "Local client settings";
            statusColor = 0xFFD0D0D0;
        }
        graphics.drawCenteredString(this.font, status, this.width / 2, compactLayout ? 26 : 30, statusColor);

        if (!pageMessage.isBlank()) {
            graphics.drawString(this.font, pageMessage, panelLeft, contentTop + rowHeight, 0xFFD0D0D0, false);
        }

        for (RowText row : rowText) {
            int x = row.xOverride >= 0 ? row.xOverride : panelLeft;
            int labelColor = row.locked ? 0xFFB8B8B8 : 0xFFFFFFFF;
            int labelY = row.y + (compactLayout ? 7 : 2);
            graphics.drawString(this.font, row.label, x, labelY, labelColor, false);
            if (!compactLayout && !row.description.isBlank()) {
                graphics.drawString(this.font, row.description, x, row.y + 15, 0xFFC0C0C0, false);
            }
        }
    }

    private int controlY(int rowY) {
        return rowY + (compactLayout ? 1 : 5);
    }

    private static String onOff(boolean value) {
        return value ? "On" : "Off";
    }

    private static String percent(double value) {
        return Math.round(value * 100.0) + "%";
    }

    private static String formatNumber(int value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private static void replace(List<String> target, List<String> source) {
        target.clear();
        target.addAll(source);
    }

    private enum Page {
        GENERAL("General", "Main"),
        COLORS("Colors", "Color"),
        VEGETATION("Plants", "Plants"),
        SEASON("Season", "Season"),
        REGIONS("Regions", "Areas"),
        ADVANCED("Advanced", "Adv.");

        private final String label;
        private final String shortLabel;

        Page(String label, String shortLabel) {
            this.label = label;
            this.shortLabel = shortLabel;
        }
    }

    private static final class Draft {
        private boolean enabled;
        private Axis axis;
        private final List<AutumnRange> ranges;
        private boolean forceTintUntintedModels;
        private double leafStrength;
        private double foliageVibrancy;
        private double foliageBrightness;
        private double saplingStrength;
        private double grassStrength;
        private double vineAndShrubStrength;
        private double evergreenStrength;
        private boolean autumnalTropics;
        private int colorPatchSize;
        private boolean calendarTimingEnabled;
        private String autumnStartDate;
        private String autumnEndDate;
        private int seasonBlendDays;
        private int seasonalVariationDays;
        private final List<String> speciesTimingOffsets;
        private final List<String> forceInclude;
        private final List<String> forceExclude;
        private final List<String> evergreenKeywords;
        private final List<String> tropicalKeywords;
        private final List<String> tropicalBiomeKeywords;

        private Draft(ClientConfigSnapshot source) {
            enabled = source.enabled();
            axis = source.axis();
            ranges = new ArrayList<>(source.ranges());
            forceTintUntintedModels = source.forceTintUntintedModels();
            leafStrength = source.leafStrength();
            foliageVibrancy = source.foliageVibrancy();
            foliageBrightness = source.foliageBrightness();
            saplingStrength = source.saplingStrength();
            grassStrength = source.grassStrength();
            vineAndShrubStrength = source.vineAndShrubStrength();
            evergreenStrength = source.evergreenStrength();
            autumnalTropics = source.autumnalTropics();
            colorPatchSize = source.colorPatchSize();
            calendarTimingEnabled = source.calendarTimingEnabled();
            autumnStartDate = source.autumnStartDate();
            autumnEndDate = source.autumnEndDate();
            seasonBlendDays = source.seasonBlendDays();
            seasonalVariationDays = source.seasonalVariationDays();
            speciesTimingOffsets = new ArrayList<>(source.speciesTimingOffsets());
            forceInclude = new ArrayList<>(source.forceInclude());
            forceExclude = new ArrayList<>(source.forceExclude());
            evergreenKeywords = new ArrayList<>(source.evergreenKeywords());
            tropicalKeywords = new ArrayList<>(source.tropicalKeywords());
            tropicalBiomeKeywords = new ArrayList<>(source.tropicalBiomeKeywords());
        }

        private void applyEffectiveWorld(RuntimeSettings effective) {
            enabled = effective.enabled();
            axis = effective.axis();
            ranges.clear();
            ranges.addAll(effective.ranges());
            leafStrength = effective.leafStrength();
            foliageVibrancy = effective.foliageVibrancy();
            foliageBrightness = effective.foliageBrightness();
            saplingStrength = effective.saplingStrength();
            grassStrength = effective.grassStrength();
            vineAndShrubStrength = effective.vineAndShrubStrength();
            evergreenStrength = effective.evergreenStrength();
            autumnalTropics = effective.autumnalTropics();
            colorPatchSize = effective.colorPatchSize();
        }

        private ClientConfigSnapshot snapshot() {
            return new ClientConfigSnapshot(
                    enabled, axis, List.copyOf(ranges), forceTintUntintedModels,
                    leafStrength, foliageVibrancy, foliageBrightness, saplingStrength, grassStrength,
                    vineAndShrubStrength, evergreenStrength, autumnalTropics, colorPatchSize,
                    calendarTimingEnabled, autumnStartDate, autumnEndDate, seasonBlendDays, seasonalVariationDays,
                    List.copyOf(speciesTimingOffsets),
                    List.copyOf(forceInclude), List.copyOf(forceExclude), List.copyOf(evergreenKeywords),
                    List.copyOf(tropicalKeywords), List.copyOf(tropicalBiomeKeywords)
            );
        }
    }

    private record RowText(int y, String label, String description, boolean locked, int xOverride) {
        private RowText(int y, String label, String description, boolean locked) {
            this(y, label, description, locked, -1);
        }
    }

    private static final class ValueSlider extends AbstractSliderButton {
        private final double min;
        private final double max;
        private final DoubleConsumer setter;
        private final java.util.function.Function<Double, String> formatter;
        private String prefix = "";

        private ValueSlider(int x, int y, int width, int height, double initial, double min, double max,
                            DoubleConsumer setter, java.util.function.Function<Double, String> formatter) {
            super(x, y, width, height, Component.empty(), normalize(initial, min, max));
            this.min = min;
            this.max = max;
            this.setter = setter;
            this.formatter = formatter;
            updateMessage();
        }

        private void setPrefix(String prefix) {
            this.prefix = prefix;
            updateMessage();
        }

        private double actualValue() {
            double raw = min + value * (max - min);
            return Math.round(raw * 100.0) / 100.0;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(prefix + formatter.apply(actualValue())));
        }

        @Override
        protected void applyValue() {
            setter.accept(actualValue());
        }

        private static double normalize(double value, double min, double max) {
            if (max <= min) return 0.0;
            return Math.max(0.0, Math.min(1.0, (value - min) / (max - min)));
        }
    }

    private static final class IntValueSlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final IntConsumer setter;
        private final java.util.function.IntFunction<String> formatter;
        private String prefix = "";

        private IntValueSlider(int x, int y, int width, int height, int initial, int min, int max,
                               IntConsumer setter, java.util.function.IntFunction<String> formatter) {
            super(x, y, width, height, Component.empty(), normalize(initial, min, max));
            this.min = min;
            this.max = max;
            this.setter = setter;
            this.formatter = formatter;
            updateMessage();
        }

        private void setPrefix(String prefix) {
            this.prefix = prefix;
            updateMessage();
        }

        private int actualValue() {
            return (int) Math.round(min + value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(prefix + formatter.apply(actualValue())));
        }

        @Override
        protected void applyValue() {
            setter.accept(actualValue());
        }

        private static double normalize(int value, int min, int max) {
            if (max <= min) return 0.0;
            return Math.max(0.0, Math.min(1.0, (value - min) / (double) (max - min)));
        }
    }
}
