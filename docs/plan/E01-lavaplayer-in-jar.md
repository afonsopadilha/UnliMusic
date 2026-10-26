# E01 · Bundle lavaplayer inside the jar

> **Phase 0 — Foundations** · Difficulty ●●●○○ · Depends on: E00 · Old roadmap: —

## In a nutshell

lavaplayer is the library that knows how to open MP3s, radio streams, etc. and turn them into "raw" sound. Players don't have it installed, so it has to go **inside** the mod's jar. This entry is only about the build: getting a jar that works in a real Minecraft, not just in `runClient`.

## What you'll see at the end

The mod starts in a real instance (Modrinth App, Prism or the official launcher with Fabric) and the log shows a line like "lavaplayer ready".

## What gets built

- **Dependency** `dev.arbjerg:lavaplayer` (2.2.7 was the latest on Maven Central in Oct 2026; confirm).
- **Loom's `include` is not transitive.** Including only lavaplayer leaves out everything it depends on. Dependencies of 2.2.7 (according to the pom):
  - compile: `lava-common`, `httpclient` 4.5.14, `jackson-core`, `jackson-databind`
  - runtime: `lavaplayer-natives`, `rhino-engine`, `slf4j-api`, `commons-io`, `jsoup`, `base64`, `org.json`, `annotations`
- Two ways to solve it: list each one with `include`, or create a dedicated Gradle configuration and include everything it resolves. The second scales better; see the Loom docs.
- **Don't duplicate what Minecraft already ships** (`slf4j`, `commons-io`, `gson` and `jackson` show up in the Gradle cache; confirm which ones really come from the game and exclude those).
- **`rhino-engine` and `jsoup`** serve sources we won't use (YouTube and the like). Test whether they can be excluded without breaking anything → smaller jar.
- **Fix `CLAUDE.md`**: it mentions an "AAC decoder via JitPack", but the 2.2.7 pom doesn't have one. Confirm how 2.2.7 decodes AAC (many radio stations are AAC).
- **Smoke test**: on client startup, create an `AudioPlayerManager`, register only the local and HTTP source managers (never `registerRemoteSources`) and write to the log.

## New concepts

- **Transitive dependencies**: the dependencies of your dependencies.
- **Jar-in-jar**: Fabric loads jars that come inside the mod's jar.
- **`runClient` vs a real jar**: in `runClient` Gradle puts everything on the classpath, so it "always works". `ClassNotFoundException`s only show up in the real jar.
- **Native libraries** (`.dll`, `.so`, `.dylib`): code compiled for each operating system.

## Suggested steps

1. Add the dependency with `implementation` only → smoke test in `runClient`.
2. `./gradlew build` → open the jar (it's a zip) and see what's in `META-INF/jars`.
3. Test the jar in a real instance → fix missing classes one by one.
4. Exclude duplicates and whatever isn't needed; test again.
5. Note down the final jar size.

## Done when

- [x] The jar starts in a real instance with no `ClassNotFoundException`.
- [x] Log shows "lavaplayer ready".
- [x] No lavaplayer code in `src/main`.
- [x] `CLAUDE.md` fixed regarding AAC.

## Pitfalls

- **Natives**: lavaplayer uses native libraries for some formats and for sample rate conversion. Confirm which systems they work on (Windows x64, Linux, macOS Intel and Apple Silicon). A Mac may fail where your PC works.
- Different versions of the same library in the game and in the mod → strange runtime errors.
- **Licenses**: lavaplayer is Apache-2.0; distributing it inside the jar means keeping the license notices (left for E20, but note the list now).

## APIs

- **Confirmed (Loom 1.18, Gradle 9.7.1):** `include` is not transitive (Loom docs). `ClientProductionRunTask` (`net.fabricmc.loom.task.prod`) with `mods.from(...)` and `runDir`.
- **Confirmed (lavaplayer 2.2.7, with `javap`):** `new DefaultAudioPlayerManager()`, `registerSourceManager(AudioSourceManager)`, `new LocalAudioSourceManager()`, `new HttpAudioSourceManager()`, `getConfiguration().setOutputFormat(StandardAudioDataFormats.COMMON_PCM_S16_LE)`, `ConnectorNativeLibLoader.loadConnectorLibrary()`, `shutdown()`.

## Session kickoff prompt

> Let's do entry E01 of the plan: read `docs/plan/E01-lavaplayer-in-jar.md`. The goal is a jar that works in a real Minecraft instance, not just in runClient.

## Session notes

**26 Oct 2026**

- **lavaplayer 2.2.7** (still the latest on Maven Central), version in `gradle.properties`.
- **`bundled` configuration** in `build.gradle`: `clientImplementation` extends it (so only `src/client` compiles against lavaplayer), and every jar it resolves to is added to `include` (via `addAllLater`). Going by jars and not by modules skips `jackson-bom`, which is a pom with no jar.
- **Surprise:** excludes declared on a configuration are inherited by the configurations that extend it. With them on `bundled`, `src/client` lost Minecraft's own slf4j and stopped compiling. They now sit on the lavaplayer dependency itself.
- **What Minecraft 26.3 already ships** (`mojang_minecraft_info.json` for the client, `META-INF/libraries.list` in the server jar): slf4j-api 2.0.17 and commons-io 2.20.0 on both; commons-codec 1.22 only on the client; no Jackson, no Apache HttpClient.
- **Excluded:** slf4j-api and commons-io (the game has them); rhino, jsoup, org.json, base64 and commons-codec (`jdeps` shows only the YouTube, Bandcamp, Niconico, getyarn and Yandex sources use them); jetbrains annotations (compile-time only).
- **Bundled (9 jars in `META-INF/jars`):** lavaplayer, lava-common, lavaplayer-natives, httpclient 4.5.14, httpcore 4.4.16, commons-logging 1.2, jackson-core/databind/annotations 2.15.2. Loom warns "(1.2) is not valid semver" for commons-logging; harmless, it still loads.
- **AAC:** no JitPack decoder. `natives/aac/AacDecoder` calls native code inside the `connector` library (fdk-aac), like MP3 (libmpg123), Vorbis and the sample rate converter.
- **Natives:** `connector` exists for win-x86-64, win-x86, linux-x86-64, linux-x86, linux-aarch64, linux-arm/armhf/aarch32, linux-musl x86-64/aarch64 and `darwin` (a universal binary: x86_64 + arm64, checked in its header). **Windows ARM64 is missing**: there, MP3/AAC/OGG won't play. Only tested on Windows x64 so far (E18).
- **Smoke test:** `client/audio/LavaplayerCheck`, on a background thread at client startup: creates the manager, registers only the local and HTTP sources, sets 16-bit LE PCM and forces the natives to load. Replaced by the engine in E02/E03.
- **`./gradlew prodClient`** (new): runs the built jar through Fabric's real launcher in `run-prod/` (in `.gitignore`), with Fabric API next to it. This is the test that catches a library missing from the jar; `runClient` can't.
- **Results:** `runClient` and `prodClient` both log `lavaplayer ready (2 sources, natives for Windows 11 amd64)`. In `prodClient` Fabric lists the 9 libraries as nested mods of `unlimusic`.
- **Real instance (Prism Launcher 11.1.1):** Minecraft 26.3, Fabric Loader 0.20.1, Fabric API 0.162.0+26.3, Mojang's Java runtime (`java-runtime-epsilon`, 25.0.1). Same `lavaplayer ready` line, no `ClassNotFoundException`. Prism had picked Fabric Loader 0.18.4 when creating the instance and the game refused to start (Fabric API needs ≥ 0.19.3, we ask for ≥ 0.19.5): worth a line in the install instructions (E20).
- **Jar size: 30.9 MB**, of which 27.1 MB is `lavaplayer-natives` (natives for 12 platforms). → New decision D11.
- **Licenses to credit in E20:** lavaplayer, lava-common, lavaplayer-natives, httpclient, httpcore, commons-logging, Jackson: all Apache-2.0. Inside the natives: libmpg123 (LGPL-2.1), fdk-aac (Fraunhofer FDK AAC license, has its own conditions: read it before publishing), libsamplerate (BSD-2), libopus and libvorbis (BSD-3).
- **Left to do:** the dedicated server with the jar (lavaplayer is only used by the client entrypoint, so it should only load the nested jars) → E18.
