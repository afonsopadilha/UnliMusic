# E12 · Permissions, security and limits

> **Phase 3 — Listening together** · Difficulty ●●●○○ · Depends on: E10 · Old roadmap: step 5

## In a nutshell

On a server with other people, not everyone should be able to change everyone's music. And a modified client must not be able to make other people's PCs open arbitrary links or burn through the Jamendo quota.

## What you'll see at the end

A player without permission gets "no permission". Too many requests get throttled. Nothing a client sends reaches the others without going through the server.

## What gets built

- **Who's in control (decision D3)**: configurable OP level, everyone, or a player list. Voting to skip stays an idea.
- **Radio in shared mode**: the client sends only the station UUID; the server checks it with Radio Browser and uses the URL *Radio Browser* returns. **Never** forward a URL typed by a player.
- **Jamendo**: the client sends the track id; the server resolves the URL.
- **Limits**: searches per player per minute; maximum size of text and lists in packets; queue size per player.
- **Validation** of everything that arrives by packet.
- **Log** of who changed what.

## New concepts

- **Never trust the client**: any packet may have been forged.
- **Rate limiting**.
- **Permissions** in Minecraft (OP levels).

## Suggested steps

1. Permission checks on commands and on control packets.
2. Radio by UUID, resolved by the server.
3. Search and queue limits.
4. Size validation on every payload.
5. Configuration in `unlimusic-server.json`.

## Done when

- [ ] No permission → message, and the state doesn't change.
- [ ] A packet with a forged URL is ignored (test with a debug command).
- [ ] Search spam is throttled without affecting other players.

## Pitfalls

- Checking permissions only on the client's screen isn't enough: it has to be on the server.
- In singleplayer the player is the "owner": don't block them.

## APIs

- **To confirm:** how to check the permission level of a `CommandSourceStack` and a `ServerPlayer` in 26.3 (the permissions API changed in recent versions).

## Session kickoff prompt

> Let's do entry E12 of the plan: read `docs/plan/E12-permissions-security.md`. First we decide D3; then we handle the rule "the server never forwards URLs from players".

## Session notes

*(fill in at the end: decisions made, what was left to do)*
