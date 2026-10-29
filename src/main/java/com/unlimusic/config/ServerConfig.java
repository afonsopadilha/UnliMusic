package com.unlimusic.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.unlimusic.UnliMusic;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Settings that belong to whoever runs the server: the integrated one in singleplayer, or a dedicated one.
 * They live in {@code config/unlimusic-server.json}, a file kept out of Git, because the Jamendo client ID is
 * personal (Jamendo's terms, clause 2) and anyone who copies it would spend the server's monthly quota.
 *
 * <p>Read again every time a server starts, so an owner can fix the file and reopen the world without
 * restarting the game. Until then, and whenever the file is missing or broken, Jamendo stays off: the rest of
 * the mod never depends on it.
 */
public final class ServerConfig {
	public static final String FILE_NAME = "unlimusic-server.json";
	// Fabric always puts the config folder in the game folder; the short form keeps usernames out of the log.
	private static final String SHOWN_PATH = "config/" + FILE_NAME;
	private static final String GET_AN_ID = "register an application at https://devportal.jamendo.com"
			+ " and put its client ID in " + SHOWN_PATH + ", then reopen the world";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	// volatile: written on the server thread, read later by the threads that call Jamendo.
	private static volatile ServerConfig current = new ServerConfig(null);

	private final String jamendoClientId;

	private ServerConfig(String jamendoClientId) {
		this.jamendoClientId = jamendoClientId == null || jamendoClientId.isBlank() ? null : jamendoClientId.strip();
	}

	/** The settings of the server running now (Jamendo off if none has started). */
	public static ServerConfig get() {
		return current;
	}

	/** Reads the file, creating it with empty values the first time. Never throws: a bad file turns Jamendo off. */
	public static void load() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		current = read(file);
		if (current.hasJamendo()) {
			// Only that there is one: the ID itself never goes into the log, where bug reports would share it.
			UnliMusic.LOGGER.info("Jamendo client ID found in {}", SHOWN_PATH);
		} else {
			UnliMusic.LOGGER.warn("Jamendo is disabled, there is no client ID. To turn it on, {}", GET_AN_ID);
		}
	}

	private static ServerConfig read(Path file) {
		if (Files.notExists(file)) {
			createDefault(file);
			return new ServerConfig(null);
		}
		try {
			FileJson json = GSON.fromJson(Files.readString(file), FileJson.class);
			// An empty file reads as null.
			return new ServerConfig(json == null ? null : json.jamendoClientId());
		} catch (IOException | JsonParseException e) {
			// Left as it is, so a typo doesn't cost the owner the ID they pasted; Gson's message says where it is.
			UnliMusic.LOGGER.error("Could not read {}: {}", SHOWN_PATH, e.getMessage());
			return new ServerConfig(null);
		}
	}

	private static void createDefault(Path file) {
		try {
			Files.createDirectories(file.getParent());
			// CREATE_NEW: never overwrite a file that appeared in the meantime.
			Files.writeString(file, GSON.toJson(new FileJson("")) + System.lineSeparator(),
					StandardOpenOption.CREATE_NEW);
			UnliMusic.LOGGER.info("Created {} with empty settings", SHOWN_PATH);
		} catch (FileAlreadyExistsException e) {
			// Someone else wrote it first; it will be read at the next start.
		} catch (IOException e) {
			UnliMusic.LOGGER.error("Could not create {}: {}", SHOWN_PATH, e.toString());
		}
	}

	public boolean hasJamendo() {
		return jamendoClientId != null;
	}

	/**
	 * The ID every Jamendo request carries, or null when there is none. It is a secret: never put it in a
	 * log, a chat message or a packet, and mind the audio URLs Jamendo sends back, which contain it.
	 */
	public String jamendoClientId() {
		return jamendoClientId;
	}

	/** The file exactly as written on disk; Gson fills it by name and ignores anything else in it. */
	private record FileJson(String jamendoClientId) {
	}
}
