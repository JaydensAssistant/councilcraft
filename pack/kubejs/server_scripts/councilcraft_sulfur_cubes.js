// PPM41 (0.4.5): Vanilla Backport sulfur cubes swallow full-cube blocks from Regions Unexplored, Create and Supplementaries.
// Each block goes into the minecraft:sulfur_cube_archetype/<type> item tag that matches how Vanilla Backport 1.1.7.10 sorts
// the vanilla blocks of the same material (minecraft:sulfur_cube_swallowable already includes every archetype tag):
//   regular      dirt, mud, grass blocks, clay, bone block      -> RU soils/grass blocks/mud, overgrown bone block; Supplementaries daub
//   bouncy       #minecraft:planks, #minecraft:logs              -> RU woods are already in those tags; Supplementaries fine wood
//   slow_bouncy  stone, bricks, polished/cut stone, nylium, quartz, obsidian, observer, redstone lamp
//                -> RU stones and nyliums (prismoss = moss on stone, like nylium); Create's stone palette, rose quartz;
//                   Supplementaries stone tiles/bricks/lamps, flint block, checker block, relayer, redstone illuminator
//   slow_flat    iron/gold/copper/netherite blocks, raw metal blocks, metal ores, cut copper
//                -> Create brass/zinc/andesite alloy/industrial iron blocks, zinc ores, raw zinc, casings, copper shingles/tiles
//   slow_sliding mushroom blocks, #minecraft:wart_blocks          -> RU glistering wart (RU's bioshroom blocks are already in wart_blocks)
//   fast_flat    moss block, hay, dried kelp                    -> RU mycotoxic moss; Supplementaries flax block
//   fast_sliding ice, packed/blue ice, snow block                -> Supplementaries soap block
//   light        #minecraft:wool                                  -> Supplementaries feather block
// Full cubes only (every blockstate model is a 16x16x16 cube), checked against the block models in the mod jars. Left out on
// purpose: leaves, glass/windows, falling ash, cacti, block entities and machines (vaults, observers, chassis, rollers, notice
// board, speaker, turn table), blocks disabled or EMI-hidden in this pack (cog/lock/pulley block, end stone lamp), and blocks
// with no vanilla counterpart in any archetype (raw redstone block, cardboard, sugar cube, slidy block, suspicious gravel bricks).
const CC_SULFUR_CUBE_ARCHETYPES = {
  regular: [
    'regions_unexplored:alpha_grass_block', 'regions_unexplored:argillite_grass_block', 'regions_unexplored:chalk_grass_block',
    'regions_unexplored:deepslate_grass_block', 'regions_unexplored:peat_grass_block', 'regions_unexplored:silt_grass_block',
    'regions_unexplored:stone_grass_block', 'regions_unexplored:ashen_dirt', 'regions_unexplored:peat_dirt',
    'regions_unexplored:peat_coarse_dirt', 'regions_unexplored:peat_podzol', 'regions_unexplored:peat_mud',
    'regions_unexplored:silt_dirt', 'regions_unexplored:silt_coarse_dirt', 'regions_unexplored:silt_podzol',
    'regions_unexplored:silt_mud', 'regions_unexplored:overgrown_bone_block', 'supplementaries:daub',
    'supplementaries:daub_brace', 'supplementaries:daub_cross_brace', 'supplementaries:daub_frame'
  ],
  bouncy: [
    'supplementaries:fine_wood'
  ],
  slow_bouncy: [
    'regions_unexplored:argillite', 'regions_unexplored:chalk', 'regions_unexplored:chalk_bricks',
    'regions_unexplored:chalk_pillar', 'regions_unexplored:polished_chalk', 'regions_unexplored:mossy_stone',
    'regions_unexplored:cobalt_obsidian', 'regions_unexplored:brimsprout_nylium', 'regions_unexplored:cobalt_nylium',
    'regions_unexplored:glistering_nylium', 'regions_unexplored:viridescent_nylium',
    'regions_unexplored:deepslate_viridescent_nylium', 'regions_unexplored:prismoss', 'regions_unexplored:deepslate_prismoss',
    'create:asurine', 'create:crimsite', 'create:limestone', 'create:ochrum', 'create:scorchia', 'create:scoria',
    'create:veridium', 'create:cut_asurine', 'create:cut_asurine_bricks', 'create:polished_cut_asurine',
    'create:small_asurine_bricks', 'create:layered_asurine', 'create:asurine_pillar', 'create:cut_crimsite',
    'create:cut_crimsite_bricks', 'create:polished_cut_crimsite', 'create:small_crimsite_bricks', 'create:layered_crimsite',
    'create:crimsite_pillar', 'create:cut_limestone', 'create:cut_limestone_bricks', 'create:polished_cut_limestone',
    'create:small_limestone_bricks', 'create:layered_limestone', 'create:limestone_pillar', 'create:cut_ochrum',
    'create:cut_ochrum_bricks', 'create:polished_cut_ochrum', 'create:small_ochrum_bricks', 'create:layered_ochrum',
    'create:ochrum_pillar', 'create:cut_scorchia', 'create:cut_scorchia_bricks', 'create:polished_cut_scorchia',
    'create:small_scorchia_bricks', 'create:layered_scorchia', 'create:scorchia_pillar', 'create:cut_scoria',
    'create:cut_scoria_bricks', 'create:polished_cut_scoria', 'create:small_scoria_bricks', 'create:layered_scoria',
    'create:scoria_pillar', 'create:cut_veridium', 'create:cut_veridium_bricks', 'create:polished_cut_veridium',
    'create:small_veridium_bricks', 'create:layered_veridium', 'create:veridium_pillar', 'create:cut_andesite',
    'create:cut_andesite_bricks', 'create:polished_cut_andesite', 'create:small_andesite_bricks', 'create:layered_andesite',
    'create:andesite_pillar', 'create:cut_calcite', 'create:cut_calcite_bricks', 'create:polished_cut_calcite',
    'create:small_calcite_bricks', 'create:layered_calcite', 'create:calcite_pillar', 'create:cut_deepslate',
    'create:cut_deepslate_bricks', 'create:polished_cut_deepslate', 'create:small_deepslate_bricks', 'create:layered_deepslate',
    'create:deepslate_pillar', 'create:cut_diorite', 'create:cut_diorite_bricks', 'create:polished_cut_diorite',
    'create:small_diorite_bricks', 'create:layered_diorite', 'create:diorite_pillar', 'create:cut_dripstone',
    'create:cut_dripstone_bricks', 'create:polished_cut_dripstone', 'create:small_dripstone_bricks', 'create:layered_dripstone',
    'create:dripstone_pillar', 'create:cut_granite', 'create:cut_granite_bricks', 'create:polished_cut_granite',
    'create:small_granite_bricks', 'create:layered_granite', 'create:granite_pillar', 'create:cut_tuff',
    'create:cut_tuff_bricks', 'create:polished_cut_tuff', 'create:small_tuff_bricks', 'create:layered_tuff',
    'create:tuff_pillar', 'create:rose_quartz_block', 'create:rose_quartz_tiles', 'create:small_rose_quartz_tiles',
    'create:rose_quartz_lamp', 'supplementaries:ash_bricks', 'supplementaries:blackstone_tile', 'supplementaries:stone_tile',
    'supplementaries:checker_block', 'supplementaries:flint_block', 'supplementaries:gravel_bricks',
    'supplementaries:lapis_bricks', 'supplementaries:blackstone_lamp', 'supplementaries:deepslate_lamp',
    'supplementaries:stone_lamp', 'supplementaries:redstone_illuminator', 'supplementaries:relayer'
  ],
  slow_flat: [
    'create:andesite_alloy_block', 'create:brass_block', 'create:zinc_block', 'create:raw_zinc_block', 'create:zinc_ore',
    'create:deepslate_zinc_ore', 'create:industrial_iron_block', 'create:weathered_iron_block', 'create:andesite_casing',
    'create:brass_casing', 'create:copper_casing', 'create:railway_casing', 'create:shadow_steel_casing',
    'create:refined_radiance_casing', 'create:copper_shingles', 'create:copper_tiles', 'create:waxed_copper_shingles',
    'create:waxed_copper_tiles', 'create:exposed_copper_shingles', 'create:exposed_copper_tiles',
    'create:waxed_exposed_copper_shingles', 'create:waxed_exposed_copper_tiles', 'create:weathered_copper_shingles',
    'create:weathered_copper_tiles', 'create:waxed_weathered_copper_shingles', 'create:waxed_weathered_copper_tiles',
    'create:oxidized_copper_shingles', 'create:oxidized_copper_tiles', 'create:waxed_oxidized_copper_shingles',
    'create:waxed_oxidized_copper_tiles'
  ],
  slow_sliding: [
    'regions_unexplored:glistering_wart'
  ],
  fast_flat: [
    'regions_unexplored:mycotoxic_moss', 'supplementaries:flax_block'
  ],
  fast_sliding: [
    'supplementaries:soap_block'
  ],
  light: [
    'supplementaries:feather_block'
  ]
}

ServerEvents.tags('item', event => {
  Object.keys(CC_SULFUR_CUBE_ARCHETYPES).forEach(type => {
    event.add('minecraft:sulfur_cube_archetype/' + type, CC_SULFUR_CUBE_ARCHETYPES[type])
  })
})
