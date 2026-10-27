package com.unlimusic.client.hud;

import com.unlimusic.UnliMusic;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * The "now playing" box in the corner of the screen. Display-only (decision D14): nothing on it can be clicked.
 *
 * <p>Runs every frame, so it only reads state and queues what to draw; the game draws it later.
 */
public final class NowPlayingHud implements HudElement {
	private static final int MARGIN = 4;
	// Colours are ARGB: text with an alpha of 0 is silently skipped, so white must be 0xFFFFFFFF, not 0xFFFFFF.
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	// Safe to build once: the translation is looked up when drawn, so it follows a change of language.
	private static final Component NOTHING_PLAYING = Component.translatable("hud.unlimusic.nothing_playing");

	private NowPlayingHud() {
	}

	public static void register() {
		// Attaching to a vanilla element inherits its "hidden with F1" condition (addLast would not).
		// Before the chat, so the chat and the player list (Tab) cover the box rather than the other way round.
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, UnliMusic.id("now_playing"), new NowPlayingHud());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		graphics.text(Minecraft.getInstance().font, NOTHING_PLAYING, MARGIN, MARGIN, TEXT_COLOR);
	}
}
