// CouncilCraft MVP recipes (SPEC KubeJS section, S209 placement). Rebuilt 2026-10-19.
// Coin MINTING is NOT here: it is implemented in the councilcraft Java mod (BasinMint mixin + data/councilcraft/recipes/mint_*).

const RU_WOODS = ['baobab','blackwood','cypress','dead','eucalyptus','joshua','kapok','larch','magnolia','maple','mauve','palm','pine','redwood','socotra','willow','brimwood','cobalt']
const NC_WOODS = ['oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry','bamboo','crimson','warped']

// --- PPM73/PPM74 (0.4.9): closest vanilla wood look for each RU wood, pale oak and RU painted planks (Nifty Carts, 11 woods).
// Mirror of dev.councilcraft.rules.WoodLookMap.CART (Release049RulesTest keeps them equal).
const WOOD_LOOK = {
  alpha: 'oak', cypress: 'oak', maple: 'oak', pine: 'oak', baobab: 'cherry',
  blackwood: 'dark_oak', brimwood: 'dark_oak', dead: 'dark_oak',
  eucalyptus: 'jungle', joshua: 'jungle', kapok: 'jungle', socotra: 'jungle', larch: 'jungle',
  magnolia: 'mangrove', redwood: 'acacia', palm: 'birch', willow: 'birch', pale_oak: 'birch',
  yellow_bioshroom: 'bamboo', green_bioshroom: 'bamboo', mauve: 'crimson', pink_bioshroom: 'crimson',
  blue_bioshroom: 'warped', cobalt: 'crimson',
  black_painted: 'dark_oak', blue_painted: 'crimson', brown_painted: 'dark_oak', cyan_painted: 'warped',
  gray_painted: 'warped', green_painted: 'warped', light_blue_painted: 'warped', light_gray_painted: 'warped',
  lime_painted: 'bamboo', magenta_painted: 'crimson', orange_painted: 'acacia', pink_painted: 'crimson',
  purple_painted: 'crimson', red_painted: 'mangrove', white_painted: 'cherry', yellow_painted: 'oak'
}
const woodLookPlanks = w => (w === 'pale_oak' ? 'minecraft:' : 'regions_unexplored:') + w + '_planks'
const woodLookStripped = w => w === 'pale_oak' ? 'minecraft:stripped_pale_oak_log'
  : w.endsWith('_painted') || w === 'alpha' ? null
  : 'regions_unexplored:stripped_' + w + (w.endsWith('_bioshroom') ? '_stem' : '_log')
const vanillaStripped = v => 'minecraft:stripped_' + v + (v === 'bamboo' ? '_block' : v === 'crimson' || v === 'warped' ? '_stem' : '_log')
// Nifty Carts recipe ingredient -> wood-look tag (vanilla planks / stripped log of that wood plus every RU wood mapped to it)
const woodLookKey = ing => {
  if (!ing || !ing.item) return ing
  for (let i = 0; i < NC_WOODS.length; i++) {
    const v = NC_WOODS[i]
    if (ing.item === 'minecraft:' + v + '_planks') return { tag: 'councilcraft:wood_look/' + v + '_planks' }
    if (ing.item === vanillaStripped(v)) return { tag: 'councilcraft:wood_look/' + v + '_stripped_logs' }
  }
  return ing
}

ServerEvents.recipes(event => {
  // ---- Illager Invasion / Paragliders / Mutant Monsters trims (S82, S149, S154/S156, S213) ----
  event.remove({ output: 'illagerinvasion:imbuing_table' })
  event.remove({ output: 'paraglider:deku_leaf' })
  event.remove({ input: 'paraglider:deku_leaf' })
  ;['goddess_statue','kakariko_goddess_statue','goron_goddess_statue','rito_goddess_statue','horned_statue'].forEach(s => event.remove({ output: 'paraglider:' + s }))
  event.remove({ id: /^paraglider:(goddess_statue|horned_statue)\// })
  event.remove({ id: 'paraglider:paraglider' })
  ;['mutant_skeleton_chestplate','mutant_skeleton_leggings','mutant_skeleton_boots','mutant_skeleton_arms','mutant_skeleton_limb','mutant_skeleton_pelvis','mutant_skeleton_rib','mutant_skeleton_rib_cage','mutant_skeleton_shoulder_pad','hulk_hammer','endersoul_hand','creeper_minion_tracker','creeper_shard']
    .forEach(i => event.remove({ output: 'mutantmonsters:' + i }))

  // Paraglider: Create mechanical crafter 5x3 (S148/S213/S216)
  event.custom({
    type: 'create:mechanical_crafting',
    pattern: [' SSS ', 'SIAIS', ' L L '],
    key: { S: { item: 'create:white_sail' }, I: { item: 'create:iron_sheet' }, A: { item: 'create:andesite_alloy' }, L: { item: 'minecraft:leather' } },
    result: { item: 'paraglider:paraglider' },
    acceptMirrored: false
  }).id('councilcraft:paraglider_mechanical_crafting')

  // ---- Vanilla Backport happy ghast harnesses disabled (PPM93): all 16 wool-coloured harness recipes
  // out; EMI hides /^minecraft:[a-z_]+_harness$/ too. No loot table gives a harness, so no inventory cleanup. ----
  event.remove({ output: /^minecraft:[a-z_]+_harness$/ })

  // ---- Horseshoes (S114/S221/S230; PPM4 brings gold back): iron, gold and diamond via sequenced assembly only ----
  event.remove({ output: /^horseshoes:/ })
  event.custom({
    type: 'create:sequenced_assembly',
    ingredient: { item: 'create:iron_sheet' },
    transitionalItem: { item: 'create:iron_sheet' },
    sequence: [
      { type: 'create:deploying', ingredients: [{ item: 'create:iron_sheet' }, { item: 'minecraft:iron_nugget' }], results: [{ item: 'create:iron_sheet' }] },
      { type: 'create:deploying', ingredients: [{ item: 'create:iron_sheet' }, { item: 'minecraft:iron_nugget' }], results: [{ item: 'create:iron_sheet' }] },
      { type: 'create:pressing', ingredients: [{ item: 'create:iron_sheet' }], results: [{ item: 'create:iron_sheet' }] }
    ],
    results: [{ item: 'horseshoes:iron_horseshoes' }],
    loops: 2
  }).id('councilcraft:iron_horseshoes_sequenced')
  // --- PPM4 (2026-10-20): gold horseshoes, the iron recipe with golden sheets and gold nuggets
  event.custom({
    type: 'create:sequenced_assembly',
    ingredient: { item: 'create:golden_sheet' },
    transitionalItem: { item: 'create:golden_sheet' },
    sequence: [
      { type: 'create:deploying', ingredients: [{ item: 'create:golden_sheet' }, { item: 'minecraft:gold_nugget' }], results: [{ item: 'create:golden_sheet' }] },
      { type: 'create:deploying', ingredients: [{ item: 'create:golden_sheet' }, { item: 'minecraft:gold_nugget' }], results: [{ item: 'create:golden_sheet' }] },
      { type: 'create:pressing', ingredients: [{ item: 'create:golden_sheet' }], results: [{ item: 'create:golden_sheet' }] }
    ],
    results: [{ item: 'horseshoes:gold_horseshoes' }],
    loops: 2
  }).id('councilcraft:gold_horseshoes_sequenced')
  event.custom({
    type: 'create:sequenced_assembly',
    ingredient: { item: 'horseshoes:iron_horseshoes' },
    transitionalItem: { item: 'horseshoes:iron_horseshoes' },
    sequence: [
      { type: 'create:deploying', ingredients: [{ item: 'horseshoes:iron_horseshoes' }, { item: 'minecraft:diamond' }], results: [{ item: 'horseshoes:iron_horseshoes' }] },
      { type: 'create:pressing', ingredients: [{ item: 'horseshoes:iron_horseshoes' }], results: [{ item: 'horseshoes:iron_horseshoes' }] }
    ],
    results: [{ item: 'horseshoes:diamond_horseshoes' }],
    loops: 2
  }).id('councilcraft:diamond_horseshoes_sequenced')

  // ---- Small Ships (S114/S120): same layouts moved to the Create mechanical crafter; powder by mixing ----
  const mech = (id, pattern, key, result) => event.custom({ type: 'create:mechanical_crafting', pattern: pattern, key: key, result: result, acceptMirrored: true }).id(id)
  event.remove({ id: 'smallships:cannon' })
  mech('councilcraft:smallships_cannon', ['  S', 'III', ' LL'], { L: { tag: 'minecraft:logs' }, S: { item: 'minecraft:string' }, I: { item: 'minecraft:iron_block' } }, { item: 'smallships:cannon' })
  event.remove({ id: 'smallships:cannon_ball' })
  mech('councilcraft:smallships_cannon_ball', [' # ', '#G#', ' # '], { '#': { item: 'minecraft:iron_nugget' }, G: { item: 'minecraft:gunpowder' } }, { item: 'smallships:cannon_ball', count: 2 })
  // PM43 1b / PM44: grapeshot and chainshot follow the cannonball onto the mechanical crafter (same layouts and counts)
  event.remove({ id: 'smallships:grape_shot' })
  mech('councilcraft:smallships_grape_shot', ['nnn', 'nGn', 'nnn'], { n: { item: 'minecraft:iron_nugget' }, G: { item: 'minecraft:gunpowder' } }, { item: 'smallships:grape_shot', count: 1 })
  event.remove({ id: 'smallships:chained_shot' })
  mech('councilcraft:smallships_chained_shot', ['B B', ' C ', 'B B'], { B: { item: 'smallships:cannon_ball' }, C: { item: 'minecraft:chain' } }, { item: 'smallships:chained_shot', count: 2 })
  event.remove({ id: 'smallships:copper_plating' })
  mech('councilcraft:smallships_copper_plating', [' I ', 'IBI', ' I '], { I: { item: 'create:copper_sheet' }, B: { item: 'minecraft:copper_block' } }, { item: 'smallships:copper_plating' })
  event.remove({ id: 'smallships:iron_scantlings' })
  mech('councilcraft:smallships_iron_scantlings', ['I I', ' B ', 'I I'], { I: { item: 'create:iron_sheet' }, B: { item: 'minecraft:iron_block' } }, { item: 'smallships:iron_scantlings' })
  event.remove({ id: 'smallships:dockyard' })
  mech('councilcraft:smallships_dockyard', ['LML', 'C#C', 'HBH'], { L: { item: 'minecraft:lead' }, M: { item: 'create:mechanical_saw' }, C: { item: 'create:andesite_casing' }, '#': { item: 'minecraft:crafting_table' }, H: { item: 'create:shaft' }, B: { item: 'minecraft:barrel' } }, { item: 'smallships:dockyard' })
  event.remove({ id: 'smallships:fine_grain_powder' })
  event.custom({ type: 'create:mixing', ingredients: [{ item: 'minecraft:bone_meal' }, { item: 'minecraft:gunpowder' }, { item: 'minecraft:charcoal' }], results: [{ item: 'smallships:fine_grain_powder', count: 3 }] }).id('councilcraft:fine_grain_powder_mixing')

  // ---- Platinum sheet pressing 1:1 (S114) ----
  event.remove({ output: 'illagerinvasion:platinum_sheet' })
  event.custom({ type: 'create:pressing', ingredients: [{ item: 'illagerinvasion:platinum_chunk' }], results: [{ item: 'illagerinvasion:platinum_sheet' }] }).id('councilcraft:platinum_sheet_pressing')

  // ---- Nifty Carts heavy carts (plow/reaper/seed drill): mechanical crafter, sticks -> Create shafts (S114) ----
  // PPM74: planks / stripped logs become the PPM73 wood-look tags, so RU and painted woods craft the closest vanilla variant
  NC_WOODS.forEach(w => {
    ;['plow', 'reaper', 'seed_drill'].forEach(kind => {
      const rid = 'niftycarts:' + w + '_' + kind
      event.forEachRecipe({ id: rid }, r => {
        const j = JSON.parse(r.json.toString())
        if (j.type !== 'minecraft:crafting_shaped') return
        Object.keys(j.key).forEach(k => { j.key[k] = j.key[k].item === 'minecraft:stick' ? { item: 'create:shaft' } : woodLookKey(j.key[k]) })
        event.custom({ type: 'create:mechanical_crafting', pattern: j.pattern, key: j.key, result: j.result, acceptMirrored: true }).id('councilcraft:niftycarts_' + w + '_' + kind)
      })
      event.remove({ id: rid })
    })
    // PPM74: supply cart, animal cart and wagon stay on the crafting table with wood-look tags
    ;['supply_cart', 'animal_cart', 'wagon'].forEach(kind => {
      const rid = 'niftycarts:' + w + '_' + kind
      event.forEachRecipe({ id: rid }, r => {
        const j = JSON.parse(r.json.toString())
        if (j.type !== 'minecraft:crafting_shaped') return
        Object.keys(j.key).forEach(k => { j.key[k] = woodLookKey(j.key[k]) })
        event.custom(j).id('councilcraft:niftycarts_' + w + '_' + kind)
      })
      event.remove({ id: rid })
    })
  })

  // ---- Just Enough Breeding pages that show villager / trader llama spawn eggs (PPM95): villagers are removed (S69),
  // so drop the villager->witch lightning page and both trader llama pages. These are every JEB 3.1.1 recipe that names a
  // villager, zombie villager, wandering trader or trader llama; all other JEB pages stay. ----
  ;['transformation/minecraft/witch', 'breeding/minecraft/trader_llama', 'temper/minecraft/trader_llama']
    .forEach(r => event.remove({ id: 'justenoughbreeding:' + r }))

  // ---- Coin melting (S128): Create crushing, 80% value in diamonds (PM32: the 20 coin is now the 25 Diamond Quarter).
  // PPM106: the 1-coin penny melts too, to 1 diamond at an 80% chance (the same 80% value). Tooltips: councilcraft_tooltips.js ----
  ;[['bevel', 4], ['sprocket', 8], ['cog', 20], ['crown', 40], ['sun', 80]].forEach(c => {
    event.custom({ type: 'create:crushing', ingredients: [{ item: 'numismatics:' + c[0] }], results: [{ item: 'minecraft:diamond', count: c[1] }], processingTime: 250 }).id('councilcraft:melt_' + c[0])
  })
  event.custom({ type: 'create:crushing', ingredients: [{ item: 'numismatics:spur' }], results: [{ item: 'minecraft:diamond', count: 1, chance: 0.8 }], processingTime: 250 }).id('councilcraft:melt_spur')

  // ---- FD integration (S217) ----
  event.remove({ id: 'minecraft:bread' })
  event.remove({ id: 'create:smelting/bread' })
  event.remove({ id: 'create:smoking/bread' })
  event.replaceOutput({ mod: 'create' }, 'create:dough', 'farmersdelight:wheat_dough')
  event.replaceInput({}, 'create:dough', 'farmersdelight:wheat_dough')
  // RU logs on the FD cutting board (S242 / batch-1017 P4)
  RU_WOODS.forEach(w => {
    ;['log', 'wood'].forEach(t => {
      const inp = 'regions_unexplored:' + w + '_' + t, out = 'regions_unexplored:stripped_' + w + '_' + t
      if (!Item.exists(inp) || !Item.exists(out)) return
      event.custom({ type: 'farmersdelight:cutting', ingredients: [{ item: inp }], result: [{ item: out }, { item: 'farmersdelight:tree_bark' }], sound: 'minecraft:item.axe.strip', tool: { tag: 'minecraft:axes' } }).id('councilcraft:cutting/ru_' + w + '_' + t)
    })
  })

  // ---- FD extras (S227) ----
  ;['minecraft:mushroom_stew', 'minecraft:beetroot_soup', 'minecraft:rabbit_stew_from_brown_mushroom', 'minecraft:rabbit_stew_from_red_mushroom'].forEach(id => event.remove({ id: id }))
  event.replaceOutput({ id: 'farmersdelight:rope' }, 'farmersdelight:rope', 'supplementaries:rope')
  event.replaceInput({}, 'farmersdelight:rope', 'supplementaries:rope')
  event.remove({ id: 'minecraft:cookie' })
  event.shaped(Item.of('minecraft:cookie', 8), ['DCD'], { D: '#c:dough', C: 'minecraft:cocoa_beans' }).id('councilcraft:cookie_from_dough')
  event.replaceInput({ id: 'supplementaries:pancake' }, 'minecraft:wheat', '#c:dough')

  // ---- Cake / pie bypasses (S244 C7): keep only Create dough cake, FD cake_from_slices, FD crust pumpkin pie ----
  event.remove({ output: 'minecraft:cake', not: [{ id: 'create:crafting/curiosities/cake' }, { id: 'farmersdelight:cake_from_slices' }] })
  event.remove({ output: 'minecraft:pumpkin_pie', not: [{ id: 'farmersdelight:pumpkin_pie_from_pie_crust' }, { id: 'farmersdelight:pumpkin_pie_from_slices' }] })

  // ---- Naturalist cooked egg removed; fried egg unification (S204/S245) ----
  event.remove({ output: 'naturalist:cooked_egg' })
  ;['minecraft:blue_egg', 'minecraft:brown_egg', 'naturalist:duck_egg'].forEach(e => {
    const n = e.split(':')[1]
    event.smelting('farmersdelight:fried_egg', e).xp(0.35).id('councilcraft:fried_egg_from_' + n)
    event.smoking('farmersdelight:fried_egg', e).xp(0.35).id('councilcraft:fried_egg_from_' + n + '_smoking')
    event.campfireCooking('farmersdelight:fried_egg', e).xp(0.35).id('councilcraft:fried_egg_from_' + n + '_campfire')
  })

  // ---- Horse feed (S222) ----
  event.remove({ output: 'farmersdelight:horse_feed' })

  // --- PPM23 (Jayden): every horse armor is craftable, same shape as vanilla leather horse armor (X X / XXX / X X).
  // Netherite stays a smithing upgrade only (Simple Netherite Horse Armor, PPM22). Horse armor loot is untouched.
  const horseArmor = [['iron', 'minecraft:iron_ingot'], ['golden', 'minecraft:gold_ingot'], ['diamond', 'minecraft:diamond']]
  horseArmor.forEach(a => {
    event.shaped('minecraft:' + a[0] + '_horse_armor', ['X X', 'XXX', 'X X'], { X: a[1] }).id('councilcraft:' + a[0] + '_horse_armor')
  })

  // ---- Bell (S242) ----
  event.shaped('minecraft:bell', [' S ', 'GGG', ' B '], { S: 'minecraft:stick', G: 'minecraft:gold_ingot', B: 'minecraft:gold_block' }).id('councilcraft:bell')

  // ---- RU blackstone cluster (S244 C6) ----
  event.remove({ id: 'regions_unexplored:blackstone_cluster' })
  event.stonecutting('regions_unexplored:blackstone_cluster', 'minecraft:blackstone').id('councilcraft:blackstone_cluster_stonecutting')

  // ---- Ash family (S245) ----
  event.remove({ id: 'regions_unexplored:ash' })
  event.shaped('regions_unexplored:ash', ['AA', 'AA'], { A: 'supplementaries:ash' }).id('councilcraft:ru_ash_from_supplementaries_ash')
  event.shapeless(Item.of('supplementaries:ash', 4), ['regions_unexplored:ash']).id('councilcraft:supplementaries_ash_from_ru_ash')
  // ---- 0.4.2 ----
  // PM2a: loot-only enchanting (no enchanting table, no CEI Blaze Enchanter of either kind)
  event.remove({ output: 'minecraft:enchanting_table' })
  event.remove({ output: 'create_enchantment_industry:blaze_enchanter' })
  event.remove({ output: 'create_enchantment_industry:classic_blaze_enchanter' })
  // PM20 (1): no Dragons Plus "ending" (dragon breath) processing
  event.remove({ type: 'create_dragons_plus:ending' })
  // PM46b: Steam 'n' Rails vanilla-wood tracks go; Create's track, gauges (create_andesite narrow/wide), monorail and the non-wood S'n'R tracks stay
  event.remove({ output: /^railways:track_(oak|spruce|birch|jungle|acacia|dark_oak|mangrove|cherry|bamboo|stripped_bamboo|crimson|warped)(_narrow|_wide)?$/ })
  // PM47: the Nifty Carts hand cart is disabled
  event.remove({ output: /^niftycarts:[a-z_]+_hand_cart$/ })
  // PM26 (1): no interplanar projector (projecting is off server-side too)
  event.remove({ output: 'exposure:interplanar_projector' })
  event.remove({ output: 'exposure:broken_interplanar_projector' })
  // --- PPM11 (2026-10-20): no dried ghast (enforces S60; Vanilla Backport's minecraft:dried_ghast recipe)
  event.remove({ id: 'minecraft:dried_ghast' })
  // PM26a: camera
  event.remove({ id: 'exposure:camera' })
  event.shaped('exposure:camera', ['LIB', 'IGI', 'ICI'], { L: 'minecraft:lever', I: 'create:iron_sheet', B: '#minecraft:buttons', G: 'minecraft:glass_pane', C: 'create:cogwheel' }).id('councilcraft:exposure_camera')
  // PM26b: Create-flavored film (same layout as Exposure's, 1 roll each)
  const film = (id, nugget, sheet, mid, top) => {
    event.remove({ id: 'exposure:' + id })
    event.shaped('exposure:' + id, ['NTT', 'SMM', 'SKK'], { N: nugget, T: top, S: sheet, M: mid, K: 'minecraft:dried_kelp' }).id('councilcraft:exposure_' + id)
  }
  film('black_and_white_film', 'create:zinc_nugget', 'create:iron_sheet', 'minecraft:gunpowder', 'minecraft:bone_meal')
  film('color_film', 'minecraft:gold_nugget', 'create:golden_sheet', 'minecraft:gunpowder', 'minecraft:lapis_lazuli')
  film('high_sensitivity_black_and_white_film', 'create:zinc_nugget', 'create:iron_sheet', 'minecraft:prismarine_crystals', 'minecraft:bone_meal')
  film('high_sensitivity_color_film', 'minecraft:gold_nugget', 'create:golden_sheet', 'minecraft:prismarine_crystals', 'minecraft:lapis_lazuli')
  // PM25 / PM25a: Antique Atlas item + craftable antique ink
  event.shapeless('supplementaries:antique_ink', ['minecraft:ink_sac', '#minecraft:coals']).id('councilcraft:antique_ink')
  ;['minecraft:leather', 'create:cardboard', 'farmersdelight:canvas'].forEach(c => {
    event.shapeless('councilcraft:antique_atlas', ['minecraft:paper', 'minecraft:paper', 'minecraft:paper', c, 'minecraft:compass', 'supplementaries:antique_ink']).id('councilcraft:antique_atlas_' + c.split(':')[1])
  })
  event.shapeless('councilcraft:antique_atlas', ['minecraft:book', 'minecraft:compass', 'supplementaries:antique_ink']).id('councilcraft:antique_atlas_from_book')
  // --- PPM43 (0.4.5): respawn lockdown. No respawn anchors and no end crystals (every recipe that makes them).
  // End crystals are only needed to RE-summon the dragon; the first fight's pillar crystals are world-generated.
  event.remove({ output: 'minecraft:respawn_anchor' })
  event.remove({ output: 'minecraft:end_crystal' })
  // --- PT14 (0.4.16): copies are made in the CEI Printer (councilcraft printing behaviours), not in a crafting grid.
  // Ballot copies (PM49, signed books) therefore need a Printer too.
  event.remove({ id: 'minecraft:book_cloning' })
  event.remove({ id: 'exposure:photograph_copying' })
  event.remove({ id: 'exposure:photograph_copying_black_and_white' })
  // PT14: Album = Book and Quill + Leather (replaces Exposure's Book and Quill + Phantom Membrane).
  event.remove({ id: 'exposure:album' })
  event.shapeless('exposure:album', ['minecraft:writable_book', 'minecraft:leather']).id('councilcraft:exposure_album')
  // PT14 amendment 2: Process Ink (color Printer copies) in a Create Mixer, unheated. Create's basin holds only
  // 2 input fluids, so black is the dye fluid and cyan/magenta/yellow are dye items: 4 dyes per 250 mB.
  // Create Fabric fluid amounts are in droplets (81 per mB): 250 mB = 20250.
  event.custom({
    type: 'create:mixing',
    ingredients: [
      { fluidTag: 'c:dyes/black', amount: 20250 },
      { item: 'minecraft:cyan_dye' },
      { item: 'minecraft:magenta_dye' },
      { item: 'minecraft:yellow_dye' }
    ],
    results: [{ fluid: 'councilcraft:process_ink', amount: 20250 }]
  }).id('councilcraft:process_ink_mixing')
})

ServerEvents.tags('item', event => {
  // PPM73/PPM74: Nifty Carts wood-look tags (vanilla wood + every RU / painted / pale oak wood mapped to it)
  NC_WOODS.forEach(v => {
    event.add('councilcraft:wood_look/' + v + '_planks', 'minecraft:' + v + '_planks')
    event.add('councilcraft:wood_look/' + v + '_stripped_logs', vanillaStripped(v))
  })
  Object.keys(WOOD_LOOK).forEach(w => {
    const v = WOOD_LOOK[w], planks = woodLookPlanks(w), stripped = woodLookStripped(w)
    if (Item.exists(planks)) event.add('councilcraft:wood_look/' + v + '_planks', planks)
    if (stripped && Item.exists(stripped)) event.add('councilcraft:wood_look/' + v + '_stripped_logs', stripped)
  })
  // S244 F8 / S245
  ;['c:eggs', 'forge:eggs'].forEach(t => { event.remove(t, 'naturalist:alligator_egg'); event.remove(t, 'naturalist:tortoise_egg') })
  event.add('c:ashes', 'supplementaries:ash')
  ;['c:dough', 'c:foods/doughs', 'c:doughs', 'c:foods/dough'].forEach(t => event.remove(t, 'create:dough')) // S217
  // S217: Naturalist meats into FD meal ingredient tags
  event.add('c:foods/raw_meats', ['naturalist:venison', 'naturalist:duck', 'naturalist:bushmeat'])
  event.add('c:foods/raw_meats/raw_beef', 'naturalist:venison')
  event.add('c:foods/raw_meats/raw_chicken', 'naturalist:duck')
  event.add('c:foods/raw_meats/raw_pork', 'naturalist:bushmeat')
  event.add('c:foods/cooked_meats/cooked_beef', 'naturalist:cooked_venison')
  event.add('c:foods/cooked_meats/cooked_chicken', 'naturalist:cooked_duck')
  event.add('c:foods/cooked_meats/cooked_pork', 'naturalist:cooked_bushmeat')
  event.add('c:foods/raw_fishes', ['naturalist:bass', 'naturalist:catfish'])
  event.add('c:foods/raw_fishes/cod', ['naturalist:bass', 'naturalist:catfish'])
  event.add('c:foods/cooked_fishes', ['naturalist:cooked_bass', 'naturalist:cooked_catfish'])
})

ServerEvents.tags('block', event => {
  event.add('farmersdelight:heat_sources', 'create:blaze_burner') // S217
})
