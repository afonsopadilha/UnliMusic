package com.unlimusic.client.network;

import com.unlimusic.UnliMusic;
import com.unlimusic.network.PingPayload;
import com.unlimusic.network.PongPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;

/** What the client does with the packets the server sends it, and the helpers to send ours. */
public final class ClientNetworking {
	private ClientNetworking() {
	}

	/** After {@code ModNetworking.register()}: a receiver needs its payload type registered first. */
	public static void register() {
		// Fabric runs this on the game (render) thread, so it may write to the chat directly.
		ClientPlayNetworking.registerGlobalReceiver(PongPayload.TYPE, (payload, context) -> {
			long millis = (System.nanoTime() - payload.sentAtNanos()) / 1_000_000;
			UnliMusic.LOGGER.info("Pong from the server after {} ms", millis);
			context.player().sendSystemMessage(Component.translatable("command.unlimusic.ping.pong", millis));
		});
	}

	/**
	 * Sends a ping, if the server can take it. When a player joins, each side lists the channels it can
	 * receive; a server without the mod never lists ours, and a ping sent anyway would get no answer.
	 *
	 * @return false when the server does not have the mod (or there is no server)
	 */
	public static boolean ping() {
		if (!ClientPlayNetworking.canSend(PingPayload.TYPE)) {
			return false;
		}
		ClientPlayNetworking.send(new PingPayload(System.nanoTime()));
		return true;
	}
}
