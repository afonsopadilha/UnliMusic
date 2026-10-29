package com.unlimusic.client.command;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.unlimusic.UnliMusic;
import com.unlimusic.radio.RadioBrowserClient;
import com.unlimusic.radio.RadioBrowserClient.SearchBy;
import com.unlimusic.radio.RadioStation;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
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
										StringArgumentType.getString(context, "country")))))));
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
			source.sendFeedback(resultLine(i + 1, stations.get(i)));
		}
	}

	/** " 1. TSF Rádio Notícias · MP3 128 kbps" */
	private static MutableComponent resultLine(int number, RadioStation station) {
		String details = station.bitrate() > 0 ? station.codec() + " " + station.bitrate() + " kbps" : station.codec();
		return Component.literal(" " + number + ". ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(shorten(station.name())).withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" · " + details).withStyle(ChatFormatting.DARK_GRAY));
	}

	private static String shorten(String name) {
		return name.length() <= MAX_NAME_LENGTH ? name : name.substring(0, MAX_NAME_LENGTH - 1).strip() + "…";
	}
}
