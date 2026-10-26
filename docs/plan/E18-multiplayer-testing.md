# E18 · Real testing: multiplayer, dedicated server, other systems

> **Phase 6 — Release** · Difficulty ●●○○○ · Depends on: everything being released · Old roadmap: —

## In a nutshell

Make sure the mod works off your PC and outside `runClient`: with several players, on a dedicated server with the real jar, and on other operating systems.

## What you'll see at the end

A filled-in checklist of what was tested where, and the bugs found either fixed or noted down.

## What gets done

- **`runServer` + 2 clients** with different names (set up two run configurations with `--username`).
- **Dedicated server with the real jar**: it must not load client classes or lavaplayer. This is the classic crash in new mods.
- **Real jar in a real instance** (Prism, Modrinth App) on Windows; Linux (GitHub Actions already builds on Ubuntu); macOS because of the natives, if someone can test.
- **Failure scenarios**: no internet, dead radio station, Jamendo quota used up, server without the mod, client without the mod joining a server with the mod.
- **Performance**: memory and thread count after 1 h of playback; switching tracks 50 times.
- **Unit tests** (JUnit) for the pure logic: position calculation (E11), queue (E10), JSON parsing (E06/E07), cache.

## New concepts

- **Unit tests vs in-game tests**: what can be tested without opening Minecraft.
- **Client/server separation** as seen from the dedicated server side.

## Done when

- [ ] Scenario list filled in.
- [ ] Dedicated server starts with the real jar and no errors.
- [ ] No thread or memory leaks after 1 h.
- [ ] Unit tests run in `./gradlew build` (and therefore in CI).

## Pitfalls

- "It works on my PC": runClient hides missing dependencies (see E01).

## Session kickoff prompt

> Let's do entry E18 of the plan: read `docs/plan/E18-multiplayer-testing.md`. Start by putting together the list of scenarios and the run configurations for 2 clients + server.

## Session notes

*(fill in at the end: decisions made, what was left to do)*
