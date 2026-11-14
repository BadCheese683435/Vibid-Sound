package com.vivid.sound;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Scans {@code config/vivid-sound/fonts/} for {@code .ttf}/{@code .otf} files
 * and loads them as {@link java.awt.Font} instances. The default Minecraft
 * font is represented by the special entry {@code "default"}.
 */
public class FontManager {

    /** Special name meaning "use the vanilla Minecraft font". */
    public static final String DEFAULT_FONT = "default";

    private static final Path FONTS_DIR =
            FabricLoader.getInstance().getConfigDir().resolve("vivid-sound").resolve("fonts");

    /** Display name -> loaded AWT font. */
    private static final Map<String, Font> fonts = new LinkedHashMap<>();
    private static boolean scanned = false;

    /**
     * Scans the fonts folder (once) and registers every usable font file.
     * Missing folder is created automatically.
     */
    public static synchronized void scan() {
        if (scanned) return;
        scanned = true;
        fonts.clear();

        try {
            Files.createDirectories(FONTS_DIR);
        } catch (IOException e) {
            VividSound.LOGGER.error("Failed to create fonts directory", e);
            return;
        }

        try (Stream<Path> stream = Files.list(FONTS_DIR)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".ttf") || name.endsWith(".otf");
                    })
                    .toList();

            for (Path file : files) {
                String name = file.getFileName().toString();
                try (InputStream in = Files.newInputStream(file)) {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, in);
                    // Register it so it can be used in Graphics2D context.
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                    fonts.put(name, font);
                    VividSound.LOGGER.info("Loaded font: {}", name);
                } catch (Exception e) {
                    VividSound.LOGGER.warn("Failed to load font file {}: {}", name, e.getMessage());
                }
            }
        } catch (IOException e) {
            VividSound.LOGGER.error("Failed to scan fonts directory", e);
        }
    }

    /** Returns the list of selectable font names, starting with "default". */
    public static List<String> getAvailableFonts() {
        List<String> names = new ArrayList<>();
        names.add(DEFAULT_FONT);
        names.addAll(fonts.keySet());
        return names;
    }

    /**
     * Returns the AWT font for the given display name, or {@code null} when
     * the name is {@code "default"} or not found.
     */
    public static Font getFont(String name) {
        if (name == null || DEFAULT_FONT.equals(name)) return null;
        return fonts.get(name);
    }

    /** Whether the given name refers to a loaded custom font. */
    public static boolean isCustomFont(String name) {
        return getFont(name) != null;
    }

    /** Returns the physical path of the fonts folder (for user reference). */
    public static Path getFontsDir() {
        return FONTS_DIR;
    }
}
