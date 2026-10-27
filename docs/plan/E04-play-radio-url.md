# E04 · Play a radio station from a fixed URL

> **Phase 1 — Listening alone** · Difficulty ●●○○○ · Depends on: E03 · Old roadmap: step 2

## In a nutshell

The same as E02, but the source is an online radio station: an HTTP stream that never ends. It proves the networking side works and that we handle "no duration and no seek" properly.

## What you'll see at the end

The test key plays a Portuguese radio station from a fixed URL (taken by hand from Radio Browser). The HUD doesn't exist yet, but the log shows the station's name.

## What gets built

- **Register the `HttpAudioSourceManager`** in lavaplayer. Never `registerRemoteSources`.
- **`RadioSource implements MusicSource`**: no seek, no duration.
- **Reconnection**: if the stream drops, retry with growing waits (1 s, 2 s, 4 s…) and give up after N attempts, with a message to the player.
- **ICY metadata** ("StreamTitle", the song currently on air): investigate whether lavaplayer exposes it. If not, it stays an extra.
- **URL types**: start with direct MP3 and AAC; then `.pls`/`.m3u` playlists; HLS (`.m3u8`) only if lavaplayer supports it well.
- **Connect and read timeouts**, and our own `User-Agent` on stream requests too.
- **Pausing a radio** = on resume, it goes back to live (not to where it stopped).

## New concepts

- **HTTP streaming** and why there is always a few seconds of delay (buffer).
- **ICY / Shoutcast**: the "protocol" of online radio, with metadata mixed into the audio.
- **Exponential backoff**: waiting longer and longer between attempts, so you don't hammer a server that's down.

## Suggested steps

1. Register the HTTP source manager and play a direct MP3 URL.
2. `RadioSource` integrated into the E03 `MusicPlayer`.
3. Turn off the internet midway → reconnection and messages.
4. Test an AAC station and a `.pls` playlist.
5. Investigate ICY metadata.

## Done when

- [ ] A radio station plays for 10 minutes or more without dropouts.
- [ ] No internet: error message, no crash, and it retries.
- [ ] Stop/pause/resume work as described.
- [ ] The game never freezes while the radio connects.

## Pitfalls

- URLs that redirect; HTTPS certificates; stations that take a long time to start.
- **Pause (found in E03):** `MusicPlayer.pause()` pauses the OpenAL channel in place (D6). For a radio that would resume stale audio, so resume must reopen the stream live (`play(track, 0)`, `TrackInfo.isLive()`).
- **HE-AAC (AAC+)**, common on radio: confirm whether lavaplayer decodes it.
- 48 kHz and mono stations: the output format is always ours (44.1 kHz stereo, D12), lavaplayer converts.
- **Prebuffer before playing** (found in E02): `PcmAudioStream.read()` blocks the sound engine's thread while it waits for frames, and the engine asks for 4 s of audio as soon as the stream is attached. A stream that arrives in real time would freeze every game sound, so wait until lavaplayer has a few seconds buffered before creating the sound instance.

## APIs

- **To confirm:** timeout and `User-Agent` configuration in 2.2.x's `HttpAudioSourceManager`; lavaplayer events for error/end (`TrackExceptionEvent`, `TrackEndEvent`?); ICY metadata support.

## Session kickoff prompt

> Let's do entry E04 of the plan: read `docs/plan/E04-play-radio-url.md`. We'll start with a direct MP3 URL from a Portuguese radio station.

## Session notes

*(fill in at the end: decisions made, what was left to do)*
