package com.unlimusic.client.audio;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.unlimusic.music.TrackInfo;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

/** Audio files on the player's own computer. */
public final class LocalFileSource implements MusicSource {
	public static final LocalFileSource INSTANCE = new LocalFileSource();

	private LocalFileSource() {
	}

	@Override
	public void open(TrackInfo track, long positionMs, BiConsumer<PcmAudioStream, TrackInfo> onReady,
			Consumer<Component> onError) {
		LavaplayerEngine.load(track.location(), positionMs,
				(stream, info) -> onReady.accept(stream, withTags(track, info)), onError);
	}

	/** The file's own title, artist and length; lavaplayer says "Unknown title/artist" when a tag is missing. */
	private static TrackInfo withTags(TrackInfo track, AudioTrackInfo info) {
		String title = info.title == null || MediaContainerDetection.UNKNOWN_TITLE.equals(info.title)
				? track.title() : info.title;
		String artist = info.author == null || MediaContainerDetection.UNKNOWN_ARTIST.equals(info.author)
				? null : info.author;
		long durationMs = info.isStream ? 0 : info.length;
		return new TrackInfo(track.source(), track.location(), title, artist, durationMs, track.credit(), track.coverUrl());
	}
}
