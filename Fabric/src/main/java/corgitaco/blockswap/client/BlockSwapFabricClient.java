package corgitaco.blockswap.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class BlockSwapFabricClient implements ClientModInitializer {

    private static KeyMapping openConfig;

    @Override
    public void onInitializeClient() {
        openConfig = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.blockswap.open_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.blockswap"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (openConfig.consumeClick()) {
                client.setScreen(new BlockIndexScreen(client.screen));
            }
        });
    }
}
