package com.unlimusic.jamendo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.unlimusic.UnliMusic;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.time.ZoneOffset;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Counts the requests this server sends to Jamendo each month, so its owner hears about it before the free
 * quota runs out. Jamendo doesn't block an app that goes over: it adds a warning to its answers, emails the
 * owner, and may limit or ban the app if nobody replies. So this only warns in the log; it never stops a search.
 *
 * <p>Kept in {@code config/unlimusic-jamendo-usage.json}, so restarting the server doesn't reset the count.
 * The file holds only a month and a number, nothing secret.
 */
final class JamendoUsage {
	// The developer portal's figure for free non-commercial apps; the API docs give none.
	static final int MONTHLY_QUOTA = 35_000;
	private static final int WARN_AT = MONTHLY_QUOTA * 80 / 100;
	private static final String FILE_NAME = "unlimusic-jamendo-usage.json";

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);

	// Read from the file on the first request, so a server that never uses Jamendo never creates it.
	private boolean loaded;
	private YearMonth month;
	private int requests;

	/** One more request sent. Called from the request threads, hence synchronized. */
	synchronized void count() {
		if (!loaded) {
			load();
			loaded = true;
		}
		// Jamendo doesn't say which time zone its month follows; UTC is the likeliest.
		YearMonth now = YearMonth.now(ZoneOffset.UTC);
		if (!now.equals(month)) {
			month = now;
			requests = 0;
		}
		requests++;
		// Exactly at each mark, so each warning is written once.
		if (requests == WARN_AT) {
			UnliMusic.LOGGER.warn("UnliMusic has sent {} of the {} Jamendo requests a free app gets this month",
					requests, MONTHLY_QUOTA);
		} else if (requests == MONTHLY_QUOTA) {
			UnliMusic.LOGGER.error("UnliMusic has used all {} Jamendo requests a free app gets this month;"
					+ " Jamendo may email the owner of the client ID and limit it", MONTHLY_QUOTA);
		}
		save();
	}

	synchronized int requestsThisMonth() {
		return requests;
	}

	private void load() {
		if (Files.notExists(file)) {
			return;
		}
		try {
			UsageJson json = gson.fromJson(Files.readString(file), UsageJson.class);
			if (json != null && json.month() != null) {
				month = YearMonth.parse(json.month());
				requests = Math.max(0, json.requests());
			}
		} catch (IOException | JsonParseException | DateTimeException e) {
			// Counting again from zero is the only harm.
			UnliMusic.LOGGER.warn("Could not read {}, counting Jamendo requests from 0: {}", FILE_NAME, e.toString());
		}
	}

	private void save() {
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, gson.toJson(new UsageJson(month.toString(), requests)) + System.lineSeparator());
		} catch (IOException e) {
			UnliMusic.LOGGER.warn("Could not save {}: {}", FILE_NAME, e.toString());
		}
	}

	private record UsageJson(String month, int requests) {
	}
}
