# E00 · Clean up the project and first key

> **Phase 0 — Foundations** · Difficulty ●○○○○ · Depends on: — · Old roadmap: step 0

## In a nutshell

Clean up what came with the template, align the configuration and create the mod's first key: you press a key and a message appears in chat. It's the "hello world" that proves the mod loads and the client code runs.

## What you'll see at the end

Under *Options → Controls* there is an **UnliMusic** category with a "Test" key. Pressing it shows a message from the mod in chat.

## What gets built

- **`fabric.mod.json`**: description, authors and contact links are still the template's. The license is left for E20, but note it as pending.
- **Remove the example mixins** (`ExampleMixin`, `ExampleClientMixin` and their entries in the `.mixins.json` files). The mod doesn't need mixins for now.
- **Minecraft version (decision D1)**: `CLAUDE.md` says 26.1.x, but `gradle.properties` has 26.3 and that is what `runClient` already launched with. Pick one and fix the other.
- **JDK 25**: the `javap` on the PATH is from JDK 17. Confirm that Gradle and IntelliJ use 25.
- **`.gitignore`**: add the secrets file E07 will use (e.g. `unlimusic-server.json`) right away, as `CLAUDE.md` asks.
- **Package layout** (proposal, so every entry follows the same one):
  - `com.unlimusic` (`src/main`): `net/` (packets), `server/` (shared state, commands), `api/jamendo/`, `api/radiobrowser/`, `config/`
  - `com.unlimusic.client` (`src/client`): `audio/`, `hud/`, `screen/`, `input/` (keys)
- **Test key**: register a `KeyMapping` and, on every client tick, check whether it was pressed and write to chat.
- **Language files**: `assets/unlimusic/lang/en_us.json` and `pt_pt.json` with the key and category names.

## New concepts

- **Entrypoints** `main` vs `client`, and why the code lives in two folders.
- **Client tick**: the game "thinks" 20 times per second; that's where keys are checked.
- **`KeyMapping`**: a key the player can rebind (not "listen for the M key" directly).
- **Language files**: text is never written in the code; a translatable key goes there instead.
- **Mixin**: what it is, and why we don't need one now.

## Suggested steps

1. Clean up `fabric.mod.json` and remove the example mixins → `runClient` still launches.
2. Decide D1 and fix whichever file is wrong.
3. Register the key (not doing anything yet) → it shows up in Controls.
4. Hook the key up to the chat message.
5. pt_pt / en_us translations.

## Done when

- [x] The key shows up in Controls with a translated name, in its own category.
- [x] One press = one message (not one per tick while held).
- [x] Example mixins removed; `./gradlew build` passes.
- [x] D1 decided and `CLAUDE.md` consistent with `gradle.properties`.

## Pitfalls

- `isDown()` alone fires on every tick, and in 26.3 `consumeClick()` alone fires on every OS key repeat while the key is held (`KeyboardHandler.keyPress` calls `KeyMapping.click` for both press and repeat). Use `SinglePress` (`client/input/`), which combines both.
- Client code (keys, the player's chat) never goes in `src/main`: on a dedicated server those classes don't exist.
- Old tutorials use Yarn (`KeyBinding`, `MinecraftClient`…). In 26.x it's the official names (`KeyMapping`, `Minecraft`…).

## APIs

- **Confirmed (26.3):** `KeyMappingHelper.registerKeyMapping(KeyMapping)` in `net.fabricmc.fabric.api.client.keymapping.v1`.
- **To confirm:** the `KeyMapping` constructor and how the category is created (it changed in recent versions); `ClientTickEvents.END_CLIENT_TICK`; how to show a message to the player.

## Session kickoff prompt

> Let's do entry E00 of the plan: read `docs/plan/E00-project-setup.md`. Start with decision D1 (Minecraft version) and then go step by step, testing with runClient.

## Session notes

**26 Oct 2026**

- **D1 decided: Minecraft 26.3.** `CLAUDE.md` now matches `gradle.properties`; nothing else changed.
- **JDK 25:** IntelliJ already used `ms-25` (`~/.jdks/ms-25.0.4.1`), but `./gradlew` from a terminal ran on JDK 17 (PATH, no `JAVA_HOME`). Fixed with `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=25`): Gradle finds a JDK 25 on its own, on any PC, without touching environment variables.
- **Template cleaned:** example mixins and both `.mixins.json` files removed (and the `mixins` block in `fabric.mod.json`); description, author and GitHub links filled in. The license is still the template's CC0 → pending, D10 (E20).
- **`.gitignore`:** `unlimusic-server.json` (secrets for E07).
- **Test key:** `client/input/ModKeys` registers the "UnliMusic" category (`KeyMapping.Category.register(UnliMusic.id("main"))`, translated via `key.category.unlimusic.main`) and the "Test" key on `J`; on `END_CLIENT_TICK` it writes to chat with `LocalPlayer.sendSystemMessage(Component)`. Translations in `en_us.json` and `pt_pt.json`, both tested in game.
- **Surprise:** holding the key sent ~25 messages per second. In 26.3 the OS key repeats also count as clicks, so `consumeClick()` alone isn't enough. Fixed with `client/input/SinglePress` (drains the clicks + ignores them if the key was already down on the previous tick). Use it for E03's keys too.
- **APIs confirmed (26.3):** `new KeyMapping(String, InputConstants.Type, int, KeyMapping.Category)`, `KeyMapping.Category.register(Identifier)`, `ClientTickEvents.END_CLIENT_TICK` (`Minecraft`), `LocalPlayer.sendSystemMessage(Component)`.
- **Package layout:** adopted as proposed above; folders are created when the first class needs them (for now only `client/input/`).
