package com.unlimusic.network;

import com.unlimusic.UnliMusic;
import com.unlimusic.jamendo.JamendoClient;
import com.unlimusic.jamendo.JamendoException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The mod's packets: which ones exist, and what the server does when one arrives. The client's side of the
 * handling lives in {@code ClientNetworking} (src/client), since a dedicated server has no client classes.
 */
public final class ModNetworking {
	/**
	 * Goes up by one whenever a packet is added, removed or changes its fields, so that two different
	 * versions of the mod notice it instead of misreading each other's bytes.
	 */
	public static final int PROTOCOL_VERSION = 1;

	// The protocol each connected player's mod speaks, by player. A player without the mod is never added.
	private static final Map<UUID, Integer> CLIENT_PROTOCOLS = new ConcurrentHashMap<>();

	private ModNetworking() {
	}

	/** Called from the main entrypoint, so it runs on the client and on dedicated servers alike. */
	public static void register() {
		// Both sides must know every payload's codec: the sender to write it, the receiver to read it.
		// "serverbound" = client → server (C2S), "clientbound" = server → client (S2C).
		// Hello goes both ways, so it is registered in both.
		PayloadTypeRegistry.serverboundPlay().register(HelloPayload.TYPE, HelloPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(HelloPayload.TYPE, HelloPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PingPayload.TYPE, PingPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PongPayload.TYPE, PongPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(JamendoSearchPayload.TYPE, JamendoSearchPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(JamendoResultsPayload.TYPE, JamendoResultsPayload.CODEC);

		// Fabric runs these on the server thread, so they may touch the world and the players safely.
		ServerPlayNetworking.registerGlobalReceiver(HelloPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			CLIENT_PROTOCOLS.put(player.getUUID(), payload.protocol());
			if (payload.protocol() != PROTOCOL_VERSION) {
				UnliMusic.LOGGER.warn("{} has a different version of UnliMusic (protocol {}, the server's is {}): "
						+ "its music features stay off", player.getScoreboardName(), payload.protocol(), PROTOCOL_VERSION);
			}
			context.responseSender().sendPacket(new HelloPayload(PROTOCOL_VERSION));
		});
		ServerPlayNetworking.registerGlobalReceiver(PingPayload.TYPE, (payload, context) -> {
			UnliMusic.LOGGER.info("Ping from {}", context.player().getScoreboardName());
			context.responseSender().sendPacket(new PongPayload(payload.sentAtNanos()));
		});
		ServerPlayNetworking.registerGlobalReceiver(JamendoSearchPayload.TYPE, (payload, context) ->
				searchJamendo(context.server(), context.player(), payload));
		ServerPlayConnectionEvents.DISCONNECT.register((listener, server) ->
				CLIENT_PROTOCOLS.remove(listener.player.getUUID()));
	}

	/**
	 * Server thread. Searches for a player and sends the answer back when Jamendo replies, a second or so
	 * later, without ever making the server thread wait.
	 */
	private static void searchJamendo(MinecraftServer server, ServerPlayer player, JamendoSearchPayload payload) {
		// Never trust the client: the screen checks the length too, but a tampered client could skip it.
		// The codec has already bounded the length from above.
		String text = payload.query().strip();
		if (!speaksOurProtocol(player) || text.length() < JamendoClient.MIN_QUERY_LENGTH) {
			UnliMusic.LOGGER.warn("Ignored a Jamendo search from {} that the mod's screen would not send",
					player.getScoreboardName());
			return;
		}
		String what = payload.by() + " \"" + text + "\" for " + player.getScoreboardName();
		JamendoClient.INSTANCE.search(payload.by(), text, JamendoClient.RESULTS).whenComplete((tracks, error) -> {
			JamendoResultsPayload results = error != null
					? JamendoResultsPayload.failed(payload.requestId(), JamendoException.reasonOf(error, what))
					: JamendoResultsPayload.found(payload.requestId(), tracks);
			// The answer arrives on a background thread; packets are sent from the server thread. By then the
			// player may have left, and nobody is waiting for it.
			server.execute(() -> {
				if (!player.hasDisconnected()) {
					ServerPlayNetworking.send(player, results);
				}
			});
		});
	}

	/**
	 * Whether this player's mod reads our packets the way the server writes them. Everything the server sends
	 * on its own (shared mode, from E10) must check this first. A game without the mod throws away packets
	 * it does not know, but one with another version knows the channel and may read the bytes wrongly, and
	 * a packet that does not decode cleanly ends the connection.
	 */
	public static boolean speaksOurProtocol(ServerPlayer player) {
		Integer protocol = CLIENT_PROTOCOLS.get(player.getUUID());
		return protocol != null && protocol == PROTOCOL_VERSION;
	}
}
