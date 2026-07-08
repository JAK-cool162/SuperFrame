package com.jak.superframe;

import com.jak.superframe.config.SuperFrameConfig;
import com.jak.superframe.framegen.FrameGenerator;
import com.jak.superframe.upscale.Upscaler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuperFrameClient implements ClientModInitializer {
    public static final String MOD_ID = "superframe";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static SuperFrameClient INSTANCE;

    private KeyMapping toggleKey;
    private KeyMapping scaleUpKey;
    private KeyMapping scaleDownKey;
    private KeyMapping fgToggleKey;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        LOGGER.info("SuperFrame initializing - Upscaling + FrameGen for all architectures");

        SuperFrame.init();

        com.jak.superframe.util.HudOverlay.register();

        // Keybinds
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.superframe.toggle", GLFW.GLFW_KEY_O, "category.superframe"
        ));
        scaleUpKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.superframe.scale_up", GLFW.GLFW_KEY_EQUAL, "category.superframe"
        ));
        scaleDownKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.superframe.scale_down", GLFW.GLFW_KEY_MINUS, "category.superframe"
        ));
        fgToggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.superframe.fg_toggle", GLFW.GLFW_KEY_P, "category.superframe"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                SuperFrameConfig cfg = SuperFrame.getConfig();
                cfg.enabled = !cfg.enabled;
                SuperFrame.CONFIG.save();
                SuperFrame.getInstance().onResolutionChanged();
                if (client.player != null) {
                    client.player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("SuperFrame: " + (cfg.enabled ? "ON" : "OFF")),
                            true
                    );
                }
            }
            while (scaleUpKey.consumeClick()) {
                adjustScale(0.05f);
            }
            while (scaleDownKey.consumeClick()) {
                adjustScale(-0.05f);
            }
            while (fgToggleKey.consumeClick()) {
                SuperFrameConfig cfg = SuperFrame.getConfig();
                cfg.frameGenMode = SuperFrameConfig.FrameGenMode.values()[
                        (cfg.frameGenMode.ordinal() + 1) % SuperFrameConfig.FrameGenMode.values().length
                ];
                SuperFrame.CONFIG.save();
                if (client.player != null) {
                    client.player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("FrameGen: " + cfg.frameGenMode.getDisplayName()),
                            true
                    );
                }
            }
        });
    }

    private void adjustScale(float delta) {
        SuperFrameConfig cfg = SuperFrame.getConfig();
        int newPercent = cfg.renderScalePercent + Math.round(delta * 100f);
        cfg.renderScalePercent = Math.max(25, Math.min(400, newPercent));
        SuperFrame.CONFIG.save();
        SuperFrame.getInstance().onResolutionChanged();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(String.format("Render Scale: %.2fx", cfg.getScale())),
                    true
            );
        }
    }

    public static SuperFrameClient getInstance() {
        return INSTANCE;
    }
}
