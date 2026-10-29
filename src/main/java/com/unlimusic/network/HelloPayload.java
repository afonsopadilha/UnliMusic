package com.unlimusic.network;

import com.unlimusic.UnliMusic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Both ways: "this is the protocol I speak". The client sends it as soon as it joins, and the server answers
 * with its own. Only when both numbers match does either side send the mod's other packets.
 *
 * <p><b>Never change this record or its codec</b>: it is the one packet that has to be readable by every
 * version of the mod, old and new, so that they can find out they do not understand each other.
 *
 * @param protocol the sender's {@link ModNetworking#PROTOCOL_VERSION}
 */
public record HelloPayload(int protocol) implements CustomPacketPayload {
	public static final Type<HelloPayload> TYPE = new Type<>(UnliMusic.id("hello"));

	// VAR_INT writes small numbers in a single byte.
	public static final StreamCodec<ByteBuf, HelloPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, HelloPayload::protocol,
			HelloPayload::new);

	@Override
	public Type<HelloPayload> type() {
		return TYPE;
	}
}
