# E20 · Publishing

> **Phase 6 — Release** · Difficulty ●●○○○ · Depends on: E19 · Old roadmap: —

## In a nutshell

Make the mod available to other people: license, library credits, Jamendo's rules, a Modrinth page and versioned releases.

## What you'll see at the end

A public page for the mod with the jar to download, and a repeatable process for releasing new versions.

## What gets done

- **The mod's license (decision D10)**: already decided, MIT (changed from the template's CC0 before the repository went public). Only check that `LICENSE` ends up in the jar.
- **`THIRD_PARTY_NOTICES`** with the licenses of everything inside the jar (lavaplayer Apache-2.0 and the dependencies listed in E01).
- **Jamendo**:
  - non-commercial terms of use; confirm whether platform reward programmes (e.g. CurseForge) count as commercial use;
  - **decision D2** settled for good: how the client ID works in the public release.
- **Radio Browser**: `User-Agent` with the mod's name and version (already done in E06).
- **Modrinth page** (and/or CurseForge): description, screenshots, a note that shared mode needs the mod on the server and on every client, and that YouTube/Spotify are not supported (on purpose).
- **Versions and changelog**; GitHub Actions building the jar when a tag is created (the template's `build.yml` already builds).
- Final **`fabric.mod.json`**: contact, homepage, issues.

## New concepts

- **Software licenses** and what redistributing libraries means.
- **Semantic versioning** (1.0.0, 1.0.1, 1.1.0…).

## Done when

- [ ] Third-party notices in the jar (the MIT license is already chosen).
- [ ] D2 settled and documented in the README.
- [ ] Page published and first downloadable version.

## Session kickoff prompt

> Let's do entry E20 of the plan: read `docs/plan/E20-publishing.md`. We'll start with the third-party notices and the Jamendo client ID (D2).

## Session notes

*(fill in at the end: decisions made, what was left to do)*
