# CouncilCraft tips v3

2026-10-26, revised 18:18 EDT per Jayden's edit list. **Total: 55 tips** (13 [N] newcomer). Every tip is ≤100 chars (script-checked; ⚑ and 🔗 markers not counted). Not installed; no repo or config changes.
**[N]** = newcomer pool. ⚑ = check in game. 🔗 = could be clickable. Removed: draft #26 (PvP head), K shaders, C zoom; 17:50 removals: album, AFK, portals, vendor change, Vanilla look, Mod settings guide. 17:57: new Exposure, cannon, Supplementaries, vendor/bank, tool belt, Carry On, paraglider, Navigator tips. 18:18: cannon tips merged, vendor tips merged, Navigator removed, Create trains added.
Tip on the world border goes stale once growth is resumed: see the border note at the end.

## Getting started

| # | Tip | Chars | Origin | Source |
|---|---|---|---|---|
| 1 | **[N]** In EMI, hover over an item and press R to see its recipe or U to see its uses. | 78 | draft #1 | PPM106 L1459 |
| 2 | **[N]** Death always sends you back to world spawn. Beds won't change that, but you can still sleep. | 92 | draft #2 | PPM43 L1115 (built 0.4.5, L1117) |
| 3 | **[N]** No villagers or raids, but bells are craftable. Explore or trade with players for gear and books. | 97 | draft #3 + raids off + bells (merged) | S69/S81; S206 L420; S95 L267; S242 L458/L634 |
| 4 | **[N]** Difficulty is locked on Hard, and mobs toughen the farther you roam from spawn, even in the Nether. | 99 | draft #4 (v2 merge) | PPM33 L1081; S84; S129 L311 |
| 5 | **[N]** Running slow? Options > Video Settings > Graphics Preset (bottom left) picks settings for your PC. | 98 | draft #37; moved to starter | draft |
| 6 | **[N]** To switch utility belt items, hold Left Alt and scroll. | 55 | draft #5 | PPM67 L1201, built L1226-1232 |
| 7 | Mutants stay at least 2,500 blocks from spawn and make up to 7.5% of hostile mobs near the border. | 98 | draft #6 (v2 merge) | PM45 L959 (built 0.4.2, L1000) |
| 8 | An Antique Atlas (book, compass, antique ink) maps your travels. Carry one and press M to open it. | 98 | draft #7 (v2: + M key) | PM25 L891; options.txt key_key.antique_atlas.open:key.keyboard.m |
| 9 | The End is sealed for now, so eyes of ender won't fit in portal frames. Elytra don't exist here. | 96 | added | S39 L56; S242 L458; PM7 L862 |
| 10 | A single sleeping player skips the night. | 41 | added | S151 L339 |
| 11 | Our world border spans 10,000 blocks, centred on spawn. | 55 | added | PPM98 L1397 (CC_BORDER_START 10000) |

## Create & travel

| # | Tip | Chars | Origin | Source |
|---|---|---|---|---|
| 12 | Hold W while hovering a Create item to watch a Ponder scene showing how it works. | 81 | draft #8 | options.txt key_key.ponder.ponder:key.keyboard.w |
| 13 | Build small ships at a Dockyard instead of a crafting grid. EMI lists what each one costs. — 🔗 EMI link | 90 | draft #9 | S114/S120 L625 |
| 14 | Paragliders come from a Create mechanical crafter and are repaired with phantom membrane. | 89 | draft #10 | S213 L427; S216 L430 |
| 15 | Carts need a saddled animal to pull them, not a player. While riding, press Z near one to attach it. | 100 | draft #11 (v2: + Z attach) | options.txt key_key.niftycarts.action:key.keyboard.z; PPM66 L1200; PPM67 L1214 |
| 16 | Paragliding uses stamina, shown in your XP bar. A full bar lasts about 20 seconds of flight. | 92 | new 17:57 | S148 L334; paraglider-server.toml maxStamina 1200, runningAndSwimming false; PM37 built L995 |
| 17 | At a Station, Train Casing on track makes a bogey. Glue blocks on and add forward-facing controls. | 98 | new 18:18 | Create 6.0.8 lang ponder.train_assembly text_1/5/7/10; train_assembly.requires_casing / no_controls ('forward-facing Train Controls') / sideways_controls |

## Ships & cannons

| # | Tip | Chars | Origin | Source |
|---|---|---|---|---|
| 18 | Fit cannons at a Dockyard, then sit by one: hold right-click to aim, scroll for ammo, jump to fire. ⚑ | 99 | 17:57 #18+#19+#20 merged 18:18 | smallships lang dockyard.click_mount; ShipTipHandler bytecode (aim hold / change ammo / jump fire). ⚑ seat mapping + default keys. Dropped: galleon no-penalty, ammo types, Fine-Grain Powder |

## Printing & photos

| # | Tip | Chars | Origin | Source |
|---|---|---|---|---|
| 19 | A Printer duplicates originals only: signed books, enchanted books, photos and albums, never copies. | 100 | draft #12 (v2: + enchanted books); v4 originals-only | PT28 L1702; S207 L421; CEI enableEnchantedBookPrinting=true |
| 20 | For colour prints you need CMYK Ink: black dye fluid mixed with cyan, magenta and yellow. — 🔗 EMI link | 89 | draft #13 | [needs PT36/0.4.18] PT36 L1774 (a) rename, (d) L1797 recipe; LOCKED, NOT BUILT |
| 21 | Wear Engineer's Goggles to check a Printer's ink cost in advance. | 65 | draft #14 | draft |
| 22 | Film develops in a crafting grid: black-and-white with a water bottle, colour with three potions. — 🔗 EMI link | 97 | draft #16 | PM23a L887; EMI info lang L11 (Awkward, Thick, Mundane) |
| 23 | Right-click a camera in your inventory to load film and add a lens, filter or flash. | 84 | new 17:57 | exposure-common/client right_click_hotswap true; lang gui.exposure.camera_attachments.* |
| 24 | A roll holds 16 shots. Colour film needs gold and lapis, and black-and-white uses zinc. — 🔗 EMI link | 87 | new 17:57 | PM26b L894; councilcraft_recipes.js L239-242 (zinc nugget / gold nugget + lapis) |
| 25 | Print photos in a Lightroom using developed film, paper and dye. It only works under bright light. | 98 | new 17:57 | Exposure lang L10; exposure-server.toml lightroom_light_requirement 13 (light just above) |
| 26 | Photo frames come in 1x1, 2x2 and 3x3, hang on floors or ceilings, and glow with a glow ink sac. | 96 | new 17:57 | exposure lang exposure.jei.info.photograph_frame |

## Enchanting & Blaze Forger

| # | Tip | Chars | Origin | Source |
|---|---|---|---|---|
| 27 | **[N]** There's no enchanting table here. All enchantments come from loot, so go exploring for books. | 93 | draft #17 | draft |
| 28 | The Blaze Forger merges enchanted books, splits multi-enchant ones and applies them to gear. ⚑ | 92 | draft #18 | PPM51 L1133/L1185. ⚑ wording check |
| 29 | Mutants rarely drop Sharpness VI, Power VI or Protection V books. A Super Blaze Forger applies them. | 100 | v3 #22 + #23 (merged) | S177 L376; PT30 L1706 (apply only in Super Forging, Experience Cake fuel) |
| 30 | Fishing never turns up enchanted books. | 39 | added | PM2a L856 (built 0.4.2, L976) |

## Death, Reborn & babies

| # | Tip | Chars | Origin | Source |
|---|---|---|---|---|
| 31 | **[N]** After dying you come back Reborn: small, weak and hungrier. Six hours of active play grows you up. | 98 | draft #21 (v2: + hunger) | S251 L819 |
| 32 | Hold right-click on a Reborn friend with food, staying close and still, to give them a Growth Spurt. | 100 | draft #22 | PM24a L889; PPM34 L1085; RebornFeeding.java L41-44 (feed runs only while feeder keeps using the item) |
| 33 | Adults can carry Reborn babies with an empty hand, but babies can wriggle free, so hold on tight. | 97 | v3 #28 + #29 (merged) | PT22 L1656; L1659/L1661 wriggle-free; built L1680; Carry On empty-hand pickup (CarryOnCommonClient: unbound carry key falls back to sneak) |
| 34 | **[N]** Downed? Friends have 2 minutes to revive you by holding right-click 2 seconds. Lava kills outright. ⚑ | 99 | v3 #30 + #31 (merged) | ticksUntilDeath 2400; rescueActionTicks 40; instantDeathSources minecraft:lava; SPEC L1428; ⚑ gesture is mod default |
| 35 | Your grave holds your items and half your XP. Return to where you fell to collect them. | 87 | draft #25 (v2: no 'anyone can loot') | forgottengraves.json percentage 50 |

## Animals

| # | Tip | Chars | Origin | Source |
|---|---|---|---|---|
| 36 | Horses leap far higher and seat two riders. Horseshoes boost speed, jump and step (hover for stats). | 100 | draft #27 + two riders (merged) | PPM15 L1026; PPM4 L1020; PPM14 L1025; S147 L332 |
| 37 | Every horse armor is craftable, netherite included. — 🔗 EMI link | 51 | draft #28 | PPM22/PPM23 L1062-1067 |
| 38 | Zebras can be ridden, carry a chest, and wear horseshoes and armor just like horses. | 84 | draft #29 (v2 merge) | PPM52 L1137 (built 0.4.6, L1186) |

## Exploration & portals

| # | Tip | Chars | Origin | Source |
|---|---|---|---|---|
| 39 | The Nether has no 8x shortcut here: each block walked there covers exactly one in the Overworld. | 96 | draft #30 | S54 L143; S61 L150; L604 (coordinate_scale 1) |

## Money & shops

| # | Tip | Chars | Origin | Source |
|---|---|---|---|---|
| 40 | **[N]** Government-minted coins run from the Diamond Penny to the Diamond Dollar, and 1¢ = 1 diamond. | 93 | v3 #38 + #39 (merged); 17:57 1¢ wording | PM32b L904; S141 L325 + S173 L372 (one set of government molds, none craftable); PPM8 L1014; PPM8 '¢ = 1 diamond' |
| 41 | **[N]** Vendors are player shops. Right-click one to trade, or shift-click yours to set the item and price. | 99 | 17:57 #43+#44 merged 18:18 | numismatics lang vendor.tooltip.summary (right click trade / shift click configure); PM33 L907. Dropped: Buy/Sell mode, change-making (PM34) |
| 42 | A Bank Terminal opens your account. Right-click a Bank Card to bind it, then pay with it anywhere. | 98 | new 17:57 | numismatics lang bank_terminal / bank_card summaries |

## Server & community

| # | Tip | Chars | Origin | Source |
|---|---|---|---|---|
| 43 | Your atlas stays private. To show friends your waypoints, use /surveyor share <player>. ⚑ | 87 | draft #34 (v2 merge) | S178 L378; surveyor.toml globalSharing=false. ⚑ syntax unverified |
| 44 | **[N]** Voice chat is on. Press V for mic, volume and group settings. | 61 | draft #35 | PT7 L1501; options.txt key_key.voice_chat:key.keyboard.v |
| 45 | Tired of these messages? Click here or type /tips off, and /tips on to bring them back. — 🔗 run_command /tips off | 87 | draft #36 | draft |

## Inventory

| # | Tip | Chars | Origin | Source |
|---|---|---|---|---|
| 46 | A sack holds 9 stacks you can carry or set down. Lugging more than 2 slows you, and it gets worse. | 98 | added; 17:57 + 9 slots | supplementaries-common.json sack_penalty, sack_increment 2; PM29 L898; sack slots 9 |
| 47 | A quiver holds 6 stacks of arrows. Keep it in your inventory and your bow fires the type you select. | 100 | added; v4 no keybind | supplementaries-common.json quiver slots 6, only_works_in_curio false; QuiverItem/SelectableContainerItem (3.1.44 jar); PPM105 L1451 |
| 48 | **[N]** Your tool belt holds a pickaxe, axe, shovel and hoe, and mining picks the right one for you. | 92 | new 17:57 | PPM68 L1216 (belt = pickaxe, axe, shovel, hoe; automatic tool selection kept) |
| 49 | Crouch and right-click with empty hands to carry chests, contents and all, or animals. It slows you. | 100 | new 17:57; 18:18 general Carry On | carryon-common.json: pickupHostileMobs false, heavyTiles/heavyEntities true, slowness 1.0, entity max 1.5w x 2.5h, blacklist smallships:*/armor stands/item frames etc.; players only Reborn babies (PT22 L1656); CarryOnCommonClient unbound key -> sneak; S164 L363 |
| 50 | A lunch basket holds 6 stacks of food. Switch it open and you can eat straight from it. ⚑ | 87 | new 17:57 | supplementaries-common.json lunch_basket slots 6; lang lunch_box.tooltip '[%s] to switch mode'; LunchBoxItem use(). ⚑ eat-from-open behaviour |

## Handy blocks

| # | Tip | Chars | Origin | Source |
|---|---|---|---|---|
| 51 | Jars store bottles, buckets, bowls, cookies or tiny mobs, and cages can catch little animals. ⚑ | 93 | new 17:57 | supplementaries lang message.jar; cage.fail / cage.fail_tamed; config require_taming true. ⚑ tameable animals must be tamed first (cage.fail_tamed) |
| 52 | A slingshot flings blocks, snowballs, fire charges and splash potions, though not ender pearls. ⚑ | 95 | new 17:57 | supplementaries-common.json slingshot allowed ammo (pearls false); S200 L414. ⚑ 'blocks' |
| 53 | Ropes can be climbed and strung sideways, and rope arrows leave one hanging where they land. ⚑ | 92 | new 17:57 | supplementaries-common.json horizontal_ropes true, rope_arrow on; lang rope_arrow. ⚑ arrow behaviour |
| 54 | Pin a book to a notice board so anyone passing by can read it. ⚑ | 62 | new 17:57 | supplementaries-common.json notice_board gui true, allow_any_item false. ⚑ accepted items |
| 55 | Address a present to a friend, and its tag shows who it's from and who it's for. ⚑ | 80 | new 17:57 | supplementaries lang present.to/from/info. ⚑ whether only the addressee can open |

## Border note

PPM98 L1397: border starts at 10,000 wide with growth PAUSED until an op runs /ccborder resume; then +50/day to a 17,500 cap (S242 L754, S165 L364). The live width is in server.persistentData cc_border_width (councilcraft_world.js L117, shown by /ccborder status L124). The tips system is not built (options.md Tier 3 KubeJS sketch), so a {border} placeholder could be filled from that value at broadcast time. Fallback without a number: "The world border is centred on spawn and grows over time once an op starts it."
