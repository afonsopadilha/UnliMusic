package com.unlimusic.client.audio;

import com.sedmelluq.discord.lavaplayer.container.MediaContainer;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerProbe;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry;
import com.sedmelluq.discord.lavaplayer.container.adts.AdtsContainerProbe;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.natives.ConnectorNativeLibLoader;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.TrackExceptionEvent;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.unlimusic.UnliMusic;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import org.apache.http.client.config.RequestConfig;

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

	// lavaplayer's defaults are 3 s for both, too short for some stations: RFM's servers take over 6 s to
	// send the first byte. Once connected, a stream that sends nothing for 10 s counts as dropped.
	private static final int CONNECT_TIMEOUT_MS = 10_000;
	private static final int READ_TIMEOUT_MS = 10_000;

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
			// Local files, and plain HTTP for radio streams (and Jamendo later).
			// Never AudioSourceManagers.registerRemoteSources: that adds YouTube and friends.
			manager.registerSourceManager(new LocalAudioSourceManager());
			manager.registerSourceManager(createHttpSource());
			manager.getConfiguration().setOutputFormat(FORMAT);
			// lavaplayer stops a player that nobody asked for audio for a minute, which is exactly what a
			// paused channel looks like. We always destroy our players ourselves, so turn that off.
			manager.setPlayerCleanupThreshold(TimeUnit.DAYS.toMillis(365));

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

	/** Plain HTTP(S) URLs: radio streams, and .pls/.m3u playlists that point to one. */
	private static HttpAudioSourceManager createHttpSource() {
		// lavaplayer guesses the format with the server's Content-Type first, then by scanning the first bytes
		// with every format in turn, MP3 before AAC. Its AAC probe only claims "audio/aac", so HE-AAC stations
		// ("audio/aacp") fall to the scan, which joins the stream mid-frame and sometimes finds a fake MP3 header.
		List<MediaContainerProbe> probes = MediaContainer.asList().stream()
				.map(probe -> probe instanceof AdtsContainerProbe ? new AacPlusProbe() : probe)
				.toList();
		HttpAudioSourceManager http = new HttpAudioSourceManager(new MediaContainerRegistry(probes));
		// Stations see the same name in their logs as Radio Browser does.
		http.configureBuilder(builder -> builder.setUserAgent(UnliMusic.USER_AGENT));
		http.configureRequests(config -> RequestConfig.copy(config)
				.setConnectTimeout(CONNECT_TIMEOUT_MS)
				.setSocketTimeout(READ_TIMEOUT_MS)
				.build());
		return http;
	}

	/**
	 * Opens {@code identifier} (a file path or an http(s) URL) and starts decoding it {@code positionMs}
	 * milliseconds in. Exactly one of the callbacks runs, on a lavaplayer thread: {@code onReady} with a stream
	 * for the sound engine and what lavaplayer read about the track (tags, length), or {@code onError} with the
	 * reason. A {@code live} stream never makes the sound engine wait (see {@link PcmAudioStream}).
	 */
	public static void load(String identifier, long positionMs, boolean live,
			BiConsumer<PcmAudioStream, AudioTrackInfo> onReady, Consumer<Component> onError) {
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
					// A track that fails while playing (a dropped stream, a broken file) just ends;
					// this is the only place that says why.
					player.addListener(event -> {
						if (event instanceof TrackExceptionEvent failure) {
							UnliMusic.LOGGER.warn("Playback of {} failed", identifier, failure.exception);
						}
					});
					player.startTrack(track, false);
					onReady.accept(new PcmAudioStream(player, live), track.getInfo());
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

	/** lavaplayer's AAC (ADTS) probe, also claiming the Content-Types stations use for HE-AAC. */
	private static final class AacPlusProbe extends AdtsContainerProbe {
		@Override
		public boolean matchesHints(MediaContainerHints hints) {
			return super.matchesHints(hints)
					|| "audio/aacp".equalsIgnoreCase(hints.mimeType) || "audio/x-aac".equalsIgnoreCase(hints.mimeType);
		}
	}

	/** Stops lavaplayer's threads when the game closes. */
	public static void shutdown() {
		MANAGER.thenAccept(AudioPlayerManager::shutdown);
	}
}
