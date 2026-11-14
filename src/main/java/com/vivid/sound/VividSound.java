package com.vivid.sound;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VividSound implements ClientModInitializer {
	public static final String MOD_ID = "vivid-sound";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("Vivid Sound initializing - rendering sound directions on screen!");
		ModConfig.getInstance(); // load config
		FontManager.scan();      // load custom fonts from config/vivid-sound/fonts/
		SoundTracker.init();
		ModKeyBindings.init();
		HudRenderCallback.EVENT.register(new SoundHudRenderer());
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
