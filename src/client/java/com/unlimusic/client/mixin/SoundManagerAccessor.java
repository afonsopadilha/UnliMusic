package com.unlimusic.client.mixin;

import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads {@code SoundManager}'s private sound engine. Mixin fills in the method's body when the game starts;
 * use it by casting: {@code ((SoundManagerAccessor) soundManager).unlimusic$soundEngine()}.
 */
@Mixin(SoundManager.class)
public interface SoundManagerAccessor {
	// The unlimusic$ prefix keeps the name from clashing with methods other mods add to the same class.
	@Accessor("soundEngine")
	SoundEngine unlimusic$soundEngine();
}
