package com.unlimusic.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.unlimusic.UnliMusic;
import com.unlimusic.client.audio.MusicPlayer;
import com.unlimusic.client.audio.TestPlayback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * The mod's key mappings. Players can rebind them under Options → Controls → UnliMusic.
 *
 * <p>The default keys are ones vanilla 26.3 leaves free; M is kept for the mod's screen (E09).
 */
public final class ModKeys {
	// Its name comes from the language key "key.category.unlimusic.main".
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(UnliMusic.id("main"));

	public static final KeyMapping TEST = register("key.unlimusic.test", InputConstants.KEY_J);
	public static final KeyMapping PAUSE = register("key.unlimusic.pause", InputConstants.KEY_K);
	public static final KeyMapping STOP = register("key.unlimusic.stop", InputConstants.KEY_U);

	private static final SinglePress TEST_PRESS = new SinglePress(TEST);
	private static final SinglePress PAUSE_PRESS = new SinglePress(PAUSE);
	private static final SinglePress STOP_PRESS = new SinglePress(STOP);

	private ModKeys() {
	}

	/** Called once from the client entrypoint; loading this class also registers the keys above. */
	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(ModKeys::onEndTick);
	}

	private static KeyMapping register(String name, int key) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping(name, InputConstants.Type.KEYBOARD, key, CATEGORY));
	}

	private static void onEndTick(Minecraft client) {
		// Poll every key on every tick, even outside a world, so a press never waits for a later tick.
		boolean test = TEST_PRESS.poll();
		boolean pause = PAUSE_PRESS.poll();
		boolean stop = STOP_PRESS.poll();
		if (client.player == null) {
			return;
		}

		if (test) {
			TestPlayback.play(client);
		}
		if (pause) {
			MusicPlayer.INSTANCE.togglePause();
		}
		if (stop) {
			MusicPlayer.INSTANCE.stop();
		}
	}
}
