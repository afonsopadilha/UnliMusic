package com.unlimusic.radio;

import com.unlimusic.music.TrackInfo;
import java.util.List;

/**
 * A station found on Radio Browser, already checked to be one we can play (not HLS, a codec lavaplayer
 * decodes, online at the last check). Only the fields the mod uses; the API sends some 40 more.
 *
 * <p>Lives in {@code src/main} because the server will also look stations up (shared mode, E10/E12).
 *
 * @param uuid        Radio Browser's id for the station; stays the same when its details change
 * @param name        trimmed, never empty
 * @param url         the stream as the station gave it, which may be a playlist or a redirect (decision D16)
 * @param homepage    the station's website, or null
 * @param favicon     a logo image, or null
 * @param tags        genres and descriptions, lower case as the API sends them; may be empty
 * @param countryCode ISO 3166-1 alpha-2 (e.g. PT), or null
 * @param codec       as Radio Browser detected it: MP3, AAC, AAC+ or OGG
 * @param bitrate     in kbit/s, or 0 when unknown
 */
public record RadioStation(String uuid, String name, String url, String homepage, String favicon, List<String> tags,
		String countryCode, String codec, int bitrate) {
	/** What the music player opens. */
	public TrackInfo toTrack() {
		return TrackInfo.radio(url, name);
	}
}
