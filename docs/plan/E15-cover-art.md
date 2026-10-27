# E15 · Cover art and images *(optional)*

> **Phase 4 — Local files & settings** · Difficulty ●●●○○ · Depends on: E05, E09 · Old roadmap: —

## In a nutshell

Show the album cover (Jamendo), the station logo (Radio Browser) and the cover embedded in local files, in the HUD and in the screen's lists.

## What you'll see at the end

A small square image next to the title in the HUD and on each row of the results.

## What gets built

- **Asynchronous download** with size and time limits.
- **Conversion to a Minecraft texture** (image → registered dynamic texture), and releasing the texture when it's no longer needed.
- **Formats**: PNG works; JPEG and others may need converting (confirm). Station favicons in ICO/SVG → ignore them and show a default icon.
- **Cache** in memory. A disk cache is only allowed for station logos and local covers: Jamendo's terms (clause 3.3, see E07) only allow caching that is reasonably necessary, so Jamendo covers stay in memory.
- Local file covers: depends on decision D8 (tag library).

## New concepts

- **Textures** in Minecraft: load, register, draw, release.
- **Graphics card memory leaks**: a texture that is never released stays there forever.

## Suggested steps

1. A fixed image (from the mod's resources) in the HUD.
2. Download a Jamendo cover → texture → HUD.
3. Cache and release.
4. Screen lists.
5. Station logos and local covers.

## Done when

- [ ] Covers show up in the HUD and in the lists.
- [ ] Scrolling through 200 results doesn't make memory shoot up.
- [ ] Broken images show the default icon.

## Pitfalls

- Creating textures off the render thread can crash.
- Huge images (some favicons are megabytes).

## APIs

- **To confirm:** `NativeImage`, `DynamicTexture`, `TextureManager` in 26.3 and which formats `NativeImage` reads.

## Session kickoff prompt

> Let's do entry E15 of the plan: read `docs/plan/E15-cover-art.md`. We'll start with a fixed image in the HUD.

## Session notes

*(fill in at the end: decisions made, what was left to do)*
