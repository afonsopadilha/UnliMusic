package com.unlimusic.network;

import com.unlimusic.UnliMusic;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * The mod's packets: which ones exist, and what the server does when one arrives. The client's side of the
 * handling lives in {@code ClientNetworking} (src/client), since a dedicated server has no client classes.
 */
public final class ModNetworking {
	private ModNetworking() {
	}

	/** Called from the main entrypoint, so it runs on the client and on dedicated servers alike. */
	public static void register() {
		// Both sides must know every payload's codec: the sender to write it, the receiver to read it.
		// "serverbound" = client → server (C2S), "clientbound" = server → client (S2C).
		PayloadTypeRegistry.serverboundPlay().register(PingPayload.TYPE, PingPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PongPayload.TYPE, PongPayload.CODEC);

		// Fabric runs this on the server thread, so it may touch the world and the players safely.
		ServerPlayNetworking.registerGlobalReceiver(PingPayload.TYPE, (payload, context) -> {
			UnliMusic.LOGGER.info("Ping from {}", context.player().getScoreboardName());
			context.responseSender().sendPacket(new PongPayload(payload.sentAtNanos()));
		});
	}
}
