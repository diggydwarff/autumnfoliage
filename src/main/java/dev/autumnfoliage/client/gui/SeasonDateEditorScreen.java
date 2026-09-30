package dev.autumnfoliage.client.gui;

import dev.autumnfoliage.client.SeasonalTiming;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BiConsumer;

/** Focused MM-dd editor for the optional real-world autumn calendar window. */
final class SeasonDateEditorScreen extends Screen {
    private final Screen parent;
    private final String initialStart;
    private final String initialEnd;
    private final BiConsumer<String, String> onSave;

    private EditBox startBox;
    private EditBox endBox;
    private Button doneButton;
    private String validationMessage = "";

    SeasonDateEditorScreen(Screen parent, String start, String end, BiConsumer<String, String> onSave) {
        super(Component.literal("Autumn Calendar Dates"));
        this.parent = parent;
        this.initialStart = start;
        this.initialEnd = end;
        this.onSave = onSave;
    }

    @Override
    protected void init() {
        int width = Math.min(220, Math.max(160, this.width - 60));
        int left = (this.width - width) / 2;
        int top = Math.max(88, this.height / 2 - 56);

        startBox = new EditBox(this.font, left, top, width, 20, Component.literal("Autumn start date"));
        startBox.setMaxLength(5);
        startBox.setValue(initialStart);
        startBox.setResponder(value -> validate());
        addRenderableWidget(startBox);

        endBox = new EditBox(this.font, left, top + 42, width, 20, Component.literal("Autumn end date"));
        endBox.setMaxLength(5);
        endBox.setValue(initialEnd);
        endBox.setResponder(value -> validate());
        addRenderableWidget(endBox);

        int footerY = Math.min(this.height - 30, top + 86);
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> Minecraft.getInstance().setScreen(parent))
                .bounds(left, footerY, (width - 6) / 2, 20)
                .build());
        doneButton = addRenderableWidget(Button.builder(Component.literal("Done"), b -> save())
                .bounds(left + (width - 6) / 2 + 6, footerY, (width - 6) / 2, 20)
                .build());

        validate();
    }

    private void validate() {
        if (doneButton == null || startBox == null || endBox == null) {
            return;
        }
        String start = startBox.getValue().trim();
        String end = endBox.getValue().trim();
        if (!SeasonalTiming.isValidMonthDay(start)) {
            validationMessage = "Invalid start date. Use MM-dd, e.g. 09-01.";
            doneButton.active = false;
            return;
        }
        if (!SeasonalTiming.isValidMonthDay(end)) {
            validationMessage = "Invalid end date. Use MM-dd, e.g. 11-30.";
            doneButton.active = false;
            return;
        }
        validationMessage = "";
        doneButton.active = true;
    }

    private void save() {
        validate();
        if (!doneButton.active) {
            return;
        }
        onSave.accept(startBox.getValue().trim(), endBox.getValue().trim());
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(260, Math.max(190, this.width - 32));
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(48, this.height / 2 - 88);
        int bottom = Math.min(this.height - 8, top + 176);
        graphics.fill(left, top, left + panelWidth, bottom, 0x70000000);

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 10, 0xFFFFFFFF);
        if (startBox != null) {
            graphics.drawString(this.font, "Start (MM-dd)", startBox.getX(), startBox.getY() - 11, 0xFFD0D0D0, false);
        }
        if (endBox != null) {
            graphics.drawString(this.font, "End (MM-dd)", endBox.getX(), endBox.getY() - 11, 0xFFD0D0D0, false);
        }
        if (!validationMessage.isBlank()) {
            graphics.drawCenteredString(this.font, validationMessage, this.width / 2,
                    Math.min(this.height - 44, (endBox == null ? top + 120 : endBox.getY() + 26)), 0xFFFF8080);
        }
    }
}
