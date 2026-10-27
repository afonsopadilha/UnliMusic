package com.unlimusic.client.audio;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormatTools;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.unlimusic.UnliMusic;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;

/**
 * Feeds the PCM a lavaplayer {@link AudioPlayer} decodes to Minecraft's sound engine.
 *
 * <p>The engine asks for about a second of audio per {@link #read}; lavaplayer hands out 20 ms frames.
 * This glues frames together until the request is full, keeping any leftover bytes for the next read.
 *
 * <p>We read frames with {@code player.provide(...)} instead of lavaplayer's {@code AudioPlayerInputStream},
 * because that one never reports the end of a track: it either blocks or returns silence forever.
 */
public final class PcmAudioStream implements AudioStream {
	// Same numbers as LavaplayerEngine.FORMAT, in the javax.sound class the engine expects.
	private static final AudioFormat FORMAT = AudioDataFormatTools.toAudioFormat(LavaplayerEngine.FORMAT);

	// How long one wait for a frame lasts, and how long we keep waiting before giving up on the track.
	private static final long FRAME_WAIT_MS = 100;
	private static final long STALL_LIMIT_MS = 5000;

	private final AudioPlayer player;

	// Part of the last frame that did not fit into the previous read.
	private byte[] pending;
	private int pendingOffset;

	// Written on the sound engine's thread, read on the game thread by MusicPlayer. Without volatile the
	// game thread could keep seeing an old value cached by its CPU core, for as long as it likes.
	private volatile long startedNanos;
	private volatile boolean ended;

	PcmAudioStream(AudioPlayer player) {
		this.player = player;
	}

	@Override
	public AudioFormat getFormat() {
		return FORMAT;
	}

	/**
	 * Runs on the sound engine's thread. Every millisecond spent here delays all other game sounds, which
	 * is fine for local files (lavaplayer decodes far ahead) but will need prebuffering for radio (E04).
	 */
	@Override
	public ByteBuffer read(int size) throws InterruptedIOException {
		// The engine reads the first buffers right before it starts playing them: from here on we are audible.
		if (startedNanos == 0) {
			startedNanos = System.nanoTime();
		}

		// lavaplayer already outputs little-endian bytes, so they are copied as they are: no byte order to set.
		ByteBuffer buffer = ByteBuffer.allocateDirect(size);

		while (buffer.hasRemaining()) {
			if (pending == null) {
				AudioFrame frame = nextFrame();
				if (frame == null) {
					ended = true;
					break; // end of the track
				}
				pending = frame.getData();
				pendingOffset = 0;
			}

			int length = Math.min(buffer.remaining(), pending.length - pendingOffset);
			buffer.put(pending, pendingOffset, length);
			pendingOffset += length;
			if (pendingOffset == pending.length) {
				pending = null;
			}
		}

		// Shorter than asked (or empty) only when the track is over: the engine then lets the sound end.
		return buffer.flip();
	}

	/** The next decoded frame, or null once the track has ended (or stalled for too long). */
	private AudioFrame nextFrame() throws InterruptedIOException {
		long deadline = System.currentTimeMillis() + STALL_LIMIT_MS;

		while (System.currentTimeMillis() < deadline) {
			try {
				AudioFrame frame = player.provide(FRAME_WAIT_MS, TimeUnit.MILLISECONDS);
				if (frame != null) {
					return frame;
				}
			} catch (TimeoutException e) {
				// Decoding is behind: wait again.
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new InterruptedIOException();
			}

			// When the last frame has been handed out, lavaplayer clears the playing track.
			if (player.getPlayingTrack() == null) {
				return null;
			}
		}

		UnliMusic.LOGGER.warn("No audio from lavaplayer for {} ms, ending the track", STALL_LIMIT_MS);
		return null;
	}

	/** When the sound engine first asked for audio ({@link System#nanoTime}), or 0 if it has not yet. */
	public long startedNanos() {
		return startedNanos;
	}

	/** True once the last frame of the track was handed to the engine (it may still be playing the last seconds). */
	public boolean hasEnded() {
		return ended;
	}

	@Override
	public void close() {
		// Called by the engine when the sound ends or is stopped: stops decoding and frees the player.
		player.destroy();
	}
}
