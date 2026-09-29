# Autumn Foliage

Autumn Foliage is a NeoForge 1.21.1 client-side vegetation renderer that applies autumn colors dynamically to vanilla and modded foliage while preserving the original block textures and models.

It was developed using the supplied **Autumnpack 3.0** resource pack as a visual reference, but replaces per-texture compatibility work with runtime vegetation detection, configurable color treatment, coordinate regions and optional server policy.


## Compatibility

- **Minecraft:** 1.21.1
- **NeoForge:** any stable **21.1.x** release, from **21.1.1** up to (but not including) 21.2
- **Java:** 21
- **Distant Horizons:** optional; DH-specific recoloring is enabled when a compatible DH 3.3.1+ build is present

The development environment intentionally targets NeoForge **21.1.247**, while the shipped mod metadata uses the broader `[21.1.1,21.2)` runtime range. The development patch version therefore does not become the minimum required version for users. NeoForge's versioning maps the `21.1.x` line to Minecraft 1.21.1, and the upper bound prevents the mod from claiming compatibility with later Minecraft patch lines.

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
- Native Distant Horizons LOD recoloring so distant foliage follows the same autumn system.
- Distant Horizons is included only in the development `runClient` environment and is not an end-user dependency.

## In-game settings

Open **Mods -> Autumn Foliage -> Config**.

The mod uses a purpose-built settings screen rather than exposing raw TOML-style values. Normal options are presented as toggles, sliders and focused editors, with the layout adapting to smaller GUI heights so controls do not overlap the footer. Settings are rendered over a subtle dark content panel and foreground labels are drawn after Minecraft's blurred menu pass so text stays crisp and readable at different GUI scales.

The screen is divided into five simple tabs. **Save & Close** commits the draft and refreshes nearby chunks and Distant Horizons render data immediately; **Cancel** discards unsaved changes. On smaller GUI heights, descriptions are condensed so the controls remain usable without overlapping.

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

Distant Horizons is optional. When present, Autumn Foliage registers a DH block-color override for detected vegetation so LOD foliage uses the same coordinate zones, tropical policy, category strengths and deterministic palette as nearby chunks.

The DH integration uses DH's representative untinted block color and applies the autumn tint through a path designed to resemble normal Minecraft/Sodium leaf tinting. This keeps the transition between nearby rendered chunks and distant LOD terrain substantially more consistent.

Changing Autumn Foliage settings asks Distant Horizons to rebuild its visible render-data cache so already-generated green LODs do not remain on screen. The underlying DH terrain/full-data database is not deleted.

## Configuration files

The GUI is the recommended way to change client settings, but the underlying files remain normal NeoForge TOML configs:

- `autumnfoliage-client.toml` - local appearance, regions and compatibility settings
- `autumnfoliage-server.toml` - optional server policy and client-override permissions

## Notes

Autumn Foliage is a rendering mod. It does not replace blocks, modify biomes, change world generation or write seasonal state into the save.
