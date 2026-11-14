package com.vivid.sound.mixin;

import com.vivid.sound.SoundTracker;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures every sound played through the sound manager and forwards it to
 * the {@link SoundTracker} so it can be rendered on screen. The subtitle
 * (if any) is resolved from the sound's {@link WeightedSoundSet}.
 */
@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Inject(at = @At("HEAD"), method = "play(Lnet/minecraft/client/sound/SoundInstance;)V")
    private void vividSound$onPlaySound(SoundInstance sound, CallbackInfo ci) {
        if (sound == null) return;
        try {
            Identifier soundId = sound.getId();
            if (soundId == null) return;

            String subtitleKey = null;
            String fallback = soundId.getPath();

            WeightedSoundSet soundSet = sound.getSoundSet((SoundManager) (Object) this);
            if (soundSet != null) {
                Text subtitle = soundSet.getSubtitle();
                if (subtitle != null) {
                    subtitleKey = subtitle.getString();
                }
            }

            SoundTracker.addSound(sound, soundId, subtitleKey, fallback);
        } catch (Throwable ignored) {
            // Never let a capture failure break sound playback.
        }
    }
}
