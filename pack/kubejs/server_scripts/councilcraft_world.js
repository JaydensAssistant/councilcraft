// CouncilCraft MVP world rules (SPEC KubeJS section, S209). Rebuilt 2026-10-19.
// Not here because the councilcraft Java mod does it: evoker patrols (PatrolEvokerMixin), creeper/mutant grief,
// horse/camel speed caps, toolbox, printed copies, Reborn, coin values/minting.

// ---- Villager removal, 2-pass (S81/S94/S135): block every join (spawn, structure, breeding, cure, eggs) = delete on load ----
const CC_NO_VILLAGERS = ['minecraft:villager', 'minecraft:wandering_trader', 'minecraft:trader_llama', 'minecraft:zombie_villager']
// ---- Sorcerer / necromancer fallbacks (S157/S158) ----
const CC_KILL = ['illagerinvasion:sorcerer', 'illagerinvasion:necromancer']
// ---- Removed mutant items (S154/S156; gold horseshoes allowed again, PPM4): never allowed to exist as item entities or in inventories ----
const CC_REMOVED_ITEMS = ['mutantmonsters:hulk_hammer', 'mutantmonsters:endersoul_hand', 'mutantmonsters:creeper_minion_tracker',
  'mutantmonsters:creeper_shard', 'mutantmonsters:creeper_minion_spawn_egg', 'mutantmonsters:mutant_skeleton_arms',
  'mutantmonsters:mutant_skeleton_limb', 'mutantmonsters:mutant_skeleton_pelvis', 'mutantmonsters:mutant_skeleton_rib',
  'mutantmonsters:mutant_skeleton_rib_cage', 'mutantmonsters:mutant_skeleton_shoulder_pad', 'mutantmonsters:mutant_skeleton_chestplate',
  'mutantmonsters:mutant_skeleton_leggings', 'mutantmonsters:mutant_skeleton_boots', 'farmersdelight:horse_feed']

EntityEvents.spawned(event => {
  const e = event.entity
  const type = String(e.type)
  if (CC_NO_VILLAGERS.indexOf(type) >= 0 || CC_KILL.indexOf(type) >= 0) { event.cancel(); return }
  if (type === 'minecraft:item') {
    const id = String(e.item.id)
    if (CC_REMOVED_ITEMS.indexOf(id) >= 0) { event.cancel(); return }
  }
  // ---- Zebra speed (S244 C2): roll once into the unshod vanilla horse range 0.1125-0.3375 ----
  if (type === 'naturalist:zebra' && !e.persistentData.getBoolean('cc_zebra_speed')) {
    const v = 0.1125 + (Math.random() + Math.random() + Math.random()) / 3 * 0.225
    e.getAttribute('minecraft:generic.movement_speed').setBaseValue(v)
    e.persistentData.putBoolean('cc_zebra_speed', true)
  }
})

// ---- Nether roof push-down (S155): every 20 ticks, y>=250 in the_nether -> safe space below; no damage, no block edits ----
const CC_AIR = ['minecraft:air', 'minecraft:cave_air', 'minecraft:void_air']
const CC_BAD_FLOOR = ['minecraft:lava', 'minecraft:fire', 'minecraft:soul_fire', 'minecraft:magma_block', 'minecraft:bedrock', 'minecraft:water']
function ccId(level, x, y, z) { return String(level.getBlock(x, y, z).id) } // Java String -> JS string (=== would fail otherwise)
function ccSafeY(level, x, z) {
  for (let y = 240; y > 8; y--) {
    if (CC_AIR.indexOf(ccId(level, x, y, z)) < 0 || CC_AIR.indexOf(ccId(level, x, y + 1, z)) < 0) continue
    const below = ccId(level, x, y - 1, z)
    if (CC_AIR.indexOf(below) < 0 && CC_BAD_FLOOR.indexOf(below) < 0) return y
  }
  return -1
}

PlayerEvents.tick(event => {
  const p = event.player
  if (p.age % 20 !== 0) return
  // removed-item cleanup (S154/S156 tick cleanup)
  if (p.age % 100 === 0) {
    CC_REMOVED_ITEMS.forEach(id => { if (p.inventory.count(id) > 0) p.server.runCommandSilent('clear ' + p.username + ' ' + id) })
  }
  if (String(p.level.dimension) !== 'minecraft:the_nether' || p.y < 250) return
  const x = Math.floor(p.x), z = Math.floor(p.z)
  let tx = x, tz = z, y = ccSafeY(p.level, x, z)
  // ring search outwards (6-block steps, up to 48 blocks: chunks within view distance are already loaded)
  for (let r = 1; y < 0 && r <= 8; r++) {
    for (let dx = -r; dx <= r && y < 0; dx++) for (let dz = -r; dz <= r && y < 0; dz++) {
      if (Math.abs(dx) !== r && Math.abs(dz) !== r) continue
      tx = x + dx * 6; tz = z + dz * 6; y = ccSafeY(p.level, tx, tz)
    }
  }
  if (y < 0) { p.setStatusMessage(Text.gold('The Nether roof is off limits. Finding a safe spot below...')); return } // retry next second, never a blind drop
  if (p.vehicle) p.stopRiding()
  p.teleportTo(tx + 0.5, y, tz + 0.5)
  p.fallDistance = 0
  p.setStatusMessage(Text.gold('The Nether roof is off limits. You have been moved below it.'))
})

// ---- World border growth (S165/S242): +50 width per real day, start 10000, cap 17500 ----
const CC_BORDER_START = 10000, CC_BORDER_CAP = 17500, CC_BORDER_STEP = 50
function ccBorder(server) {
  const day = Math.floor(Date.now() / 86400000)
  const d = server.persistentData
  if (!d.contains('cc_border_day')) {
    d.putInt('cc_border_day', day)
    d.putInt('cc_border_width', CC_BORDER_START)
    server.runCommandSilent('worldborder center 0 0')
    server.runCommandSilent('worldborder set ' + CC_BORDER_START)
    return
  }
  const last = d.getInt('cc_border_day')
  if (day <= last) return
  const w = Math.min(CC_BORDER_CAP, d.getInt('cc_border_width') + CC_BORDER_STEP * (day - last))
  d.putInt('cc_border_day', day)
  d.putInt('cc_border_width', w)
  server.runCommandSilent('worldborder set ' + w + ' 60')
}
// ---- Gamerules (SPEC line 665; S94/S95/S151/S224), applied once per world ----
function ccGamerules(server) {
  if (server.persistentData.getBoolean('cc_gamerules_v1')) return
  ;['doTraderSpawning false', 'disableRaids true', 'playersSleepingPercentage 0', 'mobGriefing true'].forEach(g => server.runCommandSilent('gamerule ' + g))
  server.persistentData.putBoolean('cc_gamerules_v1', true)
}
ServerEvents.loaded(event => { ccGamerules(event.server); ccBorder(event.server) })
ServerEvents.tick(event => { if (event.server.tickCount % 1200 === 0) ccBorder(event.server) })
