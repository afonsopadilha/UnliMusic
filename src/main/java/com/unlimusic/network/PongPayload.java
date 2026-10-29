package com.unlimusic.network;

import com.unlimusic.UnliMusic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client: the answer to a {@link PingPayload}.
 *
 * @param sentAtNanos copied unchanged from the ping it answers
 */
public record PongPayload(long sentAtNanos) implements CustomPacketPayload {
	public static final Type<PongPayload> TYPE = new Type<>(UnliMusic.id("pong"));

	public static final StreamCodec<ByteBuf, PongPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.LONG, PongPayload::sentAtNanos,
			PongPayload::new);

	@Override
	public Type<PongPayload> type() {
		return TYPE;
	}
}
