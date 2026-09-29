# Autumn Foliage

A client-side NeoForge 1.21.1 rendering mod that applies an Autumnpack-inspired fall palette to vanilla and modded vegetation only inside configurable world-coordinate ranges.

## What it does

- Supports any number of autumn coordinate ranges; two is simply the normal use case.
- Full autumn effect inside each range, smooth configurable fade outside both edges.
- Overlapping fades use the strongest range rather than stacking.
- `X` or `Z` climate axis.
- Keeps vegetation outside the ranges unchanged.
- Uses existing block tint providers when mods already define them.
- Forces untinted vegetation models through the tint path for broader mod support.
- Uses tags, vanilla block classes, resource IDs, and configurable include/exclude overrides.
- Keeps obvious tropical vegetation green by default.
- Keeps obvious evergreen/conifer foliage mostly green by default.
- Gives grass/ferns a weaker dry autumn shift than deciduous leaves.
- Uses smooth deterministic color patches so forests vary between red, rust, orange, amber and gold instead of becoming one flat color.
- Rendering only: no block changes, no save changes, no server installation required.

## Default config

NeoForge creates `config/autumnfoliage-client.toml` on first launch. The important section looks like this:

```toml
[zones]
enabled = true
axis = "Z"
ranges = ["-18000,-9000,1500", "7000,16000,1500"]
```

Each range is:

```text
min,max,fadeDistance
```

For `-18000,-9000,1500`:

- Z -18000 through -9000: full autumn.
- Z -19500 through -18000: smooth normal -> autumn transition.
- Z -9000 through -7500: smooth autumn -> normal transition.
- Everywhere beyond those fades: untouched.

Add as many range strings as needed.

## Compatibility controls

Exact block ids can be forced in or out:

```toml
[compatibility]
forceInclude = ["somemod:odd_tree_foliage"]
forceExclude = ["somemod:decorative_red_leaves"]
```

The default classifier recognizes normal `#minecraft:leaves`, vanilla leaf/sapling classes, and common mod naming conventions. It also has configurable evergreen/tropical keyword lists.

## Important rendering limitation

Minecraft's standard block tint is multiplicative. If a mod ships leaves with the green color baked directly into the texture instead of using a neutral/tintable texture, Autumn Foliage can force that model into the tint pipeline but cannot perfectly replace the baked pixel hue. Most conventional modded leaves that use biome tinting or neutral leaf textures should recolor cleanly; unusual custom renderers may need a dedicated compatibility rule.

## Sodium

The implementation stays on Minecraft/NeoForge's normal baked-model tint path rather than using a custom core shader or mixin renderer replacement. This is intentional for Sodium compatibility: Sodium also consumes baked quad tint indices and block color providers.

## Build

Requires Java 21.

This source tree is based on the official NeoForge 1.21.1 ModDevGradle MDK and targets NeoForge `21.1.252`.

If your project already has a Gradle wrapper, copy this source into it and run:

```text
gradlew build
```

The output jar will be under `build/libs/`.
