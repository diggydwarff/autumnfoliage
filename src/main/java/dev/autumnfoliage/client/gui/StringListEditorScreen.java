package dev.autumnfoliage.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Player-friendly one-item-per-row editor for compatibility lists. */
final class StringListEditorScreen extends Screen {
    enum Kind {
        FORCE_INCLUDE("Force Include Blocks", "Exact block IDs Autumn Foliage should always process.", true, false),
        FORCE_EXCLUDE("Force Exclude Blocks", "Exact block IDs that should never be recolored.", true, false),
        EVERGREEN_KEYWORDS("Evergreen Keywords", "Name fragments used to recognize conifers and evergreens.", false, false),
        TROPICAL_KEYWORDS("Tropical Block Keywords", "Name fragments used to recognize tropical vegetation.", false, false),
        TROPICAL_BIOME_KEYWORDS("Tropical Biome Keywords", "Biome name fragments treated as tropical when Autumnal Tropics is off.", false, false),
        SPECIES_TIMING_OFFSETS("Species Timing Offsets", "Tree-name timing offsets in days, e.g. birch=-5 or oak=2.", false, true);

        final String title;
        final String description;
        final boolean resourceIds;
        final boolean timingOffsets;

        Kind(String title, String description, boolean resourceIds, boolean timingOffsets) {
            this.title = title;
            this.description = description;
            this.resourceIds = resourceIds;
            this.timingOffsets = timingOffsets;
        }
    }

    private final Screen parent;
    private final Kind kind;
    private final List<String> items;
    private final Consumer<List<String>> onSave;
    private final List<String> defaults;

    private int page;
    private int rowsPerPage;
    private Button saveButton;
    private String validationMessage = "";
    private int panelLeft;
    private int panelRight;

    StringListEditorScreen(Screen parent, Kind kind, List<String> source, Consumer<List<String>> onSave, List<String> defaults) {
        super(Component.literal(kind.title));
        this.parent = parent;
        this.kind = kind;
        this.items = new ArrayList<>(source);
        this.onSave = onSave;
        this.defaults = List.copyOf(defaults);
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(520, Math.max(240, this.width - 32));
        panelWidth = Math.min(panelWidth, Math.max(220, this.width - 8));
        panelLeft = (this.width - panelWidth) / 2;
        panelRight = panelLeft + panelWidth;
        rowsPerPage = Math.max(2, Math.min(7, (this.height - 154) / 28));

        int pages = pageCount();
        page = Math.max(0, Math.min(page, pages - 1));
        int start = page * rowsPerPage;
        int end = Math.min(items.size(), start + rowsPerPage);
        int y = 76;

        for (int index = start; index < end; index++) {
            final int itemIndex = index;
            int rowY = y + (itemIndex - start) * 28;
            int removeWidth = 60;
            EditBox box = new EditBox(this.font, panelLeft + 30, rowY, panelWidth - 30 - removeWidth - 6, 20,
                    Component.literal(kind.title + " item " + (itemIndex + 1)));
            box.setMaxLength(128);
            box.setValue(items.get(itemIndex));
            box.setResponder(value -> {
                items.set(itemIndex, value);
                validate();
            });
            addRenderableWidget(box);

            addRenderableWidget(Button.builder(Component.literal("Remove"), b -> {
                        items.remove(itemIndex);
                        page = Math.min(page, pageCount() - 1);
                        rebuildWidgets();
                    })
                    .bounds(panelRight - removeWidth, rowY, removeWidth, 20)
                    .build());
        }

        int controlsY = this.height - 60;
        Button previous = Button.builder(Component.literal("<"), b -> {
                    page--;
                    rebuildWidgets();
                })
                .bounds(panelLeft, controlsY, 32, 20)
                .build();
        previous.active = page > 0;
        addRenderableWidget(previous);

        Button next = Button.builder(Component.literal(">"), b -> {
                    page++;
                    rebuildWidgets();
                })
                .bounds(panelLeft + 36, controlsY, 32, 20)
                .build();
        next.active = page + 1 < pageCount();
        addRenderableWidget(next);

        addRenderableWidget(Button.builder(Component.literal("Add"), b -> {
                    items.add("");
                    page = (items.size() - 1) / rowsPerPage;
                    rebuildWidgets();
                })
                .bounds(panelRight - 72, controlsY, 72, 20)
                .build());

        int footerY = this.height - 32;
        int footerWidth = Math.min(90, Math.max(72, (this.width - 28) / 3));
        int footerGap = 6;
        int footerTotal = footerWidth * 3 + footerGap * 2;
        int footerLeft = (this.width - footerTotal) / 2;
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
                    items.clear();
                    items.addAll(defaults);
                    page = 0;
                    rebuildWidgets();
                })
                .bounds(footerLeft, footerY, footerWidth, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> Minecraft.getInstance().setScreen(parent))
                .bounds(footerLeft + footerWidth + footerGap, footerY, footerWidth, 20)
                .build());
        saveButton = addRenderableWidget(Button.builder(Component.literal("Done"), b -> save())
                .bounds(footerLeft + (footerWidth + footerGap) * 2, footerY, footerWidth, 20)
                .build());

        validate();
    }

    private int pageCount() {
        if (rowsPerPage <= 0) return 1;
        return Math.max(1, (items.size() + rowsPerPage - 1) / rowsPerPage);
    }

    private void validate() {
        if (saveButton == null) return;
        for (String raw : items) {
            String value = raw.trim();
            if (value.isEmpty()) continue;
            if (kind.resourceIds && !isResourceId(value)) {
                validationMessage = "Invalid block ID: " + value + "  (use namespace:path)";
                saveButton.active = false;
                return;
            }
            if (kind.timingOffsets && !isTimingOffset(value)) {
                validationMessage = "Invalid timing offset: " + value + "  (use keyword=days, -30 to 30)";
                saveButton.active = false;
                return;
            }
        }
        validationMessage = "";
        saveButton.active = true;
    }

    private void save() {
        validate();
        if (!saveButton.active) return;

        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        for (String raw : items) {
            String value = raw.trim().toLowerCase(Locale.ROOT);
            if (!value.isEmpty()) cleaned.add(value);
        }
        onSave.accept(List.copyOf(cleaned));
        Minecraft.getInstance().setScreen(parent);
    }

    private static boolean isTimingOffset(String value) {
        String[] parts = value.trim().split("=", -1);
        if (parts.length != 2 || parts[0].isBlank()) return false;
        try {
            int days = Integer.parseInt(parts[1].trim());
            return days >= -30 && days <= 30;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean isResourceId(String value) {
        if (value.indexOf(' ') >= 0) return false;
        int colon = value.indexOf(':');
        if (colon <= 0 || colon >= value.length() - 1 || value.indexOf(':', colon + 1) >= 0) return false;
        String namespace = value.substring(0, colon);
        String path = value.substring(colon + 1);
        return namespace.matches("[a-z0-9_.-]+") && path.matches("[a-z0-9/._-]+");
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Background/blur is already submitted by Screen's 1.21.8 render pipeline.
        graphics.fill(panelLeft - 8, 66, panelRight + 8, this.height - 40, 0x50000000);
        super.render(graphics, mouseX, mouseY, partialTick);

        // Keep labels above the blurred background/widget pass so they remain crisp at every GUI scale.
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);
        if (this.height >= 260) {
            graphics.drawCenteredString(this.font, kind.description, this.width / 2, 34, 0xFFD0D0D0);
        }

        int start = page * rowsPerPage;
        int end = Math.min(items.size(), start + rowsPerPage);
        int y = 76;
        for (int index = start; index < end; index++) {
            graphics.drawString(this.font, Integer.toString(index + 1), panelLeft + 6,
                    y + (index - start) * 28 + 6, 0xFFB8B8B8, false);
        }

        if (items.isEmpty()) {
            graphics.drawCenteredString(this.font, "No entries. Click Add to create one.", this.width / 2, 92, 0xFFB8B8B8);
        }

        String pageText = "Page " + (page + 1) + " / " + pageCount() + "  -  " + items.size() + " entries";
        graphics.drawCenteredString(this.font, pageText, this.width / 2, this.height - 72, 0xFFB8B8B8);

        if (!validationMessage.isBlank()) {
            graphics.drawCenteredString(this.font, validationMessage, this.width / 2, 52, 0xFFE06C75);
        }
    }
}
