package com.unlimusic.client.network;

import com.unlimusic.UnliMusic;
import com.unlimusic.jamendo.JamendoClient.SearchBy;
import com.unlimusic.network.JamendoResultsPayload;
import com.unlimusic.network.JamendoSearchPayload;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;

/**
 * Jamendo searches, which the server runs for us: it holds the client ID (decision D2). Only one is
 * awaited at a time, the latest: when the player types a new search before the last one is answered, the
 * old answer is thrown away when it arrives. Game thread only.
 */
public final class JamendoSearches {
	// Numbers each search, so that an answer can be matched to its question. Never reset: an answer from
	// the previous world must not pass for one in this world.
	private static int lastRequestId;
	private static Consumer<JamendoResultsPayload> waiting;

	private JamendoSearches() {
	}

	static void register() {
		ClientPlayNetworking.registerGlobalReceiver(JamendoResultsPayload.TYPE, (payload, context) -> {
			if (payload.requestId() != lastRequestId || waiting == null) {
				UnliMusic.LOGGER.info("Ignored the answer to Jamendo search {}: a newer one is on its way", payload.requestId());
				return;
			}
			Consumer<JamendoResultsPayload> callback = waiting;
			waiting = null;
			callback.accept(payload);
		});
	}

	/**
	 * Asks the server, and hands its answer to {@code callback} on the game thread. Replaces any search
	 * still waiting, whose callback then never runs.
	 *
	 * @return why the search could not be sent, or null when it was
	 */
	public static Component search(SearchBy by, String text, Consumer<JamendoResultsPayload> callback) {
		ServerSupport support = ServerSupport.current();
		if (support != ServerSupport.READY) {
			return support.reason();
		}
		lastRequestId++;
		waiting = callback;
		ClientPlayNetworking.send(new JamendoSearchPayload(lastRequestId, by, text));
		return null;
	}

	/** On leaving a world: whatever was waiting will never be answered. */
	static void forget() {
		waiting = null;
	}
}
