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

- [ ] The jar starts in a real instance with no `ClassNotFoundException`.
- [ ] Log shows "lavaplayer ready".
- [ ] No lavaplayer code in `src/main`.
- [ ] `CLAUDE.md` fixed regarding AAC.

## Pitfalls

- **Natives**: lavaplayer uses native libraries for some formats and for sample rate conversion. Confirm which systems they work on (Windows x64, Linux, macOS Intel and Apple Silicon). A Mac may fail where your PC works.
- Different versions of the same library in the game and in the mod → strange runtime errors.
- **Licenses**: lavaplayer is Apache-2.0; distributing it inside the jar means keeping the license notices (left for E20, but note the list now).

## APIs

- **To confirm:** `include` and configuration syntax in Loom 1.18; lavaplayer 2.2.x class names (`DefaultAudioPlayerManager`, `LocalAudioSourceManager`, `HttpAudioSourceManager`).

## Session kickoff prompt

> Let's do entry E01 of the plan: read `docs/plan/E01-lavaplayer-in-jar.md`. The goal is a jar that works in a real Minecraft instance, not just in runClient.

## Session notes

*(fill in at the end: decisions made, what was left to do)*
