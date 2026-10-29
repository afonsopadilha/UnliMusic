package com.unlimusic.jamendo;

/**
 * Why a Jamendo request failed, in terms a player can be told. The message is only for the log, and never
 * holds the client ID.
 */
public final class JamendoException extends RuntimeException {
	public enum Reason {
		/** The server's config has no client ID: Jamendo is off. */
		NO_CLIENT_ID,
		/** Jamendo turned the client ID down (unknown or suspended); off until the owner fixes it. */
		REJECTED,
		/** Too many requests from this client ID or address. */
		RATE_LIMITED,
		/** No answer, or one we could not read. */
		OFFLINE
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
