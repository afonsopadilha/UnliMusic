package com.unlimusic.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.unlimusic.UnliMusic;
import com.unlimusic.client.audio.TestPlayback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * The mod's key mappings. Players can rebind them under Options → Controls → UnliMusic.
 */
public final class ModKeys {
	// Its name comes from the language key "key.category.unlimusic.main".
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(UnliMusic.id("main"));

	public static final KeyMapping TEST = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.unlimusic.test", InputConstants.Type.KEYBOARD, InputConstants.KEY_J, CATEGORY));

	private static final SinglePress TEST_PRESS = new SinglePress(TEST);

	private ModKeys() {
	}

	/** Called once from the client entrypoint; loading this class also registers the keys above. */
	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(ModKeys::onEndTick);
	}

	private static void onEndTick(Minecraft client) {
		if (TEST_PRESS.poll() && client.player != null) {
			TestPlayback.toggle(client);
		}
	}
}
