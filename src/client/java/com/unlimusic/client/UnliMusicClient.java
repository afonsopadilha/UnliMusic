package com.unlimusic.client;

import com.unlimusic.client.input.ModKeys;
import net.fabricmc.api.ClientModInitializer;

public class UnliMusicClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Runs only on the player's game, never on a dedicated server.
		ModKeys.register();
	}
}
