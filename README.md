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
| 6 Horseshoe tiers | Iron shoes are +0.05 speed / 3.5 armor / 2.1 step. Diamond shoes are +0.075 / 5.5 / 2.6. No gold tier. | `mixin/HorseshoeSpeedMixin.java`, `mixin/HorseEquipmentMixin.java`, `rules/HorseshoeTiers.java` |
| 7 Netherite horseshoes | New item: +0.1 speed, 7 armor, 0.1 knockback resistance, 3.1 step, fireproof. Smithing: netherite template + diamond horseshoes + netherite ingot. | `item/NetheriteHorseshoes.java`, `data/councilcraft/recipes/netherite_horseshoes.json` |
| 8 Gold horseshoe loot | Bastion, nether-bridge, desert-pyramid and end-city injections lose gold horseshoes. Iron and diamond injections stay. | `mixin/GoldHorseshoeLootMixin.java`, `mixin/LootPoolBuilderAccess.java`, `mixin/ItemEntryAccess.java`, `rules/GoldHorseshoeLoot.java` |
| 9 Horse step and speed cap | Step height applies only while the horse is ridden. Every AbstractHorse (horse, zebra, donkey, mule, camel, llama) is capped at 0.44 movement speed. | `mixin/HorseEquipmentMixin.java`, `mixin/AbstractHorseSpeedCapMixin.java`, `mixin/CamelSpeedCapMixin.java`, `mixin/LivingEntityHorseSpeedMixin.java`, `rules/HorseStep.java`, `rules/HorseSpeed.java` |
| 10 Mutant creeper grief | Mutant creeper and creeper minion explosions hurt entities and break no blocks. `mobGriefing` stays on. | `mixin/MutatedExplosionGriefMixin.java`, `mixin/CreeperMinionGriefMixin.java`, `rules/CreeperGrief.java` |
| 11 Riptide | Riptide launch strength is half of vanilla. | `mixin/TridentRiptideMixin.java`, `rules/RiptideLaunch.java` |
| 12 Depth Strider | Depth Strider does nothing while Dolphin's Grace is active. | `mixin/LivingEntityDepthStriderMixin.java`, `rules/DepthStriderRule.java` |
| 13 Paraglider repair | A paraglider can be repaired with a phantom membrane (leather still works too). | `mixin/ParagliderRepairMixin.java`, `rules/ParagliderRepair.java` |
| 14 Sack nesting | Sacks cannot go inside bundles or Create toolboxes. | `mixin/SackNestingMixin.java`, `rules/SackNesting.java` |
| 15 Toolbox whitelist | A Create toolbox only accepts items in the `councilcraft:toolbox_contents` tag. | `mixin/ToolboxInsertMixin.java`, `rules/ToolboxFilter.java`, `data/councilcraft/tags/items/toolbox_*.json` |
| 16 Patrol evoker | Not in this PR. | — |
| 17 RU planks in dockyard | No code. Stage 1 confirmed the dockyard already accepts them. | — |
| 18 Sides and handshake | A client without this mod, or on another version, is kicked at login, after the other login queries have finished. | `fabric.mod.json`, `handshake/LoginHandshake.java`, `mixin/LoginAcceptMixin.java`, `CouncilCraftClient.java`, `rules/HandshakeRule.java` |

Mixins into other mods are required (`defaultRequire: 1`). A renamed target crashes startup and names the mixin in the log. `CouncilCraftMixinPlugin` does the same if Horseshoes, Mutant Monsters, Paragliders, Supplementaries or Create is not installed.

## Textures

The netherite horseshoe texture is a hand recolor of Horseshoes 1.0.3 `assets/horseshoes/textures/item/diamond_horseshoes.png` (HyperPigeon, MIT). The diamond blues were replaced with a dark netherite palette. The MIT notice is in `META-INF/councilcraft-texture-credits.txt`, which is packed into the jar. No AI-generated art.

## How to test in a singleplayer world

Install this jar next to the pack mods. Create a creative world.

- **Riptide.** Enchant a trident with Riptide III, stand in rain or water, hold right click. The lunge should be clearly shorter than vanilla (about half). Riptide I and II are halved the same way. A dry trident still does not launch.
- **Depth Strider.** Wear Depth Strider III boots and swim. Note the speed. Then gain Dolphin's Grace (potion or a dolphin). Swim speed should match Grace alone, not the stacked Grace + Depth Strider sprint. Take the boots off: Grace alone is unchanged. Let Grace expire: Depth Strider alone is unchanged.
- **Paraglider.** Damage a paraglider on an anvil. A phantom membrane is a valid repair material. Leather still is, too. The 384 durability is the Paragliders config, not this mod.
- **Sacks.** Craft a Supplementaries sack. It does not fit in a bundle. It does not fit in a Create toolbox. (Supplementaries 1.20-3.1.44 already returns false here; the mixin keeps it that way.)
- **Gold horseshoes.** `/loot spawn ~ ~ ~ loot minecraft:chests/nether_bridge` and the four `minecraft:chests/bastion_*` tables. No gold horseshoes. Desert pyramid and end city loot can still roll iron or diamond horseshoes, not gold. Iron and diamond horseshoes elsewhere are unchanged.
- **Creepers.** Spawn `mutantmonsters:mutant_creeper` and `mutantmonsters:creeper_minion` and let them explode next to stone and next to a mob. The mob takes damage. The stone does not break. A vanilla creeper still breaks blocks while `mobGriefing` is true. The minion tracker's "destroys blocks" toggle cannot turn block damage back on.
- **Handshake.** Join this same world: you have the mod, so you get in. On a dedicated server, a client without the jar is disconnected with "This server requires the CouncilCraft mod. Install the CouncilCraft modpack." A client built from a different `mod_version` is disconnected with both version numbers. Both sentences are literal text, because that client is not running this mod. The disconnect is sent when login is about to accept the player, after every login query has been answered, so the client shows that sentence instead of "Connection reset by peer".
- **Horseshoes.** Put iron horseshoes on a horse and ride it at a one-block and a two-block ledge. Mounted, it steps up 2 blocks. Dismount, and it is back to one block, so a pen still holds it. Diamond steps 2 blocks (the value is 2.6). Netherite steps 3. Smith the netherite pair: netherite upgrade template, diamond horseshoes, netherite ingot. Toss the netherite pair in lava: the item survives. A fast horse (or one with a Speed potion) does not go past the cap: attribute movement of 0.44. A camel's step stays 1.5 while it is not wearing shoes. Camels, llamas and zebras are not in the Horseshoes "allowed to wear" tag, so they do not get the step bonus unless a datapack adds them. The speed cap still applies to them.
- **Toolbox.** Open a Create toolbox. A wrench, a shaft, an andesite casing, redstone, a torch and an arrow go in. A cobblestone, a plank and a sack do not. The tag files under `data/councilcraft/tags/items/` are the draft whitelist. A datapack can add an item by extending `councilcraft:toolbox_contents` or one of the four tags it includes.

`./gradlew test` covers the arithmetic (half riptide, depth-strider level, which loot entries survive, version match). It does not launch a world. The checks above need one.
