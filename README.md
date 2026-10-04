# topo_mod

Fabric mods for Minecraft 26.3. Each mod is one jar that runs on both client and dedicated server.

| Module | Mod id | What it is |
|---|---|---|
| `companion/` | `topo_companion` | Mouse companion, plus armor and weapons that grant skills |
| `damage-numbers/` | `topo_damage_numbers` | Floating damage numbers when entities are hit |
| `bundle/` | `topo_mod` | No code; embeds both mods in one jar |

## Develop

- `./gradlew :companion:runClient` / `:companion:runServer` - dev client / server for one mod
- `./gradlew build` - build every jar into each module's `build/libs/` (use the jar without `-sources`)

Requires JDK 25.

## Install

Copy either individual jar, or the single `topo_mod-*.jar` bundle, into `mods/` on the server and each client.
Fabric API is also required.
