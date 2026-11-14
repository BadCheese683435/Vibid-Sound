package com.vivid.sound;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Scrollable list showing every registered sound event with:
 * - a text field to set a custom display name (empty = default),
 * - a reset button to clear that custom name,
 * - a toggle button to enable/disable its on-screen indicator.
 * Supports text filtering.
 */
public class SoundEventListWidget extends EntryListWidget<SoundEventListWidget.SoundEntry> {

    private final List<Identifier> allSoundIds;
    private String filter = "";
    /** The text field currently receiving keyboard input, or null. */
    TextFieldWidget focusedField;
    /** Chinese tooltip text for the currently hovered row (set by entries). */
    String hoveredTooltip = null;
    /** Mouse position last frame, used to draw the tooltip. */
    int hoveredMouseX = 0;
    int hoveredMouseY = 0;

    public SoundEventListWidget(MinecraftClient client, int width, int height, int top, int itemHeight) {
        super(client, width, height, top, itemHeight);
        this.centerListVertically = false;
        this.allSoundIds = new ArrayList<>(Registries.SOUND_EVENT.getIds());
        this.allSoundIds.sort(Comparator.comparing(Identifier::toString));
        rebuildEntries();
    }

    public void setFilter(String filter) {
        this.filter = filter == null ? "" : filter.toLowerCase();
        rebuildEntries();
    }

    private void rebuildEntries() {
        clearEntries();
        focusedField = null;
        hoveredTooltip = null;
        for (Identifier id : allSoundIds) {
            String str = id.toString();
            String path = id.getPath();
            // Match against the sound ID, the path, or the Chinese name.
            String zhName = SoundNameLookup.getChineseName(path);
            boolean matches = filter.isEmpty()
                    || str.toLowerCase().contains(filter)
                    || path.toLowerCase().contains(filter)
                    || (zhName != null && zhName.toLowerCase().contains(filter));
            if (!matches) continue;
            addEntry(new SoundEntry(id, zhName));
        }
    }

    @Override
    public int getRowWidth() {
        return this.width - 16;
    }

    @Override
    protected int getScrollbarX() {
        return this.width - 6;
    }

    public void setFocusedField(TextFieldWidget field) {
        if (this.focusedField != null && this.focusedField != field) {
            this.focusedField.setFocused(false);
        }
        this.focusedField = field;
        if (field != null) {
            field.setFocused(true);
        }
    }

    public void clearFocusedField() {
        if (focusedField != null) {
            focusedField.setFocused(false);
            focusedField = null;
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (focusedField != null && focusedField.isFocused()) {
            return focusedField.charTyped(chr, modifiers);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focusedField != null && focusedField.isFocused()) {
            return focusedField.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (focusedField != null && focusedField.isFocused()) {
            return focusedField.keyReleased(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder) {
        SoundEntry selected = getSelectedOrNull();
        if (selected != null) {
            appendNarrations(builder, selected);
        }
    }

    public class SoundEntry extends EntryListWidget.Entry<SoundEntry> {
        private final Identifier soundId;
        private final String chineseName;
        private final TextFieldWidget customNameField;
        private final ButtonWidget resetButton;
        private final ButtonWidget toggleButton;

        SoundEntry(Identifier soundId, String chineseName) {
            this.soundId = soundId;
            this.chineseName = chineseName;
            String idStr = soundId.toString();
            ModConfig cfg = ModConfig.getInstance();

            boolean disabled = cfg.isSoundDisabled(idStr);
            this.toggleButton = ButtonWidget.builder(
                            Text.translatable(disabled ? "vivid-sound.settings.off" : "vivid-sound.settings.on"),
                            btn -> toggle())
                    .size(50, 20)
                    .build();

            String custom = cfg.getCustomName(idStr);
            this.customNameField = new TextFieldWidget(
                    client.textRenderer, 0, 0, 160, 20,
                    Text.translatable("vivid-sound.settings.custom_placeholder"));
            this.customNameField.setMaxLength(64);
            if (custom != null) {
                this.customNameField.setText(custom);
            }
            this.customNameField.setChangedListener(text -> {
                cfg.setCustomName(idStr, text);
                cfg.save();
            });

            this.resetButton = ButtonWidget.builder(
                            Text.translatable("vivid-sound.settings.reset_single"),
                            btn -> reset())
                    .size(50, 20)
                    .build();
        }

        private void toggle() {
            ModConfig cfg = ModConfig.getInstance();
            String id = soundId.toString();
            cfg.setSoundDisabled(id, !cfg.isSoundDisabled(id));
            boolean nowDisabled = cfg.isSoundDisabled(id);
            toggleButton.setMessage(Text.translatable(nowDisabled ? "vivid-sound.settings.off" : "vivid-sound.settings.on"));
            cfg.save();
        }

        private void reset() {
            ModConfig cfg = ModConfig.getInstance();
            cfg.resetCustomName(soundId.toString());
            customNameField.setText("");
            cfg.save();
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth,
                           int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int textColor = ModConfig.getInstance().isSoundDisabled(soundId.toString())
                    ? 0xFF888888 : 0xFFFFFFFF;

            // Lay out right-to-left: toggle -> reset -> text field.
            int tx = x + entryWidth - toggleButton.getWidth();
            toggleButton.setX(tx);
            toggleButton.setY(y + (entryHeight - 20) / 2);

            int rx = tx - 4 - resetButton.getWidth();
            resetButton.setX(rx);
            resetButton.setY(y + (entryHeight - 20) / 2);

            int fx = rx - 4 - customNameField.getWidth();
            customNameField.setX(fx);
            customNameField.setY(y + (entryHeight - 20) / 2);

            // Sound ID takes the remaining space on the left.
            int textMaxWidth = fx - 4 - x;
            String idText = soundId.toString();
            if (client.textRenderer.getWidth(idText) > textMaxWidth) {
                idText = client.textRenderer.trimToWidth(idText, Math.max(0, textMaxWidth)) + "...";
            }
            context.drawText(client.textRenderer, idText, x, y + (entryHeight - 8) / 2, textColor, false);

            customNameField.render(context, mouseX, mouseY, tickDelta);
            resetButton.render(context, mouseX, mouseY, tickDelta);
            toggleButton.render(context, mouseX, mouseY, tickDelta);

            // Set the list's hovered tooltip so the screen can draw it on top
            // (avoids clipping by the list's scissor region).
            if (hovered && chineseName != null && !chineseName.isEmpty()) {
                hoveredTooltip = chineseName;
                hoveredMouseX = mouseX;
                hoveredMouseY = mouseY;
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (customNameField.isMouseOver(mouseX, mouseY)) {
                customNameField.mouseClicked(mouseX, mouseY, button);
                setFocusedField(customNameField);
                return true;
            }
            if (resetButton.isMouseOver(mouseX, mouseY)) {
                clearFocusedField();
                resetButton.onClick(mouseX, mouseY);
                return true;
            }
            if (toggleButton.isMouseOver(mouseX, mouseY)) {
                clearFocusedField();
                toggleButton.onClick(mouseX, mouseY);
                return true;
            }
            return false;
        }
    }
}
