# CLAUDE.md — guide for future Claude instances

Read this before touching the repo. It is the project's memory: what this is, the hard rules, how RuneLite plugins are
supposed to be built, how we talk to the OSRS Wiki without being a nuisance, and how we stay current with the game.

## What this is

**Prediction Market Drops** is a RuneLite plugin that is **purely for fun**. It is a silly overlay that mimics the look
of Kalshi / Polymarket: a "market" with a live-looking odds line betting on your next big drop (e.g. *ZULRAH DROPS
TANZANITE FANG*). The odds are made up. There is no real money, no wagering, no real trading, and no real drop-rate
math. Keep it cosmetic and tongue-in-cheek. Do not make it a drop-rate calculator, a gambling feature, or anything that
gives a gameplay advantage or automates input (see "Jagex rules" below).

- Plugin id/package: `com.dropmarket`; hub entry: `plugins/drop-market` in `runelite/plugin-hub`.
- Version lives in `runelite-plugin.properties` (`version=`); keep `CHANGELOG.md` in step.
- The two drop-value settings are `marketMinimumAmount` / `winMinimumAmount` in the unit chosen by `valueUnit` (k by default);
  the old gp keys (`minimumDropValue`, `valuableDropThreshold`) are migrated once in `DropMarketPlugin.migrateLegacyValues`.
  Config sections run Data Sources, Display, Drop Lists, Market Value, Celebration, Graph Line 1/2, Header Bar 1/2.
- Repo: `TaikiShuttleRS/Drop-Market`, branch `main`, SSH remote.

## Hard rule: identity

- **Never mention the owner's real name or their employer anywhere**: not in code, comments, commit messages, PR
  text, issues, docs, test data, screenshots, or replies to the user. Don't write those terms down even to say "don't
  use them" in a tracked file.
- The only identities are the GitHub account **`TaikiShuttleRS`** and the git/author name **`Taiki Shuttle`**
  (`author=` in `runelite-plugin.properties`, license holder, commit author). Commits use the GitHub **ID-based**
  noreply email (`<id>+TaikiShuttleRS@users.noreply.github.com`), never the old name-only noreply form and never a
  real email.
- Git identity comes from an `includeIf` in the global git config that matches repos whose remote is under
  `TaikiShuttleRS`. The global `user.name`/`user.email` are deliberately **unset**; do not set them, and do not change
  the owner's global git config for other accounts.
- Local hooks in `.git/hooks` (`pre-commit`, `commit-msg`, `pre-push`) refuse commits and pushes whose identity,
  message, file names or added lines contain the blocked terms. They are untracked on purpose (the patterns must not be
  published). **Never bypass them** (`--no-verify`, editing them away). On a fresh clone they won't exist: recreate
  equivalents before committing.
- Before any commit/push: `git config user.name` must print `Taiki Shuttle`. Tool output may contain the owner's
  details (file paths, config); never repeat them in replies or files.
- Rewriting history and force-pushing is **destructive**: only do it when the user explicitly asks, take a
  backup branch, verify the tree is unchanged, then re-pin the hub PR to the new hashes.

## RuneLite plugin best practices (follow these)

**Structure & lifecycle**
- Match the official `runelite/example-plugin` template (build.gradle already does: Java 11 via
  `options.release.set(11)`, Lombok, `latest.release` RuneLite, JUnit 4). Don't use APIs newer than Java 11.
- `startUp()` registers (overlay, etc.); `shutDown()` unregisters **and** clears state. Everything you add in one must be
  undone in the other.
- Dependency injection only: `@Inject` fields/constructors, `@Singleton` helpers, `@Provides` for the config. Never
  `new Gson()`, `new OkHttpClient()`, `new Thread`/executors, or static mutable state. **The hub's build rejects fresh
  `Gson` instances** ("Do not create fresh Gson instances, always @Inject the client's Gson").
- Subscribe with `@Subscribe` on `EventBus` events (`GameTick`, `LootReceived`, `NpcLootReceived`, `ChatMessage`,
  `ConfigChanged`, `StatChanged`, `RuneScapeProfileChanged`…). Filter `ConfigChanged` by your group.
- Avoid terminally deprecated or removed APIs; the hub build fails on them.

**Threading**
- Game/client calls (`client.*`, `playSoundEffect`, item lookups) happen on the **client thread**. Event handlers
  already run there. From any other thread (OkHttp callbacks, executors) hop back with `clientThread.invoke(...)`.
- Never block the client thread: no sleeps, no synchronous network calls. Use OkHttp `enqueue` and a callback.

**Config**
- One `@ConfigGroup`; group settings with `@ConfigSection`; give every item a stable `keyName` (**never rename a
  keyName once released**: it is where players' saved settings live — rename only `name`/method names), a clear `name`
  and `description`, `@Range`/`@Units` where numeric.
- Anything that sends the player's IP to a third-party server (live wiki lookup, RuneProfile) must be **opt-in (default
  off)** and carry a `warning = "This feature submits your IP address to a 3rd-party server not controlled or
  verified by RuneLite developers"`. Bundle data in the plugin instead whenever you can.
- This RuneLite version has no conditional show/hide for config items (`unhide` doesn't exist); don't use it.

**Network & data**
- Use the injected `OkHttpClient`; set an informative User-Agent when calling third parties; cache responses; fail
  quietly (`log.debug`) and keep working offline.
- Prefer shipping data in the plugin (`src/main/resources`) over runtime requests; we bundle OSRS Wiki snapshots and
  refresh them with the tools below.

**Overlays/UI**
- Draw with `Graphics2D` in an `Overlay`; use `FontManager` fonts; keep per-frame work cheap; animate in game ticks
  where possible (it keeps everything in lockstep with the game).

**Hub & repo hygiene**
- Hub requirements: public repo, `LICENSE` (BSD-2), `runelite-plugin.properties` (displayName, author, description,
  tags, plugins, version, support), optional `icon.png` **≤ 48×72 px**, a real README.
- The hub builds the **exact commit** pinned in `plugins/drop-market`. Every release = a PR to `runelite/plugin-hub`
  changing `commit=` (and `repository=` only if the repo moved). Keep changes in a single PR; fix CI there.
- Don't add dependencies beyond RuneLite's own transitive ones.

**Jagex / RuneLite rules (do not violate)**
- No automation of input, no click assistance, no plugins that trivialize content or give an unfair PvM/PvP edge, no
  exposing player info over HTTP, no real-money gambling. See RuneLite's *Rejected or Rolled Back Features* wiki page
  and Jagex's third-party client guidelines before adding anything game-adjacent.

## Testing & verification

- `./gradlew test` (CI runs `./gradlew build`). Keep rules testable: `DropQualifier` is pure logic with unit tests;
  painter tests render to `BufferedImage` and count pixels; `MarketGraphTest` checks the drift profiles statistically.
- To look at the overlay, render it with a **temporary** JUnit test that calls `MarketPainter.paint` with a config
  stub, writes a PNG/GIF to a scratch folder, shows it, and is then **deleted** (don't commit scratch tests).
- Line endings: working tree is CRLF-ish on Windows, blobs are LF (`.gitattributes` has `text=auto`).

## Using the OSRS Wiki responsibly (important)

The OSRS Wiki is run by Weird Gloop, which has publicly asked automated clients to stop hammering it
(<https://weirdgloop.org/blog/clankers>: ~250M bot requests/month, mostly from naive crawlers that follow every link).
Don't be that. Rules for anything we run (tools, workflows, and any ad-hoc fetch a Claude instance does):

1. **Prefer not to fetch at all.** The plugin ships a bundled snapshot; tools refresh it rarely (monthly workflow).
2. **Never crawl.** No following links, no scraping rendered article pages, no walking `Special:`, history, diffs,
   `?action=`, `?oldid=`, edit screens, or search result pages. `robots.txt`
   (<https://oldschool.runescape.wiki/robots.txt>) disallows those and also `/*api.php` for generic crawlers.
3. **Use the documented API for structured data**, as tools do, in bulk and sparingly: the Bucket API
   (`action=bucket`, e.g. `dropsline`) for drop data, and `action=query` for page content/lists. Do not hit `Bucket:`
   pages directly. One bulk query beats thousands of page loads.
4. **Be a polite API client** (<https://www.mediawiki.org/wiki/API:Etiquette>): an informative `User-Agent` with a
   contact URL (`tools/wiki-api.mjs` sets it), requests **in series** with a pause (1s), `maxlag=5` with back-off,
   gzip, no parallelism, no retry storms.
5. **Use the sitemap if you need to discover pages**, not link-chasing: it is declared in `robots.txt` as
   `https://oldschool.runescape.wiki/images/sitemaps/index.xml` (gzip children like `NS_0-0.xml.gz`, with `lastmod`).
   Check `lastmod` first and fetch a child sitemap only if it changed.
6. **No editing and no bots.** Wiki editing bots need community approval and a bot flag
   (<https://oldschool.runescape.wiki/w/RuneScape:Bots>, <https://oldschool.runescape.wiki/w/RuneScape:AutoWikiBrowser>);
   we only ever read. Real-time prices are RuneLite's job (`ItemManager`), never ours.
7. Content is CC BY-NC-SA; keep attribution in the README. Bundled data is a snapshot of that content.
8. Cache and reuse; if a fetch fails, stop — don't loop.

## Tools in `tools/`

- `wiki-api.mjs` — the polite API client everything shares.
- `generate-wiki-drops.mjs` — regenerates `src/main/resources/com/dropmarket/wiki-drops.json` (monster → top drops) and
  `collection-log.json` (log items and which monsters drop them). ~10 API calls, ~1s apart. Run before each release;
  the `update-drop-data` workflow does it monthly and opens a PR.
- Also builds `valuable-components.json` (untradeable items that drop and end up, through any number of crafting steps, in an item worth 5m+) using the wiki's real-time prices API (`prices.runescape.wiki`), with the same polite User-Agent.
- `check-game-updates.mjs [days]` — lists new `Update:` pages from the wiki's recent changes. The
  `watch-game-updates` workflow runs it weekly and opens/updates a "Game update watch" issue.

## Staying current with the game (the plan)

1. **Data (automatic):** monthly `update-drop-data` PR refreshes drop tables and collection log membership from the
   wiki. Review the diff, run tests, merge, then cut a release.
2. **News (automatic):** weekly `watch-game-updates` issue lists new game-update pages. Triage it: does anything add a
   boss, raid, minigame, area, or collection log items?
3. **New content needs code, not just data.** For a new boss/raid/activity:
   - Hand-written table in `DropTables.java` (uniques first) + short name in `ABBREVIATIONS`, aliases for sub-bosses,
     `RAIDS`/`isCompletion` if it has a reward-chest completion.
   - Detection in `DropMarketPlugin.activitySource()`: a raid varbit (`VarbitID` / `gameval`), or the region IDs of
     the area (find them with RuneLite's developer tools / `WorldPoint.getRegionID()`), or NPC names via
     `markCombat`. Until then, loot/chat paths still work from `LootReceived` names.
   - Unpriced new items already count as wins (`DropQualifier`): items with no GE price yet, new collection log
     items, and priority drops win regardless of price.
   - Then regenerate data, add tests, bump `version` + `CHANGELOG.md`.
4. **RuneLite API drift (automatic):** `build.gradle` uses `latest.release`; the `Build` workflow also runs weekly so a
   breaking API change shows up in CI before the hub's build does.
5. **Hub PRs:** push to `main`, then sync the fork, branch, edit `plugins/drop-market` (`commit=`), open the PR with
   `gh`. Check the PR's build once it has had time to run (the bot comments on non-commit changes; that is normal for
   a `repository=` change). Updates have typically merged in hours to a day or two.

## Crash-proofing (a plugin bug must never reach the client)

The 1.2.0 release shipped with a package-private enum in the public config; RuneLite's config proxy couldn't read it
and the overlay threw an `IllegalAccessError` into the client every frame. The rules that came out of it:

- **Every entry point goes through `Guard`**: each `@Subscribe` handler is a one-liner `guard.run("onX", () -> handleX(e))`,
  the overlay renders through `guard.call`, `startUp`/`shutDown` are guarded piece by piece, and async callbacks
  (wiki, RuneProfile, `clientThread.invoke`) catch their own failures. A failure is logged once per place and skipped.
  New handlers must follow the same shape (`ArchitectureTest` checks it).
- **Anything the config interface touches must be `public`**: the config itself, every enum it returns (`OverlayLayout`,
  `WinSound`). Package-private is fine everywhere else.
- **RuneLite services go through our wrappers**: `ItemLookup` (names, prices, search; never null, never throws) and
  `ProfileStore` (per-account settings). Don't inject `ItemManager`/`ConfigManager` elsewhere; they can't be stubbed in
  tests (private constructors), and the wrappers keep a failed lookup from killing a whole event.
- **Assume the game hands you nulls** (NPC names, widget children, menu targets, empty events); check before chaining.
- `PluginIntegrationTest` wires the real Guice graph the way RuneLite does (plugin instance bound, members injected) and
  plays thousands of ticks of simulated kills, chat, loot and rendering against stand-in game objects: normal, null
  state and throwing state. In the normal run `Guard.failures()` must stay empty. Add new event types there.
- `ArchitectureTest` also bans threads, sleeps, `new Gson()`, mutable statics and direct `ItemManager`/`ConfigManager` use.
  The build runs `-Xlint:removal -Werror`, so API scheduled for removal fails here before it fails the hub's build.
- **Settings can be null in tests, so read them through fallbacks**: `MarketPainter.layout(config)` and `color(...)` give a
  default when an enum or colour comes back null; `ValueUnit.toGp` and `playWinSound` accept a null unit/sound. The
  `NULL_CONFIG` run in `PluginIntegrationTest` answers null for every object setting and must stay clean, and
  `paintingNeverThrowsForOddInputs` hands the painter odd sizes, titles and badges in every layout.
- **Java conventions enforced by `ArchitectureTest`**: no locale-dependent `toUpperCase()`/`toLowerCase()`/`String.format`
  (use `Locale.ROOT`/`Locale.US`; `LocaleTest` runs the matching under Turkish and German locales), no `System.out`/
  `printStackTrace`, `@Subscribe` only in the plugin, and the audio and folder-opening calls only in `CustomSound`,
  which catches its own failures (including from its background thread) and falls back to the chime.
- The overlay manager can't be stubbed either, so the integration test tolerates the two overlay register/unregister
  failures and nothing else.

## Hub reviewer rules (learned from review comments)

The hub's bot and maintainers reject, and `ArchitectureTest.restrictedApisAreNeverUsed` forbids: `javax.sound` (use
`net.runelite.client.audio.AudioPlayer`), `LinkBrowser`/`Desktop` (restricted APIs, so the plugin can never open a folder or
a URL for the player: show the path in chat instead), and direct file I/O (`java.io.File`, `Files`, `RUNELITE_DIR`; use
`net.runelite.client.util.Filepath`, obtained from `getPluginDirectory()`). Check a new API against these before using it,
and read the bot's "RL CHECKS" comment on every hub PR.

## Win sounds

`WinSound` is a list of game sound effects played in a row (the bell is six notes) at `Win Sound Volume` (0-127 in the game's
own scale, via `playSoundEffect(id, volume)`). The default is `GE_CHIME`. The Grand Exchange trade jingle itself is music and
cannot be played by a plugin, and Jagex's audio must not be bundled. `CUSTOM` plays the player's own WAV files from
the plugin's data folder, `Plugin.getPluginDirectory()` (needs `internalName` in `@PluginDescriptor`, which the hub requires to equal our hub name, `drop-market`) as a sandboxed `Filepath` (`win.wav`, plus optional `collection-log.wav` and `pet.wav`, see `SoundEvent`; modelled on
c-engineer-completed: fixed names, nothing deleted, subfolders ignored) through RuneLite's `AudioPlayer` (the hub rejects `javax.sound`) on RuneLite's injected executor (never the client thread), falling
back to the chime. The two "button" settings (`testWinSound`, `showSoundFolder`, which prints the folder path in chat) un-tick themselves in `handleConfigChanged`.

## The Fractured Archive (fourth raid)

Added on release day, so it is hand-written from the wiki's Rewards section only: `DropTables.FRACTURED_ARCHIVE` (short name
`TFA`), its eight boss names as aliases (so any fight opens the market), a fixed headline item (`featured`: the market always
bets on the Obligator, the mega-rare), and `isCompletion` accepts the chest under any of its
spellings (`chestName`). Not verified in-game: the bosses' exact NPC names, the loot tracker's name for its reward chest, and
whether the items are named exactly as listed. There is no varbit or region detection yet (the RuneLite API we build against
predates the raid): when `latest.release` brings raid varbits or region IDs, add them to `activitySource()` like CoX/ToB/ToA.
The wiki's drop Bucket had no rows for it on day one, so check `generate-wiki-drops.mjs` output after the next monthly refresh.

## Skilling markets and Hunter

Skilling markets count one action per XP drop, so the counter words are generic where a drop isn't a whole lap or voyage
(`SKILL_UNITS`). Hunter has no catch-all market: `HunterTargets` names the four pet activities (chinchompas, wyrmscraig
goats, the herbiboar, expert/master Hunters' loot sacks) and the plugin only counts Hunter XP that arrives within a tick of a
chinchompa or goat item entering the inventory (`noteHunterGains` / `resolvePendingHunter`), or an opened expert/master sack via
`LootReceived`. The herbiboar is still Hunter + Herblore XP on the same tick. The goat and sack detection rests on item and loot
names that have not been verified in-game (the sack's `LootReceived` name in particular): if a market never opens, check those.

## Multi-part items and the Font of Consumption

- Items that need several untradeable drops (Noxious halberd: blade, point, pommel; Abyssal bludgeon: axon, claw, spine)
  list their `parts` in `valuable-components.json`. Each part is valued at the finished item's price divided by how
  many parts you still need, and a part you already hold is removed from the market pick, because the game won't drop
  duplicates until you have the full set (`OwnedParts`, saved per account by `MultipartTracker`; learned from loot and
  from the inventory/bank; completing a set starts it over).
- Abyssal Sire's market is just the Unsired (every Sire unique comes from it). Standing near the Font of Consumption
  (scenery id 27029) with an Unsired in the inventory opens an "UNSIRED DROPS X" market, picking among the Unsired's
  rewards by value (bludgeon pieces you don't have, whip, dagger, jar, head, and the orphan unless you own it). The
  offering is spotted by the Unsired leaving the inventory; this part has not been verified in-game.

## Wins, in one paragraph (so you don't re-derive it)

A drop is a win via `DropQualifier`: **blacklist** never wins and **priority** always wins; otherwise Resolve Win on Every Drop (`popOffOnEveryDrop`) wins
everything; a collection log item **new to the account** wins (only trusted when the whole log is known via RuneProfile; otherwise the game's own "New item added to your collection log" message is the signal); before prices load only known log slots win; an item with
new tradeable content the GE hasn't priced yet (`NOT_PRICED_YET`) wins if it's a real chase (on the table, the market's target, or a log slot); an **untradeable** item (`NO_PRICE`) only wins if it's new to the log or a pet; untradeable drops that craft into expensive items (`valuable-components.json`, built from wiki recipes + GE prices) are priced at what they become (`ValuableComponents.valueOf`, used inside `priceOf`), except Soulreaper axe parts which are value 0 and just celebrated; a priced item must be on the
table/target and reach the **Win Minimum Drop Value**. Chat messages ("Valuable drop", "Untradeable drop", new log
item) go through the same rules. Odds drift via `DriftProfile` on a 10–15 minute internal clock that only switches after
a finished kill/action and carries over between activities.

## Working style for this project

- **Write like a person on PR comments, PR descriptions and commit messages.** Short and plain: a one-line commit subject
  (with a sentence or two of body only when the why isn't obvious), one to three sentences on a PR comment, no
  bullet-list summaries, bold labels, headings or formal filler. Maintainers read these.
- Keep it light and fun; don't over-engineer. Small, tested changes; show visual changes with a rendered preview.
- Commit locally as you go; push and open hub PRs only when the user asks. Announce what you did plainly, including
  anything you couldn't verify in-game.
