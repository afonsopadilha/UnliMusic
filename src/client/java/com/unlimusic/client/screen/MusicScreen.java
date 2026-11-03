package com.unlimusic.client.screen;

import com.unlimusic.client.audio.MusicPlayer;
import com.unlimusic.client.input.ModKeys;
import com.unlimusic.radio.RadioBrowserClient;
import com.unlimusic.radio.RadioStation;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * The mod's screen, the main way to use it (decision D14): tabs along the top (Radios for now; Jamendo, Queue and
 * Local come later), buttons along the bottom. Opened with a key (M by default, see {@link ModKeys}).
 *
 * <p>A screen's life: {@link #init()} builds the widgets when it opens, {@link #repositionElements()} moves them
 * when the window changes size, {@link #tick()} runs 20 times a second, and the game draws every widget added
 * with {@code addRenderableWidget} each frame. Closing it (ESC, Done, or the key again) drops the whole object;
 * the next opening builds a new one, which is why searches are kept in {@link RadioSearch}.
 */
public final class MusicScreen extends Screen {
	private static final Component TITLE = Component.translatable("screen.unlimusic.title");
	private static final Component PLAY = Component.translatable("screen.unlimusic.play");
	private static final Component STOP = Component.translatable("screen.unlimusic.stop");
	private static final int BUTTON_WIDTH = 100;
	private static final int BUTTON_GAP = 8;

	// The game's header/footer layout: a strip at the top (resized to the tab bar) and one at the bottom
	// for the buttons, with the tab's content in between.
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
	// Swaps the widgets of the current tab in and out of the screen when another tab is picked.
	private final TabManager tabManager = new TabManager(widget -> addRenderableWidget(widget), widget -> removeWidget(widget));

	private MenuTabBar tabBar;
	private RadiosTab radiosTab;
	private Button playButton;
	private Button stopButton;

	public MusicScreen() {
		super(TITLE);
	}

	@Override
	protected void init() {
		radiosTab = new RadiosTab(minecraft, this::play);
		tabBar = MenuTabBar.builder(tabManager, width).addTabs(radiosTab).build();
		addRenderableWidget(tabBar);

		LinearLayout buttons = layout.addToFooter(LinearLayout.horizontal().spacing(BUTTON_GAP));
		playButton = buttons.addChild(Button.builder(PLAY, button -> playSelected()).width(BUTTON_WIDTH).build());
		stopButton = buttons.addChild(Button.builder(STOP, button -> MusicPlayer.INSTANCE.stop()).width(BUTTON_WIDTH).build());
		buttons.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(BUTTON_WIDTH).build());
		layout.visitWidgets(widget -> {
			// Tab goes through the tab's widgets first and reaches the bottom buttons last.
			widget.setTabOrderGroup(1);
			addRenderableWidget(widget);
		});

		tabBar.selectTab(0, false);
		repositionElements();
		updateButtons();
	}

	/**
	 * Without this override the game rebuilds the whole screen on a resize, which would empty the search box and
	 * lose the selection; moving the widgets is enough.
	 */
	@Override
	protected void repositionElements() {
		tabBar.arrangeElements(width);
		int tabAreaTop = tabBar.getRectangle().bottom();
		layout.setHeaderHeight(tabAreaTop);
		layout.arrangeElements();
		tabManager.setTabArea(new ScreenRectangle(0, tabAreaTop, width, height - layout.getFooterHeight() - tabAreaTop));
	}

	@Override
	public void tick() {
		radiosTab.tick();
		updateButtons();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		// Ctrl+Tab and Ctrl+1, 2... switch tabs.
		if (tabBar.keyPressed(event)) {
			return true;
		}
		// The mod's keys work here too, unless the player is typing: then an M is just a letter.
		if (!isInputCaptured()) {
			if (ModKeys.OPEN.matches(event)) {
				onClose();
				return true;
			}
			if (ModKeys.PAUSE.matches(event)) {
				MusicPlayer.INSTANCE.togglePause();
				return true;
			}
			if (ModKeys.STOP.matches(event)) {
				MusicPlayer.INSTANCE.stop();
				return true;
			}
		}
		return super.keyPressed(event);
	}

	/** Keeps singleplayer running while the screen is open: the music, the world and the clock go on. */
	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/** Draws the world behind the screen, darkened, instead of the blurred menu background. */
	@Override
	public boolean isInGameUi() {
		return true;
	}

	private void playSelected() {
		RadioStation station = radiosTab.selectedStation();
		if (station != null) {
			play(station);
		}
	}

	private void play(RadioStation station) {
		MusicPlayer.INSTANCE.play(station.toTrack());
		RadioBrowserClient.INSTANCE.countClick(station);
	}

	private void updateButtons() {
		playButton.active = radiosTab.selectedStation() != null;
		stopButton.active = MusicPlayer.INSTANCE.state() != MusicPlayer.State.STOPPED;
	}
}
