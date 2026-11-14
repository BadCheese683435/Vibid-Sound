package com.vivid.sound;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Renders arbitrary text using a user-supplied TrueType/OpenType font.
 * <p>
 * The text is drawn to an off-screen {@link BufferedImage} with a black
 * outline and white fill (matching the JoJo style of the vanilla renderer),
 * uploaded to a Minecraft {@link DynamicTexture}, cached, and then drawn to
 * the screen with the caller's scale and alpha.
 */
public class CustomFontRenderer {

    /** Base font size (px) at which text is rendered into the texture. */
    private static final int BASE_SIZE = 40;
    /** Outline stroke width at the base size. */
    private static final int OUTLINE = 4;
    /** Extra padding around the glyphs (to fit the outline). */
    private static final int PAD = OUTLINE + 2;
    /** Maximum number of cached text textures. */
    private static final int MAX_CACHE = 512;

    /** Target on-screen line height at scale=1.0, matching vanilla (~9px). */
    private static final float VANILLA_HEIGHT = 9.0f;

    private record CachedText(Identifier id, int imgWidth, int imgHeight, float textHeight) {}

    /** fontName + "|" + text -> cached texture info. */
    private static final Map<String, CachedText> cache = new LinkedHashMap<>(16, 0.75f, true);
    private static int counter = 0;

    /**
     * Draws text centered at {@code (centerX, centerY)} using the custom font
     * named {@code fontName}, scaled by {@code scale} and modulated by
     * {@code alpha}. Has no effect if the font is not loaded.
     */
    public static void drawText(DrawContext context, String text, float centerX, float centerY,
                                float scale, float alpha, String fontName) {
        if (text == null || text.isEmpty()) return;
        Font font = FontManager.getFont(fontName);
        if (font == null) return;

        String key = fontName + "|" + text;
        CachedText cached = cache.get(key);
        if (cached == null) {
            cached = renderToTexture(font, text);
            if (cached == null) return;
            cache.put(key, cached);
            evictIfNeeded();
        }

        // Scale so the text height matches vanilla (~9px) at scale=1.0.
        float drawScale = scale * VANILLA_HEIGHT / cached.textHeight();
        float screenW = cached.imgWidth() * drawScale;
        float screenH = cached.imgHeight() * drawScale;

        int drawX = Math.round(centerX - screenW / 2f);
        int drawY = Math.round(centerY - screenH / 2f);

        context.setShaderColor(1f, 1f, 1f, Math.max(0f, Math.min(1f, alpha)));
        context.getMatrices().push();
        context.getMatrices().translate(drawX, drawY, 0);
        context.getMatrices().scale(drawScale, drawScale, 1f);
        context.drawTexture(cached.id(), 0, 0, 0, 0,
                cached.imgWidth(), cached.imgHeight(),
                cached.imgWidth(), cached.imgHeight());
        context.getMatrices().pop();
        context.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static CachedText renderToTexture(Font font, String text) {
        Font sizedFont = font.deriveFont(Font.BOLD, (float) BASE_SIZE);

        // First pass: measure text using a scratch image.
        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = scratch.createGraphics();
        sg.setFont(sizedFont);
        FontMetrics fm = sg.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int ascent = fm.getAscent();
        int descent = fm.getDescent();
        int textHeight = ascent + descent;
        sg.dispose();

        int imgWidth = Math.max(1, textWidth + PAD * 2);
        int imgHeight = Math.max(1, textHeight + PAD * 2);

        BufferedImage img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        // GlyphVector gives precise outline geometry for stroked + filled drawing.
        FontRenderContext frc = g.getFontRenderContext();
        GlyphVector gv = sizedFont.createGlyphVector(frc, text);
        Shape outline = gv.getOutline(PAD, PAD + ascent);

        // Black outline.
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(OUTLINE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(outline);

        // White fill.
        g.setColor(Color.WHITE);
        g.fill(outline);
        g.dispose();

        // Convert BufferedImage -> PNG bytes -> NativeImage.
        NativeImage nativeImage;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            nativeImage = NativeImage.read(new ByteArrayInputStream(baos.toByteArray()));
        } catch (Exception e) {
            VividSound.LOGGER.error("Failed to convert font texture for '{}'", text, e);
            return null;
        }

        // NativeImageBackedTexture wraps a NativeImage and extends AbstractTexture.
        NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);

        Identifier id = Identifier.of(VividSound.MOD_ID, "font/cache_" + (counter++));
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);

        return new CachedText(id, imgWidth, imgHeight, textHeight);
    }

    private static void evictIfNeeded() {
        while (cache.size() > MAX_CACHE) {
            Map.Entry<String, CachedText> eldest = cache.entrySet().iterator().next();
            MinecraftClient.getInstance().getTextureManager().destroyTexture(eldest.getValue().id());
            cache.remove(eldest.getKey());
        }
    }

    /** Clears all cached textures (call when fonts change). */
    public static void clearCache() {
        for (CachedText t : cache.values()) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(t.id());
        }
        cache.clear();
    }
}
