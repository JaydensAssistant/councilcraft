# Timed tips: options for CouncilCraft (Fabric 1.20.1), researched 2026-10-26

## Requirements checked
- random order
- 15–20 min interval
- only when players are online
- per-player persistent `/tips off`
- newcomer pool for the first few hours
- clickable text
- MiniMessage or JSON
- reloadable
- server-only, clean OSS, low overhead

## Tier 1: existing pack config (0.4.17 index, 171 files)
Nothing in the pack does timed broadcasts. The relevant pieces are:
- KubeJS 2001.6.5-build.26
- Rhino
- Architectury
- Fabric Language Kotlin 1.14.2
- DCIntegration (server)
- spark
- Ledger

The pack has no permissions API, no LuckPerms and no placeholder/text libraries.

## Tier 2: premade mods

| Mod | 1.20.1 Fabric build | Licence | Sides / deps | Fits? |
|---|---|---|---|---|
| SkiesAnnouncements https://modrinth.com/mod/skiesannouncements | 1.0.0, 2023-11-08 (1.20.1 branch frozen; main branch is 1.21) | Modrinth/GitHub say MPL-2.0, but the 1.20.1 jar's fabric.mod.json says All-Rights-Reserved | Server-only. Needs Fabric API, FLK >=1.9.5 (in pack) and fabric-permissions-api (NOT in pack). Bundles adventure-platform-fabric. | Strongest feature set: RANDOM/SEQUENTIAL groups, seconds interval, MiniMessage, permission/dimension requirements, /announcements reload, Discord webhook. No player opt-out and no playtime gate. An opt-out would need LuckPerms plus a permission node. |
| Fabric Automessage https://modrinth.com/mod/fabric-automessage | 1.0.0+1.20.1, 2023-07-24 only | MIT | Server-only | Fixed circular order. Broadcasts from a ScheduledExecutorService thread instead of the server thread, which is thread-unsafe. No opt-out, no online check, unmaintained. Rejected. |
| AutoMessage (vtggzSb5) | — | ARR | Needs a client install | Rejected. |

## Tier 3 (RECOMMENDED): KubeJS 6 server script
File: `kubejs/server_scripts/councilcraft_tips.js`. It adds no new mods.

APIs verified in the KubeJS 2001.6.5 jar:
- ServerEvents.tick and ServerEvents.commandRegistry
- server.persistentData
- PlayerKJS.getStats() → PlayerStatsJS.getPlayTime()
- Component click*/hover methods

The pack's councilcraft_world.js already registers /endborder and /ccborder this way.

```js
// SKETCH ONLY — not installed
const CC_TIP_MIN = 20 * 60 * 15, CC_TIP_MAX = 20 * 60 * 20   // 15–20 min
const CC_NEWCOMER_TICKS = 20 * 60 * 60 * 3                   // first 3 h of playtime
const CC_TIPS = [ /* general tips from tips-draft.md */ ]
const CC_NEW_TIPS = [ /* [N] tips */ ]
let ccTipNext = CC_TIP_MIN, ccTipTicks = 0
const ccBags = {}                                            // uuid -> shuffled index bag (in memory)

function ccUuid(p) { return String(p.getGameProfile().getId()) }   // PT24: Rhino can't see Entity#getUUID
function ccOptedOut(server, p) { return server.persistentData.getCompound('cc_tips_off').getBoolean(ccUuid(p)) }
function ccSetOff(server, p, off) {
  const d = server.persistentData, c = d.getCompound('cc_tips_off')
  if (off) c.putBoolean(ccUuid(p), true); else c.remove(ccUuid(p))
  d.put('cc_tips_off', c)
}
function ccDraw(p) {
  const pool = p.stats.playTime < CC_NEWCOMER_TICKS ? CC_NEW_TIPS.concat(CC_TIPS) : CC_TIPS
  const k = ccUuid(p) + ':' + pool.length
  let bag = ccBags[k]
  if (!bag || bag.length === 0) {                            // shuffle-bag: no repeats until all shown
    bag = pool.map((_, i) => i)
    for (let i = bag.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [bag[i], bag[j]] = [bag[j], bag[i]] }
    ccBags[k] = bag
  }
  return pool[bag.pop()]
}

ServerEvents.tick(event => {
  if (++ccTipTicks < ccTipNext) return
  ccTipTicks = 0
  ccTipNext = CC_TIP_MIN + Math.floor(Math.random() * (CC_TIP_MAX - CC_TIP_MIN))
  const server = event.server
  server.players.forEach(p => {                              // empty list = nothing sent
    if (ccOptedOut(server, p)) return
    p.tell(Text.gold('Tip: ').append(Text.white(ccDraw(p)))
      .append(Text.darkGray(' [hide tips]').clickRunCommand('/tips off').hover('Type /tips on to bring them back')))
  })
})

ServerEvents.commandRegistry(event => {
  const { commands: Commands } = event
  event.register(Commands.literal('tips')
    .then(Commands.literal('off').executes(ctx => { ccSetOff(ctx.source.server, ctx.source.player, true); ctx.source.sendSuccess(Text.of('Tips off. /tips on to re-enable.'), false); return 1 }))
    .then(Commands.literal('on').executes(ctx => { ccSetOff(ctx.source.server, ctx.source.player, false); ctx.source.sendSuccess(Text.of('Tips on.'), false); return 1 }))
    .then(Commands.literal('now').executes(ctx => { ctx.source.player.tell(Text.of(ccDraw(ctx.source.player))); return 1 })))
})
```

### Drawbacks
- We maintain it ourselves, and it needs one in-game test of the commands, the opt-out and reload.
- Rhino quirks: avoid destructuring swaps if Rhino rejects them, and get the UUID through the GameProfile (see PT24).
- There is no MiniMessage. Text uses KubeJS Text chaining (JSON parsing via Text.of is not verified).
- Tip edits need `/kubejs reload server_scripts`. The tick counter and bags reset on reload, which is harmless. Use the tick counter rather than scheduleRepeating, so a reload never stacks timers.
- playTime is the vanilla statistic and counts AFK time.
- The opt-out lives in the world's KubeJS server data, so it is lost if the world is reset.

## Tier 4: councilcraft Java
Not needed. Tier 3 covers everything.
