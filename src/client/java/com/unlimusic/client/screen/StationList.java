package com.unlimusic.client.screen;

import com.unlimusic.client.audio.MusicPlayer;
import com.unlimusic.music.TrackInfo;
import com.unlimusic.radio.RadioStation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;

/**
 * The scrolling list of stations. A click selects a station, a double click or Enter plays it. While there are
 * no rows (searching, nothing found, an error) it shows a message in the middle instead.
 *
 * <p>Only the rows in view are drawn each frame (the game's list skips the rest), so 100 stations cost the
 * same as ten.
 */
final class StationList extends ObjectSelectionList<StationList.Entry> {
	// Two lines of text plus the padding the game's list puts around each row's content.
	private static final int ROW_HEIGHT = 2 * 9 + 2 + 4;
	private static final int MAX_ROW_WIDTH = 360;
	// Room on the right of the rows for the scroll bar.
	private static final int SIDE_MARGIN = 16;
	private static final int TEXT_INSET = 2;
	private static final int LINE_GAP = 2;
	private static final int FORMAT_GAP = 8;
	// Tags after the first few say little and would not fit anyway.
	private static final int MAX_TAGS = 4;

	private static final int NAME_COLOR = 0xFFFFFFFF;
	private static final int PLAYING_COLOR = 0xFF55FF55;
	private static final int DETAIL_COLOR = 0xFFA0A0A0;
	private static final int MESSAGE_COLOR = 0xFFFFFFFF;
	private static final int ERROR_COLOR = 0xFFFF5555;

	private final Consumer<RadioStation> play;

	// Shown in the middle while the list is empty; null shows nothing.
	private Component message;
	private int messageColor = MESSAGE_COLOR;
	private boolean loading;

	StationList(Minecraft minecraft, Consumer<RadioStation> play) {
		// The size and position are set by the tab's doLayout, once the screen's size is known.
		super(minecraft, 0, 0, 0, ROW_HEIGHT);
		this.play = play;
	}

	void showStations(List<RadioStation> stations) {
		List<Entry> entries = new ArrayList<>(stations.size());
		for (RadioStation station : stations) {
			entries.add(new Entry(station));
		}
		replaceEntries(entries);
		setScrollAmount(0);
		message = null;
		loading = false;
	}

	void showMessage(Component message) {
		show(message, MESSAGE_COLOR, false);
	}

	/** The message in red. */
	void showError(Component message) {
		show(message, ERROR_COLOR, false);
	}

	/** The message with animated dots under it. */
	void showLoading(Component message) {
		show(message, MESSAGE_COLOR, true);
	}

	private void show(Component message, int color, boolean loading) {
		clearEntries();
		setScrollAmount(0);
		this.message = message;
		this.messageColor = color;
		this.loading = loading;
	}

	/** The selected station, or null. */
	RadioStation selectedStation() {
		Entry entry = getSelected();
		return entry != null ? entry.station : null;
	}

	@Override
	public int getRowWidth() {
		return Math.min(MAX_ROW_WIDTH, getWidth() - 2 * SIDE_MARGIN);
	}

	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
		if (!children().isEmpty() || message == null) {
			return;
		}

		// Error messages can be longer than the list is wide: split them into lines and centre each one.
		Font font = minecraft.font;
		List<FormattedCharSequence> lines = font.split(message, getRowWidth());
		int centerX = getX() + getWidth() / 2;
		int y = getY() + getHeight() / 3;
		for (FormattedCharSequence line : lines) {
			graphics.centeredText(font, line, centerX, y, messageColor);
			y += font.lineHeight + LINE_GAP;
		}
		if (loading) {
			graphics.centeredText(font, LoadingDotsText.get(Util.getMillis()), centerX, y, DETAIL_COLOR);
		}
	}

	/** Is this station what the player is listening to (or tuning in to) right now? */
	private static boolean isPlaying(RadioStation station) {
		MusicPlayer music = MusicPlayer.INSTANCE;
		TrackInfo track = music.track();
		return track != null && track.source() == TrackInfo.Source.RADIO && track.location().equals(station.url())
				&& music.state() != MusicPlayer.State.ERROR;
	}

	/** "MP3 128 kbps", or only the codec when the bit rate is unknown. */
	private static String format(RadioStation station) {
		return station.bitrate() > 0 ? station.codec() + " " + station.bitrate() + " kbps" : station.codec();
	}

	/** Cuts {@code text} to {@code width}, ending in "…" when something was cut. */
	private static String fit(Font font, String text, int width) {
		if (font.width(text) <= width) {
			return text;
		}
		String ellipsis = "…";
		return font.plainSubstrByWidth(text, Math.max(0, width - font.width(ellipsis))).strip() + ellipsis;
	}

	final class Entry extends ObjectSelectionList.Entry<Entry> {
		private final RadioStation station;
		// "Portugal · pop, rock, news": built once, since the country name has to be looked up.
		private final String details;

		private Entry(RadioStation station) {
			this.station = station;
			List<String> parts = new ArrayList<>();
			if (station.countryCode() != null) {
				parts.add(Countries.name(station.countryCode()));
			}
			if (!station.tags().isEmpty()) {
				parts.add(String.join(", ", station.tags().subList(0, Math.min(MAX_TAGS, station.tags().size()))));
			}
			this.details = String.join(" · ", parts);
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
			Font font = minecraft.font;
			int left = getContentX() + TEXT_INSET;
			int right = getContentRight() - TEXT_INSET;
			int top = getContentY() + 1;

			String format = format(station);
			int formatWidth = font.width(format);
			graphics.text(font, format, right - formatWidth, top, DETAIL_COLOR);

			boolean playing = isPlaying(station);
			String name = (playing ? "▶ " : "") + station.name();
			graphics.text(font, fit(font, name, right - left - formatWidth - FORMAT_GAP), left, top,
					playing ? PLAYING_COLOR : NAME_COLOR);
			graphics.text(font, fit(font, details, right - left), left, top + font.lineHeight + LINE_GAP, DETAIL_COLOR);
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
			if (doubleClick) {
				play.accept(station);
			}
			// True also on a single click: that is what makes the list select this row.
			return true;
		}

		@Override
		public boolean keyPressed(KeyEvent event) {
			if (event.isSelection()) {
				play.accept(station);
				return true;
			}
			return false;
		}

		@Override
		public Component getNarration() {
			return Component.translatable("narrator.select", station.name());
		}
	}
}
