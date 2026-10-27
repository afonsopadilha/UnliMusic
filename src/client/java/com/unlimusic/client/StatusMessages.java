package com.unlimusic.client;

import com.unlimusic.client.audio.MusicPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Until the HUD (E05) exists, tells the player what the music player just did, in the line above the hotbar.
 */
public final class StatusMessages {
	private StatusMessages() {
	}

	public static void register() {
		MusicPlayer.INSTANCE.addListener(StatusMessages::onChange);
	}

	private static void onChange(MusicPlayer music) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return; // leaving the world or closing the game
		}

		switch (music.state()) {
			case LOADING -> player.sendOverlayMessage(music.reconnectAttempt() > 0
					? Component.translatable("message.unlimusic.reconnecting", music.track().title(),
							music.reconnectAttempt(), MusicPlayer.MAX_RECONNECTS)
					: Component.translatable("message.unlimusic.loading", music.track().title()));
			case PLAYING -> player.sendOverlayMessage(Component.translatable("message.unlimusic.playing", music.track().title()));
			case PAUSED -> player.sendOverlayMessage(Component.translatable("message.unlimusic.paused"));
			case STOPPED -> player.sendOverlayMessage(Component.translatable("message.unlimusic.stopped"));
			// Errors go to chat, where they stay long enough to be read.
			case ERROR -> player.sendSystemMessage(
					Component.translatable("message.unlimusic.load_failed", music.track().title(), music.error()));
		}
	}
}
