package com.unlimusic.client;

import com.unlimusic.client.audio.LavaplayerEngine;
import com.unlimusic.client.command.UnliMusicCommand;
import com.unlimusic.client.audio.MusicPlayer;
import com.unlimusic.client.hud.NowPlayingHud;
import com.unlimusic.client.input.ModKeys;
import com.unlimusic.client.network.ClientNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class UnliMusicClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Runs only on the player's game, never on a dedicated server.
		ModKeys.register();
		StatusMessages.register();
		NowPlayingHud.register();
		UnliMusicCommand.register();
		// The main entrypoint has already registered the payload types by the time this runs.
		ClientNetworking.register();
		LavaplayerEngine.startInBackground();

		ClientTickEvents.END_CLIENT_TICK.register(MusicPlayer.INSTANCE::tick);
		// Fabric does not say which thread this runs on, so hop over to the game thread first.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
				client.execute(() -> MusicPlayer.INSTANCE.stop("left the world")));
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			MusicPlayer.INSTANCE.stop("game closing");
			LavaplayerEngine.shutdown();
		});
	}
}
