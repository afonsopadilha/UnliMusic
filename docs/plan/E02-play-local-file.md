# E02 · Play a fixed local file (the pipeline)

> **Phase 1 — Listening alone** · Difficulty ●●●●○ · Depends on: E01 · Old roadmap: step 1

## In a nutshell

The most important piece of the mod. You press a key and hear an MP3 from your disk inside the game, with the volume following the "Jukebox/Note Blocks" slider. If this works, everything else is just "swapping the source" of the sound.

```
file.mp3 ─► lavaplayer ─► 16-bit LE PCM, 44.1 kHz, stereo ─► PcmAudioStream ─► MusicSoundInstance ─► Minecraft sound engine
            (decodes)                                       (ours, AudioStream)  (ours, FabricSoundInstance)   (OpenAL)
```

## What you'll see at the end

You press the test key and the song plays from start to finish; moving the Jukebox slider changes the volume.

## What gets built

- **`assets/unlimusic/sounds.json`** with one entry (e.g. `stream`) pointing at `fabric-sound-api-v1:empty` with `"stream": true`. It's the "dummy sound" the Fabric API recommends for sounds with their own audio.
- **`LavaplayerEngine`** (client): `AudioPlayerManager` with only the local source manager (HTTP comes in E04); 16-bit little-endian PCM output format; asynchronous track loading.
- **`PcmAudioStream implements AudioStream`**:
  - `getFormat()` → `javax.sound.sampled.AudioFormat(44100, 16, 2, true, false)` (signed, little-endian; see D12)
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

- [x] The test tone plays.
- [x] A local MP3 plays from start to finish without dropouts.
- [x] The Jukebox slider changes the volume.
- [x] The game doesn't freeze when the key is pressed.

## Pitfalls

- `read()` is called by the sound thread. If it blocks for too long there are dropouts; if it returns an empty buffer, Minecraft thinks the song has ended.
- **OpenAL only positions mono sound.** Stereo always plays "inside the player's head": great for background music. Jukebox mode (E17) will need mono.
- Files at other sample rates (e.g. 48 kHz) are converted to our 44.1 kHz by lavaplayer, with the native resampler (see E01).
- Paths with accented characters on Windows.

## APIs

- **Confirmed (26.3):** `FabricSoundInstance.getAudioStream(SoundBufferLibrary, Identifier, boolean)`, `FabricSoundInstance.EMPTY_SOUND`, `AudioStream` (`getFormat()`, `read(int)`, `close()`), `AbstractSoundInstance`, `SoundSource.RECORDS`.
- **Confirmed in this session:** `Minecraft.getSoundManager().play(SoundInstance)` → `SoundEngine.PlayResult`; `AbstractSoundInstance(Identifier, SoundSource, RandomSource)` with `SoundInstance.createUnseededRandom()`; `SoundInstance.canStartSilent()`; `player.provide(timeout, unit)` rather than `AudioPlayerInputStream`; `StandardAudioDataFormats.COMMON_PCM_S16_LE` (a `Pcm16AudioDataFormat`) and `AudioDataFormatTools.toAudioFormat(...)`; `ClientLifecycleEvents.CLIENT_STOPPING`.

## Session kickoff prompt

> Let's do entry E02 of the plan: read `docs/plan/E02-play-local-file.md`. I really want to start with the test tone before hooking up lavaplayer, and I want you to explain each piece of the sound path to me.

## Session notes

**26 Oct 2026**

- **Two commits:** the test tone first (`bd24f60`), then lavaplayer. From now on an entry can have several commits, one per step that works; only the last one carries `Closes #N`.
- **How the engine pulls a stream** (read in the 26.3 bytecode, `com.mojang.blaze3d.audio.Channel`): `attachBufferStream` calls `read(size)` 4 times up front, each asking for **one second** of audio (`sampleRate × channels × bytes per sample`, 176,400 bytes for us), and queues them in OpenAL. Every tick it unqueues the buffers that finished and asks for the same number again. A buffer that comes back empty is simply not queued, so the queue shrinks and the sound ends once it drains. `read()` and `close()` run on the sound engine's thread; the engine closes the stream itself when the channel is destroyed.
- **`"stream": true` is what matters** in `sounds.json`: it sends the sound through `SoundBufferLibrary.getStream`, which the Fabric API redirects to `getAudioStream`. Without it the engine loads the empty `.ogg` as a static buffer and never asks us for audio.
- **`canStartSilent()` → true:** `SoundEngine.play` skips a sound whose volume is 0 ("Skipped playing sound …, volume was zero") unless the instance says it can start silent. Without it, pressing play with the Jukebox slider at 0 would do nothing even after raising it.
- **Direct `ByteBuffer`s:** OpenAL copies from native memory (vanilla uses `BufferUtils.createByteBuffer`). The tone writes samples with `putShort`, so its buffer is set to `LITTLE_ENDIAN`; lavaplayer's bytes are already little-endian and are copied as they are.
- **Sample rate 44.1 kHz, not 48 kHz** → decision D12. `StandardAudioDataFormats.COMMON_PCM_S16_LE`, which E01 already used, is `Pcm16AudioDataFormat(2, 44100, 960, false)`: stereo, 44.1 kHz, 20 ms frames of 3,840 bytes.
- **`player.provide(timeout)` instead of `AudioPlayerInputStream`:** read in the 2.2.7 sources, the input stream never reports the end of a track (with `provideSilence` false it loops forever waiting; with true it returns silence forever). `PcmAudioStream` asks for 100 ms at a time and treats `getPlayingTrack() == null` as the end (lavaplayer clears it when it hands out the terminator frame). A 5 s stall limit keeps a stuck decoder from freezing the sound thread, and with it every game sound.
- **Built:** `sounds.json` (`unlimusic:stream`), `MusicSoundInstance` (RECORDS, relative, no attenuation), `ToneAudioStream` (440 Hz, 5 s), `LavaplayerEngine` (local source only, natives loaded at startup, replaces E01's `LavaplayerCheck`, shut down on `CLIENT_STOPPING`), `PcmAudioStream`, `TestPlayback` (the J key; toggles play/stop).
- **Test file outside Git:** `config/unlimusic-dev-track.txt` (in `run/`, ignored), created with a comment on the first press. Plain text, not `.properties`, because `.properties` treats Windows backslashes as escapes. Read as UTF-8, with Notepad's BOM and Explorer's "Copy as path" quotes stripped. With no path in it, J plays the tone.
- **Results (`runClient`, Windows 11 x64):** the tone plays; an MP3 plays from start to finish without dropouts, also while walking around and loading chunks; at the end the sound stops by itself and J plays it again from the start; the Jukebox slider changes the volume; loading is instant and the game never freezes. Pausing the game (ESC) pauses the channel and no warning appears. No errors in the log.
- **Not tested yet:** `prodClient` / a real launcher with this code (nothing new is bundled, but E18 should check); a path with accented characters.
- **For E03:** ESC already pauses our channel through the engine (it stops calling `read()`, lavaplayer just keeps its buffer full). `/stopsound` and leaving the world are the cases to detect.
- **For E04:** `read()` blocks the sound engine's thread while it waits for frames, and the engine wants 4 s of audio as soon as the stream is attached. Fine for local files (decoding is far faster than real time); a radio stream arrives in real time, so it has to be prebuffered **before** the sound instance is created.
