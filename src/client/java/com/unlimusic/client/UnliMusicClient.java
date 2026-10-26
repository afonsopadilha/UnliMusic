package com.unlimusic.client;

import com.unlimusic.client.audio.LavaplayerEngine;
import com.unlimusic.client.input.ModKeys;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class UnliMusicClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Runs only on the player's game, never on a dedicated server.
		ModKeys.register();
		LavaplayerEngine.startInBackground();
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> LavaplayerEngine.shutdown());
	}
}
