// PvP player heads (PPM103/PPM104).
// A direct PvP kill (melee or the killer's own projectile) drops the victim's head as a world item next to
// the grave (never inside it). Falls, mobs, lava, the void, /kill and self-inflicted deaths drop nothing.
// Hardcore Revival: the attacker of a knockout is remembered; if the victim then dies still knocked out
// (timer or "Accept your Fate") the head drops with the stored name, even if the attacker is offline.
// A rescue clears the record.
const CcNbtUtils = Java.loadClass('net.minecraft.nbt.NbtUtils')
const CcCompoundTag = Java.loadClass('net.minecraft.nbt.CompoundTag')
const CcListTag = Java.loadClass('net.minecraft.nbt.ListTag')
const CcStringTag = Java.loadClass('net.minecraft.nbt.StringTag')
const CcProjectile = Java.loadClass('net.minecraft.world.entity.projectile.Projectile')
const CC_HR = Platform.isLoaded('hardcorerevival') ? Java.loadClass('net.blay09.mods.hardcorerevival.HardcoreRevival') : null
const CC_HR_EXPIRED = Platform.isLoaded('hardcorerevival')
  ? Java.loadClass('net.blay09.mods.hardcorerevival.HardcoreRevivalManager').NOT_RESCUED_IN_TIME : null
const CC_KO_KEY = 'cc_ko_by'

// The killing player for a direct PvP hit, or null (mobs, environment, TNT, self-inflicted).
// KubeJS 6 (1.20.1) exposes DamageSource#getEntity as getActual() and #getDirectEntity as getImmediate();
// the vanilla names are not visible to scripts (0.4.14 fix).
function ccDirectPvpKiller(victim, source) {
  const attacker = source.getActual()
  if (attacker == null || !attacker.isPlayer()) return null
  if (String(attacker.getUUID()) === String(victim.getUUID())) return null
  const direct = source.getImmediate()
  if (direct == null) return null
  if (String(direct.getUUID()) === String(attacker.getUUID())) return attacker
  return direct instanceof CcProjectile ? attacker : null
}

function ccKnockedOut(player) {
  return CC_HR != null && CC_HR.getRevivalData(player).isKnockedOut()
}

function ccPad(n) { return (n < 10 ? '0' : '') + n }

function ccDropHead(victim, killerName) {
  const now = new Date()
  const day = Math.floor(Number(victim.getServer().overworld().getDayTime()) / 24000) + 1
  const lore = new CcListTag()
  lore.add(CcStringTag.valueOf(JSON.stringify({ text: 'Slain by ' + killerName, color: 'gray', italic: false })))
  lore.add(CcStringTag.valueOf(JSON.stringify({
    text: 'Day ' + day + ' - ' + now.getFullYear() + '-' + ccPad(now.getMonth() + 1) + '-' + ccPad(now.getDate()),
    color: 'dark_gray', italic: false
  })))
  const head = Item.of('minecraft:player_head')
  head.getOrCreateTag().put('SkullOwner', CcNbtUtils.writeGameProfile(new CcCompoundTag(), victim.getGameProfile()))
  head.getOrCreateTagElement('display').put('Lore', lore)
  // A thrown world item (logged by Ledger as a drop); graves only collect the inventory.
  victim.drop(head, true, false)
}

// Remember who knocked a player out (recorded at the hit; HR knocks out during this damage call).
EntityEvents.hurt('minecraft:player', event => {
  if (CC_HR == null) return
  const victim = event.entity
  if (ccKnockedOut(victim)) return
  const attacker = event.source.getActual()
  if (attacker == null || !attacker.isPlayer() || String(attacker.getUUID()) === String(victim.getUUID())) return
  const rec = new CcCompoundTag()
  rec.putString('uuid', String(attacker.getUUID()))
  rec.putString('name', String(attacker.getGameProfile().getName()))
  rec.putBoolean('direct', ccDirectPvpKiller(victim, event.source) != null)
  victim.persistentData.put(CC_KO_KEY, rec)
})

// Clear the record once the player is up again (rescued, or the hit did not knock them out).
PlayerEvents.tick(event => {
  const p = event.player
  if (p.age % 20 !== 0 || !p.persistentData.contains(CC_KO_KEY)) return
  if (!ccKnockedOut(p)) p.persistentData.remove(CC_KO_KEY)
})

EntityEvents.death('minecraft:player', event => {
  const victim = event.entity
  const source = event.source
  const rec = victim.persistentData.contains(CC_KO_KEY) ? victim.persistentData.getCompound(CC_KO_KEY) : null
  victim.persistentData.remove(CC_KO_KEY)
  if (CC_HR_EXPIRED != null && source.is(CC_HR_EXPIRED)) {
    const hrAttacker = CC_HR.getRevivalData(victim).getKnockoutAttackerId()
    if (rec != null && rec.getBoolean('direct') && hrAttacker != null && String(hrAttacker) === rec.getString('uuid')) {
      ccDropHead(victim, rec.getString('name'))
    }
    return
  }
  const killer = ccDirectPvpKiller(victim, source)
  if (killer != null) ccDropHead(victim, String(killer.getGameProfile().getName()))
})
