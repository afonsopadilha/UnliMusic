package com.unlimusic.radio;

import com.google.gson.Gson;
import com.unlimusic.UnliMusic;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Talks to Radio Browser (https://api.radio-browser.info), the open, key-free database of radio stations.
 *
 * <p>Every request runs on its own virtual thread and hands back a {@link CompletableFuture}, so the game
 * thread asks and gets called back later; it never waits on the network.
 */
public final class RadioBrowserClient {
	/** What a search looks at, and the API parameter for it. */
	public enum SearchBy {
		/** ISO 3166-1 alpha-2, e.g. PT; the API ignores the case. */
		COUNTRY("countrycode"),
		/** Any part of the station's name. */
		NAME("name"),
		/** Any part of one of its tags (genre, language, "news"...). */
		TAG("tag");

		private final String parameter;

		SearchBy(String parameter) {
			this.parameter = parameter;
		}
	}

	// One address per server behind a single name; a reverse look-up of each gives the server's own name.
	private static final String SERVERS_DNS_NAME = "all.api.radio-browser.info";
	// What lavaplayer decodes, as Radio Browser names it. Its other codecs are video streams (H.264, MP4,
	// FLV), a handful of FLAC stations, and UNKNOWN, which the station check could not tell.
	private static final Set<String> PLAYABLE_CODECS = Set.of("MP3", "AAC", "AAC+", "OGG");
	// HLS and other codecs are filtered out after the answer arrives, so ask for more than will be shown.
	private static final int OVERFETCH = 3;
	// Long enough that paging through results or retyping a search doesn't ask again; short enough that the
	// order (clicks in the last 24 h) and broken stations stay current.
	private static final long CACHE_MS = Duration.ofMinutes(5).toMillis();
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

	// Static fields get their values from top to bottom, and the constructor reads the constants above:
	// declared any higher, INSTANCE would be built while they are still null.
	public static final RadioBrowserClient INSTANCE = new RadioBrowserClient();

	private final HttpClient http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
	private final ExecutorService executor =
			Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("UnliMusic Radio Browser").factory());
	// Gson ships with Minecraft (client and dedicated server), so it costs nothing to use.
	private final Gson gson = new Gson();
	private final Map<String, CachedSearch> cache = new ConcurrentHashMap<>();

	// Shuffled once per look-up; empty until the first request, and again after every server failed.
	private List<String> servers = List.of();
	// Index in servers of the last one that answered: later requests start there.
	private int preferred;

	private RadioBrowserClient() {
	}

	/**
	 * The most-listened playable stations (clicks in the last 24 hours) matching {@code value}, at most
	 * {@code limit} of them. Fails with an {@link IOException} (wrapped, as futures do) when no server answers.
	 */
	public CompletableFuture<List<RadioStation>> search(SearchBy by, String value, int limit) {
		String path = "/json/stations/search?" + by.parameter + "=" + encode(value.strip())
				+ "&order=clickcount&reverse=true&hidebroken=true&limit=" + limit * OVERFETCH;
		CachedSearch cached = cache.get(path);
		if (cached != null && cached.isFresh()) {
			return CompletableFuture.completedFuture(cached.stations());
		}

		return CompletableFuture.supplyAsync(() -> {
			List<RadioStation> stations = playable(get(path), limit);
			cache.values().removeIf(old -> !old.isFresh());
			cache.put(path, new CachedSearch(stations, System.currentTimeMillis() + CACHE_MS));
			return stations;
		}, executor);
	}

	/**
	 * Tells Radio Browser a player started this station: its popularity order is built from these clicks.
	 * The API counts one click per address, station and day, so calling it on every play is fine. Fire and
	 * forget: a click that didn't count is not worth bothering the player with.
	 */
	public void countClick(RadioStation station) {
		executor.execute(() -> {
			try {
				get("/json/url/" + encode(station.uuid()));
			} catch (UncheckedIOException e) {
				UnliMusic.LOGGER.warn("Could not count the click on {}: {}", station.name(), e.getMessage());
			}
		});
	}

	/** Asks each server in turn until one answers 200, and returns its body. Blocks: background threads only. */
	private String get(String path) {
		IOException failure = new IOException("No Radio Browser server found");
		for (String server : serversInOrder()) {
			HttpRequest request = HttpRequest.newBuilder(URI.create("https://" + server + path))
					.header("User-Agent", UnliMusic.USER_AGENT)
					.timeout(REQUEST_TIMEOUT)
					.build();
			try {
				HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
				if (response.statusCode() == 200) {
					remember(server);
					return response.body();
				}
				failure = new IOException(server + " answered HTTP " + response.statusCode());
			} catch (IOException e) {
				failure = e;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new UncheckedIOException(new InterruptedIOException("Radio Browser request interrupted"));
			}
			UnliMusic.LOGGER.warn("Radio Browser server {} failed: {}", server, failure.toString());
		}
		// The next request looks the servers up again: the list may have changed, or the internet come back.
		forgetServers();
		throw new UncheckedIOException(failure);
	}

	/** Every known server, starting with the last one that answered. */
	private synchronized List<String> serversInOrder() {
		if (servers.isEmpty()) {
			servers = lookUpServers();
			preferred = 0;
		}
		List<String> ordered = new ArrayList<>(servers);
		Collections.rotate(ordered, -preferred);
		return ordered;
	}

	private synchronized void remember(String server) {
		// -1 when another thread forgot the list meanwhile; it starts from the top again then.
		preferred = Math.max(0, servers.indexOf(server));
	}

	private synchronized void forgetServers() {
		servers = List.of();
	}

	/**
	 * Finds the servers the way the API docs ask: every address behind {@link #SERVERS_DNS_NAME}, each turned
	 * back into its own name (HTTPS certificates are issued to names, not addresses), shuffled so that players
	 * spread over them. Names are never written into the code, since servers come and go.
	 */
	private static List<String> lookUpServers() {
		List<String> names = new ArrayList<>();
		try {
			// IPv4 and IPv6 addresses of the same server both come back with its one name.
			for (InetAddress address : InetAddress.getAllByName(SERVERS_DNS_NAME)) {
				String name = address.getCanonicalHostName();
				// Without a reverse record the address itself comes back, which HTTPS can't verify.
				if (!name.equals(address.getHostAddress()) && !names.contains(name)) {
					names.add(name);
				}
			}
		} catch (UnknownHostException e) {
			UnliMusic.LOGGER.warn("Could not look up the Radio Browser servers: {}", e.toString());
		}
		Collections.shuffle(names);
		if (names.isEmpty()) {
			// The shared name works too (its certificate covers it) and reaches one of them at random.
			names.add(SERVERS_DNS_NAME);
		}
		UnliMusic.LOGGER.info("Radio Browser servers: {}", names);
		return List.copyOf(names);
	}

	private List<RadioStation> playable(String json, int limit) {
		return Arrays.stream(gson.fromJson(json, StationJson[].class))
				.filter(StationJson::isPlayable)
				.limit(limit)
				.map(StationJson::toStation)
				.toList();
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

	private record CachedSearch(List<RadioStation> stations, long expiresAt) {
		boolean isFresh() {
			return System.currentTimeMillis() < expiresAt;
		}
	}

	/**
	 * A station exactly as the API sends it, with its names and its 0/1 flags; Gson fills it field by field
	 * and ignores the fields left out.
	 */
	private record StationJson(String stationuuid, String name, String url, String homepage, String favicon,
			String tags, String countrycode, String codec, int bitrate, int hls, int lastcheckok) {
		boolean isPlayable() {
			// HLS: lavaplayer only reads MPEG-TS segments, and many stations send packed AAC (decision D16).
			return hls == 0 && lastcheckok == 1 && PLAYABLE_CODECS.contains(codec)
					&& blankToNull(url) != null && blankToNull(name) != null;
		}

		RadioStation toStation() {
			List<String> tagList = tags == null ? List.of() : Arrays.stream(tags.split(","))
					.map(String::strip)
					.filter(tag -> !tag.isEmpty())
					.toList();
			// Names come with stray spaces, even line breaks, which chat would show as they are.
			String cleanName = name.strip().replaceAll("\\s+", " ");
			return new RadioStation(stationuuid, cleanName, url.strip(), blankToNull(homepage), blankToNull(favicon),
					tagList, blankToNull(countrycode), codec, bitrate);
		}
	}
}
