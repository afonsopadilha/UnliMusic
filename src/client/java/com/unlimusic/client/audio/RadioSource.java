package com.unlimusic.client.audio;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.unlimusic.UnliMusic;
import com.unlimusic.music.TrackInfo;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

/**
 * Online radio: an endless HTTP stream with no length and no seek, so {@code positionMs} is ignored and
 * every open tunes in to whatever is live.
 */
public final class RadioSource implements MusicSource {
	public static final RadioSource INSTANCE = new RadioSource();

	// The engine takes 4 s of audio as soon as the sound starts; the rest is slack for a slow network.
	private static final long PREBUFFER_MS = 6_000;
	// Most stations send a burst of several seconds on connect; one that only trickles gets this long.
	private static final long PREBUFFER_TIMEOUT_MS = 20_000;

	private RadioSource() {
	}

	@Override
	public void open(TrackInfo track, long positionMs, BiConsumer<PcmAudioStream, TrackInfo> onReady,
			Consumer<Component> onError) {
		LavaplayerEngine.load(track.location(), 0, true, (stream, info) -> {
			logStation(track, info);
			TrackInfo named = withStationName(track, info);
			// Waiting for the buffer takes seconds; a virtual thread is a cheap place to block,
			// and it keeps lavaplayer's own loader threads free.
			Thread.ofVirtual().name("UnliMusic radio buffering").start(() -> prebuffer(stream, named, onReady, onError));
		}, onError);
	}

	/** A station opened without a name (its title is still its address) takes the one its server sends, if any. */
	private static TrackInfo withStationName(TrackInfo track, AudioTrackInfo info) {
		if (!track.title().equals(track.location()) || info.author == null || info.author.isBlank()
				|| MediaContainerDetection.UNKNOWN_ARTIST.equals(info.author)) {
			return track;
		}
		return TrackInfo.radio(track.location(), info.author.strip());
	}

	private static void prebuffer(PcmAudioStream stream, TrackInfo track, BiConsumer<PcmAudioStream, TrackInfo> onReady,
			Consumer<Component> onError) {
		long start = System.currentTimeMillis();
		boolean filled;
		try {
			filled = stream.prebuffer(PREBUFFER_MS, PREBUFFER_TIMEOUT_MS);
		} catch (InterruptedException e) {
			filled = false;
		}

		if (filled) {
			UnliMusic.LOGGER.info("Buffered {} ms of {} in {} ms", PREBUFFER_MS, track.title(), System.currentTimeMillis() - start);
			onReady.accept(stream, track);
		} else {
			// Nobody else has the stream yet, so closing it (and the connection) is our job.
			stream.close();
			onError.accept(Component.translatable("message.unlimusic.load_failed.no_audio"));
		}
	}

	/**
	 * Stations describe themselves in Icecast/Shoutcast headers, which lavaplayer puts in the track info:
	 * {@code icy-name} as the author and {@code icy-description} (or {@code icy-url}) as the title. The song
	 * on air ({@code StreamTitle}) would need the {@code Icy-MetaData} request header and lavaplayer has no
	 * way to strip that metadata out of the audio again, so it is not read.
	 */
	private static void logStation(TrackInfo track, AudioTrackInfo info) {
		UnliMusic.LOGGER.info("Radio {}: icy-name \"{}\", icy-description \"{}\", stream {}",
				track.title(), info.author, info.title, info.uri);
	}
}
