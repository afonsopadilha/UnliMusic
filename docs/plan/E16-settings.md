# E16 · Settings and configuration

> **Phase 4 — Local files & settings** · Difficulty ●●○○○ · Depends on: E05, E14 · Old roadmap: step 6

## In a nutshell

A settings screen and the files where settings are saved: music folder, HUD (show, position, size), default radio country, accepting shared music, lowering Minecraft's own music while yours plays.

## What you'll see at the end

A "Settings" button in the mod's screen (and, if you have Mod Menu, a "Configure" button in the mod list). Changing an option takes effect immediately and is saved.

## What gets built

- **`config/unlimusic-client.json`** (client) and **`config/unlimusic-server.json`** (server, exists since E07) with Gson; default values; tolerate old or corrupted files.
- **Own screen vs library (decision D7)**: recommendation = a simple screen of our own, so players don't have to install more mods.
- **Optional Mod Menu integration** (optional dependency, only active if installed).
- **Muting Minecraft's music** while ours plays: look into `MusicManager`. It may need the project's first real mixin, a good moment to learn.
- Options gathered from earlier entries: HUD (E05), folder (E14), country (E06), accept shared (E11), jukebox mode (E17, if it exists).
- **Announce Jamendo tracks in chat**: a chat message with the artist, license and a clickable link to the track's page when a track starts. Off by default; the credit is already in the HUD and the link in the mod's screen (E05, E09).

## New concepts

- **Config serialization** and compatibility between versions.
- **Optional mod dependencies**.
- **Mixins**, if needed for the game's music.

## Suggested steps

1. Config class + file, with default values.
2. Hook up the options that already exist in the code.
3. Settings screen.
4. Mod Menu (optional).
5. Mute the game's music.

## Done when

- [ ] Options saved and read on startup.
- [ ] Deleting the file → recreated with default values.
- [ ] Corrupted file → warning in the log, default values, no crash.

## Pitfalls

- Saving the file on every keystroke → save when the screen closes.

## APIs

- **To confirm:** config folder (`FabricLoader.getInstance().getConfigDir()`?); the Mod Menu entrypoint for the version compatible with 26.3; `MusicManager`.

## Session kickoff prompt

> Let's do entry E16 of the plan: read `docs/plan/E16-settings.md`. First we decide D7.

## Session notes

*(fill in at the end: decisions made, what was left to do)*
