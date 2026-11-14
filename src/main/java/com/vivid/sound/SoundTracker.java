package com.vivid.sound;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe registry of currently playing sounds. Sounds are captured from
 * the sound engine via a Mixin and rendered on the HUD. Each entry expires
 * after the configured lifetime.
 * <p>
 * A per-sound cooldown (configurable in seconds) limits how often the same
 * sound event can produce a new on-screen indicator, which prevents rapid
 * repeated sounds (e.g. continuous footsteps) from flooding the screen.
 */
public class SoundTracker {
    private static final CopyOnWriteArrayList<ActiveSound> activeSounds = new CopyOnWriteArrayList<>();

    /** Last time (ms) an indicator was shown for each sound event ID. */
    private static final Map<String, Long> lastShownMs = new ConcurrentHashMap<>();

    /** Maximum number of simultaneous sound indicators to avoid clutter. */
    private static final int MAX_SOUNDS = 24;

    public static void init() {
        activeSounds.clear();
        lastShownMs.clear();
    }

    /**
     * Registers a newly played sound. If the same sound event is already
     * active within 1 block of the new position, its lifetime is refreshed
     * instead of creating a duplicate indicator.
     * <p>
     * Sounds disabled in the config, or (when enabled) sounds without a
     * subtitle, are ignored. If a frequency cooldown is configured and the
     * same sound event was displayed more recently than the cooldown, the
     * sound is dropped.
     */
    public static void addSound(SoundInstance instance, Identifier soundId,
                                String subtitleKey, String fallbackName) {
        if (instance == null || soundId == null) return;

        ModConfig cfg = ModConfig.getInstance();
        String idStr = soundId.toString();
        if (cfg.isSoundDisabled(idStr)) return;
        if (cfg.showOnlyWithSubtitle && (subtitleKey == null || subtitleKey.isEmpty())) return;

        // ---- Frequency cooldown ----
        float cooldown = cfg.frequencyCooldown;
        if (cooldown > 0f) {
            long cooldownMs = (long) (cooldown * 1000f);
            long now = System.currentTimeMillis();
            Long last = lastShownMs.get(idStr);
            if (last != null && now - last < cooldownMs) {
                // Too soon since the last indicator for this sound.
                return;
            }
            lastShownMs.put(idStr, now);
        }

        cleanup();

        Vec3d newPos = new Vec3d(instance.getX(), instance.getY(), instance.getZ());

        for (ActiveSound existing : activeSounds) {
            if (existing.soundId.equals(soundId) && existing.getPos().distanceTo(newPos) < 1.0) {
                activeSounds.remove(existing);
                break;
            }
        }

        if (activeSounds.size() >= MAX_SOUNDS) {
            activeSounds.remove(0);
        }

        activeSounds.add(new ActiveSound(instance, soundId, subtitleKey, fallbackName,
                instance.getVolume(), instance.getPitch()));
    }

    /** Removes sounds that have exceeded their configured lifetime. */
    public static void cleanup() {
        long lifetime = ModConfig.getInstance().getLifetimeMs();
        long now = System.currentTimeMillis();
        activeSounds.removeIf(s -> now - s.startTime > lifetime);
    }

    public static CopyOnWriteArrayList<ActiveSound> getActiveSounds() {
        cleanup();
        return activeSounds;
    }
}
