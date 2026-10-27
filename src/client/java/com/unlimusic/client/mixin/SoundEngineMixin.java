package com.unlimusic.client.mixin;

import com.unlimusic.client.audio.MusicPlayer;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps paused music paused. Closing any screen (chat, inventory, pause menu) calls {@code resume()},
 * which unpauses every channel, ours included.
 */
@Mixin(SoundEngine.class)
abstract class SoundEngineMixin {
	@Inject(method = "resume", at = @At("TAIL"))
	private void unlimusic$keepMusicPaused(CallbackInfo info) {
		// resume() only queued "unpause everything" on the sound thread; pausing ours right now queues it
		// straight behind, so OpenAL never gets to play any of it. Waiting for the next tick was audible.
		MusicPlayer.INSTANCE.onEngineResumed();
	}
}
