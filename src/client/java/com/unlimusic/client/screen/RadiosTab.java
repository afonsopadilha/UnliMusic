package com.unlimusic.client.screen;

import com.unlimusic.radio.RadioBrowserClient;
import com.unlimusic.radio.RadioBrowserClient.SearchBy;
import com.unlimusic.radio.RadioStation;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * The Radios tab: a row to search Radio Browser (by country, name or genre) above the list of stations.
 *
 * <p>Built again every time the screen opens; what was searched lives in {@link RadioSearch}, which this tab
 * reads on every tick and shows when it changes.
 */
final class RadiosTab implements Tab {
	private static final Component TITLE = Component.translatable("screen.unlimusic.tab.radios");
	private static final Component SEARCH_BY = Component.translatable("screen.unlimusic.radios.search_by");
	private static final Component SEARCH = Component.translatable("screen.unlimusic.radios.search");

	private static final int MAX_WIDTH = 360;
	private static final int MARGIN = 6;
	private static final int GAP = 4;
	private static final int SEARCH_BY_WIDTH = 124;
	private static final int SEARCH_BUTTON_WIDTH = 70;
	private static final int ROW_HEIGHT = 20;
	// Long enough for any station name or genre worth typing.
	private static final int MAX_TEXT_LENGTH = 64;

	private final RadioSearch search = RadioSearch.INSTANCE;
	private final CycleButton<SearchBy> searchBy;
	private final EditBox searchBox;
	private final StationList list;
	private final LinearLayout searchRow = LinearLayout.horizontal().spacing(GAP);
	private final LinearLayout content = LinearLayout.vertical().spacing(MARGIN);

	// The RadioSearch version on show; -1 so the first tick always fills the list.
	private int shownVersion = -1;

	RadiosTab(Minecraft minecraft, Consumer<RadioStation> play) {
		searchBy = CycleButton.builder(RadiosTab::label, search.by())
				.withValues(SearchBy.values())
				.create(0, 0, SEARCH_BY_WIDTH, ROW_HEIGHT, SEARCH_BY, (button, by) -> onSearchByChanged(by));

		searchBox = new EditBox(minecraft.font, 0, ROW_HEIGHT, SEARCH) {
			@Override
			public boolean keyPressed(KeyEvent event) {
				// Enter in the box searches, as in any search field.
				if (event.isConfirmation()) {
					runSearch();
					return true;
				}
				return super.keyPressed(event);
			}
		};
		searchBox.setMaxLength(MAX_TEXT_LENGTH);
		searchBox.setValue(search.text());
		searchBox.setHint(hint(search.by()));
		searchBox.setResponder(this::onTextChanged);

		Button searchButton = Button.builder(SEARCH, button -> runSearch()).size(SEARCH_BUTTON_WIDTH, ROW_HEIGHT).build();
		list = new StationList(minecraft, play);

		searchRow.addChild(searchBy);
		searchRow.addChild(searchBox);
		searchRow.addChild(searchButton);
		content.addChild(searchRow);
		content.addChild(list);

		// The first time the screen opens in a game, show something straight away: the player's own country.
		if (search.status() == RadioSearch.Status.IDLE) {
			searchBox.setValue(Countries.name(Countries.defaultCode()));
			runSearch();
		}
	}

	@Override
	public Component getTabTitle() {
		return TITLE;
	}

	@Override
	public Component getTabExtraNarration() {
		return Component.empty();
	}

	@Override
	public void visitChildren(Consumer<AbstractWidget> childrenConsumer) {
		content.visitWidgets(childrenConsumer);
	}

	@Override
	public Layout getLayout() {
		return content;
	}

	/**
	 * Places everything inside {@code area}, the space between the tab bar and the buttons at the bottom. Runs
	 * again when the window is resized, so all sizes come from {@code area}. Done by hand rather than with
	 * {@code content.arrangeElements()}: the list has to take whatever height is left, which a LinearLayout
	 * cannot do, and only {@code updateSizeAndPosition} moves its rows along with it.
	 */
	@Override
	public void doLayout(ScreenRectangle area) {
		int width = Math.min(MAX_WIDTH, area.width() - 2 * MARGIN);
		searchBox.setWidth(width - SEARCH_BY_WIDTH - SEARCH_BUTTON_WIDTH - 2 * GAP);
		searchRow.arrangeElements();
		searchRow.setPosition(area.left() + (area.width() - width) / 2, area.top() + MARGIN);

		int listTop = searchRow.getY() + searchRow.getHeight() + MARGIN;
		// The list spans the whole width, like the game's own lists; its rows stay in the middle.
		list.updateSizeAndPosition(area.width(), area.bottom() - listTop, area.left(), listTop);
	}

	/** Called by the screen every tick: shows what {@link RadioSearch} has, when it changed. */
	void tick() {
		if (shownVersion == search.version()) {
			return;
		}
		shownVersion = search.version();
		switch (search.status()) {
			case IDLE -> {
				// Only before the first search, which the constructor starts.
			}
			case LOADING -> list.showLoading(Component.translatable("command.unlimusic.radios.searching", search.what()));
			case FAILED -> list.showError(Component.translatable("command.unlimusic.radios.offline"));
			case DONE -> {
				if (search.stations().isEmpty()) {
					list.showMessage(Component.translatable("command.unlimusic.radios.none", search.what()));
				} else {
					list.showStations(search.stations());
				}
			}
		}
	}

	/** The selected station, or null. */
	RadioStation selectedStation() {
		return list.selectedStation();
	}

	private void runSearch() {
		SearchBy by = searchBy.getValue();
		String text = searchBox.getValue().strip();
		if (by == SearchBy.COUNTRY) {
			String code = Countries.find(text);
			if (code == null) {
				list.showError(Component.translatable("screen.unlimusic.radios.unknown_country", text));
				return;
			}
			String name = Countries.name(code);
			// "port" becomes "Portugal", so the box says what was searched.
			searchBox.setValue(name);
			search.start(by, code, Component.translatable("command.unlimusic.radios.in_country", name));
			return;
		}

		if (text.length() < RadioBrowserClient.MIN_TEXT_LENGTH) {
			list.showError(Component.translatable("command.unlimusic.radios.too_short", RadioBrowserClient.MIN_TEXT_LENGTH));
			return;
		}
		String key = by == SearchBy.NAME ? "command.unlimusic.radios.named" : "command.unlimusic.radios.in_genre";
		search.start(by, text, Component.translatable(key, text));
	}

	private void onSearchByChanged(SearchBy by) {
		searchBox.setHint(hint(by));
		// A country name makes no sense as a station name, nor the other way round: start the box afresh.
		searchBox.setValue(by == SearchBy.COUNTRY ? Countries.name(Countries.defaultCode()) : "");
	}

	private void onTextChanged(String text) {
		SearchBy by = searchBy.getValue();
		search.remember(by, text);
		// Grey text after the cursor with the rest of the country's name; Enter searches it.
		searchBox.setSuggestion(by == SearchBy.COUNTRY && searchBox.getCursorPosition() == text.length()
				? Countries.completion(text) : null);
	}

	private static Component label(SearchBy by) {
		return Component.translatable(switch (by) {
			case COUNTRY -> "screen.unlimusic.radios.by.country";
			case NAME -> "screen.unlimusic.radios.by.name";
			case TAG -> "screen.unlimusic.radios.by.genre";
		});
	}

	private static Component hint(SearchBy by) {
		return Component.translatable(switch (by) {
			case COUNTRY -> "screen.unlimusic.radios.hint.country";
			case NAME -> "screen.unlimusic.radios.hint.name";
			case TAG -> "screen.unlimusic.radios.hint.genre";
		});
	}
}
