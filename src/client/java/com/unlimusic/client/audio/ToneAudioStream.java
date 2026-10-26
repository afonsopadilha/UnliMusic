package com.unlimusic.client.audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;

/**
 * Test stream (E02): a few seconds of a 440 Hz sine wave, generated on the fly.
 *
 * <p>It proves the Minecraft side of the pipeline (sound instance → engine → OpenAL) without lavaplayer.
 * It uses the same PCM format lavaplayer will hand us, so swapping it for real music changes nothing else.
 */
public final class ToneAudioStream implements AudioStream {
	private static final int SAMPLE_RATE = 44100;
	private static final int CHANNELS = 2;
	private static final int BYTES_PER_FRAME = CHANNELS * Short.BYTES;

	// 16-bit signed samples, little-endian ("bigEndian" = false), 2 channels interleaved as L R L R...
	private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, CHANNELS, true, false);

	private static final double FREQUENCY = 440.0;
	// 20% of full scale: loud enough to hear, quiet enough not to hurt.
	private static final double AMPLITUDE = 0.2 * Short.MAX_VALUE;

	private final long totalFrames;
	private long framesWritten;

	public ToneAudioStream(double seconds) {
		this.totalFrames = (long) (seconds * SAMPLE_RATE);
	}

	@Override
	public AudioFormat getFormat() {
		return FORMAT;
	}

	/**
	 * Called by the sound engine's own thread whenever OpenAL has played a buffer and wants the next one.
	 * {@code size} is in bytes (the engine asks for about one second at a time).
	 */
	@Override
	public ByteBuffer read(int size) {
		int frames = (int) Math.min(size / BYTES_PER_FRAME, totalFrames - framesWritten);

		// OpenAL copies from native memory, so the buffer must be direct. The byte order has to match
		// FORMAT, otherwise every sample is read with its two bytes swapped: loud noise instead of a tone.
		ByteBuffer buffer = ByteBuffer.allocateDirect(frames * BYTES_PER_FRAME).order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < frames; i++) {
			// Counting from the very first frame keeps the wave continuous across reads (no clicks).
			double time = (double) (framesWritten + i) / SAMPLE_RATE;
			short sample = (short) (Math.sin(2 * Math.PI * FREQUENCY * time) * AMPLITUDE);
			buffer.putShort(sample); // left
			buffer.putShort(sample); // right
		}
		framesWritten += frames;

		// An empty buffer once the tone is over: OpenAL plays what it has queued, then the sound ends.
		return buffer.flip();
	}

	@Override
	public void close() {
		// Nothing to release: the tone holds no files, threads or native resources.
	}
}
