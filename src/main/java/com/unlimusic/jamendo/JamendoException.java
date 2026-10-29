package com.unlimusic.jamendo;

import com.unlimusic.UnliMusic;
import java.util.concurrent.CompletionException;
import net.minecraft.network.chat.Component;

/**
 * Why a Jamendo request failed, in terms a player can be told. The message is only for the log, and never
 * holds the client ID.
 */
public final class JamendoException extends RuntimeException {
	/** Sent to clients as its position in this list (E08): add new reasons at the end only. */
	public enum Reason {
		/** The server's config has no client ID: Jamendo is off. */
		NO_CLIENT_ID("command.unlimusic.jamendo.no_client_id"),
		/** Jamendo turned the client ID down (unknown or suspended); off until the owner fixes it. */
		REJECTED("command.unlimusic.jamendo.rejected"),
		/** Too many requests from this client ID or address. */
		RATE_LIMITED("command.unlimusic.jamendo.rate_limited"),
		/** No answer, or one we could not read. */
		OFFLINE("command.unlimusic.jamendo.offline");

		private final String translationKey;

		Reason(String translationKey) {
			this.translationKey = translationKey;
		}

		/** What to tell the player, in their language. */
		public Component message() {
			return Component.translatable(translationKey);
		}
	}

	/**
	 * Why a search's future failed, logging what a player cannot be told.
	 *
	 * @param what the search, for the log, e.g. {@code TEXT "lofi"}
	 */
	public static Reason reasonOf(Throwable error, String what) {
		Throwable cause = error instanceof CompletionException ? error.getCause() : error;
		if (!(cause instanceof JamendoException jamendo)) {
			UnliMusic.LOGGER.error("Jamendo search {} failed", what, cause);
			return Reason.OFFLINE;
		}
		if (jamendo.reason() == Reason.OFFLINE) {
			UnliMusic.LOGGER.warn("Jamendo search {} failed: {}", what, jamendo.getMessage());
		}
		return jamendo.reason();
	}

	private final Reason reason;

	public JamendoException(Reason reason) {
		this(reason, reason.name());
	}

	public JamendoException(Reason reason, String message) {
		super(message);
		this.reason = reason;
	}

	public Reason reason() {
		return reason;
	}
}
