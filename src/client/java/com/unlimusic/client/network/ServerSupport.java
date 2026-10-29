package com.unlimusic.client.network;

import net.minecraft.network.chat.Component;

/**
 * What the server the player is on can do for the mod: Jamendo and shared mode need UnliMusic on the server
 * too (decision D9). Only touched on the game thread.
 */
public enum ServerSupport {
	/** In a world, waiting for the server's answer to our hello. */
	CHECKING("message.unlimusic.server.checking"),
	/** The server does not have the mod, or the player is not in a world. */
	MISSING("message.unlimusic.server.missing"),
	/** The server has a newer version of the mod than the player. */
	CLIENT_OUTDATED("message.unlimusic.server.client_outdated"),
	/** The server has an older version of the mod than the player. */
	SERVER_OUTDATED("message.unlimusic.server.server_outdated"),
	READY(null);

	private static ServerSupport current = MISSING;

	private final String reasonKey;

	ServerSupport(String reasonKey) {
		this.reasonKey = reasonKey;
	}

	public static ServerSupport current() {
		return current;
	}

	static void set(ServerSupport support) {
		current = support;
	}

	/** Why the server's features are off, to show the player; null when they are on. */
	public Component reason() {
		return reasonKey != null ? Component.translatable(reasonKey) : null;
	}
}
