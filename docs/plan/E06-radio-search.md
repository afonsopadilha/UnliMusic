# E06 · Radio search (Radio Browser)

> **Phase 2 — Finding music** · Difficulty ●●○○○ · Depends on: E04 · Old roadmap: step 4

## In a nutshell

Connect to Radio Browser, an open database of radio stations from around the world, with no key. Search by country (e.g. PT), name or genre, and play the chosen one. No nice screen yet: it's tested with a chat command.

## What you'll see at the end

`/unlimusic radios PT` (a client command, separate from the server's `/music`) lists the most-listened stations in Portugal in chat. Clicking one plays it.

## What gets built

- **`RadioBrowserClient` in `src/main`**: the server will also need it to validate stations in shared mode (E12).
- **Server choice**: the API has several mirrors. Pick one from the official list and switch mirrors if it fails.
- **`User-Agent: UnliMusic/<version>`** on every request (requested by the API and required by `CLAUDE.md`).
- **Search** by `countrycode`, name and tag, sorted by popularity, hiding broken stations, with a result limit.
- **Filter** to codecs lavaplayer can play; use the station's "resolved" URL.
- **Count the click** when the player plays a station (the API asks for this to keep popularity accurate).
- **Asynchronous HTTP** with `java.net.http.HttpClient` and parsing with Gson (it ships with Minecraft).
- **Client command** with clickable results.
- Short cache for searches (minutes), so requests aren't repeated.

## New concepts

- **REST APIs and JSON**; turning JSON into Java records with Gson.
- **Brigadier**: how commands and arguments are defined.
- **Clickable text components** in chat.
- **`CompletableFuture`**: asking the network for something without blocking the game.

## Suggested steps

1. Read the official API docs (api.radio-browser.info) and pick the endpoints.
2. HTTP client + parsing into `RadioStation` (record) → test with a log.
3. Command `/unlimusic radios <country>`.
4. Clickable results that play the station (via the E04 `RadioSource`).
5. Search by name and by genre; click counting.

## Done when

- [ ] Search by country, by name and by genre.
- [ ] Clicking a result plays the station.
- [ ] No internet → friendly message.
- [ ] The game never freezes during a search.

## Pitfalls

- Exact endpoints and parameters: confirm them in the docs, not from memory.
- Stations marked OK that don't play → handle the error in `RadioSource`, not here.
- Names with emojis or characters Minecraft's font doesn't have.

## APIs

- **Confirmed (26.3):** `ClientCommandRegistrationCallback`, `ClientCommands`, `FabricClientCommandSource`.
- **To confirm:** Radio Browser endpoints (`/json/stations/search`, `/json/url/{uuid}`, server list); how to create clickable text that runs a command in 26.3.

## Session kickoff prompt

> Let's do entry E06 of the plan: read `docs/plan/E06-radio-search.md`. Start by reading the official Radio Browser docs and show me the endpoints we'll use.

## Session notes

*(fill in at the end: decisions made, what was left to do)*
