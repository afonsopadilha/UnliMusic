package com.unlimusic.client.audio;

import com.unlimusic.UnliMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.network.chat.Component;

/**
 * What the test key plays (E02). The real playback engine (E03) replaces it.
 */
public final class TestPlayback {
	private static MusicSoundInstance current;

	private TestPlayback() {
	}

	/** Game thread only: starts the test sound, or stops it if it is still playing. */
	public static void toggle(Minecraft client) {
		if (current != null && client.getSoundManager().isActive(current)) {
			client.getSoundManager().stop(current);
			current = null;
			return;
		}

		current = new MusicSoundInstance(new ToneAudioStream(5.0));
		SoundEngine.PlayResult result = client.getSoundManager().play(current);
		UnliMusic.LOGGER.info("Test tone: {}", result);
		client.player.sendSystemMessage(Component.translatable("message.unlimusic.test_tone"));
	}
}
