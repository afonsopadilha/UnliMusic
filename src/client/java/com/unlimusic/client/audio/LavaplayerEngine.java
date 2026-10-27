package com.unlimusic.client.audio;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.natives.ConnectorNativeLibLoader;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.unlimusic.UnliMusic;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

/**
 * The client's lavaplayer: opens audio files and decodes them into PCM for {@link PcmAudioStream}.
 *
 * <p>Everything here runs on lavaplayer's own threads; the game thread only asks and gets called back.
 */
public final class LavaplayerEngine {
	/**
	 * 16-bit signed little-endian PCM, stereo, 44.1 kHz, in 20 ms frames (960 samples per channel).
	 * 44.1 kHz is what almost every MP3, Jamendo track and radio stream already uses, so lavaplayer
	 * rarely has to resample; OpenAL accepts any rate.
	 */
	public static final AudioDataFormat FORMAT = StandardAudioDataFormats.COMMON_PCM_S16_LE;

	// Completed by the startup thread; requests made before that simply wait for it.
	private static final CompletableFuture<AudioPlayerManager> MANAGER = new CompletableFuture<>();

	private LavaplayerEngine() {
	}

	public static void startInBackground() {
		// Unpacking the natives touches the disk, so keep it off the game thread.
		Thread thread = new Thread(LavaplayerEngine::start, "UnliMusic lavaplayer startup");
		thread.setDaemon(true);
		thread.start();
	}

	private static void start() {
		try {
			AudioPlayerManager manager = new DefaultAudioPlayerManager();
			// Only local files for now; plain HTTP (radio, Jamendo) comes in E04.
			// Never AudioSourceManagers.registerRemoteSources: that adds YouTube and friends.
			manager.registerSourceManager(new LocalAudioSourceManager());
			manager.getConfiguration().setOutputFormat(FORMAT);

			// MP3, AAC, Vorbis and resampling live in a native library that is normally loaded
			// on the first track. Loading it now makes an unsupported system fail at startup.
			ConnectorNativeLibLoader.loadConnectorLibrary();

			UnliMusic.LOGGER.info("lavaplayer ready (natives for {} {})",
					System.getProperty("os.name"), System.getProperty("os.arch"));
			MANAGER.complete(manager);
		} catch (Exception | LinkageError e) {
			// LinkageError covers NoClassDefFoundError (library missing from the jar)
			// and UnsatisfiedLinkError (no native library for this system).
			UnliMusic.LOGGER.error("lavaplayer failed to start", e);
			MANAGER.completeExceptionally(e);
		}
	}

	/**
	 * Opens {@code identifier} (a file path for now) and starts decoding it {@code positionMs} milliseconds in.
	 * Exactly one of the callbacks runs, on a lavaplayer thread: {@code onReady} with a stream for the sound
	 * engine and what lavaplayer read about the track (tags, length), or {@code onError} with the reason.
	 */
	public static void load(String identifier, long positionMs, BiConsumer<PcmAudioStream, AudioTrackInfo> onReady,
			Consumer<Component> onError) {
		MANAGER.whenComplete((manager, startError) -> {
			if (startError != null) {
				onError.accept(Component.translatable("message.unlimusic.load_failed.engine"));
				return;
			}

			// loadItem returns at once: reading the file's header happens on lavaplayer's loader threads.
			manager.loadItem(identifier, new AudioLoadResultHandler() {
				@Override
				public void trackLoaded(AudioTrack track) {
					// A position set before the track starts is kept and applied when decoding begins,
					// so the first frame is already at the right place (used when resuming after a pause).
					if (positionMs > 0) {
						track.setPosition(positionMs);
					}
					// Starting the track makes lavaplayer decode ahead into its frame buffer,
					// so the first read() from the sound engine finds audio waiting.
					AudioPlayer player = manager.createPlayer();
					player.startTrack(track, false);
					onReady.accept(new PcmAudioStream(player), track.getInfo());
				}

				@Override
				public void playlistLoaded(AudioPlaylist playlist) {
					onError.accept(Component.translatable("message.unlimusic.load_failed.playlist"));
				}

				@Override
				public void noMatches() {
					onError.accept(Component.translatable("message.unlimusic.load_failed.not_found"));
				}

				@Override
				public void loadFailed(FriendlyException exception) {
					UnliMusic.LOGGER.warn("Could not load {}", identifier, exception);
					onError.accept(Component.literal(exception.getMessage()));
				}
			});
		});
	}

	/** Stops lavaplayer's threads when the game closes. */
	public static void shutdown() {
		MANAGER.thenAccept(AudioPlayerManager::shutdown);
	}
}
