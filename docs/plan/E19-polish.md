# E19 · Polish: translations, messages and errors

> **Phase 6 — Release** · Difficulty ●●○○○ · Depends on: E18 · Old roadmap: —

## In a nutshell

Make the mod look "finished": all text translated, errors explained to the player instead of hidden in the log, and a README for the people who will install it.

## What you'll see at the end

The game in English and in Portuguese with no untranslated keys; clear warnings when something fails ("Couldn't connect to the radio station"); a README with installation and usage.

## What gets done

- **All text** in `assets/unlimusic/lang/` (pt_pt, en_us); look for strings written directly in the code.
- **Friendly error messages**: toasts or chat messages instead of stack traces; one error type → one message.
- **Logs** with the right levels (no spam at `INFO`).
- **README** for players: update the existing one with what the mod does, how to install it, shared mode (mod on the server and on every client), music sources, credits.
- **Mod icon** (the current one is the template's).
- Review `CLAUDE.md` so it reflects what the project is now.

## New concepts

- **Toasts** (the notifications in the corner of the screen).
- **Translation keys with arguments** (`%s`).

## Done when

- [ ] No untranslated keys in pt_pt and en_us.
- [ ] The E18 failure scenarios show understandable messages.
- [ ] Updated README and new icon.

## APIs

- **To confirm:** the toast API in 26.3.

## Session kickoff prompt

> Let's do entry E19 of the plan: read `docs/plan/E19-polish.md`. Start by looking for text written directly in the code.

## Session notes

*(fill in at the end: decisions made, what was left to do)*
