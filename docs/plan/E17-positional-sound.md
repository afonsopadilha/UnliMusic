# E17 · Positional jukebox-style sound *(optional)*

> **Phase 5 — Extras** · Difficulty ●●●●○ · Depends on: E02, E11 · Old roadmap: —

## In a nutshell

Instead of music "in your headphones", the sound comes from a point in the world: walk closer and it gets louder, walk away and you stop hearing it, like a jukebox.

## What you'll see at the end

`/music here` (shared) makes the music come from the block you're standing on. In personal mode, an option does the same just for you.

## What gets built

- **Mono audio**: OpenAL only positions mono sources. Convert stereo → mono in `PcmAudioStream`, or ask lavaplayer for mono output when positional mode is on.
- **Sound instance with a fixed position**, linear attenuation and configurable range.
- **Shared**: the server stores the position; players far away don't hear it and, ideally, don't even download the stream (saves bandwidth).
- **Idea for later**: hook into a game jukebox or a custom "speaker" block (a new block is much more work: model, texture, registration).

## New concepts

- **3D audio**: attenuation, distance, mono vs stereo.
- **Sound instances that change over time** (tickable).

## Suggested steps

1. Mono test tone at a fixed position → walk around and hear the difference.
2. Stereo → mono conversion of lavaplayer's PCM.
3. Option in personal mode.
4. `/music here` in shared mode.

## Done when

- [ ] Moving closer/further changes the volume and direction of the sound.
- [ ] Out of range, it can't be heard.
- [ ] Turning the option off goes back to normal stereo.

## Pitfalls

- Switching from stereo to mono mid-track means recreating the sound.
- Different dimensions (Nether/End): the sound must not carry across.

## APIs

- **To confirm:** `SoundInstance.Attenuation`, tickable sound instances (`AbstractTickableSoundInstance`?), sound range.

## Session kickoff prompt

> Let's do entry E17 of the plan: read `docs/plan/E17-positional-sound.md`. We'll start with the mono test tone at a fixed position.

## Session notes

*(fill in at the end: decisions made, what was left to do)*
