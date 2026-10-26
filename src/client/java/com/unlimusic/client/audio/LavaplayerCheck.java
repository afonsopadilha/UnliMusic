package com.unlimusic.client.audio;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.natives.ConnectorNativeLibLoader;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.unlimusic.UnliMusic;

/**
 * Startup check (E01): proves that lavaplayer, the libraries it needs and its native
 * decoders all load from inside the mod jar. The playback engine (E02/E03) replaces it.
 */
public final class LavaplayerCheck {
	private LavaplayerCheck() {
	}

	public static void runInBackground() {
		// Unpacking the natives touches the disk, so keep it off the game thread.
		Thread thread = new Thread(LavaplayerCheck::run, "UnliMusic lavaplayer check");
		thread.setDaemon(true);
		thread.start();
	}

	private static void run() {
		AudioPlayerManager manager = null;
		try {
			manager = new DefaultAudioPlayerManager();
			// Only local files and plain HTTP (radio streams, Jamendo MP3s).
			// Never AudioSourceManagers.registerRemoteSources: that adds YouTube and friends.
			manager.registerSourceManager(new LocalAudioSourceManager());
			manager.registerSourceManager(new HttpAudioSourceManager());
			manager.getConfiguration().setOutputFormat(StandardAudioDataFormats.COMMON_PCM_S16_LE);

			// MP3, AAC, Vorbis and resampling live in a native library that is normally loaded
			// on the first track. Loading it now makes an unsupported system fail at startup.
			ConnectorNativeLibLoader.loadConnectorLibrary();

			UnliMusic.LOGGER.info("lavaplayer ready ({} sources, natives for {} {})",
					manager.getSourceManagers().size(),
					System.getProperty("os.name"), System.getProperty("os.arch"));
		} catch (Exception | LinkageError e) {
			// LinkageError covers NoClassDefFoundError (library missing from the jar)
			// and UnsatisfiedLinkError (no native library for this system).
			UnliMusic.LOGGER.error("lavaplayer failed to start", e);
		} finally {
			if (manager != null) {
				manager.shutdown();
			}
		}
	}
}
