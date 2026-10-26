package com.unlimusic.client.audio;

import com.unlimusic.UnliMusic;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.network.chat.Component;

/**
 * What the test key plays (E02): the song named in a dev file, or a test tone if there is none.
 * The real playback engine (E03) replaces it.
 */
public final class TestPlayback {
	// Lives in the game's config folder (run/config in development), which Git ignores.
	// Plain text rather than .properties: Windows paths are full of backslashes, which
	// .properties would read as escape characters.
	private static final String TEST_TRACK_FILE = "unlimusic-dev-track.txt";

	// Only touched on the game thread: lavaplayer's callbacks come back through client.execute.
	private static MusicSoundInstance current;
	private static boolean loading;

	private TestPlayback() {
	}

	/** Game thread only: starts the test sound, or stops it if it is still playing. */
	public static void toggle(Minecraft client) {
		if (loading) {
			return;
		}
		if (current != null && client.getSoundManager().isActive(current)) {
			client.getSoundManager().stop(current);
			current = null;
			return;
		}

		Path configFile = FabricLoader.getInstance().getConfigDir().resolve(TEST_TRACK_FILE);
		Path track = readTestTrack(configFile);
		if (track == null) {
			say(client, Component.translatable("message.unlimusic.test_tone", configFile.toAbsolutePath().toString()));
			play(client, new ToneAudioStream(5.0));
			return;
		}

		loading = true;
		String name = track.getFileName().toString();
		say(client, Component.translatable("message.unlimusic.loading", name));
		LavaplayerEngine.load(track,
				stream -> client.execute(() -> {
					loading = false;
					if (client.player == null) {
						stream.close(); // left the world while it was loading
						return;
					}
					play(client, stream);
				}),
				reason -> client.execute(() -> {
					loading = false;
					say(client, Component.translatable("message.unlimusic.load_failed", name, reason));
				}));
	}

	private static void play(Minecraft client, AudioStream stream) {
		current = new MusicSoundInstance(stream);
		SoundEngine.PlayResult result = client.getSoundManager().play(current);
		UnliMusic.LOGGER.info("Test playback: {}", result);
	}

	/** The first line of the dev file that is not empty or a # comment, or null if there is none. */
	private static Path readTestTrack(Path configFile) {
		try {
			if (Files.notExists(configFile)) {
				Files.writeString(configFile, "# Path of an audio file for UnliMusic's test key (E02), on the next line.\n");
				return null;
			}

			// UTF-8 so that paths with accents survive; Notepad may also add an invisible BOM at the start.
			for (String line : Files.readAllLines(configFile, StandardCharsets.UTF_8)) {
				line = line.replace("﻿", "").strip();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				// Explorer's "Copy as path" wraps the path in quotes.
				if (line.length() > 1 && line.startsWith("\"") && line.endsWith("\"")) {
					line = line.substring(1, line.length() - 1);
				}
				return Path.of(line);
			}
		} catch (IOException | RuntimeException e) {
			UnliMusic.LOGGER.warn("Could not read {}", configFile, e);
		}
		return null;
	}

	private static void say(Minecraft client, Component message) {
		if (client.player != null) {
			client.player.sendSystemMessage(message);
		}
	}
}
