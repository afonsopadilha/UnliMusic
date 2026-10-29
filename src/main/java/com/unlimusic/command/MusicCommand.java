package com.unlimusic.command;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.unlimusic.jamendo.JamendoClient;
import com.unlimusic.jamendo.JamendoClient.SearchBy;
import com.unlimusic.jamendo.JamendoException;
import com.unlimusic.jamendo.JamendoText;
import com.unlimusic.jamendo.JamendoTrack;
import java.util.List;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The server's {@code /music} command, for admins, the console and testing (decision D14): players will use
 * the mod's screen. It runs on the server, which in singleplayer is the integrated one, so it needs the mod
 * there.
 */
public final class MusicCommand {
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
		if (value.length() < JamendoClient.MIN_QUERY_LENGTH) {
			source.sendFailure(Component.translatable("command.unlimusic.jamendo.too_short", JamendoClient.MIN_QUERY_LENGTH));
			return 0;
		}
		if (value.length() > JamendoClient.MAX_QUERY_LENGTH) {
			source.sendFailure(Component.translatable("command.unlimusic.jamendo.too_long", JamendoClient.MAX_QUERY_LENGTH));
			return 0;
		}
		source.sendSuccess(() -> Component.translatable("command.unlimusic.jamendo.searching", value)
				.withStyle(ChatFormatting.GRAY), false);
		JamendoClient.INSTANCE.search(by, value, JamendoClient.RESULTS).whenComplete((tracks, error) ->
				// The answer arrives on a background thread; messages are sent from the server thread.
				source.getServer().execute(() -> {
					if (error != null) {
						source.sendFailure(JamendoException.reasonOf(error, by + " \"" + value + "\"").message());
					} else {
						showResults(source, tracks, value);
					}
				}));
		return Command.SINGLE_SUCCESS;
	}

	private static void showResults(CommandSourceStack source, List<JamendoTrack> tracks, String value) {
		if (tracks.isEmpty()) {
			source.sendSuccess(() -> Component.translatable("command.unlimusic.jamendo.none", value), false);
			return;
		}
		source.sendSuccess(() -> Component.translatable("command.unlimusic.jamendo.header", value)
				.withStyle(ChatFormatting.YELLOW), false);
		for (int i = 0; i < tracks.size(); i++) {
			MutableComponent line = JamendoText.resultLine(i + 1, tracks.get(i));
			source.sendSuccess(() -> line, false);
		}
	}
}
