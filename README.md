# Autumn Foliage

Autumn Foliage is a NeoForge 1.21.1 vegetation renderer that applies autumn colors dynamically to vanilla and modded foliage while preserving the original block textures and models.

It was designed from the supplied **Autumnpack 3.0** resource pack as a visual reference, but replaces per-texture compatibility work with broad runtime vegetation detection and tinting.

## Main features

- Dynamic autumn colors for vanilla and modded vegetation.
- Detects leaves through vanilla tags/classes plus mod-friendly naming fallbacks.
- Covers leaves, saplings, grass/ferns, vines, shrubs and other common vegetation.
- Species-aware red/orange/gold palette families plus deterministic world-space patch variation, with controlled cross-family variation so even single-species forests do not collapse into one color.
- Birch/aspen/poplar-like trees favor vivid golds, maple/acacia-like trees favor reds, oak/beech-like trees favor oranges, and unknown modded species receive a stable family automatically.
- Adjustable foliage vibrancy and brightness, with stronger high-chroma defaults and gamma-based midtone lifting intended to survive multiplication against dark leaf textures.
- Obvious conifers remain mostly green by default.
- Tropical/jungle vegetation can remain lush or be made autumnal with one setting.
- Optional coordinate ranges with smooth fade distances.
- Any number of coordinate ranges is supported; an empty range list means autumn everywhere.
- Client installation is sufficient; the server does not require the mod.
- If the server also installs the mod, it can provide world-wide policy to clients that have Autumn Foliage.
- Native Distant Horizons LOD recoloring through the DH API, so distant forests use the same autumn treatment as nearby chunks.
- Distant Horizons is included as a development-only `runClient` dependency for compatibility testing; it is not bundled or required by Autumn Foliage.

## In-game configuration

Open **Mods -> Autumn Foliage -> Config**. NeoForge's built-in configuration screen exposes the client configuration and, where permitted, the current world's server configuration. Changes are picked up by Autumn Foliage and trigger a chunk render refresh; no Minecraft restart is intended for normal visual settings.

The coordinate range list uses:

```text
min,max,fadeDistance
```

Example:

```text
-18000,-9000,1500
7000,16000,1500
```

With `ranges = []`, autumn applies to the entire world.

## Tropical vegetation

`autumnalTropics = false` is the default. Tropical blocks and vegetation inside jungle/rainforest-like biomes remain green. Set it to `true` for autumnal tropical vegetation as well.

Detection is configurable through tropical block and biome keyword lists.

## Optional server policy

The server mod is optional. If absent, the client uses its local configuration normally.

If the server has Autumn Foliage installed, its synced server config can provide:

- master autumn enabled state
- coordinate axis and autumn ranges
- autumnal tropical policy
- leaf/sapling/grass/vine/evergreen strengths
- foliage vibrancy and brightness
- color patch size

The server can selectively allow client overrides:

```toml
[clientOverrides]
allowClientEnabledOverride = false
allowClientZoneOverride = false
allowClientTropicalOverride = false
allowClientAppearanceOverride = true
```

For example, a server can enforce its climate/coordinate bands while setting `allowClientTropicalOverride = true` so each player decides whether jungle vegetation becomes autumnal.

Client-only compatibility values such as force-included/excluded blocks and tint-model workarounds always remain local because different clients may use different rendering or vegetation mods.

## Distant Horizons

Distant Horizons is optional. When it is installed, Autumn Foliage registers a DH block-color override for detected vegetation so LOD foliage uses the same coordinate zones, tropical policy, category strengths and deterministic palette as normal Minecraft chunks.

DH exposes both its finished LOD color and the untinted representative base color sampled from the block texture. Alpha.8 now rebuilds the DH autumn result from that base color multiplied by the same autumn tint used by normal Minecraft/Sodium rendering. This mirrors the near-chunk tint pipeline instead of painting DH foliage with a flat final palette color, which greatly reduces the bright-LOD/dull-near-chunk handoff while retaining the same red/orange/gold selection.

Changing Autumn Foliage settings also asks Distant Horizons to clear and regenerate its render-data cache. This is intentional: otherwise already-built green LOD buffers can remain visible after the nearby vanilla chunks have changed color. The underlying DH terrain/database data is not deleted.

## Notes

The mod only changes rendering. It does not replace blocks, modify biomes, or write seasonal state into the world save.
