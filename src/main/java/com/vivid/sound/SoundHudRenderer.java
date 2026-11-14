package com.vivid.sound;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.math.Vec3d;

/**
 * Renders active sound sources on the HUD. Each sound is projected from its
 * 3D world position to 2D screen coordinates and drawn as bold outlined text
 * (similar to JoJo's sound-effect text), with size scaling by distance and
 * a lifetime fade-out. Sounds behind the camera are clamped to the nearest
 * horizontal edge.
 * <p>
 * All visual parameters (scale, duration, follow-screen, max distance,
 * master enable) are read from {@link ModConfig}.
 */
public class SoundHudRenderer implements HudRenderCallback {

    /** Screen-edge margin for clamped/off-screen indicators. */
    private static final int EDGE_MARGIN = 18;

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        ModConfig config = ModConfig.getInstance();
        if (!config.enabled) return;

        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();

        // ---- Camera basis vectors (matches Minecraft's coordinate system) ----
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double cosP = Math.cos(pitchRad);
        double sinP = Math.sin(pitchRad);
        double cosY = Math.cos(yawRad);
        double sinY = Math.sin(yawRad);

        Vec3d forward = new Vec3d(-sinY * cosP, -sinP, cosY * cosP);
        Vec3d right = new Vec3d(-cosY * cosP, 0.0, -sinY * cosP);
        Vec3d up = right.crossProduct(forward);

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        double fov = client.options.getFov().getValue();
        double tanHalfFov = Math.tan(Math.toRadians(fov) / 2.0);
        double aspect = (double) screenWidth / screenHeight;

        TextRenderer textRenderer = client.textRenderer;

        for (ActiveSound sound : SoundTracker.getActiveSounds()) {
            Vec3d soundPos = sound.getPos();
            Vec3d rel = soundPos.subtract(cameraPos);

            double dist = rel.length();
            if (dist > config.maxDistance) continue;

            double depth = rel.dotProduct(forward);
            boolean behind = depth <= 0.1;

            double ndcX;
            double ndcY;
            if (behind) {
                // Sound is behind the camera. The "follow screen" toggle
                // controls whether it is pinned to a screen edge.
                if (!config.followScreen) {
                    // Not following: off-screen sounds are not rendered.
                    continue;
                }
                // Following: compute which edge it belongs to and pin it.
                double rightOffset = rel.dotProduct(right);
                double targetNdcX = rightOffset >= 0 ? 1.0 : -1.0;
                double upOffset = rel.dotProduct(up);
                double targetNdcY = -(upOffset / (Math.abs(depth) + 1.0) / tanHalfFov);
                if (!sound.hasStoredNdc()) {
                    sound.storeNdc(targetNdcX, targetNdcY);
                }
                ndcX = sound.getStoredNdcX();
                ndcY = sound.getStoredNdcY();
            } else {
                // Sound is in front of the camera: always project from its
                // live 3D position so it moves naturally as you look around.
                ndcX = rel.dotProduct(right) / (depth * tanHalfFov * aspect);
                ndcY = rel.dotProduct(up) / (depth * tanHalfFov);
            }

            int screenX = (int) (screenWidth / 2.0 * (1.0 + ndcX));
            int screenY = (int) (screenHeight / 2.0 * (1.0 - ndcY));

            screenX = Math.max(EDGE_MARGIN, Math.min(screenWidth - EDGE_MARGIN, screenX));
            screenY = Math.max(EDGE_MARGIN, Math.min(screenHeight - EDGE_MARGIN, screenY));

            String displayName = resolveDisplayName(sound, config);

            // Lifetime fade-out (last 30% of life fades to transparent).
            float life = config.getLifetimeMs() / 1000f;
            float age = sound.getAge();
            float alpha = 1.0f;
            if (age > life * 0.7f) {
                alpha = 1.0f - (age - life * 0.7f) / (life * 0.3f);
            }
            alpha = Math.max(0.0f, Math.min(1.0f, alpha));
            if (alpha <= 0.01f) continue;

            // Distance-based scale (closer = bigger).
            float scale = (float) Math.max(0.6f, Math.min(2.2f, 1.8f - dist / 45.0f));
            // Volume influences size slightly.
            scale *= Math.max(0.7f, Math.min(1.4f, 0.8f + sound.volume * 0.4f));
            // Apply the user-configured global text scale.
            scale *= Math.max(0.3f, Math.min(3.0f, config.textScale));

            if (FontManager.isCustomFont(config.selectedFont)) {
                CustomFontRenderer.drawText(context, displayName,
                        screenX, screenY, scale, alpha, config.selectedFont);
            } else {
                renderOutlinedText(context, textRenderer, displayName, screenX, screenY, scale, alpha);
            }
        }
    }

    /**
     * Draws centered text with a thick black outline (JoJo-style). The text
     * is drawn 8 times offset by 1px in every direction in black, then the
     * coloured fill on top.
     */
    private void renderOutlinedText(DrawContext context, TextRenderer textRenderer,
                                    String text, int centerX, int centerY,
                                    float scale, float alpha) {
        if (text == null || text.isEmpty()) return;

        context.getMatrices().push();
        context.getMatrices().translate(centerX, centerY, 0);
        context.getMatrices().scale(scale, scale, 1.0f);

        int textWidth = textRenderer.getWidth(text);
        int textHeight = textRenderer.fontHeight;
        int drawX = -textWidth / 2;
        int drawY = -textHeight / 2;

        int a = Math.round(alpha * 255);
        int black = (a << 24) | 0x000000;
        int white = (a << 24) | 0xFFFFFF;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                context.drawText(textRenderer, text, drawX + dx, drawY + dy, black, false);
            }
        }
        context.drawText(textRenderer, text, drawX, drawY, white, false);

        context.getMatrices().pop();
    }

    /**
     * Resolves the text to display for a sound, in priority order:
     * <ol>
     *   <li>User-set custom name (always wins).</li>
     *   <li>Chinese name from the vanilla zh_cn language file (reads directly
     *       from assets, so it works even when the sound set did not expose a
     *       subtitle key, e.g. footsteps).</li>
     *   <li>The vanilla subtitle translation key, translated to the current
     *       game language.</li>
     *   <li>A constructed {@code subtitles.<sound-path>} key translated.</li>
     *   <li>A human-readable rendering of the sound ID path.</li>
     * </ol>
     */
    private static String resolveDisplayName(ActiveSound sound, ModConfig config) {
        String custom = config.getCustomName(sound.soundId.toString());
        if (custom != null) return custom;

        // Chinese name from vanilla zh_cn.json (most reliable for sounds
        // like footsteps whose sound set has no subtitle field).
        String zhName = SoundNameLookup.getChineseName(sound.subtitleKey, sound.soundId.getPath());
        if (zhName != null) return zhName;

        if (sound.subtitleKey != null && !sound.subtitleKey.isEmpty()) {
            String translated = I18n.translate(sound.subtitleKey);
            if (!translated.equals(sound.subtitleKey)) {
                return translated;
            }
        }

        String path = sound.soundId.getPath();
        String constructedKey = "subtitles." + path;
        String translated = I18n.translate(constructedKey);
        if (!translated.equals(constructedKey)) {
            return translated;
        }

        return humanizePath(path);
    }

    /**
     * Converts a sound ID path like {@code entity.cow.ambient} into a
     * readable string like "Entity Cow Ambient".
     */
    private static String humanizePath(String path) {
        String[] parts = path.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(' ');
            String word = parts[i].replace('_', ' ');
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) sb.append(word.substring(1));
            }
        }
        return sb.toString();
    }
}
