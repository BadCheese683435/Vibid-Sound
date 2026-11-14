package com.vivid.sound;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Persistent mod configuration stored as JSON in the config directory.
 * All fields are public for simple Gson serialization; call {@link #save()}
 * after mutating them.
 */
public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("vivid-sound.json");

    private static ModConfig instance;

    // ---- General toggles ----
    public boolean enabled = true;
    /** Only render sounds that have a vanilla subtitle line. */
    public boolean showOnlyWithSubtitle = false;
    /** Pin subtitles to a fixed screen position instead of tracking the 3D source. */
    public boolean followScreen = false;

    // ---- Appearance ----
    /** Global text scale multiplier (0.3 - 3.0). */
    public float textScale = 1.0f;
    /** How long (seconds) a subtitle stays on screen (1 - 15). */
    public int durationSeconds = 4;
    /** Maximum distance (blocks) at which sounds are rendered. */
    public double maxDistance = 80.0;

    // ---- Per-sound filter ----
    /** Sound event IDs that the user has explicitly disabled. */
    public Set<String> disabledSounds = new LinkedHashSet<>();

    // ---- Per-sound custom display names ----
    /** Sound event ID -> custom display text (empty/missing means use default). */
    public Map<String, String> customNames = new LinkedHashMap<>();

    // ---- Font ----
    /** Name of the selected font file, or "default" for the vanilla font. */
    public String selectedFont = FontManager.DEFAULT_FONT;

    // ---- Frequency control ----
    /**
     * Minimum interval (seconds) between two on-screen indicators for the
     * same sound event. 0 means no limit (every play shows a new indicator).
     */
    public float frequencyCooldown = 0.5f;

    public static ModConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static ModConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                ModConfig cfg = GSON.fromJson(reader, ModConfig.class);
                if (cfg != null) {
                    if (cfg.disabledSounds == null) cfg.disabledSounds = new LinkedHashSet<>();
                    if (cfg.customNames == null) cfg.customNames = new LinkedHashMap<>();
                    if (cfg.selectedFont == null) cfg.selectedFont = FontManager.DEFAULT_FONT;
                    return cfg;
                }
            } catch (IOException e) {
                VividSound.LOGGER.warn("Failed to read config, using defaults", e);
            }
        }
        return new ModConfig();
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            VividSound.LOGGER.error("Failed to save config", e);
        }
    }

    /** Convenience: returns the lifetime in milliseconds. */
    public long getLifetimeMs() {
        return (long) Math.max(1, Math.min(15, durationSeconds)) * 1000L;
    }

    public boolean isSoundDisabled(String soundId) {
        return disabledSounds.contains(soundId);
    }

    public void setSoundDisabled(String soundId, boolean disabled) {
        if (disabled) {
            disabledSounds.add(soundId);
        } else {
            disabledSounds.remove(soundId);
        }
    }

    // ---- Custom display names ----

    /**
     * Returns the custom display name for the given sound ID, or {@code null}
     * if none is set (meaning the default subtitle/sound name should be used).
     */
    public String getCustomName(String soundId) {
        String name = customNames.get(soundId);
        return (name == null || name.isEmpty()) ? null : name;
    }

    /**
     * Sets a custom display name. Passing an empty or null string removes the
     * custom name so the default is used.
     */
    public void setCustomName(String soundId, String name) {
        if (name == null || name.isEmpty()) {
            customNames.remove(soundId);
        } else {
            customNames.put(soundId, name);
        }
    }

    /** Removes the custom name for a single sound (resets to default). */
    public void resetCustomName(String soundId) {
        customNames.remove(soundId);
    }

    /** Removes all custom names. */
    public void resetAllCustomNames() {
        customNames.clear();
    }
}
