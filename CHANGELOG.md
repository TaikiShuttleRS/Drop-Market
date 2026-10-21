# Changelog

## 1.3.0

- Skilling counters use words that match what is counted: Mining counts "rocks", Fishing "catches", Runecrafting "crafts", and Agility and Sailing count "actions" (each XP drop is one obstacle or action, not a lap or a voyage)
- Hunter no longer has one catch-all market. Its four pets each get their own: Chinchompas (Baby chinchompa), Wyrmscraig goats (Mr McGroot), the Herbiboar (Herbi) and Hunter rumours (Quetzin, from opening expert and master Hunters' loot sacks). Hunter XP from anything else, like birds or kebbits, doesn't count
- A small dash between the two odds in the two-number Full view fills the empty space
- The Fractured Archive (the fourth raid, released 20 October 2026) has a market like the other raids: its uniques (Obligator, Zorya's tome, Ascension crossbows, Rondache, Horn of Bran, Archivist's folio, the four elemental fragments, Orion), short name "TFA", and fighting any of its bosses (Archive Guardian, Ezekiel, Bork and the Smorks, Moia, Dreadnoth the Jellied, Xarth, Dagon'hai Elder, Ocellus) opens it. Its market always bets on the Obligator, the mega-rare. Kill counts come from its reward chest
- Fix: on a win, the Full view with two numbers kept only line 1's number when the changes were large; both numbers now shrink slightly to fit
- Win sounds are louder: a new Win Sound Volume setting (100% by default) plays them at their own volume
- The Grand Exchange Chime is the default win sound
- Custom Win Sound: pick "Custom file" and play your own WAVs from the plugin's own data folder (win.wav, plus optional collection-log.wav and pet.wav for those wins), with Show Sound Folder Path and Test Win Sound buttons, a how-to file in the folder, played with RuneLite's own audio player, and the chime as a fallback
- New win sounds Grand Exchange Chime, Collection Log and Gem Alert; "Collect bloop" is gone; the town crier bell now rings up and down six times
- The change numbers stay put during a celebration instead of dropping to 0% when another drop lands
- The odds lines are drawn rigid like a real market chart: flat holds, straight runs, sharp corners and sudden jumps instead of a smooth wiggle
- Long titles never get cut off: Compact puts a title that doesn't fit on a second line (the card grows to hold it), and only if it would need a third uses the wiki short names (ToB, ToA, CoX, and items like fang, shadow, Tbow); Full wraps to two lines and scrolls only as a last resort
- Compact with the second odds number shrinks the numbers like the ticker does, so the sparkline is visible again
- New "Show Both Delta Numbers" toggle (Graph Line 2); off shows only line 1's change
- Full card: the change indicators sit 1px higher
- "Pop Off on Every Drop" is now "Resolve Win on Every Drop" and lives in Celebration
- The two drop values are entered in k by default, with a Value Units setting (k, m, b); your old gp values are carried over
- Settings sections reordered by importance (Data Sources, Display, Drop Lists, Market Value, Celebration), with Graph Line 1/2 and Header Bar 1/2 split into their own sections

## 1.2.1

- Fix: the overlay crashed the client when the plugin was installed (the config used package-private types RuneLite couldn't read)
- Every event handler, the overlay, start/stop and the network callbacks now run behind a guard, so a bug in the plugin is logged once and skipped instead of reaching the client
- Item, price and per-account settings access go through small wrappers that never throw
- A test now runs the whole plugin through simulated sessions (normal, null-filled and throwing game state) and checks the structure rules

## 1.2.0

- Abyssal Sire's market is the Unsired; at the Font of Consumption with an Unsired you get an "UNSIRED DROPS X" market valued by the rewards you can still get
- Multi-part drops (Noxious halberd, Abyssal bludgeon) split the finished item's value across the parts you're missing and skip parts you already hold
- Fix: a win no longer keeps flashing while you keep killing the same monster; it ends after Celebration Seconds
- Repeat untradeable uniques (e.g. Xeric's talisman) no longer count as wins unless they are new to your log, or a pet (or a drop that crafts into something expensive, see below)
- Untradeable drops that craft into expensive items (vestiges, Araxyte fang, Mokhaiotl cloth...) are valued at the price of what they become, so Vardorvis counts the Ultor vestige as a 110m drop. Soulreaper axe parts just count as worth celebrating
- "New to your log" is only trusted when RuneProfile sync has the whole log; otherwise the game's own new-collection-log message is used, so an item you already own no longer flashes as new
- A second big odds number (Show Second Odds Number) shown at the right of the box; Steady now ranges about 6-72%
- Drift profiles last 10-15 minutes on an internal clock and only change after a finished kill or action, whatever the activity
- Odds now move in "drift profiles": Steady (everyday), Climber (rises toward ~90 as a fight nears its end), Choppy, Sleeper, Spike and Fader; everyday highs top out around 82
- Overlay Layout setting: Full, Compact (two rows with a sparkline, wiki abbreviations like ToB) or Ticker (a single scrolling line)
- Optional Win Sound using built-in game sounds
- Both lines now flash purple on a win when the second line is shown
- Settings are grouped into collapsible sections with clearer names; saved settings are kept
- Removed Pop Off Min Value (Win Minimum Drop Value covers value gating)
- Renamed to Prediction Market Drops

## 1.1.0

- Blacklist and priority drop lists; Win Minimum Drop Value; Flash on Win; Celebration Seconds; Pop Off on Every Drop
- Wins: collection log items new to the account, items with no GE price yet, or drops over the value threshold
- Most drop tables now ship inside the plugin (OSRS Wiki snapshot); live lookups and RuneProfile sync stay opt-in
