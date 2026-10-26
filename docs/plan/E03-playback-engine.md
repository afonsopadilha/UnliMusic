# E03 · Playback engine: pause, stop and end of track

> **Phase 1 — Listening alone** · Difficulty ●●●○○ · Depends on: E02 · Old roadmap: step 3 (keys)

## In a nutshell

Turn the "test that plays a file" into a real player: a single place on the client that knows what's playing, and keys to pause, resume and stop. This entry also defines the common `MusicSource` interface, so that local files, radios and Jamendo all behave the same way.

## What you'll see at the end

One key pauses and resumes at the same spot; another stops the music; leaving the world stops the music; when the track ends, nothing is left "hanging".

## What gets built

- **`TrackInfo`** (record): title, artist, duration (or "live"), source type, credit/license, cover URL. Every screen and the HUD will use this.
- **`MusicSource`** (interface, as in `CLAUDE.md`): `play(track, position)`, `pause()`, `resume()`, `stop()`. First implementation: `LocalFileSource` (the E02 code, tidied up). Radio and Jamendo come later.
- **`MusicPlayer`** (client, just one): state (`STOPPED`, `LOADING`, `PLAYING`, `PAUSED`, `ERROR`), current track, current position and events for anyone who wants to listen (the E05 HUD).
- **Pause (decision D6)**: pause lavaplayer while keeping the Minecraft sound alive, or stop the sound and recreate it on resume with a seek. Test both and explain why you chose one.
- **Cleanup**: when leaving the server/world and when closing the game, stop everything and release threads and streams.
- **Thread rule**: networking and decoding on their own threads; anything that touches Minecraft goes back to the game thread.
- **Keys** for pause/resume and stop (with translations).

## New concepts

- **State machine**: only some transitions are valid (you can't pause what is stopped).
- **Thread safety**: `volatile`, concurrent queues, and why two places touching the same object at the same time cause rare bugs.
- **Observer pattern**: the HUD "subscribes" to changes instead of asking every frame.

## Suggested steps

1. Create `TrackInfo`, `MusicSource` and move the E02 code into `LocalFileSource`.
2. `MusicPlayer` with states and a log line for each transition.
3. Stop + cleanup when leaving the world.
4. Pause/resume (decision D6).
5. Keys.

## Done when

- [ ] Pause/resume/stop 20 times in a row with no errors in the log.
- [ ] Playing another track midway never leaves two playing.
- [ ] Leaving and rejoining the world leaves everything clean.
- [ ] No lavaplayer thread stays alive after stopping (check in the debugger).

## Pitfalls

- In singleplayer, pressing ESC pauses the game and Minecraft pauses sounds. See what that does to our stream.
- The `/stopsound` command and other things in the game can stop our sound instance without us knowing → `MusicPlayer` has to detect it.
- Pausing a radio "in place" makes no sense: on resume, it goes back to live (handle in E04).

## APIs

- **To confirm:** `ClientPlayConnectionEvents.DISCONNECT`, `ClientLifecycleEvents.CLIENT_STOPPING`, how to get back to the game thread (`Minecraft.getInstance().execute(...)`?), how to tell whether a sound instance is still active (`SoundManager.isActive`?).

## Session kickoff prompt

> Let's do entry E03 of the plan: read `docs/plan/E03-playback-engine.md`. Explain the state machine to me before writing it, and test both pause options (D6).

## Session notes

*(fill in at the end: decisions made, what was left to do)*
