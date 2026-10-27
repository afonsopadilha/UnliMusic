package com.unlimusic.client.audio;

import com.mojang.blaze3d.audio.Channel;
import com.unlimusic.UnliMusic;
import com.unlimusic.client.mixin.SoundEngineAccessor;
import com.unlimusic.client.mixin.SoundManagerAccessor;
import com.unlimusic.music.TrackInfo;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.network.chat.Component;

/**
 * The client's one music player: knows what is playing and moves between states on request.
 *
 * <pre>
 * any state --play-->                 LOADING   (replaces whatever was there)
 * LOADING   --loaded / failed-->      PLAYING / ERROR
 * PLAYING   --pause-->                PAUSED
 * PAUSED    --resume-->               PLAYING
 * any state --stop, end of track, /stopsound, leaving the world-->  STOPPED
 *
 * radio only:
 * PLAYING   --stream dropped-->       LOADING   (waits 1 s, 2 s, 4 s... between attempts)
 * LOADING   --reconnect failed-->     LOADING, or ERROR after the last attempt
 * PAUSED    --resume-->               LOADING   (tunes in again: what is live now, not where it paused)
 * PLAYING   --pause menu over 3 s-->  LOADING   (same, when the game unpauses)
 * </pre>
 *
 * <p>Game thread only. What happens elsewhere (lavaplayer opening a track) comes back through
 * {@code client.execute(...)} before touching anything here, so the fields need no locks or volatile.
 */
public final class MusicPlayer {
	public static final MusicPlayer INSTANCE = new MusicPlayer();

	public enum State {
		STOPPED,
		LOADING,
		PLAYING,
		PAUSED,
		ERROR
	}

	/** Told after every change of state, on the game thread. The HUD needs no listener: it reads the state every frame. */
	@FunctionalInterface
	public interface Listener {
		void onChange(MusicPlayer music);
	}

	// The arrows of the diagram above. The public methods ignore requests that make no sense (pausing
	// while stopped), so asking for any other arrow is a bug in this class, and it fails loudly.
	private static final Map<State, Set<State>> ALLOWED = Map.of(
			State.STOPPED, EnumSet.of(State.LOADING),
			State.LOADING, EnumSet.of(State.LOADING, State.PLAYING, State.ERROR, State.STOPPED),
			State.PLAYING, EnumSet.of(State.LOADING, State.PAUSED, State.STOPPED),
			State.PAUSED, EnumSet.of(State.LOADING, State.PLAYING, State.STOPPED),
			State.ERROR, EnumSet.of(State.LOADING, State.STOPPED));

	// Reconnecting a dropped radio: attempt n waits 2^(n-1) seconds first, so a station that is down
	// is asked five times in about half a minute and then left alone.
	public static final int MAX_RECONNECTS = 5;
	private static final long FIRST_RECONNECT_WAIT_MS = 1_000;
	// A station that plays this long counts as working again, and gets all its attempts back.
	private static final long STABLE_MS = 60_000;
	// A radio left behind by the pause menu for longer than this tunes in again on unpause; a quick
	// look at the menu just carries on, a few seconds late, instead of costing a reconnect.
	private static final long LIVE_RESYNC_MS = 3_000;

	private final List<Listener> listeners = new ArrayList<>();

	private State state = State.STOPPED;
	private TrackInfo track;
	private Component error;

	// What is in the sound engine right now: set while PLAYING and PAUSED.
	private MusicSoundInstance sound;
	private PcmAudioStream stream;

	// Bumped by every load and stop: a track that finishes loading with an older number is no longer wanted.
	private int loadId;

	// Which reconnect attempt is waiting or loading (0 = not reconnecting), and when the next one starts
	// (System.nanoTime(), or 0 when none is waiting).
	private int reconnectAttempt;
	private long reconnectAtNanos;
	// True once the current radio has played: from then on a failed load is retried instead of reported.
	private boolean livePlayed;

	// When singleplayer's pause menu paused a playing radio (System.nanoTime()), or 0.
	private long gamePausedNanos;

	// lavaplayer decodes seconds ahead of what is heard, so we time what is heard ourselves.
	private long positionMs;        // where the clock stood when it last stopped
	private long clockStartNanos;   // when it last started running, or 0 while it is stopped
	private boolean clockEverRan;   // false until the current stream is first heard

	private MusicPlayer() {
	}

	public State state() {
		return state;
	}

	/** The current track, or null when stopped. */
	public TrackInfo track() {
		return track;
	}

	/** Why the last track failed, or null unless in {@link State#ERROR}. */
	public Component error() {
		return error;
	}

	/** How far into the track the player is, as heard (not as decoded). */
	public long positionMs() {
		if (clockStartNanos == 0) {
			return positionMs;
		}
		return positionMs + (System.nanoTime() - clockStartNanos) / 1_000_000;
	}

	/** While a dropped radio is being reconnected (state LOADING), which attempt it is (1 to {@link #MAX_RECONNECTS}); otherwise 0. */
	public int reconnectAttempt() {
		return reconnectAttempt;
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void play(TrackInfo track) {
		play(track, 0);
	}

	/** Plays {@code track} from {@code positionMs}, replacing whatever was playing or loading. */
	public void play(TrackInfo track, long positionMs) {
		reconnectAttempt = 0;
		livePlayed = false;
		load(track, positionMs, "play");
	}

	/** The pause key: pauses what is playing, resumes what is paused, and ignores anything else. */
	public void togglePause() {
		switch (state) {
			case PLAYING -> pause();
			case PAUSED -> resume();
			default -> {
				// Nothing to pause while stopped, loading or failed.
			}
		}
	}

	/**
	 * Pauses our OpenAL channel in place, like the pause menu does (decision D6). Stopping the sound and
	 * opening the track again on resume was tried too: lavaplayer's seek in VBR MP3s lands seconds early.
	 *
	 * <p>A radio is the exception: paused in place it would resume minutes behind, so its sound and
	 * connection stop, and resuming tunes in again.
	 */
	public void pause() {
		if (state != State.PLAYING) {
			return;
		}
		stopClock();
		if (track.isLive()) {
			stopSound();
		} else {
			setChannelPaused(true);
		}
		setState(State.PAUSED, "at " + positionMs + " ms");
	}

	public void resume() {
		if (state != State.PAUSED) {
			return;
		}
		if (track.isLive()) {
			reconnectAttempt = 0;
			load(track, 0, "resume live");
			return;
		}
		// The sound is still in the engine, only silent. The clock restarts on the next tick.
		setChannelPaused(false);
		setState(State.PLAYING, "resume at " + positionMs + " ms");
	}

	/** Called by {@code SoundEngineMixin} right after the game resumed every channel: ours stays paused. */
	public void onEngineResumed() {
		if (state == State.PAUSED && sound != null) {
			setChannelPaused(true);
		}
	}

	public void stop() {
		stop("stop");
	}

	/** Stops everything and forgets the track. Safe in any state and any number of times. */
	public void stop(String reason) {
		if (state == State.STOPPED) {
			return;
		}
		loadId++; // a track still loading is thrown away when it arrives
		stopSound();
		track = null;
		error = null;
		reconnectAttempt = 0;
		reconnectAtNanos = 0;
		livePlayed = false;
		resetClock(0);
		setState(State.STOPPED, reason);
	}

	/** Every client tick: notices sounds that ended without us and keeps the clock in step with the game. */
	public void tick(Minecraft client) {
		if (sound != null && !client.getSoundManager().isActive(sound)) {
			// The engine dropped our sound: the track ran out, /stopsound, or the world was left.
			boolean ended = stream.hasEnded();
			sound = null;
			stream = null;
			if (ended && track.isLive()) {
				// A radio never runs out: its stream dropped (lavaplayer logged why).
				reconnectOrFail(Component.translatable("message.unlimusic.load_failed.connection_lost"));
			} else {
				stop(ended ? "track ended" : "sound stopped by the game");
			}
			return;
		}

		if (reconnectAtNanos != 0 && System.nanoTime() - reconnectAtNanos >= 0) {
			reconnectAtNanos = 0;
			load(track, 0, "reconnect " + reconnectAttempt + "/" + MAX_RECONNECTS);
			return;
		}
		if (reconnectAttempt > 0 && state == State.PLAYING && positionMs() >= STABLE_MS) {
			UnliMusic.LOGGER.info("Music: {} is stable again", track.title());
			reconnectAttempt = 0;
		}

		// The pause menu pauses every channel in place, ours too: right for a file, stale for a radio.
		boolean liveAndPlaying = state == State.PLAYING && track.isLive();
		if (client.isPaused() && liveAndPlaying && gamePausedNanos == 0) {
			gamePausedNanos = System.nanoTime();
		} else if (!client.isPaused() && gamePausedNanos != 0) {
			long pausedMs = (System.nanoTime() - gamePausedNanos) / 1_000_000;
			gamePausedNanos = 0;
			if (liveAndPlaying && pausedMs > LIVE_RESYNC_MS) {
				reconnectAttempt = 0;
				load(track, 0, "back to live after " + pausedMs + " ms in the pause menu");
				return;
			}
		}

		// Singleplayer's pause menu pauses our channel too, so the clock stops with it.
		boolean audible = state == State.PLAYING && stream != null && stream.startedNanos() != 0 && !client.isPaused();
		if (audible && clockStartNanos == 0) {
			// The first time, count from the moment the engine took the first audio; after any pause, from now.
			clockStartNanos = clockEverRan ? System.nanoTime() : stream.startedNanos();
			clockEverRan = true;
		} else if (!audible && clockStartNanos != 0) {
			stopClock();
		}
	}

	private void load(TrackInfo track, long startMs, String reason) {
		MusicSource source = switch (track.source()) {
			case LOCAL -> LocalFileSource.INSTANCE;
			case RADIO -> RadioSource.INSTANCE;
			case JAMENDO -> throw new UnsupportedOperationException(track.source() + " is not playable yet (E07)");
		};

		// Whatever was playing stops now, so two tracks can never overlap.
		stopSound();
		this.track = track;
		error = null;
		reconnectAtNanos = 0;
		resetClock(startMs);
		int id = ++loadId;
		setState(State.LOADING, reason + " " + track.title() + " at " + startMs + " ms");

		Minecraft client = Minecraft.getInstance();
		source.open(track, startMs,
				(stream, info) -> client.execute(() -> onLoaded(id, stream, info)),
				failure -> client.execute(() -> onLoadFailed(id, failure)));
	}

	private void onLoaded(int id, PcmAudioStream stream, TrackInfo info) {
		if (id != loadId) {
			// Another play() or a stop() came while this was loading: nobody wants it any more.
			stream.close();
			return;
		}

		MusicSoundInstance sound = new MusicSoundInstance(stream);
		SoundEngine.PlayResult result = Minecraft.getInstance().getSoundManager().play(sound);
		if (result == SoundEngine.PlayResult.NOT_STARTED) {
			// The engine never took the stream, so closing it is still our job.
			stream.close();
			fail(Component.translatable("message.unlimusic.load_failed.no_channel"));
			return;
		}

		track = info;
		this.sound = sound;
		this.stream = stream;
		livePlayed = info.isLive();
		setState(State.PLAYING, "loaded " + info.title() + " (" + result + ")");
	}

	private void onLoadFailed(int id, Component reason) {
		if (id != loadId) {
			return;
		}
		// Only a station that already played gets retried (after a drop, a pause or the pause menu): one that
		// never worked (a wrong address, no internet from the start) is reported at once.
		if (livePlayed) {
			reconnectOrFail(reason);
		} else {
			fail(reason);
		}
	}

	/** Waits before the next reconnect attempt, or gives up with {@code reason} after the last one. */
	private void reconnectOrFail(Component reason) {
		if (reconnectAttempt >= MAX_RECONNECTS) {
			UnliMusic.LOGGER.info("Music: giving up on {} after {} attempts ({})", track.title(), MAX_RECONNECTS, reason.getString());
			reconnectAttempt = 0;
			fail(Component.translatable("message.unlimusic.load_failed.connection_lost"));
			return;
		}
		reconnectAttempt++;
		long waitMs = FIRST_RECONNECT_WAIT_MS << (reconnectAttempt - 1);
		reconnectAtNanos = System.nanoTime() + waitMs * 1_000_000;
		resetClock(0);
		setState(State.LOADING, "reconnect " + reconnectAttempt + "/" + MAX_RECONNECTS + " in " + waitMs + " ms: "
				+ reason.getString());
	}

	private void fail(Component reason) {
		error = reason;
		setState(State.ERROR, reason.getString());
	}

	private void stopSound() {
		if (sound != null) {
			// The engine closes our stream when it stops the sound, and closing it frees the lavaplayer player.
			Minecraft.getInstance().getSoundManager().stop(sound);
			sound = null;
			stream = null;
		}
	}

	private void resetClock(long startMs) {
		positionMs = startMs;
		clockStartNanos = 0;
		clockEverRan = false;
	}

	private void stopClock() {
		positionMs = positionMs();
		clockStartNanos = 0;
	}

	private void setState(State next, String reason) {
		if (!ALLOWED.get(state).contains(next)) {
			throw new IllegalStateException("Music player cannot go from " + state + " to " + next + " (" + reason + ")");
		}
		UnliMusic.LOGGER.info("Music: {} -> {} ({})", state, next, reason);
		state = next;
		for (Listener listener : listeners) {
			listener.onChange(this);
		}
	}

	/** Pauses or unpauses our OpenAL channel. The engine keeps channels private, so the accessor mixins reach them. */
	private void setChannelPaused(boolean paused) {
		SoundEngine engine = ((SoundManagerAccessor) Minecraft.getInstance().getSoundManager()).unlimusic$soundEngine();
		Map<SoundInstance, ChannelAccess.ChannelHandle> channels = ((SoundEngineAccessor) engine).unlimusic$channels();
		ChannelAccess.ChannelHandle handle = channels.get(sound);
		if (handle != null) {
			// Runs on the sound engine's thread, like everything that touches OpenAL.
			// Channel.pause() only acts on a playing channel and unpause() on a paused one.
			handle.execute(paused ? Channel::pause : Channel::unpause);
		}
	}
}
