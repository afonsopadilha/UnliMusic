package com.unlimusic.client.audio;

import com.unlimusic.music.TrackInfo;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

/**
 * Turns a {@link TrackInfo} into audio: local files now, radio (E04) and Jamendo (E07) later.
 *
 * <p>A source only opens tracks. Pausing, stopping and noticing the end work the same way for every
 * source, so they live in {@link MusicPlayer} instead of being written three times.
 */
public interface MusicSource {
	/**
	 * Starts opening {@code track}, {@code positionMs} milliseconds in. Returns at once; exactly one of the
	 * callbacks runs later, on a lavaplayer thread: {@code onReady} with the stream and the track's details
	 * as the file or stream describes them, or {@code onError} with the reason.
	 */
	void open(TrackInfo track, long positionMs, BiConsumer<PcmAudioStream, TrackInfo> onReady,
			Consumer<Component> onError);
}
