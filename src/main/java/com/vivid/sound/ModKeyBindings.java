package com.vivid.sound;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding;
import net.fabricmc.fabric.api.client.keybinding.KeyBindingRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Registers the mod's key bindings and wires them to actions.
 * Pressing the configured key opens the {@link ConfigScreen}.
 */
public class ModKeyBindings {

    private static final String CATEGORY = "key.categories.vivid-sound";

    public static final FabricKeyBinding OPEN_CONFIG = FabricKeyBinding.Builder
            .create(VividSound.id("open_config"), InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY)
            .build();

    public static void init() {
        KeyBindingRegistry.INSTANCE.register(OPEN_CONFIG);
        ClientTickEvents.END_CLIENT_TICK.register(ModKeyBindings::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        while (OPEN_CONFIG.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new ConfigScreen(null));
            }
        }
    }
}
