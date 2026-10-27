# E07 · Server-side Jamendo client, with cache

> **Phase 2 — Finding music** · Difficulty ●●●○○ · Depends on: E00 (to play the results: E04) · Old roadmap: step 4

## In a nutshell

Search for Creative Commons music on Jamendo. The key (client ID) and the searches live on the **server**, with a cache, because the free quota (~35,000 requests/month) is shared by every player using that client ID.

## What you'll see at the end

`/music jamendo search lofi` lists tracks with artist, duration and license. It's a server command, but it works in singleplayer because there is an integrated server.

## What gets built

- **Client ID (a manual step for you)**: register an application on the Jamendo developer portal.
- **Server configuration outside Git**: `config/unlimusic-server.json` with `jamendoClientId`. In dev it lives in `run/config/` (already ignored), but the file name also goes into `.gitignore`.
- **`JamendoClient` (`src/main`)**: track search by text and by tags, sorting and limit. Fields we care about: id, name, artist, duration, audio URL, album image, license URL, track link.
- **Cache**: **search results and track metadata only**, in memory, with a short expiry (e.g. 1 h). No disk cache, and never cache audio (see "Jamendo API terms" below). A monthly request counter with a warning near the limit.
- **No client ID → Jamendo disabled** with a clear message. The mod has to work without it. The same goes for an ID Jamendo rejects (revoked or invalid): disable Jamendo, don't retry in a loop.
- **Test server command.**
- **CC licenses**: store the type (BY, BY-SA, BY-NC…) for the credit in the HUD.

## New concepts

- **Secrets outside Git**, and why a client ID in a public repository is a problem.
- **Cache with TTL** and API quotas.
- **Integrated vs dedicated server**.
- **Creative Commons licenses** and what they require you to show.

## Suggested steps

1. Read the API v3.0 docs and the terms of use.
2. Server config file (read it, create it with default values if it doesn't exist).
3. `JamendoClient` with one search → log.
4. Cache and request counter.
5. Command `/music jamendo search`.

## Done when

- [ ] Search works in singleplayer.
- [ ] The same search repeated doesn't use up a request (log says "cache").
- [ ] Without a client ID, the game starts and explains what's missing.
- [ ] The client ID never appears in Git, in chat or in the logs.

## Pitfalls

- The audio URL may contain the client ID → never show it in chat or in the logs.
- API errors may come inside the JSON with a 200 status: read the response header.
- **Decision D2** (client ID in a public release): it doesn't need to be settled yet, but the configuration should allow the different options.

## Jamendo API terms

Read on 2026-10-27 from the [API Terms of Use](https://devportal.jamendo.com/api_terms_of_use) (last modified 2013). What they mean for the mod:

- **Non-commercial only (3.3).** "Commercial" is any use aimed at commercial advantage or "any monetary compensation", including affiliate programmes and advertising. A free mod with no ads is fine. Donations (GitHub Sponsors) and platform reward programmes are not mentioned: see decision D13.
- **Caching (3.3).** Apps must not be designed to cache content or offer offline access; caching only "to the extent reasonably necessary", and changes on Jamendo's side must show up soon. So: short in-memory cache of searches and metadata, never audio files, no "download for offline" feature.
- **Request limits (3.3).** Jamendo may limit how many requests are made, and the limits must not be circumvented (e.g. no rotating several client IDs). The ~35,000/month figure is from the developer portal, not the terms.
- **Credentials (2).** The client ID is personal and must not be shared with third parties, and each application needs its own ID. This backs the D2 recommendation: no ID baked into the public jar.
- **Attribution (4.1).** Credit the artist as creator, credit Jamendo as provider, and give a **direct link to each track's page** on Jamendo. Jamendo's buttons/logos, if used, must not be modified, and "Jamendo" can't be part of the mod's name (4.2). So `JamendoClient` must keep the track's page URL (`shareurl`/`shorturl`, to confirm).
- **Termination (7).** If access is revoked, stop using the API and remove the content and references to Jamendo. A rejected ID must disable Jamendo cleanly.

## APIs

- **Confirmed (26.3):** `CommandRegistrationCallback`.
- **To confirm:** Jamendo API v3.0 endpoints and parameters (`/tracks`, `search`, `include`, `audioformat`…); which field holds the track's page on Jamendo (for the backlink); how a rejected client ID is reported; exact quota limits; where the config folder is (`FabricLoader.getInstance().getConfigDir()`?).

## Session kickoff prompt

> Let's do entry E07 of the plan: read `docs/plan/E07-jamendo-client.md`. I already have (or will create now) a Jamendo client ID. Start with the configuration outside Git.

## Session notes

*(fill in at the end: decisions made, what was left to do)*
