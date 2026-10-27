package com.unlimusic.client.audio;

import com.unlimusic.UnliMusic;
import com.unlimusic.music.TrackInfo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * What the test key plays: the song or radio named in a dev file. The local library (E14), the radio
 * search (E06) and the screen (E09) replace it.
 */
public final class TestPlayback {
	// Lives in the game's config folder (run/config in development), which Git ignores.
	// Plain text rather than .properties: Windows paths are full of backslashes, which
	// .properties would read as escape characters.
	private static final String TEST_TRACK_FILE = "unlimusic-dev-track.txt";

	private TestPlayback() {
	}

	/** Game thread only: plays the test song from the start, replacing whatever was playing. */
	public static void play(Minecraft client) {
		Path configFile = FabricLoader.getInstance().getConfigDir().resolve(TEST_TRACK_FILE);
		TrackInfo track = readTestTrack(configFile);
		if (track == null) {
			if (client.player != null) {
				client.player.sendSystemMessage(
						Component.translatable("message.unlimusic.no_test_track", configFile.toAbsolutePath().toString()));
			}
			return;
		}
		MusicPlayer.INSTANCE.play(track);
	}

	/** The first line of the dev file that is not empty or a # comment, or null if there is none. */
	private static TrackInfo readTestTrack(Path configFile) {
		try {
			if (Files.notExists(configFile)) {
				Files.writeString(configFile, """
						# What UnliMusic's test key plays, on the next line: the path of an audio file,
						# or a radio URL optionally followed by the station's name, e.g.
						# https://example.com/stream.mp3 My Station
						""");
				return null;
			}

			// UTF-8 so that paths with accents survive; Notepad may also add an invisible BOM at the start.
			for (String line : Files.readAllLines(configFile, StandardCharsets.UTF_8)) {
				line = line.replace("﻿", "").strip();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				if (line.startsWith("http://") || line.startsWith("https://")) {
					// URLs have no spaces, so whatever follows the first one is the name.
					String[] parts = line.split("\\s+", 2);
					return TrackInfo.radio(parts[0], parts.length > 1 ? parts[1] : null);
				}
				// Explorer's "Copy as path" wraps the path in quotes.
				if (line.length() > 1 && line.startsWith("\"") && line.endsWith("\"")) {
					line = line.substring(1, line.length() - 1);
				}
				return TrackInfo.localFile(Path.of(line));
			}
		} catch (IOException | RuntimeException e) {
			UnliMusic.LOGGER.warn("Could not read {}", configFile, e);
		}
		return null;
	}
}
