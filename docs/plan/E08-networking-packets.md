# E08 · Networking: packets between client and server

> **Phase 2 — Finding music** · Difficulty ●●●○○ · Depends on: E04, E07 · Old roadmap: step 5

## In a nutshell

Teach the client and the server to talk to each other with custom messages ("packets"). It's the basis for Jamendo search on screen and for the whole shared mode.

## What you'll see at the end

The client asks the server for a Jamendo search, gets the results and can play a track just for itself (personal mode). Tested in singleplayer and on a local dedicated server.

## What gets built

- **Payloads** (records) with their `StreamCodec`, registered in `src/main` with `PayloadTypeRegistry.serverboundPlay()` / `clientboundPlay()`.
- **Handlers** with `ServerPlayNetworking` (on the server) and `ClientPlayNetworking` (on the client).
- **First packets**:
  - C2S `JamendoSearch(requestId, query)` → S2C `JamendoResults(requestId, tracks)`
  - protocol version, so client and server know whether they understand each other
- **Server without the mod**: the client detects that the server doesn't speak our protocol and disables whatever needs it (decision D9: Jamendo needs the mod on the server).
- **`JamendoSource implements MusicSource`** (client): plays the audio URL over HTTP, with seek (needed in E11).

## New concepts

- **C2S vs S2C**, serialization and what a codec is.
- **Request ids**: matching each answer to the right question (and ignoring stale answers).
- **Network thread**: handlers don't run on the game thread.
- **Never trust the client**: an introduction to what E12 goes into in depth.

## Suggested steps

1. A "ping" packet C2S → S2C with a log line on both sides.
2. Protocol version and detecting a server without the mod.
3. Jamendo search via packet.
4. `JamendoSource` and playing a result.
5. Test with `runServer` + `runClient`.

## Done when

- [x] Search via packet in singleplayer and on a local dedicated server.
- [x] Server without the mod → the client doesn't crash and shows a warning.
- [x] Strings and lists in packets have a maximum size.
- [x] A Jamendo track plays in personal mode.

## Pitfalls

- Register the payloads in `src/main` (both sides need to know them), but the client handlers in `src/client`.
- A client without the mod joining a server with the mod: confirm the server doesn't send it packets it doesn't know.
- **The audio URL may carry the server's client ID** (see the E07 pitfall). Jamendo's terms (clause 2) say the ID must not be disclosed to third parties, and here it would reach every player. Confirm what the URL looks like; if the ID is in it, decide in this session whether that is acceptable (it is how any app that plays Jamendo on the user's device works) or whether to ask Jamendo. The server must not relay the audio to hide it (see E10).
- `JamendoResults` must also carry the track's page URL, for the credit link (E05, E09).

## APIs

- **Confirmed (26.3):** `PayloadTypeRegistry.serverboundPlay()` / `clientboundPlay()`, `ServerPlayNetworking`, `ClientPlayNetworking`, `PacketSender`, `PlayerLookup`.
- **To confirm:** the payload interface (`CustomPacketPayload`?) and `StreamCodec` in 26.3; how to tell whether the server accepts a channel (`ClientPlayNetworking.canSend`?).

## Session kickoff prompt

> Let's do entry E08 of the plan: read `docs/plan/E08-networking-packets.md`. Start with a "ping" packet and explain each piece to me (payload, codec, registration, handler).

## Session notes

**29 Oct – 3 Nov 2026**

- **Built:** in `src/main/.../network`, the payloads `HelloPayload` (both ways), `PingPayload`/`PongPayload` and `JamendoSearchPayload`/`JamendoResultsPayload`, all registered in `ModNetworking` with the server's handlers. In `src/client/.../network`, `ClientNetworking`, `ServerSupport` (what the current server can do) and `JamendoSearches` (request ids). `JamendoSource` and `Prebuffering` in `src/client/.../audio`. Test commands `/unlimusic ping`, `/unlimusic jamendo search|tag|play`.
- **Confirmed (26.3, Fabric API 0.162.0 / networking 6.3.9):** `CustomPacketPayload` with `Type<T>(Identifier)` (not `createType(String)`, which puts the id in the `minecraft:` namespace); `StreamCodec.composite` (up to 12 fields), `ByteBufCodecs.LONG/VAR_INT/stringUtf8(max)/optional/idMapper/list(max)`; `PayloadTypeRegistry.serverboundPlay()/clientboundPlay().register(type, codec)`; `ServerPlayNetworking/ClientPlayNetworking.registerGlobalReceiver`, `send`, `canSend(Type)`; `Context.player()/server()/responseSender()`; `ClientPlayConnectionEvents.JOIN/DISCONNECT`, `ServerPlayConnectionEvents.DISCONNECT`; `ServerPlayer.hasDisconnected()`.
- **Threads (corrects "New concepts" above):** in this Fabric version the play handlers run on the server thread (server) and the render thread (client), not on the network thread. Slow work still goes elsewhere: the Jamendo search runs on a virtual thread and hops back with `server.execute`.
- **Detecting the server:** the channels each side can receive are exchanged during the configuration phase, so `canSend` is already right when `JOIN` fires (on the game thread, at the end of `handleLogin`). The client then sends `Hello(PROTOCOL_VERSION)`; the server records each player's version (`speaksOurProtocol`, for E10) and answers with its own. `HelloPayload` must never change.
- **Limits:** queries 2–100 characters (checked again on the server), at most 20 tracks per answer, texts 256 and URLs 1024 characters. A track over a limit is left out instead of breaking the send. The client never chooses how many results: always `JamendoClient.RESULTS` (10).
- **Shared code (D14):** `JamendoClient.RESULTS/MIN_QUERY_LENGTH/MAX_QUERY_LENGTH`, `JamendoException.Reason.message()` and `reasonOf`, and `JamendoText` (the chat line) serve both `/music` and the packets.
- **Jamendo playback:** each client streams the audio URL itself. A track pauses in place and seeks like a file, but arrives over the network like a radio: the stream never makes the engine wait and is prebuffered (6 s, about 0.3 s on this connection). The audio URL never reaches logs: `LavaplayerEngine.load` takes a log name, failures are logged without stack trace and with URL queries hidden, and `TrackInfo.toString()` leaves it out. The HUD credit reads "CC … · via Jamendo"; client results play on click, with a `[↗]` link to the track's page.
- **Jamendo answers intermittently empty:** "lofi" gave 0 tracks twice and then 10, with the very same request (same ID, same URL). An empty answer now stays in the cache for 1 minute instead of 1 hour, and the log says when our filter, not Jamendo, left tracks out.
- **Build:** `runServer` now uses `run-server/` (in `.gitignore`): `run/` is the client's, and the two run together. The dedicated server needs `eula=true`, `online-mode=false` and `white-list=false` (26.3 turns the whitelist on by default) for the dev client, and its own copy of `config/unlimusic-server.json`.
- **Results (Windows 11 x64):** singleplayer: handshake READY, pong in 11–44 ms (the integrated server only reads packets once per 50 ms tick), search and playback. Vanilla 26.3 server: the client joins, logs that the mod is missing, and `/unlimusic ping` shows the warning; no packet errors on the server. Dedicated server with the mod (`runServer`): READY, pong in 45 ms, search with 10 tracks, playback (buffered in 357 ms). `from=`, `storage.jamendo` and `client_id` appear 0 times in both logs.
- **Decisions:** D9 confirmed, D21 (when to warn about the server) and D22 (the audio URL goes to the clients); see `README.md`.
- **Left for later:** a track started in its last 6 s (or shorter than 6 s) never fills the prebuffer and fails as "no audio" (matters for E11's late joiners). Two different protocol versions were not tested live. `TrackInfo` has no page URL yet, for the screen's "Open on Jamendo" (E09). Per-player search limits are E12.
