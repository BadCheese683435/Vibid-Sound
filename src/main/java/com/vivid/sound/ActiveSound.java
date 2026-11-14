package com.vivid.sound;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * Represents a currently playing sound that should be rendered on screen.
 * Stores a reference to the live SoundInstance so the position can be updated
 * each frame (handles moving sound sources such as entities).
 * <p>
 * For "follow screen" mode the first computed screen-space NDC is cached so
 * the indicator stays pinned to a fixed position as the camera rotates.
 */
public class ActiveSound {
    public final SoundInstance instance;
    public final Identifier soundId;
    /** Already-translated subtitle text, or null if the sound has no subtitle. */
    public final String subtitleKey;
    /** Fallback display name used when there is no subtitle. */
    public final String fallbackName;
    public final long startTime;
    public final float volume;
    public final float pitch;

    /** Cached screen-space NDC for follow-screen mode. NaN until computed. */
    private double storedNdcX = Double.NaN;
    private double storedNdcY = Double.NaN;

    public ActiveSound(SoundInstance instance, Identifier soundId, String subtitleKey,
                       String fallbackName, float volume, float pitch) {
        this.instance = instance;
        this.soundId = soundId;
        this.subtitleKey = subtitleKey;
        this.fallbackName = fallbackName;
        this.startTime = System.currentTimeMillis();
        this.volume = volume;
        this.pitch = pitch;
    }

    public Vec3d getPos() {
        return new Vec3d(instance.getX(), instance.getY(), instance.getZ());
    }

    /** Age in seconds since the sound started playing. */
    public float getAge() {
        return (System.currentTimeMillis() - startTime) / 1000f;
    }

    public boolean hasStoredNdc() {
        return !Double.isNaN(storedNdcX);
    }

    public void storeNdc(double ndcX, double ndcY) {
        this.storedNdcX = ndcX;
        this.storedNdcY = ndcY;
    }

    public double getStoredNdcX() {
        return storedNdcX;
    }

    public double getStoredNdcY() {
        return storedNdcY;
    }
}
