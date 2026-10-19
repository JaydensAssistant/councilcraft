# CouncilCraft

Custom gameplay mod for the CouncilCraft vanilla+ pack. Minecraft **1.20.1**, Fabric Loader **0.19.5**, Fabric API **0.92.12+1.20.1**, Java **17**. The mod id is `councilcraft`. It is required on the client and on the server.

Jayden builds the jar from this source. The only command is:

```sh
./gradlew build
```

That compiles against the locked pack mods (fetched from Modrinth Maven), runs the unit tests, and writes `build/libs/councilcraft-<version>.jar`. Nothing is published. Do not commit that jar. The Gradle wrapper jar is the only binary in the repo; it is the build tool, not a mod.

You need JDK 17 or newer on the path (Gradle can run on 21; the class files are still Java 17) and a network connection the first time, so Gradle can download Minecraft, Yarn, Fabric, and the hooked mods.

## What this repo is

One mod, four review slices. Each slice is a pull request. Numbers such as S226 refer to `SPEC.md`. A line tagged SUPERSEDED is not what the code does; the newer entry named in the tag is. Every constant is in `CouncilCraftRules`.

Hooks 19–21 and the POST-MVP list in `councilcraft-hooks.md` are not implemented. Hook 17 (Regions Unexplored planks in the Small Ships dockyard) is not implemented either: Stage 1 already checked that every dockyard recipe uses `#minecraft:planks` and that Regions Unexplored planks are in that tag, so no mixin is required.

## Hook map

| Hook | In game | Source |
|---|---|---|
| 1 Coin values | Not in this PR. | — |
| 2 Government molds | Not in this PR. | — |
| 3 Baby respawn | Not in this PR. | — |
| 4 Blaze Forger clamp | Not in this PR. | — |
| 5 Printed copies | Not in this PR. | — |
| 6 Horseshoe tiers | Not in this PR. | — |
| 7 Netherite horseshoes | Not in this PR. | — |
| 8 Gold horseshoe loot | Bastion, nether-bridge, desert-pyramid and end-city injections lose gold horseshoes. Iron and diamond injections stay. | `mixin/GoldHorseshoeLootMixin.java`, `mixin/LootPoolBuilderAccess.java`, `mixin/ItemEntryAccess.java`, `rules/GoldHorseshoeLoot.java` |
| 9 Horse step and speed cap | Not in this PR. | — |
| 10 Mutant creeper grief | Mutant creeper and creeper minion explosions hurt entities and break no blocks. `mobGriefing` stays on. | `mixin/MutatedExplosionGriefMixin.java`, `mixin/CreeperMinionGriefMixin.java`, `rules/CreeperGrief.java` |
| 11 Riptide | Riptide launch strength is half of vanilla. | `mixin/TridentRiptideMixin.java`, `rules/RiptideLaunch.java` |
| 12 Depth Strider | Depth Strider does nothing while Dolphin's Grace is active. | `mixin/LivingEntityDepthStriderMixin.java`, `rules/DepthStriderRule.java` |
| 13 Paraglider repair | A paraglider can be repaired with a phantom membrane (leather still works too). | `mixin/ParagliderRepairMixin.java`, `rules/ParagliderRepair.java` |
| 14 Sack nesting | Sacks cannot go inside bundles or Create toolboxes. | `mixin/SackNestingMixin.java`, `rules/SackNesting.java` |
| 15 Toolbox whitelist | Not in this PR. | — |
| 16 Patrol evoker | Not in this PR. | — |
| 17 RU planks in dockyard | No code. Stage 1 confirmed the dockyard already accepts them. | — |
| 18 Sides and handshake | A client without this mod, or on another version, is kicked at login, after the other login queries have finished. | `fabric.mod.json`, `handshake/LoginHandshake.java`, `mixin/LoginAcceptMixin.java`, `CouncilCraftClient.java`, `rules/HandshakeRule.java` |

Mixins into other mods are required (`defaultRequire: 1`). A renamed target crashes startup and names the mixin in the log. `CouncilCraftMixinPlugin` does the same if Horseshoes, Mutant Monsters, Paragliders or Supplementaries is not installed.

## How to test in a singleplayer world

Install this jar next to the pack mods. Create a creative world.

- **Riptide.** Enchant a trident with Riptide III, stand in rain or water, hold right click. The lunge should be clearly shorter than vanilla (about half). Riptide I and II are halved the same way. A dry trident still does not launch.
- **Depth Strider.** Wear Depth Strider III boots and swim. Note the speed. Then gain Dolphin's Grace (potion or a dolphin). Swim speed should match Grace alone, not the stacked Grace + Depth Strider sprint. Take the boots off: Grace alone is unchanged. Let Grace expire: Depth Strider alone is unchanged.
- **Paraglider.** Damage a paraglider on an anvil. A phantom membrane is a valid repair material. Leather still is, too. The 384 durability is the Paragliders config, not this mod.
- **Sacks.** Craft a Supplementaries sack. It does not fit in a bundle. It does not fit in a Create toolbox. (Supplementaries 1.20-3.1.44 already returns false here; the mixin keeps it that way.)
- **Gold horseshoes.** `/loot spawn ~ ~ ~ loot minecraft:chests/nether_bridge` and the four `minecraft:chests/bastion_*` tables. No gold horseshoes. Desert pyramid and end city loot can still roll iron or diamond horseshoes, not gold. Iron and diamond horseshoes elsewhere are unchanged.
- **Creepers.** Spawn `mutantmonsters:mutant_creeper` and `mutantmonsters:creeper_minion` and let them explode next to stone and next to a mob. The mob takes damage. The stone does not break. A vanilla creeper still breaks blocks while `mobGriefing` is true. The minion tracker's "destroys blocks" toggle cannot turn block damage back on.
- **Handshake.** Join this same world: you have the mod, so you get in. On a dedicated server, a client without the jar is disconnected with "This server requires the CouncilCraft mod. Install the CouncilCraft modpack." A client built from a different `mod_version` is disconnected with both version numbers. Both sentences are literal text, because that client is not running this mod. The disconnect is sent when login is about to accept the player, after every login query has been answered, so the client shows that sentence instead of "Connection reset by peer".

`./gradlew test` covers the arithmetic (half riptide, depth-strider level, which loot entries survive, version match). It does not launch a world. The checks above need one.
