# E10 · Shared mode: server state and `/music` commands

> **Phase 3 — Listening together** · Difficulty ●●●●○ · Depends on: E08 · Old roadmap: step 5

## In a nutshell

The server becomes "the DJ": it keeps the queue, what's playing and since when, and tells every player. `/music` commands to control it. Only Jamendo and radio (local files are never shared).

## What you'll see at the end

`/music radio <name>` and every player hears the station. `/music jamendo <query>`, `/music skip`, `/music stop`, `/music pause`, `/music queue`. The queue moves on by itself when a track ends.

## What gets built

- **`SharedMusicState`** (server): current track, start time (server clock), pause (and the position it paused at), queue.
- **Automatic advance**: the server knows the duration of Jamendo tracks and moves to the next one when the current one ends. Use the server tick, or a scheduler that always goes back to the server thread.
- **S2C packets**: `SharedPlay(source, track, positionMs, serverTime)`, `SharedPause`, `SharedStop`, `QueueUpdate`.
- **Players joining midway** get the current state right away.
- **Commands** with Brigadier, with suggestions (autocomplete) from results.
- **Scope (decision D4)**: recommendation = whole server first.
- Saving the queue across server restarts: optional.

## New concepts

- **Source of truth**: the server decides, the clients obey.
- **Server clock**: why not to use each client's clock.
- **Broadcast**: sending the same packet to everyone.

## Suggested steps

1. `SharedMusicState` + `/music radio` command → packet to everyone → everyone plays.
2. `/music stop` and `/music pause`.
3. Jamendo and queue, with automatic advance.
4. Send the state to players who join.
5. Command suggestions.

## Done when

- [ ] With 2 clients connected to `runServer`, both hear the same thing.
- [ ] skip/stop/pause reach everyone.
- [ ] The queue advances by itself.
- [ ] A player joining midway starts hearing it (fine sync is E11).

## Pitfalls

- Schedulers that run on another thread and touch the state at the same time as a command.
- Live radio has no end: the queue only advances with skip.
- What happens to shared music when the last player leaves? (Stop, so no quota is wasted.)

## APIs

- **Confirmed (26.3):** `CommandRegistrationCallback`, `PlayerLookup`, `ServerPlayNetworking`.
- **To confirm:** `ServerPlayConnectionEvents.JOIN`, `ServerTickEvents`, `SavedData` (if the queue is saved), Brigadier suggestions.

## Session kickoff prompt

> Let's do entry E10 of the plan: read `docs/plan/E10-shared-mode-server.md`. First we decide D4 and then we start with `/music radio` playing for everyone.

## Session notes

*(fill in at the end: decisions made, what was left to do)*
