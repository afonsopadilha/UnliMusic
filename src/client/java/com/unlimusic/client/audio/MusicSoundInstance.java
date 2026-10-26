package com.unlimusic.client.audio;

import com.unlimusic.UnliMusic;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

/**
 * One playback of our own audio inside Minecraft's sound engine.
 *
 * <p>The engine only plays sounds it finds in {@code sounds.json}, so this points at the
 * {@code unlimusic:stream} entry. That entry is marked {@code "stream": true}, which makes the engine
 * ask {@link #getAudioStream} for the audio instead of loading the (empty) {@code .ogg} it names.
 *
 * <p>Each instance plays its stream once: the engine closes the stream when the sound ends or is stopped.
 */
public class MusicSoundInstance extends AbstractSoundInstance {
	private static final Identifier SOUND = UnliMusic.id("stream");

	private final AudioStream stream;

	public MusicSoundInstance(AudioStream stream) {
		// RECORDS is the "Jukebox/Note Blocks" volume slider.
		super(SOUND, SoundSource.RECORDS, SoundInstance.createUnseededRandom());
		this.stream = stream;
		this.volume = 1.0F;
		this.pitch = 1.0F;
		// Relative to the listener at (0, 0, 0) with no attenuation: same volume wherever the player is.
		this.relative = true;
		this.attenuation = Attenuation.NONE;
	}

	@Override
	public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary library, Identifier id, boolean repeatInstantly) {
		// Called on the game thread, so the stream has to exist already: nothing slow happens here.
		return CompletableFuture.completedFuture(stream);
	}

	@Override
	public boolean canStartSilent() {
		// Without this the engine skips the sound when the slider is at 0 and raising it later does nothing.
		return true;
	}
}
