# E02 · Play a fixed local file (the pipeline)

> **Phase 1 — Listening alone** · Difficulty ●●●●○ · Depends on: E01 · Old roadmap: step 1

## In a nutshell

The most important piece of the mod. You press a key and hear an MP3 from your disk inside the game, with the volume following the "Jukebox/Note Blocks" slider. If this works, everything else is just "swapping the source" of the sound.

```
file.mp3 ─► lavaplayer ─► 16-bit LE PCM, 48 kHz, stereo ─► PcmAudioStream ─► MusicSoundInstance ─► Minecraft sound engine
            (decodes)                                       (ours, AudioStream)  (ours, FabricSoundInstance)   (OpenAL)
```

## What you'll see at the end

You press the test key and the song plays from start to finish; moving the Jukebox slider changes the volume.

## What gets built

- **`assets/unlimusic/sounds.json`** with one entry (e.g. `stream`) pointing at `fabric-sound-api-v1:empty` with `"stream": true`. It's the "dummy sound" the Fabric API recommends for sounds with their own audio.
- **`LavaplayerEngine`** (client): `AudioPlayerManager` with only the local source manager (HTTP comes in E04); 16-bit little-endian PCM output format; asynchronous track loading.
- **`PcmAudioStream implements AudioStream`**:
  - `getFormat()` → `javax.sound.sampled.AudioFormat(48000, 16, 2, true, false)` (signed, little-endian)
  - `read(int)` → returns a `ByteBuffer` with the next chunk of PCM
  - `close()` → releases everything
- **`MusicSoundInstance extends AbstractSoundInstance`**, overriding `getAudioStream(...)` to return the `PcmAudioStream`. Category `SoundSource.RECORDS`, relative to the player and with no attenuation (sounds the same everywhere).
- **Temporary "play test" key** with a fixed path (in a dev file outside Git, not in the code).

## New concepts

- **PCM**, sample rate, channels, **little vs big-endian**.
- The sound engine runs on its own thread and **pulls** data by calling `read()`. We don't push it.
- **Stream vs loaded sound**: normal sounds go into memory whole; a long song has to go in chunks.
- **`SoundSource`**: volume category. `RECORDS` = Jukebox slider.

## Suggested steps

1. `sounds.json` + a sound instance that plays `empty` with no "unknown sound" error in the log.
2. **A `PcmAudioStream` that generates a tone** (sine wave, e.g. 440 Hz). It proves the `AudioStream` **without** lavaplayer: if the tone plays, the Minecraft side is right.
3. Hook up lavaplayer: file → PCM → stream.
4. Confirm the RECORDS slider controls the volume.

## Done when

- [ ] The test tone plays.
- [ ] A local MP3 plays from start to finish without dropouts.
- [ ] The Jukebox slider changes the volume.
- [ ] The game doesn't freeze when the key is pressed.

## Pitfalls

- `read()` is called by the sound thread. If it blocks for too long there are dropouts; if it returns an empty buffer, Minecraft thinks the song has ended.
- **OpenAL only positions mono sound.** Stereo always plays "inside the player's head": great for background music. Jukebox mode (E17) will need mono.
- 44.1 kHz files have to be converted to 48 kHz (lavaplayer does it, possibly with natives; see E01).
- Paths with accented characters on Windows.

## APIs

- **Confirmed (26.3):** `FabricSoundInstance.getAudioStream(SoundBufferLibrary, Identifier, boolean)`, `FabricSoundInstance.EMPTY_SOUND`, `AudioStream` (`getFormat()`, `read(int)`, `close()`), `AbstractSoundInstance`, `SoundSource.RECORDS`.
- **To confirm:** how to play the instance (`SoundManager.play`?); the `AbstractSoundInstance` constructor; in lavaplayer 2.2.x, whether `AudioPlayerInputStream` or reading frames with `player.provide()` is better; the PCM format class (e.g. `Pcm16AudioDataFormat`).

## Session kickoff prompt

> Let's do entry E02 of the plan: read `docs/plan/E02-play-local-file.md`. I really want to start with the test tone before hooking up lavaplayer, and I want you to explain each piece of the sound path to me.

## Session notes

*(fill in at the end: decisions made, what was left to do)*
