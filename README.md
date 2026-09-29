# Autumn Foliage

NeoForge 1.21.1 vegetation renderer based on the supplied **Autumnpack 3.0** look, but designed to work broadly with vanilla and modded vegetation instead of requiring per-mod texture replacements.

## What changed in alpha.2

- Brighter, more saturated reds/oranges/ambers/golds; the first pass leaned too heavily brown/rust.
- **Default is now autumn everywhere.** An empty range list means the entire world is affected.
- Coordinate ranges are still fully supported when wanted; any number of ranges can be configured.
- The mod is **not required on the server**.
- If the server also has Autumn Foliage installed, its zone settings automatically become authoritative for clients that have the mod.
- Clients without the mod remain unaffected by the server installation.

## Zones

Local/client fallback config (used when a remote multiplayer server does not have the mod):

```toml
[zones]
enabled = true
axis = "Z"
ranges = []
```

`ranges = []` means full-world autumn.

To limit the effect, add one or more ranges:

```toml
ranges = ["-18000,-9000,1500", "7000,16000,1500"]
```

Each entry is:

```text
min,max,fadeDistance
```

Inside `min..max`, autumn is full strength. `fadeDistance` smoothly blends back to normal vegetation outside each edge. Overlapping fades use the strongest effect.

## Optional server policy

The same JAR may be installed on a dedicated server, but it is optional. NeoForge stores the server config in the world's `serverconfig` folder as `autumnfoliage-server.toml`.

Example:

```toml
[zones]
enabled = true
axis = "Z"
ranges = ["-18000,-9000,1500", "7000,16000,1500"]
```

When a client with Autumn Foliage joins a server that also has it:

1. NeoForge syncs the server zone config to that client.
2. An optional Autumn Foliage network handshake tells the client to use the server's zones instead of its local fallback ranges.
3. On disconnect, the client returns to its own local fallback config.

Single-player runs an integrated server, so it uses the world's `serverconfig/autumnfoliage-server.toml`. This is useful because different worlds can have different autumn bands.

The network payload itself is optional, so either side may be missing the mod without making it a required server/client dependency.

## Vegetation compatibility

The renderer is deliberately mod-agnostic:

- Wraps existing vanilla/mod block-color providers rather than replacing them.
- Recognizes `#minecraft:leaves`, vanilla leaf/sapling classes, plus common modded vegetation naming conventions.
- Can assign tint index 0 to otherwise untinted vegetation quads for broader mod support.
- Avoids forced whole-model tint on grass/turf blocks so dirt faces are not recolored.
- Exact block IDs can be force-included or force-excluded.
- Tropical foliage is left green by default.
- Obvious evergreen/conifer foliage receives only a very weak autumn shift.
- Grass, ferns, vines, shrubs and saplings use weaker strengths than deciduous leaves.
- Classification is cached for chunk-building performance.

## Autumnpack reference

The original pack was used as the visual direction: mixed red, orange, amber, gold and dry grass instead of one flat color. Alpha.2 pushes the palette toward the brighter parts of that pack because alpha.1 looked too brown/dreary in dense forests.

The old Autumnpack foliage/grass overrides should not be enabled simultaneously if you want coordinate fades to return to normal green outside configured ranges. Direct resource-pack replacements are global; this mod is coordinate-aware.

## Rendering limitation

Minecraft's normal block tint is multiplicative. Conventional neutral or biome-tinted leaves recolor very cleanly. A mod that bakes a strong green hue directly into its pixels or uses a completely custom renderer may still need a compatibility rule.

## Build

- Minecraft: 1.21.1
- NeoForge: 21.1.252
- Java: 21
- ModDevGradle: 2.0.147
- Version: 0.1.0-alpha.2

Build with:

```text
gradlew build
```

The JAR will be under `build/libs/`.
