package com.unlimusic.client.audio;

import com.unlimusic.UnliMusic;
import com.unlimusic.music.TrackInfo;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

/**
 * Fills a stream that comes over the network (radio, Jamendo) before handing it on, so the sound engine's
 * first reads find audio waiting instead of silence.
 */
final class Prebuffering {
	// The engine takes 4 s of audio as soon as the sound starts; the rest is slack for a slow network.
	private static final long PREBUFFER_MS = 6_000;
	// Most servers send a burst of several seconds on connect; one that only trickles gets this long.
	private static final long PREBUFFER_TIMEOUT_MS = 20_000;

	private Prebuffering() {
	}

	/**
	 * Returns at once. Waiting for the buffer takes seconds; a virtual thread is a cheap place to block, and
	 * it keeps lavaplayer's own loader threads free. Then exactly one of the callbacks runs, on that thread.
	 */
	static void thenReady(PcmAudioStream stream, TrackInfo track, BiConsumer<PcmAudioStream, TrackInfo> onReady,
			Consumer<Component> onError) {
		Thread.ofVirtual().name("UnliMusic buffering").start(() -> {
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
		});
	}
}
