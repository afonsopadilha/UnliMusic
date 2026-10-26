# E05 · "Now playing" HUD

> **Phase 1 — Listening alone** · Difficulty ●●●○○ · Depends on: E03 (E04 helps with testing) · Old roadmap: step 3 (HUD)

## In a nutshell

A small box in the corner of the screen that says what's playing: title, artist, a progress bar (or "● LIVE" for radio) and, for Jamendo, the required credit (artist + Creative Commons license).

## What you'll see at the end

Title and artist in the corner, a progress bar moving along, a pause icon when paused. Long titles scroll instead of being cut off.

## What gets built

- **HUD element** registered with `HudElementRegistry`.
- **Watch out for the new drawing model**: in 26.x the HUD implements `extractRenderState(GuiGraphicsExtractor, DeltaTracker)`. Tutorials using `DrawContext` or `GuiGraphics.drawString` are from old versions and won't work.
- **Reads the state from `MusicPlayer`** (E03). No heavy work here: this runs every frame.
- **Credit/license**: the space is prepared now, even before Jamendo exists (E07).
- **Hiding**: with F1 and, later, through an option in the settings (E16). A configurable position is left for E16.
- All text via language files.

## New concepts

- **Render loop**: drawing 60+ times per second; anything slow here = lag.
- **Render state extraction**: Minecraft separates "collecting what to draw" from "drawing".
- **GUI scale**: HUD coordinates are scaled; it has to look right at every scale.

## Suggested steps

1. A HUD element that writes fixed text.
2. Hook it up to `MusicPlayer`: real title and artist.
3. Progress bar (files) and "LIVE" (radio).
4. Semi-transparent background, pause icon, scrolling text.
5. Hide with F1.

## Done when

- [ ] Correct HUD for a local file and for radio.
- [ ] It doesn't flicker or jitter.
- [ ] It doesn't cover the hotbar, health or chat at GUI scales 1 to 4.
- [ ] F1 hides it.

## Pitfalls

- The drawing API has changed a lot in recent versions: confirm everything in the 26.3 sources.
- Element order: decide what to draw before/after (`VanillaHudElements`).
- Fonts and special characters (accents, Japanese, emojis in station names).

## APIs

- **Confirmed (26.3):** `HudElementRegistry` (`addLast`, `attachElementAfter`…), `HudElement.extractRenderState(GuiGraphicsExtractor, DeltaTracker)`, `VanillaHudElements`.
- **To confirm:** `GuiGraphicsExtractor` methods for text, rectangles and textures; how to tell whether the HUD is hidden (F1).

## Session kickoff prompt

> Let's do entry E05 of the plan: read `docs/plan/E05-now-playing-hud.md`. Before drawing anything, show me in the 26.3 sources how `GuiGraphicsExtractor` works.

## Session notes

*(fill in at the end: decisions made, what was left to do)*
