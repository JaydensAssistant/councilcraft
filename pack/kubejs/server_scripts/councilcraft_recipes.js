// CouncilCraft MVP recipes (SPEC KubeJS section, S209 placement). Rebuilt 2026-10-19.
// Coin MINTING is NOT here: it is implemented in the councilcraft Java mod (BasinMint mixin + data/councilcraft/recipes/mint_*).

const RU_WOODS = ['baobab','blackwood','cypress','dead','eucalyptus','joshua','kapok','larch','magnolia','maple','mauve','palm','pine','redwood','socotra','willow','brimwood','cobalt']
const NC_WOODS = ['oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry','bamboo','crimson','warped']

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

  // ---- Horseshoes (S114/S221/S230): iron + diamond via sequenced assembly only; no gold ----
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
  NC_WOODS.forEach(w => {
    ;['plow', 'reaper', 'seed_drill'].forEach(kind => {
      const rid = 'niftycarts:' + w + '_' + kind
      event.forEachRecipe({ id: rid }, r => {
        const j = JSON.parse(r.json.toString())
        if (j.type !== 'minecraft:crafting_shaped') return
        Object.keys(j.key).forEach(k => { if (j.key[k].item === 'minecraft:stick') j.key[k] = { item: 'create:shaft' } })
        event.custom({ type: 'create:mechanical_crafting', pattern: j.pattern, key: j.key, result: j.result, acceptMirrored: true }).id('councilcraft:niftycarts_' + w + '_' + kind)
      })
      event.remove({ id: rid })
    })
  })

  // ---- Coin melting (S128): Create crushing, 80% value in diamonds, no 1-coin recipe ----
  ;[['bevel', 4], ['sprocket', 8], ['cog', 16], ['crown', 40], ['sun', 80]].forEach(c => {
    event.custom({ type: 'create:crushing', ingredients: [{ item: 'numismatics:' + c[0] }], results: [{ item: 'minecraft:diamond', count: c[1] }], processingTime: 250 }).id('councilcraft:melt_' + c[0])
  })

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

  // ---- Horse feed (S222), gold horseshoes (S230; covered by the horseshoes block above) ----
  event.remove({ output: 'farmersdelight:horse_feed' })

  // ---- Bell (S242) ----
  event.shaped('minecraft:bell', [' S ', 'GGG', ' B '], { S: 'minecraft:stick', G: 'minecraft:gold_ingot', B: 'minecraft:gold_block' }).id('councilcraft:bell')

  // ---- RU blackstone cluster (S244 C6) ----
  event.remove({ id: 'regions_unexplored:blackstone_cluster' })
  event.stonecutting('regions_unexplored:blackstone_cluster', 'minecraft:blackstone').id('councilcraft:blackstone_cluster_stonecutting')

  // ---- Ash family (S245) ----
  event.remove({ id: 'regions_unexplored:ash' })
  event.shaped('regions_unexplored:ash', ['AA', 'AA'], { A: 'supplementaries:ash' }).id('councilcraft:ru_ash_from_supplementaries_ash')
  event.shapeless(Item.of('supplementaries:ash', 4), ['regions_unexplored:ash']).id('councilcraft:supplementaries_ash_from_ru_ash')
})

ServerEvents.tags('item', event => {
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
