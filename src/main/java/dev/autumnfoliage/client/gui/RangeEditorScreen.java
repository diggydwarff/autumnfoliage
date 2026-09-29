package dev.autumnfoliage.client.gui;

import dev.autumnfoliage.config.AutumnRange;
import dev.autumnfoliage.config.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Small, explicit editor for one coordinate range. */
final class RangeEditorScreen extends Screen {
    private final Screen parent;
    private final Axis axis;
    private final AutumnRange initial;
    private final Consumer<AutumnRange> onSave;

    private EditBox startBox;
    private EditBox endBox;
    private EditBox fadeBox;
    private Button saveButton;
    private String validationMessage = "";
    private int left;

    RangeEditorScreen(Screen parent, Axis axis, AutumnRange initial, Consumer<AutumnRange> onSave) {
        super(Component.literal("Autumn Coordinate Range"));
        this.parent = parent;
        this.axis = axis;
        this.initial = initial;
        this.onSave = onSave;
    }

    @Override
    protected void init() {
        int width = Math.min(360, Math.max(240, this.width - 32));
        width = Math.min(width, Math.max(220, this.width - 8));
        left = (this.width - width) / 2;
        boolean compact = this.height < 260;
        int labelWidth = compact ? Math.min(138, width / 2) : Math.min(170, width / 2);
        int fieldX = left + labelWidth;
        int fieldWidth = width - labelWidth;
        int y = compact ? 66 : 78;
        int rowGap = compact ? 32 : 38;

        startBox = integerBox(fieldX, y, fieldWidth, true, Integer.toString(initial.min()), "Start coordinate");
        endBox = integerBox(fieldX, y + rowGap, fieldWidth, true, Integer.toString(initial.max()), "End coordinate");
        fadeBox = integerBox(fieldX, y + rowGap * 2, fieldWidth, false, Integer.toString(initial.fadeDistance()), "Fade distance");
        addRenderableWidget(startBox);
        addRenderableWidget(endBox);
        addRenderableWidget(fadeBox);

        int buttonY = this.height - 30;
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> Minecraft.getInstance().setScreen(parent))
                .bounds(this.width / 2 - 104, buttonY, 100, 20)
                .build());
        saveButton = addRenderableWidget(Button.builder(Component.literal("Save Range"), b -> save())
                .bounds(this.width / 2 + 4, buttonY, 100, 20)
                .build());

        validate();
    }

    private EditBox integerBox(int x, int y, int width, boolean signed, String initialValue, String narration) {
        EditBox box = new EditBox(this.font, x, y, width, 20, Component.literal(narration));
        box.setMaxLength(12);
        box.setFilter(value -> value.isEmpty() || (signed ? value.matches("-?\\d*") : value.matches("\\d*")));
        box.setValue(initialValue);
        box.setResponder(value -> validate());
        return box;
    }

    private void validate() {
        if (saveButton == null || startBox == null || endBox == null || fadeBox == null) return;
        try {
            int start = Integer.parseInt(startBox.getValue());
            int end = Integer.parseInt(endBox.getValue());
            int fade = Integer.parseInt(fadeBox.getValue());
            if (start > end) {
                validationMessage = "Start must be less than or equal to End.";
                saveButton.active = false;
                return;
            }
            if (fade < 0) {
                validationMessage = "Fade distance cannot be negative.";
                saveButton.active = false;
                return;
            }
            validationMessage = "";
            saveButton.active = true;
        } catch (NumberFormatException ex) {
            validationMessage = "Enter a whole number in all three fields.";
            saveButton.active = false;
        }
    }

    private void save() {
        validate();
        if (!saveButton.active) return;
        int start = Integer.parseInt(startBox.getValue());
        int end = Integer.parseInt(endBox.getValue());
        int fade = Integer.parseInt(fadeBox.getValue());
        onSave.accept(new AutumnRange(start, end, fade));
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        boolean compact = this.height < 260;

        graphics.fill(left - 8, compact ? 56 : 66, this.width - left + 8, this.height - 40, 0x50000000);
        super.render(graphics, mouseX, mouseY, partialTick);

        // Foreground text intentionally comes after widget rendering; otherwise Minecraft's blurred
        // menu background can make these labels look like part of the world behind the screen.
        graphics.drawCenteredString(this.font, this.title, this.width / 2, compact ? 14 : 20, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                "World " + axis.name() + " coordinate range",
                this.width / 2, compact ? 30 : 38, 0xD0D0D0);

        int y = compact ? 66 : 78;
        int rowGap = compact ? 32 : 38;
        graphics.drawString(this.font, "Start", left, y + 5, 0xFFFFFF, false);
        graphics.drawString(this.font, "End", left, y + rowGap + 5, 0xFFFFFF, false);
        graphics.drawString(this.font, "Fade Distance", left, y + rowGap * 2 + 5, 0xFFFFFF, false);

        if (!compact) {
            graphics.drawString(this.font, "First coordinate at full autumn strength.", left, y + 18, 0xC0C0C0, false);
            graphics.drawString(this.font, "Last coordinate at full autumn strength.", left, y + rowGap + 18, 0xC0C0C0, false);
            graphics.drawString(this.font, "Blocks used to blend smoothly back to normal foliage.", left, y + rowGap * 2 + 18, 0xC0C0C0, false);
        }

        if (!validationMessage.isBlank()) {
            graphics.drawCenteredString(this.font, validationMessage, this.width / 2, this.height - 47, 0xE06C75);
        }
    }
}
