package com.unlimusic.jamendo;

/**
 * A Creative Commons track found on Jamendo. Only the fields the mod uses; the API sends a few more.
 *
 * <p>Lives in {@code src/main} because the server searches (it holds the client ID) and will send tracks to
 * the clients, which then stream the audio straight from Jamendo (E08, E11).
 *
 * @param id              Jamendo's id for the track
 * @param name            the track's title
 * @param artist          the artist's name, credited as the creator (Jamendo's terms, 4.1)
 * @param durationSeconds length in seconds
 * @param audioUrl        the stream to play. It carries a token tied to our client ID: never put it in a log
 *                        or in chat, which is why {@link #toString()} leaves it out
 * @param imageUrl        the album cover, or the track's own image for a single; null when there is none
 * @param license         a short name such as "CC BY-NC-SA 3.0", or null when the URL is not one we know
 * @param licenseUrl      the license's page on creativecommons.org
 * @param pageUrl         the track's page on Jamendo: the direct link the terms ask for (4.1)
 */
public record JamendoTrack(String id, String name, String artist, int durationSeconds, String audioUrl,
		String imageUrl, String license, String licenseUrl, String pageUrl) {
	@Override
	public String toString() {
		return "JamendoTrack[" + id + " \"" + name + "\" by " + artist + ", " + license + "]";
	}
}
