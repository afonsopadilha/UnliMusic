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

- [ ] Search via packet in singleplayer and on a local dedicated server.
- [ ] Server without the mod → the client doesn't crash and shows a warning.
- [ ] Strings and lists in packets have a maximum size.
- [ ] A Jamendo track plays in personal mode.

## Pitfalls

- Register the payloads in `src/main` (both sides need to know them), but the client handlers in `src/client`.
- A client without the mod joining a server with the mod: confirm the server doesn't send it packets it doesn't know.

## APIs

- **Confirmed (26.3):** `PayloadTypeRegistry.serverboundPlay()` / `clientboundPlay()`, `ServerPlayNetworking`, `ClientPlayNetworking`, `PacketSender`, `PlayerLookup`.
- **To confirm:** the payload interface (`CustomPacketPayload`?) and `StreamCodec` in 26.3; how to tell whether the server accepts a channel (`ClientPlayNetworking.canSend`?).

## Session kickoff prompt

> Let's do entry E08 of the plan: read `docs/plan/E08-networking-packets.md`. Start with a "ping" packet and explain each piece to me (payload, codec, registration, handler).

## Session notes

*(fill in at the end: decisions made, what was left to do)*
