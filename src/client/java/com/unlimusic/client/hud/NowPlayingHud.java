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
import net.minecraft.util.Util;

/**
 * The "now playing" box in the corner of the screen. Display-only (decision D14): nothing on it can be clicked.
 *
 * <p>Runs every frame, so it only reads state and queues what to draw; the game draws it later. It runs on the
 * same thread as {@link MusicPlayer}, so reading the player needs no locks.
 */
public final class NowPlayingHud implements HudElement {
	private static final int MARGIN = 4;
	private static final int PADDING = 3;
	private static final int LINE_GAP = 1;
	// Width of the content inside the box: long lines scroll instead of making the box wider.
	private static final int WIDTH = 140;
	private static final int BAR_HEIGHT = 2;
	// The pause icon: two bars, drawn with rectangles so they stay sharp at every GUI scale.
	private static final int PAUSE_BAR_WIDTH = 2;
	private static final int PAUSE_GAP = 2;
	private static final int PAUSE_HEIGHT = 7;
	private static final int PAUSE_WIDTH = 2 * PAUSE_BAR_WIDTH + PAUSE_GAP;

	// Scrolling: wait at each end, then slide at this speed (GUI pixels per second).
	private static final double SCROLL_SPEED = 20;
	private static final double SCROLL_HOLD_SECONDS = 2;

	// Colours are ARGB: text with an alpha of 0 is silently skipped, so white must be 0xFFFFFFFF, not 0xFFFFFF.
	private static final int BACKGROUND = 0x80000000;
	private static final int TITLE_COLOR = 0xFFFFFFFF;
	private static final int DETAIL_COLOR = 0xFFAAAAAA;
	private static final int BAR_BACKGROUND = 0xFF555555;
	private static final int BAR_FILL = 0xFFFFFFFF;
	private static final int LIVE_COLOR = 0xFFFF5555;
	// Safe to build once: the translation is looked up when drawn, so it follows a change of language.
	private static final Component LOADING = Component.translatable("hud.unlimusic.loading");
	private static final Component LIVE = Component.translatable("hud.unlimusic.live");

	// The track on screen, and since when: every line starts scrolling from its beginning when it changes.
	private TrackInfo shownTrack;
	private long shownSinceMillis;

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
		MusicPlayer.State state = music.state();
		// Stopped there is nothing to say, and a failure is already in the chat, where it can be read.
		if (state == MusicPlayer.State.STOPPED || state == MusicPlayer.State.ERROR) {
			shownTrack = null;
			return;
		}

		Font font = Minecraft.getInstance().font;
		TrackInfo track = music.track();
		if (!track.equals(shownTrack)) {
			shownTrack = track;
			shownSinceMillis = Util.getMillis();
		}
		Component detail = detailLine(music, track);
		Component credit = track.credit() != null ? Component.literal(track.credit()) : null;
		int progressHeight = state == MusicPlayer.State.LOADING ? 0 : progressHeight(font, track);

		// Measure first: a later element that overlaps an earlier one is drawn above it, so the background
		// has to be queued before the text.
		int lineStep = font.lineHeight + LINE_GAP;
		int lines = 1 + (detail != null ? 1 : 0) + (credit != null ? 1 : 0);
		int height = lines * lineStep + (progressHeight > 0 ? LINE_GAP + progressHeight : -LINE_GAP);
		graphics.fill(MARGIN, MARGIN, MARGIN + WIDTH + 2 * PADDING, MARGIN + height + 2 * PADDING, BACKGROUND);

		int x = MARGIN + PADDING;
		int y = MARGIN + PADDING;
		int titleWidth = WIDTH;
		if (state == MusicPlayer.State.PAUSED) {
			titleWidth -= PAUSE_WIDTH + PADDING;
			extractPauseIcon(graphics, x + WIDTH - PAUSE_WIDTH, y);
		}
		scrollingText(graphics, font, Component.literal(track.title()), x, y, titleWidth, TITLE_COLOR);
		y += lineStep;

		if (detail != null) {
			scrollingText(graphics, font, detail, x, y, WIDTH, DETAIL_COLOR);
			y += lineStep;
		}
		// The license and "via Jamendo" that Jamendo's terms ask for (E07); null for every other source.
		if (credit != null) {
			scrollingText(graphics, font, credit, x, y, WIDTH, DETAIL_COLOR);
			y += lineStep;
		}

		if (progressHeight > 0) {
			extractProgress(graphics, font, music, track, x, y + LINE_GAP);
		}
	}

	/** While loading, what is going on; otherwise the artist, or null when unknown. */
	private static Component detailLine(MusicPlayer music, TrackInfo track) {
		if (music.state() == MusicPlayer.State.LOADING) {
			return music.reconnectAttempt() > 0
					? Component.translatable("hud.unlimusic.reconnecting", music.reconnectAttempt(), MusicPlayer.MAX_RECONNECTS)
					: LOADING;
		}
		return track.artist() != null ? Component.literal(track.artist()) : null;
	}

	private static int progressHeight(Font font, TrackInfo track) {
		boolean bar = !track.isLive() && track.durationMs() > 0;
		return bar ? BAR_HEIGHT + 2 * LINE_GAP + font.lineHeight : font.lineHeight;
	}

	/** "● LIVE" for a radio; for a file, a bar with the time heard and the length (only the time, if the length is unknown). */
	private static void extractProgress(GuiGraphicsExtractor graphics, Font font, MusicPlayer music, TrackInfo track, int x, int y) {
		if (track.isLive()) {
			graphics.text(font, LIVE, x, y, LIVE_COLOR);
			return;
		}

		long positionMs = music.positionMs();
		long durationMs = track.durationMs();
		if (durationMs <= 0) {
			graphics.text(font, formatTime(positionMs), x, y, DETAIL_COLOR);
			return;
		}

		// The clock can run a little past the end before the engine notices the track is over.
		positionMs = Math.min(positionMs, durationMs);
		int filled = (int) (WIDTH * positionMs / durationMs);
		graphics.fill(x, y, x + WIDTH, y + BAR_HEIGHT, BAR_BACKGROUND);
		graphics.fill(x, y, x + filled, y + BAR_HEIGHT, BAR_FILL);

		y += BAR_HEIGHT + 2 * LINE_GAP;
		String length = formatTime(durationMs);
		graphics.text(font, formatTime(positionMs), x, y, DETAIL_COLOR);
		graphics.text(font, length, x + WIDTH - font.width(length), y, DETAIL_COLOR);
	}

	private static void extractPauseIcon(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.fill(x, y, x + PAUSE_BAR_WIDTH, y + PAUSE_HEIGHT, TITLE_COLOR);
		int secondBar = x + PAUSE_BAR_WIDTH + PAUSE_GAP;
		graphics.fill(secondBar, y, secondBar + PAUSE_BAR_WIDTH, y + PAUSE_HEIGHT, TITLE_COLOR);
	}

	/**
	 * Draws {@code text} cut to {@code width}. If it is too long it waits, slides to its end, waits and slides back.
	 *
	 * <p>The game's scrolling button labels move a whole GUI pixel at a time, which is several screen pixels at a
	 * GUI scale above 1 and looks jerky on something always in view. Here the offset is rounded to screen pixels instead: smooth, and the
	 * letters still land on whole pixels, so they stay sharp.
	 */
	private void scrollingText(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int width, int color) {
		int overflow = font.width(text) - width;
		if (overflow <= 0) {
			graphics.text(font, text, x, y, color);
			return;
		}

		double slideSeconds = overflow / SCROLL_SPEED;
		double cycle = 2 * (SCROLL_HOLD_SECONDS + slideSeconds);
		double t = (Util.getMillis() - shownSinceMillis) / 1000.0 % cycle;
		double offset;
		if (t < SCROLL_HOLD_SECONDS) {
			offset = 0;
		} else if (t < SCROLL_HOLD_SECONDS + slideSeconds) {
			offset = (t - SCROLL_HOLD_SECONDS) * SCROLL_SPEED;
		} else if (t < 2 * SCROLL_HOLD_SECONDS + slideSeconds) {
			offset = overflow;
		} else {
			offset = overflow - (t - 2 * SCROLL_HOLD_SECONDS - slideSeconds) * SCROLL_SPEED;
		}
		int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		float screenPixelOffset = Math.round(offset * guiScale) / (float) guiScale;

		// The scissor is set before moving, so it stays put while the text slides under it.
		graphics.enableScissor(x, y, x + width, y + font.lineHeight);
		graphics.pose().pushMatrix();
		graphics.pose().translate(-screenPixelOffset, 0);
		graphics.text(font, text, x, y, color);
		graphics.pose().popMatrix();
		graphics.disableScissor();
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
