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
- **Filter** to codecs lavaplayer can play; play the station's `url`, not `url_resolved` (decision D16).
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

- [x] Search by country, by name and by genre.
- [x] Clicking a result plays the station.
- [x] No internet → friendly message.
- [x] The game never freezes during a search.

## Pitfalls

- Exact endpoints and parameters: confirm them in the docs, not from memory.
- Stations marked OK that don't play → handle the error in `RadioSource`, not here.
- Names with emojis or characters Minecraft's font doesn't have.

## APIs

- **Confirmed (26.3):** `ClientCommandRegistrationCallback`, `ClientCommands`, `FabricClientCommandSource` (fabric-command-api-v2 3.1.2); `SharedSuggestionProvider.suggest(String[], SuggestionsBuilder)`; `ClickEvent.RunCommand(String)`, `HoverEvent.ShowText(Component)`, `Style.withClickEvent`/`withHoverEvent`; Gson 2.14.0 ships with Minecraft (reads records).
- **Confirmed (Radio Browser docs, checked against the live API):** server list from a DNS look-up of `all.api.radio-browser.info` plus reverse look-ups (`/json/servers` only for clients without DNS); `GET /json/stations/search`; `GET /json/url/{stationuuid}` to count a click.

## Session kickoff prompt

> Let's do entry E06 of the plan: read `docs/plan/E06-radio-search.md`. Start by reading the official Radio Browser docs and show me the endpoints we'll use.

## Session notes

**29 Oct 2026**

- **Built:** `RadioStation` (record) and `RadioBrowserClient` in `src/main/.../radio`, and the client command `UnliMusicCommand` (`client/command`): `/unlimusic radios <country>`, `radios name <text>`, `radios genre <text>`, and `radios play <station>`, which is what clicking a result runs. `UnliMusic.USER_AGENT` is now the one User-Agent, shared by lavaplayer's HTTP source and Radio Browser.
- **Endpoints:** `GET /json/stations/search?<countrycode|name|tag>=…&order=clickcount&reverse=true&hidebroken=true&limit=…` and `GET /json/url/{stationuuid}`. `clickcount` is clicks in the last 24 h (what people listen to now); `votes` only ever grows. Without `limit` the API sends up to 100,000 stations. Country codes are case-insensitive; `name` and `tag` match any part.
- **Servers:** DNS look-up of `all.api.radio-browser.info`, a reverse look-up of each address (HTTPS needs the name), shuffled; a failing server moves on to the next, the last one that answered is tried first, and when all fail the list is looked up again on the next request. If DNS gives no names, the shared name itself is used (its certificate covers it). **Right now there is only one server (`de1`)**, in DNS, SRV and `/json/servers` alike.
- **Decision D18 (which stations are offered):** see `README.md`. MP3, AAC, AAC+ and OGG only; no HLS, no UNKNOWN codec, nothing that failed the last check. The filter runs on the answer, so the request asks for 3× the stations shown.
- **Threads:** each request runs on a virtual thread and returns a `CompletableFuture`; the command returns at once and the result hops to the game thread with `client.execute(...)`, since only that thread may touch the chat. Offline, the search gives up after the 5 s connect timeout with a friendly message; the game never stops.
- **Cache:** 5 minutes per search, in memory.
- **Clicking a result:** a chat click can only run a command, so each line carries `/unlimusic radios play <uuid>` and the command keeps the stations it has shown in a map (the last 200). A URL would not fit: some are over 300 characters, longer than a command may be. Fabric runs client commands from `sendUnattendedCommand` (the click's path) before the game's own checks, so no confirmation screen, and the chat stays open after a click (handy for trying several stations; vanilla would close it). Hovering a line shows the full name, country, format and up to 6 tags.
- **Click counting** (planned for step 5) went in with the clicks in step 4: one call to `/json/url` per play, fire and forget. The API itself counts one click per address, station and day.
- **Pitfall hit:** a `static final INSTANCE` declared above the constants its constructor reads is built while they are still null (static fields get their values top to bottom); `INSTANCE` now comes after them.
- **Results (`runClient`, Windows 11 x64):** PT, GB, US, `name comercial`, `name radio comercial`, `genre jazz`, `genre smooth jazz` list stations; too-short text, a bad country code and an empty result give their messages; offline gives the friendly message and the next search after reconnecting finds the server again; clicking plays (TSF, Observador, Comercial, Mega Hits, Classic Vinyl HD, Nostalgie New York…), also while another station is still loading. Accents are right (the `�` in `latest.log` is the Windows code page). No emoji squares seen yet.
- **Seen along the way:** RFM's `url` pins one StreamTheWorld edge, which once timed out on connect; the chat then says "the station sent no audio", misleading for a timeout (for E19). Once, TSF played low-pitched and slow: suspected false MP3 frame sync in lavaplayer when joining a stream mid-way (it trusts the first header for the sample rate); not proven yet, to look into apart from this entry. Some station names are very long (200+ characters): shortened to 40 in chat.
- **For E09:** `/json/countries` and `/json/tags` can fill menus; names still need cutting to fit. **For E12:** the server can reuse `RadioBrowserClient` to check a station id before sharing it.
