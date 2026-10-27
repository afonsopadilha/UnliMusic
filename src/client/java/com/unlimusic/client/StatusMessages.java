package com.unlimusic.client;

import com.unlimusic.client.audio.MusicPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Tells the player in chat when a track fails. Everything else (loading, playing, paused) is on the HUD, which
 * hides itself after a failure, so this is the one place the reason can be read.
 */
public final class StatusMessages {
	private StatusMessages() {
	}

	public static void register() {
		MusicPlayer.INSTANCE.addListener(StatusMessages::onChange);
	}

	private static void onChange(MusicPlayer music) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || music.state() != MusicPlayer.State.ERROR) {
			return; // leaving the world or closing the game, or nothing went wrong
		}
		player.sendSystemMessage(Component.translatable("message.unlimusic.load_failed", music.track().title(), music.error()));
	}
}
