package com.unlimusic.client.screen;

import com.unlimusic.UnliMusic;
import com.unlimusic.radio.RadioBrowserClient;
import com.unlimusic.radio.RadioBrowserClient.SearchBy;
import com.unlimusic.radio.RadioStation;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * The search behind the Radios tab, kept apart from the widgets so that it outlives the screen: closing and
 * reopening it shows the same box and results without asking Radio Browser again. The screen's widgets are
 * rebuilt every time it opens; this is not.
 *
 * <p>Game thread only. Answers arrive on a background thread and hop over with {@code client.execute(...)}
 * before touching anything here.
 */
final class RadioSearch {
	static final RadioSearch INSTANCE = new RadioSearch();

	// Enough for a big country (the US has thousands of stations) without a second page.
	static final int RESULTS = 100;

	enum Status {
		/** Nothing asked yet since the game started. */
		IDLE,
		LOADING,
		DONE,
		FAILED
	}

	private Status status = Status.IDLE;
	private List<RadioStation> stations = List.of();
	// What the last search looked for, as the messages say it: "in Portugal", "with “jazz” in the name".
	private Component what = Component.empty();

	// The box and the mode as the player left them.
	private SearchBy by = SearchBy.COUNTRY;
	private String text = "";

	// Bumped by every search: an answer that arrives with an older number belongs to a search nobody wants now.
	private int searchId;
	// Bumped by every change of status, so the tab can tell when to refresh without being told.
	private int version;

	private RadioSearch() {
	}

	Status status() {
		return status;
	}

	List<RadioStation> stations() {
		return stations;
	}

	Component what() {
		return what;
	}

	int version() {
		return version;
	}

	SearchBy by() {
		return by;
	}

	String text() {
		return text;
	}

	void remember(SearchBy by, String text) {
		this.by = by;
		this.text = text;
	}

	/** Asks Radio Browser and returns at once; {@link #status()} says LOADING until the answer arrives. */
	void start(SearchBy by, String value, Component what) {
		int id = ++searchId;
		status = Status.LOADING;
		stations = List.of();
		this.what = what;
		version++;

		Minecraft client = Minecraft.getInstance();
		RadioBrowserClient.INSTANCE.search(by, value, RESULTS).whenComplete((found, error) -> client.execute(() -> {
			if (id != searchId) {
				return; // the player searched for something else meanwhile
			}
			if (error != null) {
				UnliMusic.LOGGER.warn("Radio search {} {} failed", by, value, error);
				status = Status.FAILED;
			} else {
				stations = found;
				status = Status.DONE;
			}
			version++;
		}));
	}
}
