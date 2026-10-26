# E09 · Main screen: radios and Jamendo

> **Phase 2 — Finding music** · Difficulty ●●●●○ (probably 2 sessions) · Depends on: E06, E08 · Old roadmap: step 4

## In a nutshell

A mod window, opened with a key, with **Radios** and **Jamendo** tabs (later **Local**, E14, and **Queue**, E13). You type, search, see the list and press "Play".

## What you'll see at the end

The screen opens; radio search by country (PT by default) and by name; Jamendo search; a "Play" button; a "loading…" indicator and error messages.

## What gets built

- **`MusicScreen extends Screen`** with tabs.
- **Widgets**: text box, scrolling list, buttons.
- **Asynchronous searches**: the screen never waits. It shows "loading…" and updates when the answer arrives; answers from stale searches are ignored.
- **Country picker** (Radio Browser has a list of countries).
- **Mini player** at the bottom of the screen: what's playing + pause/stop.
- **Favourite radios** saved locally (optional in this entry).
- **Suggested split**: session 1 = screen basics + Radios; session 2 = Jamendo + polish.

## New concepts

- **Lifecycle of a `Screen`**: `init`, render, tick, close.
- **Widgets** and a layout that adapts to the window size and GUI scale.
- **Keyboard focus** (typing in the box vs screen shortcuts).

## Suggested steps

1. An empty screen that opens with a key and closes with ESC.
2. Radios tab: text box + list + button.
3. Hook it up to the E06 search (asynchronous).
4. Jamendo tab hooked up to E08.
5. Mini player and polish.

## Done when

- [ ] Everything navigable with mouse and keyboard; ESC closes.
- [ ] Resizing the window doesn't break the layout.
- [ ] 100 results scroll without lag.
- [ ] Network errors show up on screen, not just in the log.

## Pitfalls

- The screen and widget API has changed a lot between versions: confirm everything in the 26.3 sources.
- Text that doesn't fit (long names) → truncate with "…".
- Opening the screen must not pause the music (nor the game, in multiplayer).

## APIs

- **To confirm:** `Screen`, `EditBox`, `Button`, scrolling lists (`ObjectSelectionList`?), how to draw in screens in 26.3 (the same extraction model as the HUD?).

## Session kickoff prompt

> Let's do entry E09 of the plan: read `docs/plan/E09-main-screen.md`. In this session we'll only do the screen basics and the Radios tab.

## Session notes

*(fill in at the end: decisions made, what was left to do)*
