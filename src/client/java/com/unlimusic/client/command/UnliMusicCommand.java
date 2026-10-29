package com.unlimusic.client.command;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.unlimusic.UnliMusic;
import com.unlimusic.client.audio.MusicPlayer;
import com.unlimusic.radio.RadioBrowserClient;
import com.unlimusic.radio.RadioBrowserClient.SearchBy;
import com.unlimusic.radio.RadioStation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/**
 * The client's own {@code /unlimusic} command, for testing until the mod's screen exists (E09). It runs only
 * on the player's game, so it works on any server, even one without the mod; the server's {@code /music}
 * (E10) is a separate command.
 */
public final class UnliMusicCommand {
	private static final int RESULTS = 10;
	// The chat is about 50 characters wide at the default size: longer names would wrap onto a second line.
	private static final int MAX_NAME_LENGTH = 40;
	// Some stations list twenty tags or more; the first few say enough.
	private static final int MAX_TAGS = 6;
	private static final int MIN_TEXT_LENGTH = 2;
	// Stations shown in chat by id, so that clicking a line can find what to play. Only touched on the game
	// thread. The oldest are forgotten past the limit, by which time their lines have scrolled far away.
	private static final Map<String, RadioStation> SHOWN = new LinkedHashMap<>() {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, RadioStation> eldest) {
			return size() > 200;
		}
	};

	private UnliMusicCommand() {
	}

	public static void register() {
		// Brigadier builds commands as a tree: each literal() is a fixed word, each argument() a value the
		// player types, and executes() is what runs when the input stops at that node.
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(
				literal("unlimusic").then(literal("radios")
						.then(argument("country", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(Locale.getISOCountries(), builder))
								.executes(context -> searchByCountry(context.getSource(),
										StringArgumentType.getString(context, "country"))))
						// A literal wins over the country argument when the input is exactly that word, and
						// no country code is longer than two letters. greedyString takes the rest of the line,
						// spaces included ("radio comercial", "smooth jazz").
						.then(literal("name").then(argument("text", StringArgumentType.greedyString())
								.executes(context -> searchByText(context.getSource(), SearchBy.NAME,
										StringArgumentType.getString(context, "text")))))
						.then(literal("genre").then(argument("text", StringArgumentType.greedyString())
								.executes(context -> searchByText(context.getSource(), SearchBy.TAG,
										StringArgumentType.getString(context, "text")))))
						// What clicking a result runs.
						.then(literal("play").then(argument("station", StringArgumentType.word())
								.executes(context -> play(context.getSource(),
										StringArgumentType.getString(context, "station"))))))));
	}

	private static int searchByCountry(FabricClientCommandSource source, String country) {
		if (!country.matches("[A-Za-z]{2}")) {
			source.sendError(Component.translatable("command.unlimusic.radios.bad_country", country));
			return 0;
		}
		String code = country.toUpperCase(Locale.ROOT);
		search(source, SearchBy.COUNTRY, code, Component.translatable("command.unlimusic.radios.in_country", code));
		return Command.SINGLE_SUCCESS;
	}

	private static int searchByText(FabricClientCommandSource source, SearchBy by, String text) {
		String value = text.strip();
		// Radio Browser matches any part of a name or tag, so a single letter would match almost everything.
		if (value.length() < MIN_TEXT_LENGTH) {
			source.sendError(Component.translatable("command.unlimusic.radios.too_short", MIN_TEXT_LENGTH));
			return 0;
		}
		String key = by == SearchBy.NAME ? "command.unlimusic.radios.named" : "command.unlimusic.radios.in_genre";
		search(source, by, value, Component.translatable(key, value));
		return Command.SINGLE_SUCCESS;
	}

	/** Asks Radio Browser and lists the answer in chat when it arrives; the command itself returns at once. */
	private static void search(FabricClientCommandSource source, SearchBy by, String value, Component what) {
		source.sendFeedback(Component.translatable("command.unlimusic.radios.searching", what).withStyle(ChatFormatting.GRAY));
		RadioBrowserClient.INSTANCE.search(by, value, RESULTS).whenComplete((stations, error) ->
				// The answer arrives on a background thread, and the chat belongs to the game thread.
				source.getClient().execute(() -> {
					if (error != null) {
						UnliMusic.LOGGER.warn("Radio search {} {} failed", by, value, error);
						source.sendError(Component.translatable("command.unlimusic.radios.offline"));
					} else {
						showResults(source, stations, what);
					}
				}));
	}

	private static void showResults(FabricClientCommandSource source, List<RadioStation> stations, Component what) {
		if (stations.isEmpty()) {
			source.sendFeedback(Component.translatable("command.unlimusic.radios.none", what));
			return;
		}
		source.sendFeedback(Component.translatable("command.unlimusic.radios.header", what).withStyle(ChatFormatting.YELLOW));
		for (int i = 0; i < stations.size(); i++) {
			RadioStation station = stations.get(i);
			SHOWN.put(station.uuid(), station);
			source.sendFeedback(resultLine(i + 1, station));
		}
	}

	/**
	 * " 1. TSF Rádio Notícias · MP3 128 kbps", which plays the station when clicked and shows its details
	 * under the mouse.
	 */
	private static MutableComponent resultLine(int number, RadioStation station) {
		// The click can only run a command, so it carries the station's id, not the station itself;
		// a URL could also be longer than a command is allowed to be.
		ClickEvent click = new ClickEvent.RunCommand("/unlimusic radios play " + station.uuid());
		HoverEvent hover = new HoverEvent.ShowText(details(station));
		return Component.literal(" " + number + ". ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(shorten(station.name())).withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" · " + format(station)).withStyle(ChatFormatting.DARK_GRAY))
				.withStyle(style -> style.withClickEvent(click).withHoverEvent(hover));
	}

	/** The full name, country, format and tags, then what a click does. */
	private static Component details(RadioStation station) {
		MutableComponent text = Component.literal(station.name()).withStyle(ChatFormatting.YELLOW);
		String country = station.countryCode() != null ? station.countryCode() + " · " : "";
		text.append(Component.literal("\n" + country + format(station)).withStyle(ChatFormatting.GRAY));
		if (!station.tags().isEmpty()) {
			List<String> tags = station.tags().subList(0, Math.min(MAX_TAGS, station.tags().size()));
			text.append(Component.literal("\n" + String.join(", ", tags)).withStyle(ChatFormatting.GRAY));
		}
		return text.append(Component.literal("\n").append(Component.translatable("command.unlimusic.radios.click_to_play"))
				.withStyle(ChatFormatting.GREEN));
	}

	private static String format(RadioStation station) {
		return station.bitrate() > 0 ? station.codec() + " " + station.bitrate() + " kbps" : station.codec();
	}

	/** Game thread (as every command): plays a station listed by an earlier search, and counts the click. */
	private static int play(FabricClientCommandSource source, String uuid) {
		RadioStation station = SHOWN.get(uuid);
		if (station == null) {
			// A line from before the game was restarted, or from long ago.
			source.sendError(Component.translatable("command.unlimusic.radios.unknown_station"));
			return 0;
		}
		MusicPlayer.INSTANCE.play(station.toTrack());
		RadioBrowserClient.INSTANCE.countClick(station);
		return Command.SINGLE_SUCCESS;
	}

	private static String shorten(String name) {
		return name.length() <= MAX_NAME_LENGTH ? name : name.substring(0, MAX_NAME_LENGTH - 1).strip() + "…";
	}
}
