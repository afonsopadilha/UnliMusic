package com.unlimusic.network;

import com.unlimusic.UnliMusic;
import com.unlimusic.jamendo.JamendoException.Reason;
import com.unlimusic.jamendo.JamendoTrack;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client: the answer to a {@link JamendoSearchPayload}.
 *
 * <p>Each track carries its audio URL, which each client streams straight from Jamendo (the server never
 * relays audio). The URL holds a {@code from=} token that ties plays to the app: like on the server, it never
 * goes into a log or the chat.
 *
 * @param requestId copied from the search it answers
 * @param failure   why the search failed, or null when it worked
 * @param tracks    what Jamendo found (empty when it failed), at most {@link #MAX_TRACKS}
 */
public record JamendoResultsPayload(int requestId, Reason failure, List<JamendoTrack> tracks)
		implements CustomPacketPayload {
	public static final Type<JamendoResultsPayload> TYPE = new Type<>(UnliMusic.id("jamendo_results"));

	public static final int MAX_TRACKS = 20;
	// Far above anything Jamendo sends (a name of 100 characters is already rare; its URLs are under 200).
	// They only bound what a packet may carry: a track past them is left out, see fits().
	public static final int MAX_TEXT_LENGTH = 256;
	public static final int MAX_URL_LENGTH = 1024;

	private static final StreamCodec<ByteBuf, String> TEXT = ByteBufCodecs.stringUtf8(MAX_TEXT_LENGTH);
	private static final StreamCodec<ByteBuf, String> URL = ByteBufCodecs.stringUtf8(MAX_URL_LENGTH);
	private static final StreamCodec<ByteBuf, String> OPTIONAL_URL = nullable(URL);

	private static final StreamCodec<ByteBuf, JamendoTrack> TRACK_CODEC = StreamCodec.composite(
			TEXT, JamendoTrack::id,
			TEXT, JamendoTrack::name,
			TEXT, JamendoTrack::artist,
			ByteBufCodecs.VAR_INT, JamendoTrack::durationSeconds,
			URL, JamendoTrack::audioUrl,
			OPTIONAL_URL, JamendoTrack::imageUrl,
			nullable(TEXT), JamendoTrack::license,
			OPTIONAL_URL, JamendoTrack::licenseUrl,
			URL, JamendoTrack::pageUrl,
			JamendoTrack::new);

	private static final StreamCodec<ByteBuf, Reason> FAILURE_CODEC =
			nullable(ByteBufCodecs.idMapper(id -> Reason.values()[id], Reason::ordinal));

	// list(max) writes the size first, then each track; on reading, a size over the limit fails at once.
	public static final StreamCodec<ByteBuf, JamendoResultsPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, JamendoResultsPayload::requestId,
			FAILURE_CODEC, JamendoResultsPayload::failure,
			TRACK_CODEC.apply(ByteBufCodecs.list(MAX_TRACKS)), JamendoResultsPayload::tracks,
			JamendoResultsPayload::new);

	public static JamendoResultsPayload found(int requestId, List<JamendoTrack> tracks) {
		List<JamendoTrack> fitting = tracks.stream().filter(JamendoResultsPayload::fits).toList();
		if (fitting.size() < tracks.size()) {
			UnliMusic.LOGGER.warn("Left out {} Jamendo tracks too long to send", tracks.size() - fitting.size());
		}
		return new JamendoResultsPayload(requestId, null, fitting.subList(0, Math.min(fitting.size(), MAX_TRACKS)));
	}

	public static JamendoResultsPayload failed(int requestId, Reason failure) {
		return new JamendoResultsPayload(requestId, failure, List.of());
	}

	/**
	 * Whether the codec can write this track. Writing a string over its limit throws, and an exception
	 * while sending would end the player's connection: better one track fewer.
	 */
	private static boolean fits(JamendoTrack track) {
		return fits(track.id(), MAX_TEXT_LENGTH) && fits(track.name(), MAX_TEXT_LENGTH)
				&& fits(track.artist(), MAX_TEXT_LENGTH) && fits(track.license(), MAX_TEXT_LENGTH)
				&& fits(track.audioUrl(), MAX_URL_LENGTH) && fits(track.imageUrl(), MAX_URL_LENGTH)
				&& fits(track.licenseUrl(), MAX_URL_LENGTH) && fits(track.pageUrl(), MAX_URL_LENGTH);
	}

	private static boolean fits(String value, int maxLength) {
		return value == null || value.length() <= maxLength;
	}

	/** A value that may be missing: one byte (present or not), then the value itself when present. */
	private static <T> StreamCodec<ByteBuf, T> nullable(StreamCodec<ByteBuf, T> codec) {
		return ByteBufCodecs.optional(codec).map(optional -> optional.orElse(null), Optional::ofNullable);
	}

	@Override
	public Type<JamendoResultsPayload> type() {
		return TYPE;
	}
}
