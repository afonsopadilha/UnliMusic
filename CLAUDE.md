# Minecraft music mod

Fabric mod for listening to music inside the game, on your own or synced across every player in the same world. Personal project; I'm new to modding, so explain the decisions and not just the code.

## Language

- Everything in the repository (code, comments, docs, README, commit messages) is written in English. In-game text goes through the language files (`en_us` and `pt_pt`).

## Stack

- Minecraft Java 26.3 (decision D1), Fabric Loader + Fabric API, Java 25, Gradle (project generated from the official Fabric template).
- Since 26.1 Minecraft is no longer obfuscated: it uses the official names from Minecraft's code. **Don't use Yarn** or examples/tutorials from old versions.
- Before using any Minecraft or Fabric API, confirm the name and signature in the Fabric documentation for the exact version in `gradle.properties` (or in the decompiled code in the IDE). Don't guess from memory.
- Mod ID: `unlimusic` (see `fabric.mod.json`).

## Music sources (closed decision)

Only these three, all legal and with no account/Premium needed by players:

1. The player's **local files**.
2. **Jamendo** (Creative Commons music, API v3.0, needs a client ID).
3. **Online radio** via **Radio Browser** (no key; search by country, e.g. `countrycode=PT`).

**Forbidden:** YouTube, Spotify, Deezer or any extraction/scraping of commercial services. In lavaplayer, register only the HTTP and local source managers; never call `AudioSourceManagers.registerRemoteSources(...)`.

## Modes

- **Shared** (server in charge): Jamendo and radio. The server keeps the queue/state and sends clients "track X from time T". Radio = live stream, no seek (everyone tunes in). Jamendo = sync by track ID + start time, each client seeks.
- **Personal** (only the player themselves): local files, and also Jamendo/radio.
- Local files are NOT shareable (they only exist on one PC). Possible future: a music folder on the server served to clients.
- The mod has to be on the server and on every client for shared mode.

## Audio architecture

- `MusicSource` interface (or equivalent) with `play(track, position)`, `pause()`, `stop()`; each source implements it.
- Decoding: **lavaplayer** (`dev.arbjerg:lavaplayer`), 16-bit little-endian PCM output format (not Opus).
- PCM → our own `AudioStream` → sound instance via the Fabric API (`FabricSoundInstance` lets you provide an `AudioStream`). That way the volume follows a game slider (Jukebox/Records) and there's an option for positional jukebox-style sound.
- All slow work (network, decoding, API requests) runs off the game thread. Never block in `ClientTickEvents`.

## Notes per source

- **Jamendo:** free quota for non-commercial apps (~35,000 requests/month) shared by every player using the client ID: run searches on the server and cache them. Always credit artist and license in the HUD. Non-commercial use.
- **Radio Browser:** send our own `User-Agent` on requests.
- **Local:** folder chosen in the settings; use tags (title, artist, cover) for the HUD.

## Build

- Gradle runs on JDK 25 because of `gradle/gradle-daemon-jvm.properties`; the JDK 17 on the PATH is not used.
- lavaplayer and its dependencies (HTTP client, Jackson, natives) go inside the mod jar through the `bundled` configuration in `build.gradle`: Loom's `include` alone is not transitive. Libraries Minecraft already ships (slf4j, commons-io) and those only used by sources we don't register are excluded there.
- MP3, AAC, Vorbis and resampling are decoded by lavaplayer's native `connector` library (`lavaplayer-natives`, no JitPack). It exists for Windows x64/x86, Linux (x64, ARM, musl) and macOS (Intel + Apple Silicon), **not Windows ARM64**.
- `ClassNotFoundException` at runtime = dependency missing from the jar. `runClient` can't catch that: test the built jar with `./gradlew prodClient` (Fabric's real launcher, in `run-prod/`).

## Secrets

- The Jamendo client ID and any other key live in a config file outside Git (add it to `.gitignore`).

## Roadmap

Full plan in `docs/plan/README.md`. Each entry is one work session and has its own file in `docs/plan/` (what to do, how to test, APIs to confirm, open decisions). The old roadmap (steps 0–6) is fully covered by these entries.

**Phase 0 — Foundations**
- [x] E00 Clean up the project and first key (test keybind in chat)
- [x] E01 Bundle lavaplayer inside the jar

**Phase 1 — Listening alone**
- [ ] E02 Play a fixed local file (lavaplayer → AudioStream → sound instance pipeline)
- [ ] E03 Playback engine: pause, stop, end of track
- [ ] E04 Play a radio station from a fixed URL
- [ ] E05 "Now playing" HUD

**Phase 2 — Finding music**
- [ ] E06 Radio search (Radio Browser)
- [ ] E07 Server-side Jamendo client, with cache
- [ ] E08 Networking: client ↔ server packets
- [ ] E09 Main screen: radios and Jamendo

**Phase 3 — Listening together**
- [ ] E10 Shared mode: server state and `/music`
- [ ] E11 Synchronization on the clients
- [ ] E12 Permissions, security and limits
- [ ] E13 Shared queue on screen

**Phase 4 — Local files and settings**
- [ ] E14 Local file library
- [ ] E15 Cover art and images (optional)
- [ ] E16 Settings and configuration

**Phase 5 — Extras**
- [ ] E17 Positional jukebox-style sound (optional)

**Phase 6 — Release**
- [ ] E18 Real testing: multiplayer, dedicated server, other systems
- [ ] E19 Polish: translations, messages, errors
- [ ] E20 Publishing

## How we work

- Each session starts by reading its entry in `docs/plan/` and confirms the APIs marked "To confirm" before writing code.
- Small steps; run the client (`./gradlew runClient`) and confirm it works before moving on.
- At the end of the session: fill in "Session notes" in the entry, record decisions made (the "Open decisions" table in `docs/plan/README.md`) and tick the entry in the roadmap above.
