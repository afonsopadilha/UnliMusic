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

- [x] A radio station plays for 10 minutes or more without dropouts (6 min 25 s in one go, judged enough).
- [x] No internet: error message, no crash, and it retries.
- [x] Stop/pause/resume work as described.
- [x] The game never freezes while the radio connects.

## Pitfalls

- URLs that redirect; HTTPS certificates; stations that take a long time to start.
- **Pause (found in E03):** `MusicPlayer.pause()` pauses the OpenAL channel in place (D6). For a radio that would resume stale audio, so resume must reopen the stream live (`play(track, 0)`, `TrackInfo.isLive()`).
- **HE-AAC (AAC+)**, common on radio: confirm whether lavaplayer decodes it.
- 48 kHz and mono stations: the output format is always ours (44.1 kHz stereo, D12), lavaplayer converts.
- **Prebuffer before playing** (found in E02): `PcmAudioStream.read()` blocks the sound engine's thread while it waits for frames, and the engine asks for 4 s of audio as soon as the stream is attached. A stream that arrives in real time would freeze every game sound, so wait until lavaplayer has a few seconds buffered before creating the sound instance.

## APIs

- **Confirmed in this session (2.2.7 sources):** `HttpAudioSourceManager(MediaContainerRegistry)`, `configureBuilder(builder -> builder.setUserAgent(...))` and `configureRequests(config -> RequestConfig.copy(config)...build())` (lavaplayer sets no User-Agent of its own; defaults are 3 s connect/read); `AudioPlayer.addListener` with `TrackExceptionEvent.exception`; non-blocking `AudioPlayer.provide()`; `AudioPlayerManager.setPlayerCleanupThreshold`; `AudioTrackInfo.author` = `icy-name`, `title` = `icy-description` (or `icy-url`); `MediaContainer.asList()` and `AdtsContainerProbe.matchesHints`; `Minecraft.isPaused()`.

## Session kickoff prompt

> Let's do entry E04 of the plan: read `docs/plan/E04-play-radio-url.md`. We'll start with a direct MP3 URL from a Portuguese radio station.

## Session notes

**27 Oct 2026**

- **Built:** `RadioSource` (no seek, no length; ignores the position), `TrackInfo.radio(url, name)`, the HTTP source in `LavaplayerEngine`, a live mode in `PcmAudioStream`, radio pause/reconnect in `MusicPlayer`. The test key's dev file now also takes `<url> [station name]`.
- **HTTP source:** our own `User-Agent` (`UnliMusic/<version> (Minecraft mod; +repo URL)`), connect and read timeouts of 10 s (lavaplayer's 3 s are too short: RFM's servers took over 6 s to send the first byte). lavaplayer handles `.pls`/`.m3u` (it follows the first entry) and HTTP redirects at load time; the track keeps the URL it was given, so every reconnect re-reads the playlist or redirect and can land on a healthier server.
- **Prebuffer:** 6 s of frames are pulled from lavaplayer into the stream (`PcmAudioStream.prebuffer`) on a virtual thread before the sound instance exists, because the engine takes 4 s at once. Stations that burst on connect (Bauer, Observador) fill it in 0.3–2 s; RFM trickles in real time and needs about 6 s.
- **Live streams never block the sound thread:** `nextLiveFrame()` uses the non-blocking `provide()`. When the stream runs dry it hands out silence and **rebuffers** until 3 s have piled up, then plays on. The first version resumed with whatever had arrived, which turned a slow stream into a 1 s on / 1 s off stutter. After 15 s of silence the stream is ended (and reconnected). Once ended, `read()` returns empty at once (the engine asks a few more times).
- **Decision D15 (radio pause and drops):**
  - K pause stops the sound and the connection; resume tunes in again live (`load(track, 0)`). The pause menu (singleplayer, `Minecraft.isPaused()`) for more than 3 s does the same on unpause; a quicker look carries on a few seconds late instead of costing a reconnect.
  - A dropped stream (lavaplayer ends the track) reconnects after 1, 2, 4, 8 and 16 s, then gives up with "lost the connection to the station" in chat. Only a station that has already played is retried (also after a pause or the pause menu); a wrong URL or no internet from the start fails at once. 60 s of playing gives all five attempts back.
- **Found in E03's code:** lavaplayer stops ("cleans up") a player nobody asked for audio for a minute, which is what a paused channel looks like: a local song paused for more than a minute ended about 4 s after resuming. `setPlayerCleanupThreshold(365 days)`; tested with the FLAC paused 81 s, it played to the end.
- **HE-AAC was being detected as MP3.** lavaplayer probes the `Content-Type` first, but its ADTS probe only claims `audio/aac`; HE-AAC stations send `audio/aacp`, fall to the byte scan (MP3 before ADTS), and joining mid-frame often finds a fake MP3 header ("Mp3 frame not found"). On RFM's edge: MP3 14 of 15 times with the default registry, ADTS 14 of 15 with ours (`AacPlusProbe` also claims `audio/aacp` and `audio/x-aac`); the other one failed to connect.
- **HE-AAC decodes correctly:** 33 s of RFM decoded offline into 38 s of PCM (the connect burst included). Its stutters came from StreamTheWorld's servers: one edge (`23603`) delivered 0.67× real time, measured over 70 s; the redirect URL (`playerservices…/livestream-redirect/RFMAAC_SC`) picked edges that kept up, though they were slow and flaky that evening.
- **ICY:** lavaplayer only reads the headers (`icy-name`, `icy-description`). A station opened without a name takes its `icy-name` (RFM, Radio Observador). The song on air (`StreamTitle`) needs the `Icy-MetaData: 1` header, and lavaplayer has no way to strip the metadata out of the audio again, so it stays an extra.
- **Results (`runClient`, Windows 11 x64):** Rádio Comercial (MP3 128k) 6 min 25 s without a gap; M80 80s (AAC); RFM (HE-AAC, `.aac` and redirect); Rádio Observador (`.pls` → Shoutcast MP3); pause/resume back to live, five times; Wi‑Fi off → five attempts → error, no crash; Wi‑Fi back on attempt 5 → played on by itself; ESC over 3 s back to live, quick ESC carries on; local FLAC paused 81 s.
- **Not tested:** HLS (`.m3u8`, used by RTP's Antena 1/3 and Comercial's HLS URL); TSF (48 kHz MP3); `prodClient`/launcher with the HTTP source (E18). Stopping while a station prebuffers frees its connection only when the prebuffer ends (up to 20 s). Reconnect messages above the hotbar fade after a few seconds (E05's HUD).
- **For E05:** `MusicPlayer.reconnectAttempt()` (with `MAX_RECONNECTS`) tells "reconnecting" apart from a first load.
- **For E06:** prefer Radio Browser's `url` over `url_resolved` (decision D16): `url` may be a playlist or a redirect that picks a working server, `url_resolved` pins one edge. Skip HLS stations (`hls=1`) until it is tested.
