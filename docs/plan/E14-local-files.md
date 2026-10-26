# E14 · Local file library

> **Phase 4 — Local files & settings** · Difficulty ●●●○○ · Depends on: E03, E09 · Old roadmap: step 6

## In a nutshell

A **Local** tab in the screen: the mod reads a music folder of yours, shows title and artist, and plays. Just for you: local files are never shared.

## What you'll see at the end

The mod's music folder is created automatically. An **Open folder** button (like the one for resource packs) opens it in the file explorer. The list has search, plus play, shuffle and repeat.

## What gets built

- **Default folder** (e.g. inside `.minecraft/unlimusic/`) and a path configurable in E16.
- **No native folder picker "for free"**: Minecraft 26.3 doesn't ship the tinyfd library (checked in the Gradle cache). Solution: an "Open folder" button + a text field for the path. As an extra, look into SDL3 dialogs (`lwjgl-sdl` ships with the game).
- **Background scanning** of the extensions lavaplayer can play (mp3, flac, wav, ogg, m4a…), with a cached index so it doesn't reread everything each time, and detection of new/deleted files.
- **Tags**: title and artist (lavaplayer provides some). Album and cover art need a tag library, e.g. jaudiotagger (decision D8: one more dependency in the jar).
- **Personal queue**: play the folder, shuffle, repeat one/all, next/previous.

## New concepts

- **File I/O** in the background and cross-platform paths (`Path`, not strings).
- **Cached index** and how to tell whether it's out of date (modification date).

## Suggested steps

1. Create the folder and the "Open folder" button.
2. Scan and list file names.
3. Tags via lavaplayer and the cached index.
4. Play from the list; personal queue.
5. Decide D8.

## Done when

- [ ] 500 files index without freezing the game.
- [ ] Names with accented characters work.
- [ ] A file deleted in the meantime → handled error, not a crash.
- [ ] Shuffle and repeat work.

## Pitfalls

- Reading tags from hundreds of files can take a while: always off the game thread.
- Network folders or external drives that disappear.

## APIs

- **To confirm:** how to open a folder in the system's file explorer (`Util.getPlatform().openPath`?); track info lavaplayer returns for local files.

## Session kickoff prompt

> Let's do entry E14 of the plan: read `docs/plan/E14-local-files.md`. We'll start with the folder and the "Open folder" button.

## Session notes

*(fill in at the end: decisions made, what was left to do)*
