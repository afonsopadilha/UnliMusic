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
- **Credit/license**: the space is prepared now, even before Jamendo exists (E07). Jamendo's terms (clause 4.1, see E07) ask for three things: the artist as creator, **Jamendo as provider** ("via Jamendo"), and a **direct link to the track's page**. The HUD shows the first two; the link lives in the mod's screen, as an "Open on Jamendo" button (E09).
- **The HUD is display-only (decision D14).** While playing, Minecraft captures the mouse to move the camera, so nothing drawn on the HUD can be clicked. Every control lives in the mod's screen (E09), opened with a key, plus the quick keys from E03.
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

- [x] Correct HUD for a local file and for radio.
- [x] It doesn't flicker or jitter.
- [x] It doesn't cover the hotbar, health or chat at GUI scales 1 to 4.
- [x] F1 hides it.

## Pitfalls

- The drawing API has changed a lot in recent versions: confirm everything in the 26.3 sources.
- Element order: decide what to draw before/after (`VanillaHudElements`).
- Fonts and special characters (accents, Japanese, emojis in station names).

## APIs

- **Confirmed (26.3):** `HudElementRegistry` (`addLast`, `attachElementAfter`…), `HudElement.extractRenderState(GuiGraphicsExtractor, DeltaTracker)`, `VanillaHudElements`.
- **Confirmed in this session (decompiled 26.3 with `genSources`, and Fabric API 0.162.0 sources):** `GuiGraphicsExtractor.text(Font, Component|String|FormattedCharSequence, x, y, argb[, shadow])` (skips colours with alpha 0), `fill(x0, y0, x1, y1, argb)` (corners), `outline(x, y, w, h, argb)` (size), `blitSprite(RenderPipelines.GUI_TEXTURED, Identifier, x, y, w, h)`, `enableScissor`/`disableScissor`, `pose()` (`Matrix3x2fStack`: `pushMatrix`, `translate`, `popMatrix`), `guiWidth()`/`guiHeight()`; `Font.width(...)`, `Font.lineHeight` (9); `Window.getGuiScale()` (int); `Util.getMillis()`. F1: `Gui` toggles `Hud.toggle()` on `options.keyToggleGui`, read with `Hud.isHidden()`; `attachElementBefore/After` inherit it, `addFirst/addLast` do not. Drawing order: `GuiRenderState` puts each new element above the highest earlier one it overlaps.

## Session kickoff prompt

> Let's do entry E05 of the plan: read `docs/plan/E05-now-playing-hud.md`. Before drawing anything, show me in the 26.3 sources how `GuiGraphicsExtractor` works.

## Session notes

**27 Oct 2026**

- **Built:** `NowPlayingHud` (`client/hud`): a semi-transparent box in the top-left corner with the title, then the artist (or "Loading…" / "Reconnecting (n of 5)…"), the credit line for Jamendo (E07; null for every other source), and a progress bar with the time heard and the length for files, the time alone when the length is unknown, or a red "● LIVE" for radio. A pause icon (two bars) sits at the right of the title while paused.
- **Decision D17 (what the HUD shows):** only while loading, playing or paused; nothing when stopped or after an error. `StatusMessages` now only sends errors to chat; the messages above the hotbar it showed until now repeated the HUD and are gone, with their language keys.
- **Layering and F1:** registered with `attachElementBefore(VanillaHudElements.CHAT, …)`. Attaching to a vanilla element inherits its "hidden by F1" condition (`addLast` would not), and being before the chat lets the chat and the Tab list cover the box rather than the other way round.
- **Two-phase drawing:** nothing is drawn during `extractRenderState`; each call adds an element (with a copy of the current matrix and scissor) to `GuiRenderState`, drawn later. An element that overlaps an earlier one goes above it, so the background is measured and queued before the text.
- **Colours are ARGB:** `text(...)` silently skips a colour with alpha 0, so white is `0xFFFFFFFF`.
- **Scrolling text:** lines longer than the 140 px content width wait 2 s, slide at 20 GUI px/s, wait at the end and slide back; they restart from the beginning when the track changes. The game's own scrolling labels (`ActiveTextCollector.acceptScrolling`) were not used: they move a whole GUI pixel at a time (several screen pixels at scale 2+), which looked jerky on something always in view, are always white, and use a global clock. Ours rounds the offset to screen pixels (`pose().translate`, inside a scissor set before the translation): smooth, and the letters stay sharp.
- **Threads:** the HUD runs on the render thread, which is the same thread the `MusicPlayer` changes state on (the log shows both as "Render thread"), so it reads the player without locks. `MusicPlayer.positionMs()` uses `System.nanoTime()`, so the bar moves every frame and stops with the pause.
- **Results (`runClient`, Windows 11 x64):** Heart 80s and Rádio Comercial (LIVE label, back to live after the pause menu); a FLAC with tags, played to the end (bar, 0:30, pause and resume at the same point); an MP3 with a long title (scrolls smoothly, pause icon); F1 hides the box; an accented station name shows correctly (the `R�dio` in `latest.log` is only the log file being written in the Windows code page).
- **GUI scales:** the box sits in the top-left, away from the hotbar, health and chat (all at the bottom). Worst case, an open chat (180 px tall, its bottom 40 px above the screen's) at scale 4 on a 1080 px tall screen reaches y 50; the box ends at y 44 with two text lines and at y 54 with a Jamendo credit line, where the chat would cover its bottom edge: `Hud` calls `nextStratum()` before the chat, so the chat is always drawn above us. At scale 4 on a 1920 px wide window the box's right edge (x 150) reaches where a boss bar starts (x 149); left as is, the position becomes a setting in E16.
- **Not tested:** emojis in a station name (Japanese is covered by the game's Unifont); a track with a `credit` (needs E07).
- **For E16:** position (corner) and hiding the HUD as settings; maybe background opacity. **For the radio's song on air (idea for later):** the HUD's artist line is where `StreamTitle` would go.
