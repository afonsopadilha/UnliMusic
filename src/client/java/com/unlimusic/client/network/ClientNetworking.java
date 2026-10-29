package com.unlimusic.client.network;

import com.unlimusic.UnliMusic;
import com.unlimusic.network.HelloPayload;
import com.unlimusic.network.ModNetworking;
import com.unlimusic.network.PingPayload;
import com.unlimusic.network.PongPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;

/** What the client does with the packets the server sends it, and the helpers to send ours. */
public final class ClientNetworking {
	private ClientNetworking() {
	}

	/** After {@code ModNetworking.register()}: a receiver needs its payload type registered first. */
	public static void register() {
		// Before the world loads, each side tells the other which channels it can receive. So by the time
		// JOIN fires, canSend already knows whether the server has the mod. Runs on the game thread.
		ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> {
			if (ClientPlayNetworking.canSend(HelloPayload.TYPE)) {
				ServerSupport.set(ServerSupport.CHECKING);
				sender.sendPacket(new HelloPayload(ModNetworking.PROTOCOL_VERSION));
			} else {
				// Not worth a chat message: most servers have no mods. The warning comes when the player
				// tries something that needs the server.
				ServerSupport.set(ServerSupport.MISSING);
				UnliMusic.LOGGER.info("This server does not have UnliMusic: Jamendo and shared mode are off");
			}
		});
		// Fabric does not say which thread this runs on, so hop over to the game thread first.
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> client.execute(() -> {
			ServerSupport.set(ServerSupport.MISSING);
			JamendoSearches.forget();
		}));
		JamendoSearches.register();

		// Fabric runs these on the game (render) thread, so they may write to the chat directly.
		ClientPlayNetworking.registerGlobalReceiver(HelloPayload.TYPE, (payload, context) -> {
			int server = payload.protocol();
			ServerSupport support = server == ModNetworking.PROTOCOL_VERSION ? ServerSupport.READY
					: server > ModNetworking.PROTOCOL_VERSION ? ServerSupport.CLIENT_OUTDATED
					: ServerSupport.SERVER_OUTDATED;
			ServerSupport.set(support);
			UnliMusic.LOGGER.info("Server speaks UnliMusic protocol {} (ours: {}): {}", server, ModNetworking.PROTOCOL_VERSION, support);
			if (support != ServerSupport.READY) {
				// Unlike a server without the mod, this is worth saying at once: updating fixes it.
				context.player().sendSystemMessage(support.reason());
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(PongPayload.TYPE, (payload, context) -> {
			long millis = (System.nanoTime() - payload.sentAtNanos()) / 1_000_000;
			UnliMusic.LOGGER.info("Pong from the server after {} ms", millis);
			context.player().sendSystemMessage(Component.translatable("command.unlimusic.ping.pong", millis));
		});
	}

	/**
	 * Sends a ping, if the server can take it.
	 *
	 * @return why it could not be sent, or null when it was
	 */
	public static Component ping() {
		ServerSupport support = ServerSupport.current();
		if (support != ServerSupport.READY) {
			return support.reason();
		}
		ClientPlayNetworking.send(new PingPayload(System.nanoTime()));
		return null;
	}
}
