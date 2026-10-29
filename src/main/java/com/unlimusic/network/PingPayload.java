package com.unlimusic.network;

import com.unlimusic.UnliMusic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server: "are you there?". The server answers with a {@link PongPayload} carrying the same number.
 *
 * @param sentAtNanos the client's {@link System#nanoTime()} when it sent the ping. Only the client reads it
 *                    back, to measure the round trip: the server's clock never comes into it.
 */
public record PingPayload(long sentAtNanos) implements CustomPacketPayload {
	// The channel name both sides use to recognise this packet: "unlimusic:ping".
	public static final Type<PingPayload> TYPE = new Type<>(UnliMusic.id("ping"));

	// How the record becomes bytes and back: one field, a long, written as 8 bytes.
	public static final StreamCodec<ByteBuf, PingPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.LONG, PingPayload::sentAtNanos,
			PingPayload::new);

	@Override
	public Type<PingPayload> type() {
		return TYPE;
	}
}
