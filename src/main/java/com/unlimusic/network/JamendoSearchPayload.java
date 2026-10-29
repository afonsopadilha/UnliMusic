package com.unlimusic.network;

import com.unlimusic.UnliMusic;
import com.unlimusic.jamendo.JamendoClient;
import com.unlimusic.jamendo.JamendoClient.SearchBy;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server: "search Jamendo for this". The server answers with a {@link JamendoResultsPayload}
 * carrying the same {@code requestId}.
 *
 * <p>The client does not say how many tracks it wants: the server always asks Jamendo for the same number,
 * since every search spends the server owner's quota.
 *
 * @param requestId picked by the client, so that it can tell which search an answer belongs to
 * @param by        what to search: names (TEXT) or genre tags (TAG)
 * @param query     what the player typed, at most {@link JamendoClient#MAX_QUERY_LENGTH} characters
 */
public record JamendoSearchPayload(int requestId, SearchBy by, String query) implements CustomPacketPayload {
	public static final Type<JamendoSearchPayload> TYPE = new Type<>(UnliMusic.id("jamendo_search"));

	// An enum travels as its position in the list (TEXT = 0, TAG = 1). A number past the end fails to
	// decode, and a packet that fails to decode disconnects whoever sent it: only a tampered client would.
	private static final StreamCodec<ByteBuf, SearchBy> SEARCH_BY_CODEC =
			ByteBufCodecs.idMapper(id -> SearchBy.values()[id], SearchBy::ordinal);

	// stringUtf8(max) refuses a longer string on both ends: the sender cannot write it, and the receiver
	// stops reading instead of filling its memory with whatever a client sends.
	public static final StreamCodec<ByteBuf, JamendoSearchPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, JamendoSearchPayload::requestId,
			SEARCH_BY_CODEC, JamendoSearchPayload::by,
			ByteBufCodecs.stringUtf8(JamendoClient.MAX_QUERY_LENGTH), JamendoSearchPayload::query,
			JamendoSearchPayload::new);

	@Override
	public Type<JamendoSearchPayload> type() {
		return TYPE;
	}
}
