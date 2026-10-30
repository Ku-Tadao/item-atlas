# Item Atlas

Where to get every item in Old School RuneScape and what it's used for: shops, drops, ground spawns, recipes and uses, sourced from the OSRS Wiki.

Item Atlas is a fork of [Not Enough Runes](https://github.com/Hannah-GBS/notenoughrunes) by Hannah_GBS and LlemonDuck (BSD 2-Clause, see [LICENSE](LICENSE)).

Item data comes from [Ku-Tadao/runelite-wiki-scraper](https://github.com/Ku-Tadao/runelite-wiki-scraper), a fork of Hannah_GBS's scraper that rebuilds `data.sqlite` from the OSRS Wiki every Friday.

## Routing to sources

Right-click a shop or spawn, or use **Route to closest source** on an item's sources, to get directions. Item Atlas works with whichever route plugin you have enabled:

- **GPS**, if it's enabled (preferred, sent on GPS's own channel so it never routes twice)
- **Shortest Path** otherwise

**Route to closest source** covers shops, every ground-spawn tile, and the locations of monsters and objects that drop the item (toggle each in the config). Routes don't guarantee shop stock, ground-item availability, drop rates or access requirements. Recipes are not included.

## Quest Helper integration

Quest Helper's item lookups still reach Item Atlas, since it keeps listening on Not Enough Runes' message channel.
