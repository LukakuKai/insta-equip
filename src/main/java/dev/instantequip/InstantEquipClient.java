package dev.instantequip;

import dev.instantequip.config.InstantEquipConfig;
import dev.instantequip.gui.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class InstantEquipClient implements ClientModInitializer {

	private static KeyBinding openMenuKey;

	@Override
	public void onInitializeClient() {
		InstantEquipConfig.load();

		openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.instantequip.open_menu",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_K,
			KeyBinding.Category.create(Identifier.of("instantequip", "main"))
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openMenuKey.wasPressed()) {
				client.setScreen(new ConfigScreen(client.currentScreen));
			}
		});
	}
}
