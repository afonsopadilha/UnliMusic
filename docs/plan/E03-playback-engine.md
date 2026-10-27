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

- [x] Pause/resume/stop 20 times in a row with no errors in the log.
- [x] Playing another track midway never leaves two playing.
- [x] Leaving and rejoining the world leaves everything clean.
- [x] No lavaplayer thread stays alive after stopping (checked with a thread dump, `jcmd <pid> Thread.print`).

## Pitfalls

- In singleplayer, pressing ESC pauses the game and Minecraft pauses sounds. Seen in E02: the engine pauses our channel and stops calling `read()`, lavaplayer keeps its buffer full, and playback resumes with no warning.
- The `/stopsound` command and other things in the game can stop our sound instance without us knowing → `MusicPlayer` has to detect it.
- Pausing a radio "in place" makes no sense: on resume, it goes back to live (handle in E04).

## APIs

- **Confirmed in 26.3:** `ClientPlayConnectionEvents.DISCONNECT` → `(ClientPacketListener, Minecraft)` (its thread is not documented, so we hop over with `execute`); `ClientLifecycleEvents.CLIENT_STOPPING`; `Minecraft.execute(Runnable)` (from `BlockableEventLoop`) to get back to the game thread; `SoundManager.isActive(SoundInstance)`; `SoundEngine.PlayResult` (`STARTED`, `STARTED_SILENTLY`, `NOT_STARTED`); `LocalPlayer.sendOverlayMessage(Component)` (line above the hotbar); lavaplayer `AudioTrack.setPosition(ms)` before `startTrack` (kept by the primordial executor and applied on start).

## Session kickoff prompt

> Let's do entry E03 of the plan: read `docs/plan/E03-playback-engine.md`. Explain the state machine to me before writing it, and test both pause options (D6).

## Session notes

**27 Oct 2026**

- **State machine** (`client/audio/MusicPlayer`): `STOPPED`, `LOADING`, `PLAYING`, `PAUSED`, `ERROR`. The public methods ignore requests that make no sense (pause while stopped); a table of allowed arrows inside `setState` throws on anything else, because that would be a bug in the class. One log line per transition. Game thread only: lavaplayer's callbacks come back through `client.execute`, so no locks.
- **Never two tracks:** every load and stop bumps a `loadId`; a track that finishes loading with an old number is closed and thrown away. Rapid J presses (several per second) tested.
- **`MusicSource` only opens tracks** (`open(track, positionMs, onReady, onError)`), unlike the `play/pause/stop` interface `CLAUDE.md` suggested: pausing, stopping and noticing the end are the same for every source, so they live once in `MusicPlayer`. `CLAUDE.md` updated. First source: `LocalFileSource` (title/artist from the tags, the file name when lavaplayer says "Unknown title").
- **`TrackInfo`** is a record in `src/main` (`com.unlimusic.music`), because shared mode will send it between server and clients. `durationMs` is 0 when unknown; `isLive()` means radio.
- **Position clock:** lavaplayer decodes ~4 s ahead (OpenAL queues four 1 s buffers), so its position is not what is heard. `MusicPlayer.positionMs()` runs a clock that starts when the engine first calls `read()` (`PcmAudioStream.startedNanos()`, a `volatile` written on the sound thread) and stops on pause and while the game is paused (`Minecraft.isPaused()`). The HUD (E05) can call it every frame.
- **End vs stopped by the game:** every tick, `SoundManager.isActive(sound)` turning false means the engine dropped our sound; `PcmAudioStream.hasEnded()` tells "track ended" from "sound stopped by the game". Tested with `/stopsound @s` (it needs a target, and commands need cheats or Open to LAN). Leaving the world also stops through `DISCONNECT`.
- **Decision D6: pause the channel in place.** Both options were built and tested by ear:
  - *Stop and reopen with a seek* (B) rolled back a few seconds on every resume with a VBR MP3, a different amount each time. Cause, read in lavaplayer 2.2.7: `Mp3XingSeeker` jumps using the Xing table (100 points, each stored as 1/256 of the file, rounded down), so it lands early, and the samples it then skips are counted from the wrong place. A FLAC resumed exactly. Most MP3s (Jamendo's too) are VBR, so B was out.
  - *Pause the OpenAL channel* (A) is exact with any format. Its catch: `Gui.setScreen(null)` (closing chat, inventory or the pause menu) calls `SoundManager.resume()`, which unpauses **every** channel. Re-pausing on the next tick left an audible hiccup; an inject at the end of `SoundEngine.resume()` (`SoundEngineMixin`) queues our pause right behind it on the sound thread, and the hiccup is gone.
- **First mixins** (`client/mixin`, `unlimusic.client.mixins.json`, client-only in `fabric.mod.json`): `SoundManagerAccessor` (`soundEngine`), `SoundEngineAccessor` (`instanceToChannel`), `SoundEngineMixin`. Pausing goes through `ChannelAccess.ChannelHandle.execute(Channel::pause)`, on the sound thread.
- **Keys** (all through `SinglePress`): J plays the test song from the start, K pause/resume, U stop; all free in vanilla 26.3 (M is left for the screen, E09; O is now "friends"). Until the HUD exists, `StatusMessages` shows loading/playing/paused/stopped above the hotbar and errors in chat.
- **Removed:** `ToneAudioStream` (E02's tone); `TestPlayback` now only reads the dev file and calls `MusicPlayer`.
- **Threads:** after stopping, the only lavaplayer playback thread left is the pool's idle worker (`ThreadPoolExecutor.getTask`, core size 1); no decoder.
- **Not verified:** the thread dump after the rapid J presses (the game had closed); `prodClient`/launcher with the new mixins (E18).
- **For E04:** pausing a radio in place would resume stale audio, so radio needs its own resume: reopen the stream live with `play(track, 0)` instead of unpausing.
- **For E05:** subscribe with `MusicPlayer.addListener` and read `state()`, `track()`, `positionMs()`, `error()`; the HUD replaces `StatusMessages`.
