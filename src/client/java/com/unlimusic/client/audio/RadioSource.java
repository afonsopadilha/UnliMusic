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

	private RadioSource() {
	}

	@Override
	public void open(TrackInfo track, long positionMs, BiConsumer<PcmAudioStream, TrackInfo> onReady,
			Consumer<Component> onError) {
		LavaplayerEngine.load(track.location(), track.location(), 0, true, (stream, info) -> {
			logStation(track, info);
			Prebuffering.thenReady(stream, withStationName(track, info), onReady, onError);
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
