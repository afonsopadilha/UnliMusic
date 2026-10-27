package com.unlimusic.client.hud;

import com.unlimusic.UnliMusic;
import com.unlimusic.client.audio.MusicPlayer;
import com.unlimusic.music.TrackInfo;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * The "now playing" box in the corner of the screen. Display-only (decision D14): nothing on it can be clicked.
 *
 * <p>Runs every frame, so it only reads state and queues what to draw; the game draws it later. It runs on the
 * same thread as {@link MusicPlayer}, so reading the player needs no locks.
 */
public final class NowPlayingHud implements HudElement {
	private static final int MARGIN = 4;
	private static final int LINE_GAP = 1;
	// Width of the progress bar, and of the box drawn around everything later on.
	private static final int WIDTH = 140;
	private static final int BAR_HEIGHT = 2;
	// Colours are ARGB: text with an alpha of 0 is silently skipped, so white must be 0xFFFFFFFF, not 0xFFFFFF.
	private static final int TITLE_COLOR = 0xFFFFFFFF;
	private static final int DETAIL_COLOR = 0xFFAAAAAA;
	private static final int BAR_BACKGROUND = 0xFF555555;
	private static final int BAR_FILL = 0xFFFFFFFF;
	private static final int LIVE_COLOR = 0xFFFF5555;
	// Safe to build once: the translation is looked up when drawn, so it follows a change of language.
	private static final Component LOADING = Component.translatable("hud.unlimusic.loading");
	private static final Component LIVE = Component.translatable("hud.unlimusic.live");

	private NowPlayingHud() {
	}

	public static void register() {
		// Attaching to a vanilla element inherits its "hidden with F1" condition (addLast would not).
		// Before the chat, so the chat and the player list (Tab) cover the box rather than the other way round.
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, UnliMusic.id("now_playing"), new NowPlayingHud());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		MusicPlayer music = MusicPlayer.INSTANCE;
		// Stopped there is nothing to say, and a failure is already in the chat, where it can be read.
		if (music.state() == MusicPlayer.State.STOPPED || music.state() == MusicPlayer.State.ERROR) {
			return;
		}

		Font font = Minecraft.getInstance().font;
		TrackInfo track = music.track();
		int y = MARGIN;

		graphics.text(font, track.title(), MARGIN, y, TITLE_COLOR);
		y += font.lineHeight + LINE_GAP;

		if (music.state() == MusicPlayer.State.LOADING) {
			Component status = music.reconnectAttempt() > 0
					? Component.translatable("hud.unlimusic.reconnecting", music.reconnectAttempt(), MusicPlayer.MAX_RECONNECTS)
					: LOADING;
			graphics.text(font, status, MARGIN, y, DETAIL_COLOR);
			y += font.lineHeight + LINE_GAP;
		} else if (track.artist() != null) {
			graphics.text(font, track.artist(), MARGIN, y, DETAIL_COLOR);
			y += font.lineHeight + LINE_GAP;
		}

		// The license and "via Jamendo" that Jamendo's terms ask for (E07); null for every other source.
		if (track.credit() != null) {
			graphics.text(font, track.credit(), MARGIN, y, DETAIL_COLOR);
			y += font.lineHeight + LINE_GAP;
		}

		if (music.state() != MusicPlayer.State.LOADING) {
			extractProgress(graphics, font, music, track, y + LINE_GAP);
		}
	}

	/** "● LIVE" for a radio; for a file, a bar with the time heard and the length (only the time, if the length is unknown). */
	private static void extractProgress(GuiGraphicsExtractor graphics, Font font, MusicPlayer music, TrackInfo track, int y) {
		if (track.isLive()) {
			graphics.text(font, LIVE, MARGIN, y, LIVE_COLOR);
			return;
		}

		long positionMs = music.positionMs();
		long durationMs = track.durationMs();
		if (durationMs <= 0) {
			graphics.text(font, formatTime(positionMs), MARGIN, y, DETAIL_COLOR);
			return;
		}

		// The clock can run a little past the end before the engine notices the track is over.
		positionMs = Math.min(positionMs, durationMs);
		int filled = (int) (WIDTH * positionMs / durationMs);
		graphics.fill(MARGIN, y, MARGIN + WIDTH, y + BAR_HEIGHT, BAR_BACKGROUND);
		graphics.fill(MARGIN, y, MARGIN + filled, y + BAR_HEIGHT, BAR_FILL);

		y += BAR_HEIGHT + 2 * LINE_GAP;
		String length = formatTime(durationMs);
		graphics.text(font, formatTime(positionMs), MARGIN, y, DETAIL_COLOR);
		graphics.text(font, length, MARGIN + WIDTH - font.width(length), y, DETAIL_COLOR);
	}

	/** 3:07, or 1:02:07 from an hour up. */
	private static String formatTime(long ms) {
		long seconds = ms / 1000;
		long hours = seconds / 3600;
		long minutes = seconds / 60 % 60;
		return hours > 0
				? "%d:%02d:%02d".formatted(hours, minutes, seconds % 60)
				: "%d:%02d".formatted(minutes, seconds % 60);
	}
}
