package com.unlimusic.client.audio;

import com.unlimusic.music.TrackInfo;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

/**
 * Jamendo tracks, streamed by each client straight from Jamendo over HTTP: the server only says which track,
 * it never relays audio (API terms, decision D2).
 *
 * <p>A track has an end and a position like a file, so it pauses in place and can start at any point (shared
 * mode joins a track halfway, E11). But it arrives over the network like a radio, so its stream never makes
 * the sound engine wait (the {@code live} flag) and is filled up before it starts.
 */
public final class JamendoSource implements MusicSource {
	public static final JamendoSource INSTANCE = new JamendoSource();

	private JamendoSource() {
	}

	@Override
	public void open(TrackInfo track, long positionMs, BiConsumer<PcmAudioStream, TrackInfo> onReady,
			Consumer<Component> onError) {
		// The location is the audio URL, whose from= token ties plays to the app: the log only sees the title.
		LavaplayerEngine.load(track.location(), "Jamendo \"" + track.title() + "\"", positionMs, true,
				// Jamendo's own title, artist and length are better than the MP3's tags: keep the track as it is.
				(stream, info) -> Prebuffering.thenReady(stream, track, onReady, onError), onError);
	}
}
