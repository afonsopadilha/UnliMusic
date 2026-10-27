package com.unlimusic.music;

import java.nio.file.Path;

/**
 * What the screens, the HUD and (from E10) the server know about a track. It says nothing about how to
 * decode it: that is the job of the client's music sources.
 *
 * <p>Lives in {@code src/main} because shared mode will send it between server and clients.
 *
 * @param source     where the track comes from
 * @param location   what the source opens: a file path for now; a stream URL or a Jamendo id later
 * @param title      never empty: the file name when a local file has no title tag
 * @param artist     null when unknown
 * @param durationMs length in milliseconds, or 0 when unknown (a radio stream, or a file not opened yet)
 * @param credit     license and attribution to show (Jamendo), or null
 * @param coverUrl   cover image, or null
 */
public record TrackInfo(Source source, String location, String title, String artist, long durationMs,
		String credit, String coverUrl) {
	public enum Source {
		LOCAL,
		RADIO,
		JAMENDO
	}

	/** A local file before it is opened: only its name is known until lavaplayer reads the tags. */
	public static TrackInfo localFile(Path file) {
		return new TrackInfo(Source.LOCAL, file.toString(), file.getFileName().toString(), null, 0, null, null);
	}

	/** A radio has no end and no fixed position: pausing it means coming back to what is live. */
	public boolean isLive() {
		return source == Source.RADIO;
	}
}
