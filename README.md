# Autumn Foliage

Autumn Foliage is a NeoForge 1.21.8 client-side vegetation renderer that applies autumn colors dynamically to vanilla and modded foliage while preserving the original block textures and models.

It was developed using the supplied **Autumnpack 3.0** resource pack as a visual reference, but replaces per-texture compatibility work with runtime vegetation detection, configurable color treatment, coordinate regions and optional server policy.



### Bamboo

Vanilla `minecraft:bamboo` is treated as tropical vegetation. Its living model is split by texture sprite: leaf/sapling textures and `bamboo_stalk` use separate bamboo-specific autumn multipliers. Because vanilla bamboo textures are already green, these palettes deliberately suppress green and vary smoothly in world space instead of using the normal hard red/orange/gold tree-family regions. Bamboo building blocks such as planks, fences, doors, mosaics, and stripped bamboo blocks are not recolored.

## Compatibility

- **Minecraft:** 1.21.8
- **NeoForge:** **21.8.x** for Minecraft 1.21.8 (development target: **21.8.54**)
- **Java:** 21
- **Distant Horizons:** optional; supported/tested with DH 3.3.2+ for Minecraft 1.21.8

The 1.21.8 port is developed against NeoForge **21.8.54**, but the packaged compatibility range covers the full Minecraft 1.21.8 NeoForge line (`[21.8.0-beta,21.9)`) rather than requiring one exact loader build. The development target and runtime compatibility range are stored separately so updating the dev loader does not accidentally lock out players on another 21.8.x build. Distant Horizons remains optional. The development client uses **DH 3.3.2 for 1.21.8**. The integration compiles against the standalone DH API 7.1 baseline and uses only public API classes; DH 3.3.2 ships the newer API 7.2 line while retaining the API surface used here.

For release checks, `scripts/test-neoforge-compat.ps1` compiles the same source against 21.8.0-beta, 21.8.9 (the first non-beta 21.8 build), and 21.8.54. This is a compatibility smoke test; normal development still uses the single `neo_version` target in `gradle.properties`.

## Features

- Dynamic autumn coloring for vanilla and modded vegetation.
- Broad leaf/vegetation detection through tags, block classes and mod-friendly naming fallbacks.
- Covers leaves, saplings, grass/ferns, vines, shrubs and other common foliage.
- Species-aware red, orange and gold palette families with deterministic world-space variation.
- Canopy-coherent color regions use irregular boundaries and smooth within-tree shading to avoid trees splitting into obvious half-red/half-orange blocks.
- Brightness and vibrancy controls designed to remain colorful even on dark modded leaf textures.
- Conifers stay mostly green by default.
- Tropical/jungle vegetation can remain green or become autumnal.
- Optional coordinate ranges with smooth fade distances.
- Any number of coordinate ranges is supported; no ranges means autumn applies everywhere.
- Client-only installation is supported; the server does not require the mod.
- Optional server installation can enforce world climate/appearance settings while selectively allowing client overrides.
- Native Distant Horizons LOD recoloring so distant foliage follows the same autumn system, including a dedicated blended bamboo LOD tint.
- Optional real-world calendar season that fades autumn in/out across configurable dates instead of switching instantly.
- Small configurable per-species timing offsets and local timing variation keep entire forests from changing on the exact same day.
- Distant Horizons is included only in the development `runClient` environment and is not an end-user dependency.
- Custom item renderers are left untouched. Autumn Foliage only wraps block-state models/model parts for untinted-vegetation fallback, avoiding conflicts with mods such as Create that depend on their own item rendering implementations.

## In-game settings

Open **Mods -> Autumn Foliage -> Config**.

The mod uses a purpose-built settings screen rather than exposing raw TOML-style values. Normal options are presented as toggles, sliders and focused editors, with the layout adapting to smaller GUI heights so controls do not overlap the footer. Settings are rendered over a subtle dark content panel and foreground labels are drawn after Minecraft's blurred menu pass so text stays crisp and readable at different GUI scales.

The screen is divided into six simple tabs. **Save & Close** commits the draft and refreshes nearby chunks and Distant Horizons render data immediately; **Cancel** discards unsaved changes. On smaller GUI heights, descriptions are condensed so the controls remain usable without overlapping.

### General

- **Autumn Effect** - master on/off switch.
- **Autumnal Tropics** - controls whether jungle/rainforest/tropical vegetation also receives autumn colors.
- **Coverage** - shows whether autumn currently applies to the whole world or configured coordinate ranges.

### Colors

- **Vibrancy** - saturation of autumn reds, oranges and yellows.
- **Brightness** - midtone lift for foliage colors.
- **Color Patch Size** - controls local color variation scale. Family changes use much larger irregular tree-neighborhood regions, while shading varies smoothly within a canopy.

### Plants

Separate strength sliders are provided for:

- leaves
- saplings
- grass and ferns
- vines and shrubs
- evergreens

All strengths are shown as percentages instead of raw decimal values.

### Season

Calendar timing is optional and disabled by default so existing installations keep the normal always-autumn behavior. When enabled, Autumn Foliage reads the client computer's **local calendar date** and multiplies it into the normal zone/vegetation strength.

- **Autumn Starts** - first day where the seasonal fade begins.
- **Autumn Ends** - final day of the fade-out.
- **Blend Time** - number of days used at both ends of the season for gradual transitions.
- **Local Variation** - small deterministic +/- day variation between nearby stands so a forest does not change all at once.
- **Species Timing** - editable `keyword=days` offsets. Negative values shift a species earlier and positive values later. The most-specific matching block-name keyword wins.

Default species offsets are deliberately subtle: birch/aspen/maple begin a few days earlier than oak, while beech/willow begin a few days later. Modded species can be added by name fragment. Date windows that cross New Year are supported.

Example:

```toml
[season]
calendarTimingEnabled = true
autumnStartDate = "09-01"
autumnEndDate = "11-30"
seasonBlendDays = 21
seasonalVariationDays = 2
speciesTimingOffsets = ["birch=-5", "maple=-3", "oak=0", "beech=2"]
```

The calendar is evaluated client-side. The cached date is checked once per second; when the local day changes, nearby chunks and Distant Horizons render data are refreshed automatically.

### Regions

Coordinate ranges are edited visually; there is no need to type `min,max,fadeDistance` strings in the normal GUI.

Each range has:

- start coordinate
- end coordinate
- fade distance

The axis can be switched between **X** and **Z**. Any number of ranges can be added. Removing every range returns to the default whole-world behavior.

Example conceptually:

```text
Range 1: -18,000 to -9,000 | fade 1,500
Range 2:   7,000 to 16,000 | fade 1,500
```

Inside each range the effect is full strength. The fade distance smoothly blends vegetation back to normal outside the range edges.

### Advanced

Compatibility-oriented values are kept out of the normal appearance pages:

- tint otherwise untinted modded vegetation
- force-included block IDs
- force-excluded block IDs
- evergreen keywords
- tropical block keywords
- tropical biome keywords

Lists use dedicated one-entry-per-row editors with clear Add/Remove controls, validation, page indicators and a reset-to-default button.

## Server-controlled settings

The server mod is optional.

If the server does not have Autumn Foliage installed, all settings come from the local client configuration.

If the server does have the mod installed, it can make some settings authoritative. On a **remote multiplayer server**, authoritative values are shown directly and their controls are disabled with a `Server:` label rather than leaving the player guessing why a local value is not taking effect.

In **singleplayer**, the integrated server belongs to the local player, so world/server-owned values are not locked. The same GUI edits them directly and saves those world-facing values back into the world server config. This also means the chosen world policy is already in place if the world is later opened to LAN.

The server can independently choose whether clients may override:

```toml
[clientOverrides]
allowClientEnabledOverride = false
allowClientZoneOverride = false
allowClientTropicalOverride = false
allowClientAppearanceOverride = true
```

This allows setups such as:

- server controls the world coordinate bands
- server controls whether autumn is enabled
- each client chooses whether tropical vegetation becomes autumnal
- each client chooses its own appearance intensity

Compatibility values such as force-includes/excludes remain client-local because different players may use different rendering and vegetation mods.

Dedicated server policy is stored in `autumnfoliage-server.toml`. The client settings screen intentionally does not pretend a remote player can edit server-owned policy.

## Default behavior

With no coordinate ranges configured, autumn applies across the entire world.

`autumnalTropics = false` by default, so tropical vegetation remains green unless explicitly enabled.

Current default appearance values are:

```text
Vibrancy:       135%
Brightness:     120%
Leaves:         100%
Saplings:        90%
Grass/Ferns:     46%
Vines/Shrubs:    72%
Evergreens:      10%
Patch Size:      10 blocks
```

## Distant Horizons

Distant Horizons is optional. When present, Autumn Foliage registers a DH block-color override for detected vegetation so LOD foliage uses the same coordinate zones, calendar-season strength, tropical policy, category strengths and deterministic palette as nearby chunks.

For the supported DH 3.3.2+ line, Autumn Foliage uses DH's representative untinted block color directly when available, giving LOD foliage the closest match to Minecraft's normal texture-and-tint path. Distant Horizons represents a bamboo block with one sampled color rather than Minecraft's separate leaf/stalk model quads, so bamboo LODs use a dedicated composite of the bamboo leaf and stalk palettes. A legacy fallback remains in the code for older API implementations, but DH 3.3.2+ is the supported 1.21.8 runtime combination.

Changing Autumn Foliage settings asks Distant Horizons to rebuild its visible render-data cache so already-generated green LODs do not remain on screen. The underlying DH terrain/full-data database is not deleted.

### NeoForge development client note

NeoForge 1.21.8 enables its Blaze3D validation wrapper in development. Distant Horizons accesses the backing OpenGL texture implementation directly, so a `runClient` session can otherwise crash when DH receives NeoForge's `ValidationGpuTexture` wrapper instead of `GlTexture`. The Gradle `runClient` task therefore depends on `prepareNeoForgeClientConfig`, which ensures `run/config/neoforge-client.toml` contains `enableB3DValidationLayer = false` before launch. This only affects the local development runtime; it is not packaged into the mod jar.

## Configuration files

The GUI is the recommended way to change client settings, but the underlying files remain normal NeoForge TOML configs:

- `autumnfoliage-client.toml` - local appearance, calendar season, regions and compatibility settings
- `autumnfoliage-server.toml` - optional server policy and client-override permissions

## Notes

Autumn Foliage is a rendering mod. It does not replace blocks, modify biomes, change world generation or write seasonal state into the save.

- Bamboo compatibility: `bamboo` is a default tropical keyword. Vanilla bamboo leaf/sapling quads and stalk quads are explicitly routed to separate bamboo-specific tint channels. Their palettes use smooth variation only and stronger green suppression so pre-colored vanilla bamboo cannot randomly appear unchanged beside autumn-tinted plants. Existing configs that still contain either of the older unmodified default keyword lists are automatically migrated to the current defaults.
- Tropical block defaults intentionally stay foliage/vegetation-focused: jungle, palm/palmetto, bamboo, mangrove, mahogany, teak, ebony, kapok/ceiba, banyan, baobab, rubber, rattan and liana. Fruit, vegetable and crop names are not included by default.
