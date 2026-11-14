package com.vivid.sound;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Configuration screen for Vivid Sound. All user-facing text uses translation
 * keys so it follows the game's language setting.
 */
public class ConfigScreen extends Screen {

    private final Screen parent;
    private SoundEventListWidget listWidget;
    private TextFieldWidget searchBox;
    private ButtonWidget resetAllButton;

    private final ModConfig working = ModConfig.getInstance();

    public ConfigScreen(Screen parent) {
        super(Text.translatable("vivid-sound.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        // ---- Search box ----
        this.searchBox = new TextFieldWidget(
                this.textRenderer, centerX - 150, 30, 300, 20,
                Text.translatable("vivid-sound.settings.search_placeholder"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setChangedListener(s -> listWidget.setFilter(s));
        this.addDrawableChild(this.searchBox);

        // ---- Settings row 1: toggles ----
        int row1Y = 56;
        int toggleW = 140;
        int gap = 10;
        int totalW = toggleW * 3 + gap * 2;
        int startX = centerX - totalW / 2;

        this.addDrawableChild(makeToggle(startX, row1Y, toggleW,
                "vivid-sound.settings.enabled", "vivid-sound.settings.enabled.tooltip",
                working.enabled, v -> working.enabled = v));
        this.addDrawableChild(makeToggle(startX + toggleW + gap, row1Y, toggleW,
                "vivid-sound.settings.followScreen", "vivid-sound.settings.followScreen.tooltip",
                working.followScreen, v -> working.followScreen = v));
        this.addDrawableChild(makeToggle(startX + (toggleW + gap) * 2, row1Y, toggleW,
                "vivid-sound.settings.subtitleOnly", "vivid-sound.settings.subtitleOnly.tooltip",
                working.showOnlyWithSubtitle, v -> working.showOnlyWithSubtitle = v));

        // ---- Settings row 2: sliders (size / duration / max distance / frequency) ----
        int row2Y = 82;
        int sliderCount = 4;
        int sliderW = 100;
        int sliderTotalW = sliderW * sliderCount + gap * (sliderCount - 1);
        int sx = centerX - sliderTotalW / 2;

        DoubleSlider sizeSlider = new DoubleSlider(sx, row2Y, sliderW, 20,
                0.3, 3.0, 0.1, working.textScale,
                v -> Text.translatable("vivid-sound.settings.size", String.format("%.1f", v)),
                v -> working.textScale = (float) (double) v);
        sizeSlider.setTooltip(Tooltip.of(Text.translatable("vivid-sound.settings.size.tooltip")));
        this.addDrawableChild(sizeSlider);

        DoubleSlider durationSlider = new DoubleSlider(sx + (sliderW + gap), row2Y, sliderW, 20,
                1, 15, 1, working.durationSeconds,
                v -> Text.translatable("vivid-sound.settings.duration", String.valueOf(v.intValue())),
                v -> working.durationSeconds = v.intValue());
        durationSlider.setTooltip(Tooltip.of(Text.translatable("vivid-sound.settings.duration.tooltip")));
        this.addDrawableChild(durationSlider);

        DoubleSlider distSlider = new DoubleSlider(sx + (sliderW + gap) * 2, row2Y, sliderW, 20,
                16, 160, 1, working.maxDistance,
                v -> Text.translatable("vivid-sound.settings.maxDistance", String.valueOf(v.intValue())),
                v -> working.maxDistance = v);
        distSlider.setTooltip(Tooltip.of(Text.translatable("vivid-sound.settings.maxDistance.tooltip")));
        this.addDrawableChild(distSlider);

        DoubleSlider freqSlider = new DoubleSlider(sx + (sliderW + gap) * 3, row2Y, sliderW, 20,
                0, 5, 0.1, working.frequencyCooldown,
                v -> Text.translatable("vivid-sound.settings.frequency", String.format("%.1f", v)),
                v -> working.frequencyCooldown = (float) (double) v);
        freqSlider.setTooltip(Tooltip.of(Text.translatable("vivid-sound.settings.frequency.tooltip")));
        this.addDrawableChild(freqSlider);

        // ---- Font selector row ----
        int fontY = 108;
        ButtonWidget fontButton = ButtonWidget.builder(
                        fontLabel(working.selectedFont),
                        btn -> cycleFont(btn))
                .dimensions(centerX - 150, fontY, 240, 20)
                .tooltip(Tooltip.of(Text.translatable("vivid-sound.settings.font.tooltip")))
                .build();
        this.addDrawableChild(fontButton);

        ButtonWidget reloadFontsBtn = ButtonWidget.builder(
                        Text.translatable("vivid-sound.settings.font_reload"),
                        btn -> {
                            FontManager.scan();
                            CustomFontRenderer.clearCache();
                            fontButton.setMessage(fontLabel(working.selectedFont));
                        })
                .dimensions(centerX + 100, fontY, 50, 20)
                .tooltip(Tooltip.of(Text.translatable("vivid-sound.settings.font_reload.tooltip")))
                .build();
        this.addDrawableChild(reloadFontsBtn);

        // ---- Sound list ----
        int listTop = 136;
        int listHeight = this.height - 40 - listTop;
        this.listWidget = new SoundEventListWidget(this.client, this.width,
                listHeight, listTop, 28);
        this.addDrawableChild(this.listWidget);

        // ---- Bottom buttons: Reset All (left) + Done (center) ----
        this.resetAllButton = ButtonWidget.builder(Text.translatable("vivid-sound.settings.reset_all"),
                        this::onResetAllClicked)
                .dimensions(20, this.height - 30, 130, 20)
                .build();
        this.addDrawableChild(this.resetAllButton);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("vivid-sound.settings.done"),
                        btn -> this.close())
                .dimensions(centerX - 100, this.height - 30, 200, 20)
                .build());
    }

    private boolean resetAllArmed = false;
    private long resetAllArmedAt = 0L;
    private static final long RESET_CONFIRM_MS = 4000L;

    private void onResetAllClicked(ButtonWidget btn) {
        if (!resetAllArmed) {
            // First click: arm the confirmation.
            resetAllArmed = true;
            resetAllArmedAt = System.currentTimeMillis();
            btn.setMessage(Text.translatable("vivid-sound.settings.reset_all_confirm"));
        } else {
            // Second click: confirmed.
            working.resetAllCustomNames();
            working.save();
            resetAllArmed = false;
            btn.setMessage(Text.translatable("vivid-sound.settings.reset_all"));
            // Rebuild the list so the text fields reflect empty values.
            this.listWidget.setFilter(this.searchBox.getText());
        }
    }

    /**
     * Creates a toggle button. The label is a translation key; the final text
     * is assembled via {@code vivid-sound.settings.toggle_format}.
     */
    private ButtonWidget makeToggle(int x, int y, int width, String labelKey,
                                    String tooltipKey, boolean initial,
                                    java.util.function.Consumer<Boolean> setter) {
        final boolean[] state = {initial};
        ButtonWidget btn = ButtonWidget.builder(
                        toggleText(labelKey, state[0]),
                        b -> {
                            state[0] = !state[0];
                            setter.accept(state[0]);
                            b.setMessage(toggleText(labelKey, state[0]));
                            working.save();
                        })
                .dimensions(x, y, width, 20)
                .tooltip(Tooltip.of(Text.translatable(tooltipKey)))
                .build();
        return btn;
    }

    private Text toggleText(String labelKey, boolean on) {
        return Text.translatable("vivid-sound.settings.toggle_format",
                Text.translatable(labelKey),
                Text.translatable(on ? "vivid-sound.settings.on" : "vivid-sound.settings.off"));
    }

    /** Builds the label for the font selector button. */
    private Text fontLabel(String fontName) {
        return Text.translatable("vivid-sound.settings.font",
                FontManager.DEFAULT_FONT.equals(fontName)
                        ? Text.translatable("vivid-sound.settings.font_default")
                        : Text.literal(fontName));
    }

    /** Cycles to the next available font and saves the config. */
    private void cycleFont(ButtonWidget btn) {
        java.util.List<String> fonts = FontManager.getAvailableFonts();
        if (fonts.isEmpty()) return;
        int idx = fonts.indexOf(working.selectedFont);
        if (idx < 0) idx = 0;
        int next = (idx + 1) % fonts.size();
        working.selectedFont = fonts.get(next);
        working.save();
        CustomFontRenderer.clearCache();
        btn.setMessage(fontLabel(working.selectedFont));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Clear the list's hovered tooltip before children render; entries
        // re-set it when the mouse is over them.
        if (listWidget != null) listWidget.hoveredTooltip = null;
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, 12, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("vivid-sound.settings.search_hint"),
                this.width / 2 - 150, 18, 0xAAAAAA);

        // Draw the Chinese-name tooltip for the hovered sound entry on top of
        // everything (the list clips its own contents).
        if (listWidget != null && listWidget.hoveredTooltip != null) {
            context.drawTooltip(this.textRenderer,
                    Text.literal(listWidget.hoveredTooltip),
                    listWidget.hoveredMouseX, listWidget.hoveredMouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        // If a list text field had focus but the click did not land on it,
        // drop the focus so typing goes where the user expects.
        if (listWidget != null && listWidget.focusedField != null
                && !listWidget.focusedField.isMouseOver(mouseX, mouseY)) {
            listWidget.clearFocusedField();
        }
        // Clicking anything except the reset-all button cancels its armed state.
        if (resetAllArmed && (resetAllButton == null || !resetAllButton.isMouseOver(mouseX, mouseY))) {
            resetAllArmed = false;
            if (resetAllButton != null) {
                resetAllButton.setMessage(Text.translatable("vivid-sound.settings.reset_all"));
            }
        }
        return handled;
    }

    @Override
    public void tick() {
        super.tick();
        // Auto-cancel the reset-all confirmation if the user takes too long.
        if (resetAllArmed && System.currentTimeMillis() - resetAllArmedAt > RESET_CONFIRM_MS) {
            resetAllArmed = false;
            if (resetAllButton != null) {
                resetAllButton.setMessage(Text.translatable("vivid-sound.settings.reset_all"));
            }
        }
    }

    @Override
    public void close() {
        working.save();
        this.client.setScreen(this.parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ---- Generic double slider backed by a normalized 0..1 value ----
    private static class DoubleSlider extends SliderWidget {
        private final double min, max, step;
        private final java.util.function.Function<Double, Text> labelProvider;
        private final java.util.function.Consumer<Double> setter;

        DoubleSlider(int x, int y, int width, int height,
                     double min, double max, double step, double initial,
                     java.util.function.Function<Double, Text> labelProvider,
                     java.util.function.Consumer<Double> setter) {
            super(x, y, width, height, labelProvider.apply(initial), (initial - min) / (max - min));
            this.min = min;
            this.max = max;
            this.step = step;
            this.labelProvider = labelProvider;
            this.setter = setter;
        }

        @Override
        protected void updateMessage() {
            double actual = min + this.value * (max - min);
            setMessage(labelProvider.apply(actual));
        }

        @Override
        protected void applyValue() {
            double raw = min + this.value * (max - min);
            double snapped = Math.round(raw / step) * step;
            snapped = Math.max(min, Math.min(max, snapped));
            this.value = (snapped - min) / (max - min);
            setter.accept(snapped);
        }
    }
}
