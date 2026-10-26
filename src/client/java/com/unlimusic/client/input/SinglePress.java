package com.unlimusic.client.input;

import net.minecraft.client.KeyMapping;

/**
 * Turns a {@link KeyMapping} into exactly one event per physical press.
 *
 * <p>In 26.3 the OS key-repeat events also count as clicks, so {@code consumeClick()} alone fires
 * many times while the key is held. We still drain the clicks (they catch taps shorter than a tick)
 * but ignore them when the key was already down on the previous tick.
 */
public final class SinglePress {
	private final KeyMapping key;
	private boolean wasDown;

	public SinglePress(KeyMapping key) {
		this.key = key;
	}

	/** Call once per client tick; returns true on the tick the key was pressed. */
	public boolean poll() {
		boolean clicked = false;
		while (key.consumeClick()) {
			clicked = true;
		}

		boolean pressed = clicked && !wasDown;
		wasDown = key.isDown();
		return pressed;
	}
}
