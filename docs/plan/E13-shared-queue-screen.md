# E13 · Shared queue on screen

> **Phase 3 — Listening together** · Difficulty ●●●○○ · Depends on: E09, E11, E12 · Old roadmap: step 5

## In a nutshell

Bring shared mode into the mod's screen. Each search result gets two buttons: **Play just for me** and **Add to everyone's queue**. A **Queue** tab shows what's coming next and who added it.

## What you'll see at the end

Everything the `/music` commands do can also be done from the screen. The queue updates in real time for everyone.

## What gets built

- Personal/shared buttons in the Radios and Jamendo tabs (the shared one only appears if the server has the mod and the player has permission).
- **Queue tab**: list, who added it, duration, skip/remove (with permission), reorder (optional).
- C2S control packets (add, skip, remove), all validated on the server as in E12.
- A HUD indicator that the music is shared.

## New concepts

- Keeping the interface consistent with state that changes "from outside" (another player edits the queue while you have the screen open).

## Suggested steps

1. "Add to everyone's queue" button.
2. Read-only Queue tab, updating with `QueueUpdate`.
3. Skip/remove with permission.
4. "Shared" indicator in the HUD.

## Done when

- [ ] Two clients: one adds, the other sees it straight away.
- [ ] Without permission, the control buttons don't appear (and the server refuses anyway).

## Pitfalls

- Big queue → big packets: send only the next N tracks.

## APIs

- Reuses what was confirmed in E08–E12.

## Session kickoff prompt

> Let's do entry E13 of the plan: read `docs/plan/E13-shared-queue-screen.md`.

## Session notes

*(fill in at the end: decisions made, what was left to do)*
