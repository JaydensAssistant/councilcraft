const fs=require('fs'); // PPM98 border logic check: node pack/tools/border_logic_test.js (from the repo root)
const src=fs.readFileSync(require('path').join(__dirname,'..','kubejs','server_scripts','councilcraft_world.js'),'utf8')
const a=src.indexOf('const CC_BORDER_START'), b=src.indexOf('ServerEvents.commandRegistry')
let cmds=[]; let NOW=0; const realNow=Date.now
Date.now=()=>NOW*86400000+1000
const code=src.slice(a,b)
const ctx={}; eval(code+';ctx.ccBorder=ccBorder;ctx.ccToday=ccToday')
class D{constructor(){this.m={}}contains(k){return k in this.m}putInt(k,v){this.m[k]=v}getInt(k){return this.m[k]||0}putBoolean(k,v){this.m[k]=v}getBoolean(k){return !!this.m[k]}}
const server={persistentData:new D(),runCommandSilent:c=>cmds.push(c)}
const d=server.persistentData, assert=require('assert')
NOW=100; ctx.ccBorder(server); assert.equal(d.getInt('cc_border_width'),10000); assert.equal(d.getBoolean('cc_border_growing'),false)
NOW=110; ctx.ccBorder(server); assert.equal(d.getInt('cc_border_width'),10000); assert.equal(d.getInt('cc_border_day'),110)
// resume
d.putBoolean('cc_border_growing',true); d.putInt('cc_border_day',ctx.ccToday())
NOW=113; ctx.ccBorder(server); assert.equal(d.getInt('cc_border_width'),10150)
// existing world with day but no flag -> paused
const s2={persistentData:new D(),runCommandSilent:()=>{}}; s2.persistentData.putInt('cc_border_day',50); s2.persistentData.putInt('cc_border_width',12000)
ctx.ccBorder(s2); assert.equal(s2.persistentData.getInt('cc_border_width'),12000); assert.equal(s2.persistentData.getBoolean('cc_border_growing'),false); assert.equal(s2.persistentData.getInt('cc_border_day'),113)
// end growth independent of paused OW
d.putBoolean('cc_border_growing',false); d.putBoolean('cc_end_border_growing',true); NOW=115; ctx.ccBorder(server); assert.equal(d.getInt('cc_border_width'),10150); assert.equal(d.getInt('cc_end_border_width'),5100)
// cap
d.putBoolean('cc_border_growing',true); NOW=1000; ctx.ccBorder(server); assert.equal(d.getInt('cc_border_width'),17500)
console.log('border logic: all assertions passed; last cmds', cmds.slice(-3))
