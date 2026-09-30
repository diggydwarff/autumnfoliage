# Autumn Foliage — Forge 1.20.1

A client-side seasonal foliage recoloring mod with optional server-controlled autumn zones.

This Forge 1.20.1 port preserves the final behavior from the NeoForge 1.21.x branch, including:

- autumn leaf, sapling, grass, fern, vine and shrub recoloring
- evergreen and tropical handling
- sprite-aware bamboo leaf/stalk seasonal coloring
- Distant Horizons LOD recoloring, including the dedicated bamboo LOD path
- smooth coordinate ranges with fades
- force include/exclude and compatibility keyword lists
- optional real-world calendar season timing
- configurable MM-dd season start/end dates and blend duration
- subtle per-species timing offsets and local timing variation
- optional server policy with selective client overrides
- purpose-built in-game settings screens

Calendar timing is disabled by default. When enabled it reads the computer's local date and smoothly changes seasonal strength through the configured autumn window.

## Target

- Minecraft 1.20.1
- Forge 47.4.x
- Java 17
- Distant Horizons optional; development/API target 3.3.2
