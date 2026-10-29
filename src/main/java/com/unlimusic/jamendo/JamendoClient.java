package com.unlimusic.jamendo;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.unlimusic.UnliMusic;
import com.unlimusic.config.ServerConfig;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Searches Jamendo (API v3.0, https://developer.jamendo.com/v3.0) for Creative Commons tracks. Runs on the
 * server only, because every request carries the server owner's client ID (decision D2).
 *
 * <p>Like {@code RadioBrowserClient}, every request runs on its own virtual thread and hands back a
 * {@link CompletableFuture}; the server thread never waits on the network.
 */
public final class JamendoClient {
	/** What a search looks at, and the API parameter for it. */
	public enum SearchBy {
		/** Track, album and artist names, tags and similar artists, best match first. */
		TEXT("search", "relevance"),
		/** Genre or mood tags ("lofi", "rock"); several separated by spaces must all match. */
		TAG("tags", "popularity_month");

		private final String parameter;
		private final String order;

		SearchBy(String parameter, String order) {
			this.parameter = parameter;
			this.order = order;
		}
	}

	/** Tracks per search, the same for the command and the screen. */
	public static final int RESULTS = 10;
	// Jamendo matches any part of a name, so a single letter would match almost everything.
	public static final int MIN_QUERY_LENGTH = 2;
	// Also the most a search packet may carry (E08).
	public static final int MAX_QUERY_LENGTH = 100;

	private static final String API = "https://api.jamendo.com/v3.0";
	// mp32 is variable bit rate, better than mp31's fixed 96 kbit/s. Seeking a VBR MP3 is coarse in lavaplayer
	// (decision D6), which shared mode will have to weigh up (E11).
	private static final String AUDIO_FORMAT = "mp32";
	private static final int IMAGE_SIZE = 300;
	// The licenses' own URLs, e.g. http://creativecommons.org/licenses/by-nc-sa/3.0/
	private static final Pattern CC_LICENSE = Pattern.compile("/licenses/([a-z-]+)/([0-9.]+)");
	private static final Pattern CC_ZERO = Pattern.compile("/publicdomain/zero/([0-9.]+)");
	// Search results and track details only, in memory, never audio: the API terms (3.3) allow caching only
	// as far as reasonably necessary, and changes on Jamendo's side must show up soon. An hour still turns
	// the same search repeated by a whole server into one request.
	private static final long CACHE_MS = Duration.ofHours(1).toMillis();
	// Jamendo sometimes answers a search with no tracks, and the very same request a minute later finds ten
	// (seen in E08: "lofi" gave 0, 0, then 10). An empty answer is kept only long enough to absorb repeated
	// clicks, so a bad moment on Jamendo's side doesn't stick for an hour.
	private static final long EMPTY_CACHE_MS = Duration.ofMinutes(1).toMillis();
	private static final int CACHE_MAX_SEARCHES = 500;
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

	// Declared after the constants it uses: see RadioBrowserClient.
	public static final JamendoClient INSTANCE = new JamendoClient();

	private final HttpClient http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
	private final ExecutorService executor =
			Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("UnliMusic Jamendo").factory());
	private final Gson gson = new Gson();
	// Keyed by the query without the client ID, which doesn't change the results.
	private final Map<String, CachedSearch> cache = new ConcurrentHashMap<>();
	private final JamendoUsage usage = new JamendoUsage();

	// The ID Jamendo last turned down. While the config still holds it, no request is sent: asking again
	// would only fail again (and a suspended app must stop, terms clause 7). Fixing the file clears it.
	private volatile String rejectedClientId;

	private JamendoClient() {
	}

	/**
	 * At most {@code limit} tracks matching {@code value}. Fails with a {@link JamendoException} (wrapped, as
	 * futures do) that says why, so the player can be told.
	 */
	public CompletableFuture<List<JamendoTrack>> search(SearchBy by, String value, int limit) {
		String clientId = ServerConfig.get().jamendoClientId();
		if (clientId == null) {
			return CompletableFuture.failedFuture(new JamendoException(JamendoException.Reason.NO_CLIENT_ID));
		}
		if (clientId.equals(rejectedClientId)) {
			return CompletableFuture.failedFuture(new JamendoException(JamendoException.Reason.REJECTED));
		}

		// Lower case, so "LoFi" and "lofi" share one cache entry (Jamendo ignores the case anyway).
		String text = value.strip().toLowerCase(Locale.ROOT);
		String query = "&" + by.parameter + "=" + encode(text) + "&order=" + by.order + "&limit=" + limit
				+ "&audioformat=" + AUDIO_FORMAT + "&imagesize=" + IMAGE_SIZE;
		CachedSearch cached = cache.get(query);
		if (cached != null && cached.isFresh()) {
			UnliMusic.LOGGER.info("Jamendo search {} \"{}\": {} tracks (cache)", by, text, cached.tracks().size());
			return CompletableFuture.completedFuture(cached.tracks());
		}

		return CompletableFuture.supplyAsync(() -> {
			TrackJson[] results = get(clientId, "/tracks/", query).results();
			List<JamendoTrack> tracks = Arrays.stream(results)
					.filter(TrackJson::isUsable)
					.map(TrackJson::toTrack)
					.toList();
			remember(query, tracks);
			// Says whether an empty list came from Jamendo or from our filter.
			String leftOut = tracks.size() < results.length
					? ", " + (results.length - tracks.size()) + " more left out as unusable" : "";
			UnliMusic.LOGGER.info("Jamendo search {} \"{}\": {} tracks{} (request {} this month)", by, text,
					tracks.size(), leftOut, usage.requestsThisMonth());
			return tracks;
		}, executor);
	}

	private void remember(String query, List<JamendoTrack> tracks) {
		cache.values().removeIf(old -> !old.isFresh());
		// Bounds the memory a flood of different searches could take; the next hour fills it up again.
		if (cache.size() >= CACHE_MAX_SEARCHES) {
			cache.clear();
		}
		long keepFor = tracks.isEmpty() ? EMPTY_CACHE_MS : CACHE_MS;
		cache.put(query, new CachedSearch(tracks, System.currentTimeMillis() + keepFor));
	}

	/**
	 * Sends one request and checks Jamendo's own status, which comes inside the JSON: an unknown client ID
	 * still answers HTTP 200. Blocks: background threads only.
	 */
	private TracksJson get(String clientId, String path, String query) {
		URI uri = URI.create(API + path + "?client_id=" + encode(clientId) + "&format=json" + query);
		HttpRequest request = HttpRequest.newBuilder(uri)
				.header("User-Agent", UnliMusic.USER_AGENT)
				.timeout(REQUEST_TIMEOUT)
				.build();
		TracksJson json;
		// Counted before sending: a request that fails on the way back has still reached Jamendo.
		usage.count();
		try {
			HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				throw new JamendoException(JamendoException.Reason.OFFLINE, "HTTP " + response.statusCode());
			}
			json = gson.fromJson(response.body(), TracksJson.class);
		} catch (IOException | JsonParseException e) {
			// toString() only: neither carries the URL, which holds the client ID.
			throw new JamendoException(JamendoException.Reason.OFFLINE, e.toString());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new JamendoException(JamendoException.Reason.OFFLINE, "interrupted");
		}
		if (json == null || json.headers() == null || json.results() == null) {
			throw new JamendoException(JamendoException.Reason.OFFLINE, "unexpected answer");
		}

		HeadersJson headers = json.headers();
		// headers.next is the URL of the next page, client ID included: it is never logged.
		if (!headers.warnings().isEmpty()) {
			UnliMusic.LOGGER.warn("Jamendo warning: {}", hideId(headers.warnings(), clientId));
		}
		if (headers.code() != 0) {
			throw failure(headers.code(), hideId(headers.error_message(), clientId), clientId);
		}
		return json;
	}

	/** Turns Jamendo's error code (https://developer.jamendo.com/v3.0/response-codes) into a reason. */
	private JamendoException failure(int code, String message, String clientId) {
		switch (code) {
			// 5: unknown client ID; 11: the application was suspended.
			case 5, 11 -> {
				rejectedClientId = clientId;
				UnliMusic.LOGGER.error("Jamendo rejected the client ID in {} (code {}: {}). Jamendo is off until"
						+ " the ID is fixed and the world reopened", ServerConfig.FILE_NAME, code, message);
				return new JamendoException(JamendoException.Reason.REJECTED);
			}
			case 6 -> {
				UnliMusic.LOGGER.warn("Jamendo rate limit exceeded: {}", message);
				return new JamendoException(JamendoException.Reason.RATE_LIMITED);
			}
			default -> {
				return new JamendoException(JamendoException.Reason.OFFLINE, "code " + code + ": " + message);
			}
		}
	}

	/** In case an error message ever repeats the request back. */
	private static String hideId(String text, String clientId) {
		return text == null ? "" : text.replace(clientId, "<client id>");
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

	/** "CC BY-NC-SA 3.0" from the license's URL, or null for one we don't recognise. */
	static String licenseName(String url) {
		if (url == null) {
			return null;
		}
		Matcher license = CC_LICENSE.matcher(url);
		if (license.find()) {
			return "CC " + license.group(1).toUpperCase(Locale.ROOT) + " " + license.group(2);
		}
		Matcher zero = CC_ZERO.matcher(url);
		return zero.find() ? "CC0 " + zero.group(1) : null;
	}

	private record CachedSearch(List<JamendoTrack> tracks, long expiresAt) {
		boolean isFresh() {
			return System.currentTimeMillis() < expiresAt;
		}
	}

	/** The whole answer: Jamendo's status, then the tracks. */
	private record TracksJson(HeadersJson headers, TrackJson[] results) {
	}

	/** Field names as the API sends them, so Gson can fill them. */
	private record HeadersJson(String status, int code, String error_message, String warnings) {
		@Override
		public String warnings() {
			return warnings == null ? "" : warnings;
		}
	}

	private record TrackJson(String id, String name, int duration, String artist_name, String audio,
			String album_image, String image, String license_ccurl, String shareurl) {
		/** Without these there is nothing to play, or no one to credit. */
		boolean isUsable() {
			return blankToNull(id) != null && blankToNull(name) != null && blankToNull(artist_name) != null
					&& blankToNull(audio) != null && blankToNull(shareurl) != null;
		}

		JamendoTrack toTrack() {
			String cover = blankToNull(album_image) != null ? album_image.strip() : blankToNull(image);
			return new JamendoTrack(id, name.strip(), artist_name.strip(), duration, audio, cover,
					licenseName(license_ccurl), license_ccurl, shareurl);
		}
	}
}
