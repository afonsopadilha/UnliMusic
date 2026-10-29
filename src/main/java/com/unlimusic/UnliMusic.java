package com.unlimusic;

import com.unlimusic.command.MusicCommand;
import com.unlimusic.config.ServerConfig;
import com.unlimusic.network.ModNetworking;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnliMusic implements ModInitializer {
	public static final String MOD_ID = "unlimusic";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * How the mod introduces itself to web services and radio stations. Radio Browser asks every app for a
	 * name and version, so that its maintainer can see who uses the API and get in touch.
	 */
	public static final String USER_AGENT = "UnliMusic/" + FabricLoader.getInstance().getModContainer(MOD_ID)
			.map(mod -> mod.getMetadata().getVersion().getFriendlyString())
			.orElse("dev")
			+ " (Minecraft mod; +https://github.com/afonsopadilha/UnliMusic)";

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");

		// Runs on dedicated servers and on the integrated server of each singleplayer world, before it loads.
		ServerLifecycleEvents.SERVER_STARTING.register(server -> ServerConfig.load());
		MusicCommand.register();
		ModNetworking.register();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
