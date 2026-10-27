package com.unlimusic.client.audio;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormatTools;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.track.playback.ImmutableAudioFrame;
import com.unlimusic.UnliMusic;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
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
 *
 * <p>A file decodes far faster than it plays, so waiting for the next frame is never long. A live stream
 * arrives in real time, and waiting for it would freeze every game sound (they share the engine's thread).
 * So a live stream never waits: when the network falls behind it hands out silence instead, and its
 * owner fills it up front with {@link #prebuffer} so that this rarely happens.
 */
public final class PcmAudioStream implements AudioStream {
	// Same numbers as LavaplayerEngine.FORMAT, in the javax.sound class the engine expects.
	private static final AudioFormat FORMAT = AudioDataFormatTools.toAudioFormat(LavaplayerEngine.FORMAT);

	// How long one wait for a frame lasts, and how long we keep waiting before giving up on the track.
	private static final long FRAME_WAIT_MS = 100;
	private static final long STALL_LIMIT_MS = 5000;
	// Live streams: how long we hand out silence while lavaplayer still says it is playing.
	private static final long LIVE_STALL_LIMIT_MS = 15_000;
	// Live streams: after running dry, how much audio to gather before playing again. Resuming with whatever
	// trickled in would run dry again a moment later: a stutter instead of one clean gap.
	private static final long REBUFFER_MS = 3_000;
	// Marker returned by nextLiveFrame() when there is no audio yet; compared by identity, never played.
	private static final AudioFrame SILENCE = new ImmutableAudioFrame(0, new byte[0], 0, LavaplayerEngine.FORMAT);

	private final AudioPlayer player;
	private final boolean live;

	// Live streams: frames taken from lavaplayer ahead of time (by prebuffer(), and while rebuffering), handed
	// out before asking lavaplayer for more. Filled before the stream reaches the engine and only touched on
	// the engine's thread after that, so it needs no lock.
	private final ArrayDeque<AudioFrame> buffered = new ArrayDeque<>();
	// When a live stream ran dry and started rebuffering, or 0 while it is playing.
	private long rebufferingSinceMs;

	// Part of the last frame that did not fit into the previous read.
	private byte[] pending;
	private int pendingOffset;

	// Written on the sound engine's thread, read on the game thread by MusicPlayer. Without volatile the
	// game thread could keep seeing an old value cached by its CPU core, for as long as it likes.
	private volatile long startedNanos;
	private volatile boolean ended;

	PcmAudioStream(AudioPlayer player, boolean live) {
		this.player = player;
		this.live = live;
	}

	/**
	 * Waits, on the calling thread, until {@code durationMs} of audio is decoded and set aside, so the
	 * engine's first reads (four seconds at once) find it waiting. Must run before the stream reaches the
	 * engine. Returns false if the track ended or {@code timeoutMs} passed first.
	 */
	boolean prebuffer(long durationMs, long timeoutMs) throws InterruptedException {
		long frames = durationMs / LavaplayerEngine.FORMAT.frameDuration();
		long deadline = System.currentTimeMillis() + timeoutMs;

		while (buffered.size() < frames) {
			if (System.currentTimeMillis() >= deadline || player.getPlayingTrack() == null) {
				return false;
			}
			try {
				AudioFrame frame = player.provide(FRAME_WAIT_MS, TimeUnit.MILLISECONDS);
				if (frame != null) {
					buffered.add(frame);
				}
			} catch (TimeoutException e) {
				// Nothing decoded yet: wait again.
			}
		}
		return true;
	}

	@Override
	public AudioFormat getFormat() {
		return FORMAT;
	}

	/**
	 * Runs on the sound engine's thread. Every millisecond spent here delays all other game sounds, which
	 * is fine for local files (lavaplayer decodes far ahead); live streams never wait here.
	 */
	@Override
	public ByteBuffer read(int size) throws InterruptedIOException {
		// The engine reads the first buffers right before it starts playing them: from here on we are audible.
		if (startedNanos == 0) {
			startedNanos = System.nanoTime();
		}

		// The engine may still ask a few more times before it notices the end: the answer stays "nothing".
		if (ended) {
			return ByteBuffer.allocateDirect(0);
		}

		// lavaplayer already outputs little-endian bytes, so they are copied as they are: no byte order to set.
		// A new direct buffer is all zeros, which is silence.
		ByteBuffer buffer = ByteBuffer.allocateDirect(size);

		while (buffer.hasRemaining()) {
			if (pending == null) {
				AudioFrame frame = live ? nextLiveFrame() : nextFrame();
				if (frame == SILENCE) {
					// The rest of this buffer stays silent; a short buffer would make the engine end the sound.
					buffer.position(buffer.limit());
					break;
				}
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

	/**
	 * Like {@link #nextFrame} without waiting: {@link #SILENCE} when the stream is behind. Once it runs dry,
	 * it stays silent until {@link #REBUFFER_MS} of audio has piled up again.
	 */
	private AudioFrame nextLiveFrame() {
		boolean trackEnded = player.getPlayingTrack() == null;

		if (rebufferingSinceMs != 0) {
			// Set aside everything decoded since the last read, without waiting for more.
			for (AudioFrame frame = player.provide(); frame != null; frame = player.provide()) {
				buffered.add(frame);
			}
			long silentMs = System.currentTimeMillis() - rebufferingSinceMs;
			if (buffered.size() * LavaplayerEngine.FORMAT.frameDuration() < REBUFFER_MS && !trackEnded) {
				if (silentMs > LIVE_STALL_LIMIT_MS) {
					UnliMusic.LOGGER.warn("No audio from the stream for {} ms, ending it", LIVE_STALL_LIMIT_MS);
					return null;
				}
				return SILENCE;
			}
			UnliMusic.LOGGER.info("Stream caught up after {} ms of silence", silentMs);
			rebufferingSinceMs = 0;
		}

		AudioFrame frame = buffered.isEmpty() ? player.provide() : buffered.poll();
		if (frame != null) {
			return frame;
		}
		if (trackEnded) {
			return null; // the stream ended or dropped; lavaplayer has logged why
		}
		rebufferingSinceMs = System.currentTimeMillis();
		UnliMusic.LOGGER.info("Stream fell behind, playing silence while {} ms of audio arrives", REBUFFER_MS);
		return SILENCE;
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
