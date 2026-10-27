// PT41 tips logic check: node pack/tools/tips_logic_test.js (from the repo root). Mocks the KubeJS objects.
const fs = require('fs'), path = require('path'), assert = require('assert')
const src = fs.readFileSync(path.join(__dirname, '..', 'kubejs', 'server_scripts', 'councilcraft_tips.js'), 'utf8')
class C { constructor() { this.m = {} } contains(k) { return k in this.m } getBoolean(k) { return !!this.m[k] } putBoolean(k, v) { this.m[k] = v }
  getInt(k) { return this.m[k] || 0 } putInt(k, v) { this.m[k] = v } getString(k) { return this.m[k] || '' } putString(k, v) { this.m[k] = v }
  remove(k) { delete this.m[k] } getCompound(k) { return this.m[k] || new C() } put(k, v) { this.m[k] = v } }
class T { constructor(s) { this.s = s; this.click = null; this.parts = [this] } clickRunCommand(c) { this.click = c; return this } hover() { return this }
  append(t) { this.parts.push(...t.parts); return this } text() { return this.parts.map(p => p.s).join('') } }
const Text = { gold: s => new T(s), white: s => new T(s), darkGray: s => new T(s), of: s => new T(s) }
let tick, commands
const ServerEvents = { tick: f => { tick = f }, commandRegistry: f => { commands = f } }
eval(src.replace(/^(const|let) /gm, 'var ')) // top-level bindings visible to the test
function player(name, playTime, active) {
  const tags = new Set(active ? ['cc_active'] : [])
  return { name, got: [], stats: { playTime }, persistentData: new C(), getGameProfile: () => ({ getId: () => 'uuid-' + name }),
    getTags: () => ({ contains: t => tags.has(t) }), setActive(a) { a ? tags.add('cc_active') : tags.delete('cc_active') },
    tell(t) { this.got.push(t) } }
}
const list = []
list.isEmpty = () => list.length === 0
const server = { players: list, persistentData: new C() }
function fire() { for (let i = 0; i < CC_TIP_INTERVAL; i++) tick({ server }) }
const text = t => t.parts[1].s

// interval: 15 minutes, nothing with nobody online
assert.equal(CC_TIP_INTERVAL, 18000)
fire(); assert.equal(ccTipTicks, 0)
const fresh = player('fresh', 0, true), vet = player('vet', CC_TIP_NEWCOMER_TICKS, true), afk = player('afk', 0, false), off = player('off', 0, true)
list.push(fresh, vet, afk, off)
ccTipsSetOff(server, off, true)
for (let i = 0; i < CC_TIP_INTERVAL - 1; i++) tick({ server })
assert.equal(fresh.got.length, 0, 'no tip before 15 minutes')
tick({ server }); assert.equal(fresh.got.length, 1, 'tip at 15 minutes')
for (let n = 1; n < 13; n++) fire()
// newcomer: the 13 [N] tips first, each once
assert.deepEqual(fresh.got.map(text).sort(), CC_NEW_TIPS.slice().sort())
assert.ok(fresh.persistentData.getBoolean('cc_tips_new_done'))
// then the general pool, no repeats until exhausted
for (let n = 0; n < 42; n++) fire()
assert.deepEqual(fresh.got.slice(13).map(text).map(s => s.replace(/spans [\d,]+ blocks/, 'spans {width} blocks')).sort(), CC_TIPS.slice().sort())
// veteran (3 h+): general pool only, no [N] tips
assert.equal(vet.got.length, 55)
assert.ok(vet.got.map(text).every(s => CC_NEW_TIPS.indexOf(s) < 0))
// AFK and opted-out players get nothing; AFK turns are skipped, not queued
assert.equal(afk.got.length, 0); assert.equal(off.got.length, 0)
afk.setActive(true); fire(); assert.equal(afk.got.length, 1, 'one tip, not a backlog')
// [hide tips] runs /tips off
const last = fresh.got[fresh.got.length - 1]
assert.equal(last.parts[2].s, ' [hide tips]'); assert.equal(last.parts[2].click, '/tips off')
// newcomer passing 3 h mid-bag stops [N] tips
const mid = player('mid', 0, true); list.push(mid); fire(); fire(); mid.stats.playTime = CC_TIP_NEWCOMER_TICKS; fire()
assert.ok(CC_NEW_TIPS.indexOf(text(mid.got[2])) < 0)
// border width: live value, then fallback
function borderTip() { const p = player('b', CC_TIP_NEWCOMER_TICKS, true); p.persistentData.putString('cc_tips_bag_all', String(CC_TIPS.findIndex(s => s.indexOf('{width}') >= 0))); ccTipSend(server, p); return text(p.got[0]) }
assert.equal(borderTip(), 'Our world border spans 10,000 blocks, centered on spawn.')
server.persistentData.putInt('cc_border_width', 12350)
assert.equal(borderTip(), 'Our world border spans 12,350 blocks, centered on spawn.')
// markers stripped
assert.ok(CC_NEW_TIPS.concat(CC_TIPS).every(s => !/⚑|🔗|\[N\]/.test(s) && s.length <= 100))
// /tips on clears the flag
ccTipsSetOff(server, off, false); assert.ok(!ccTipsOff(server, off))
console.log('tips logic: all assertions passed (' + CC_NEW_TIPS.length + ' newcomer, ' + CC_TIPS.length + ' general)')
