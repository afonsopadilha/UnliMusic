# E11 · Synchronization on the clients

> **Phase 3 — Listening together** · Difficulty ●●●●○ · Depends on: E10 · Old roadmap: step 5

## In a nutshell

Make everyone hear the same part of the song. Players joining midway start at the right spot; players with lag don't fall too far behind. For radio, "tuning in" is enough (it's live, no seek).

## What you'll see at the end

Two clients side by side play the same Jamendo track less than ~1 second apart. A player joining midway picks up the song at the right point.

## What gets built

- **Position calculation**: position in the packet + time since the packet arrived + estimated latency + time the track took to load.
- **Jamendo seek** over HTTP: confirm that Jamendo's audio server accepts partial requests (Range) and that lavaplayer can seek in MP3 over HTTP.
- **Periodic heartbeat** (e.g. every 30 s) with the server's position. If the drift goes past a threshold (e.g. 1.5 s), correct it.
- **Personal vs shared (decision D5)**: recommendation = shared music pauses personal music; each player can "leave shared mode" and mute it just for themselves.
- Switching tracks very quickly must not leave clients stuck on the previous one.

## New concepts

- **Every PC has its own clock**, and they don't agree.
- **Latency (ping)** and why half of it is used as the estimate.
- **Drift**: small differences that build up over time.
- **Tolerance**: correcting too often sounds worse than being 300 ms behind.

## Suggested steps

1. Simple seek: play from the position in the packet.
2. Compensate for loading time.
3. Heartbeat and drift correction.
4. Synchronized pause/resume.
5. Decision D5 and the "leave shared mode" option.

## Done when

- [ ] Two clients less than ~1 s apart.
- [ ] Joining in the middle of a track starts at the right point.
- [ ] Synchronized pause/resume.
- [ ] Switching tracks 10 times in a row leaves everyone on the last one.

## Pitfalls

- Testing on the same PC hides latency problems: try with someone on another network, if possible.
- Seeking in MP3 over HTTP can be slow or imprecise.

## APIs

- **To confirm:** seeking in lavaplayer (`AudioTrack.setPosition`?); the player's latency as seen from the client.

## Session kickoff prompt

> Let's do entry E11 of the plan: read `docs/plan/E11-synchronization.md`. First explain the position calculation to me with a worked example in numbers.

## Session notes

*(fill in at the end: decisions made, what was left to do)*
