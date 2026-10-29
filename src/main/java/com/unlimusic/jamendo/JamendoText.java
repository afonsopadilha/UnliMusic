package com.unlimusic.jamendo;

import java.net.URI;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/**
 * How a Jamendo track reads in chat, the same for the server's {@code /music} and the client's
 * {@code /unlimusic}, with the credit the API terms ask for (4.1).
 */
public final class JamendoText {
	private static final int MAX_NAME_LENGTH = 40;

	private JamendoText() {
	}

	/**
	 * " 1. Lofi Chillout Hip Hop Beat – Joystock · 2:29 · CC BY-NC-ND 3.0", which opens the track's page on
	 * Jamendo when clicked: the direct link the API terms ask for.
	 */
	public static MutableComponent resultLine(int number, JamendoTrack track) {
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
