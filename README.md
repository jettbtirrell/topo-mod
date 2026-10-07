# topo_mod

Fabric mods for Minecraft 26.3. Each mod is one jar that runs on both client and dedicated server.

| Module | Mod id | What it is |
|---|---|---|
| `allay-variants/` | `allay_variants` | Allay crossbreeds, starting with the bunnay: a rabbit-allay companion that fights, farms and hops |
| `topo/` | `topo` | The topo, a mouse companion that holds torches, shards and pearls. **Not part of the build by default** (see below) |
| `damage-numbers/` | `topo_damage_numbers` | Floating damage numbers when entities are hit |
| `bundle/` | `topo_mod` | No code; embeds the mods above in one jar |

## Develop

- `./gradlew build` - build every jar into each module's `build/libs/` (use the jar without `-sources`)
- `./gradlew :allay-variants:runClient` / `:allay-variants:runServer` - dev client / server (also loads damage numbers)
- `python3 tools/import_bbmodel.py topo` (or `bunnay`) - turn the saved Blockbench project into the model code and texture

Requires JDK 25.

### Building the topo mod

Topo is its own mod, but it is left out of the build, and out of the bundle, for now. To include it, set
`include_topo=true` in `gradle.properties`, or pass `-Pinclude_topo=true` to Gradle. Its jar is then
`topo/build/libs/topo-*.jar`, the bundle also embeds it, and `:topo:runClient` works.

## Install

Copy either individual jar, or the single `topo_mod-*.jar` bundle, into `mods/` on the server and each client.
Fabric API is also required.

Worlds saved with the old `topo_companion` mod will not find its bunnays or topos any more: the ids changed to
`allay_variants:` and `topo:`.
