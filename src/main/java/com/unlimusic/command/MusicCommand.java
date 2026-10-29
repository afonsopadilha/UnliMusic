package com.unlimusic.command;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.unlimusic.UnliMusic;
import com.unlimusic.jamendo.JamendoClient;
import com.unlimusic.jamendo.JamendoClient.SearchBy;
import com.unlimusic.jamendo.JamendoException;
import com.unlimusic.jamendo.JamendoTrack;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/**
 * The server's {@code /music} command, for admins, the console and testing (decision D14): players will use
 * the mod's screen. It runs on the server, which in singleplayer is the integrated one, so it needs the mod
 * there.
 */
public final class MusicCommand {
	private static final int RESULTS = 10;
	private static final int MAX_NAME_LENGTH = 40;
	private static final int MIN_TEXT_LENGTH = 2;

	private MusicCommand() {
	}

	public static void register() {
		// Anyone may search for now; E12 sets per-player limits, since every search spends the server's quota.
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(
				literal("music").then(literal("jamendo")
						.then(literal("search").then(argument("text", StringArgumentType.greedyString())
								.executes(context -> search(context.getSource(), SearchBy.TEXT,
										StringArgumentType.getString(context, "text")))))
						.then(literal("tag").then(argument("tags", StringArgumentType.greedyString())
								.executes(context -> search(context.getSource(), SearchBy.TAG,
										StringArgumentType.getString(context, "tags"))))))));
	}

	private static int search(CommandSourceStack source, SearchBy by, String text) {
		String value = text.strip();
		if (value.length() < MIN_TEXT_LENGTH) {
			source.sendFailure(Component.translatable("command.unlimusic.jamendo.too_short", MIN_TEXT_LENGTH));
			return 0;
		}
		source.sendSuccess(() -> Component.translatable("command.unlimusic.jamendo.searching", value)
				.withStyle(ChatFormatting.GRAY), false);
		JamendoClient.INSTANCE.search(by, value, RESULTS).whenComplete((tracks, error) ->
				// The answer arrives on a background thread; messages are sent from the server thread.
				source.getServer().execute(() -> {
					if (error != null) {
						showFailure(source, by, value, error);
					} else {
						showResults(source, tracks, value);
					}
				}));
		return Command.SINGLE_SUCCESS;
	}

	private static void showFailure(CommandSourceStack source, SearchBy by, String value, Throwable error) {
		Throwable cause = error instanceof CompletionException ? error.getCause() : error;
		if (!(cause instanceof JamendoException jamendo)) {
			UnliMusic.LOGGER.error("Jamendo search {} \"{}\" failed", by, value, cause);
			source.sendFailure(Component.translatable("command.unlimusic.jamendo.offline"));
			return;
		}
		String key = switch (jamendo.reason()) {
			case NO_CLIENT_ID -> "command.unlimusic.jamendo.no_client_id";
			case REJECTED -> "command.unlimusic.jamendo.rejected";
			case RATE_LIMITED -> "command.unlimusic.jamendo.rate_limited";
			case OFFLINE -> {
				UnliMusic.LOGGER.warn("Jamendo search {} \"{}\" failed: {}", by, value, jamendo.getMessage());
				yield "command.unlimusic.jamendo.offline";
			}
		};
		source.sendFailure(Component.translatable(key));
	}

	private static void showResults(CommandSourceStack source, List<JamendoTrack> tracks, String value) {
		if (tracks.isEmpty()) {
			source.sendSuccess(() -> Component.translatable("command.unlimusic.jamendo.none", value), false);
			return;
		}
		source.sendSuccess(() -> Component.translatable("command.unlimusic.jamendo.header", value)
				.withStyle(ChatFormatting.YELLOW), false);
		for (int i = 0; i < tracks.size(); i++) {
			MutableComponent line = resultLine(i + 1, tracks.get(i));
			source.sendSuccess(() -> line, false);
		}
	}

	/**
	 * " 1. Lofi Chillout Hip Hop Beat – Joystock · 2:29 · CC BY-NC-ND 3.0", which opens the track's page on
	 * Jamendo when clicked: the direct link the API terms ask for.
	 */
	private static MutableComponent resultLine(int number, JamendoTrack track) {
		String license = track.license() != null ? track.license() : "CC";
		ClickEvent click = new ClickEvent.OpenUrl(URI.create(track.pageUrl()));
		HoverEvent hover = new HoverEvent.ShowText(Component.literal(track.name() + "\n" + track.artist())
				.append(Component.literal("\n" + license + " · ").withStyle(ChatFormatting.GRAY))
				.append(Component.translatable("command.unlimusic.jamendo.via").withStyle(ChatFormatting.GRAY))
				.append(Component.literal("\n"))
				.append(Component.translatable("command.unlimusic.jamendo.click_to_open")
						.withStyle(ChatFormatting.YELLOW)));
		return Component.literal(" " + number + ". ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(shorten(track.name())).withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" – " + track.artist()).withStyle(ChatFormatting.GRAY))
				.append(Component.literal(" · " + duration(track.durationSeconds()) + " · " + license)
						.withStyle(ChatFormatting.DARK_GRAY))
				.withStyle(style -> style.withClickEvent(click).withHoverEvent(hover));
	}

	private static String duration(int seconds) {
		return seconds / 60 + ":" + String.format(Locale.ROOT, "%02d", seconds % 60);
	}

	private static String shorten(String name) {
		return name.length() <= MAX_NAME_LENGTH ? name : name.substring(0, MAX_NAME_LENGTH - 1) + "…";
	}
}
