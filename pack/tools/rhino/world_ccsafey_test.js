// PT44 regression: councilcraft_world.js ccSafeY. A `const below` inside the loop bound the first value only,
// so the Nether roof push-down returned -1 for every column except a flat roof. Needs prelude.js + councilcraft_world.js first.
function column(spec) { // spec: list of [fromY, toY inclusive, id]; later entries win; everything else is air
  return { getBlock: function (x, y, z) { var id = 'minecraft:air'; spec.forEach(function (s) { if (y >= s[0] && y <= s[1]) id = s[2] }); return { id: id } } }
}
// Nether-like column: bedrock roof 127..255, open cavity, netherrack floor at 40 and below
var tall = column([[0, 40, 'minecraft:netherrack'], [127, 255, 'minecraft:bedrock']])
checkEq(ccSafeY(tall, 0, 0), 41, 'tall drop: first air pair under the roof is 85 blocks above the floor, safe spot is on the floor')
// open air over a lava lake (the first air pair sits on lava), then a dry floor lower down
var cave = column([[0, 30, 'minecraft:netherrack'], [55, 60, 'minecraft:lava'], [127, 255, 'minecraft:bedrock']])
checkEq(ccSafeY(cave, 0, 0), 31, 'cave: skips the lava surface, lands on the dry floor below')
var lavaOnly = column([[0, 100, 'minecraft:lava'], [127, 255, 'minecraft:bedrock']])
checkEq(ccSafeY(lavaOnly, 0, 0), -1, 'lava column: no safe spot')
var flat = column([[0, 120, 'minecraft:netherrack'], [127, 255, 'minecraft:bedrock']])
checkEq(ccSafeY(flat, 0, 0), 121, 'flat roof: air pair sits right on solid ground')
var water = column([[0, 50, 'minecraft:water'], [127, 255, 'minecraft:bedrock']])
checkEq(ccSafeY(water, 0, 0), -1, 'water floor is never safe')
// repeated calls on different columns keep giving the right answer (a const bound once would leak across passes)
checkEq([ccSafeY(tall, 1, 1), ccSafeY(cave, 2, 2), ccSafeY(tall, 3, 3)], [41, 31, 41], 'repeat calls')
// random columns: compare with an independent straight-line reference
function ref(level, x, z) {
  for (var y = 240; y > 8; y--) {
    var a = level.getBlock(x, y, z).id, b = level.getBlock(x, y + 1, z).id, c = level.getBlock(x, y - 1, z).id
    var air = function (i) { return i === 'minecraft:air' || i === 'minecraft:cave_air' || i === 'minecraft:void_air' }
    var bad = ['minecraft:lava', 'minecraft:fire', 'minecraft:soul_fire', 'minecraft:magma_block', 'minecraft:bedrock', 'minecraft:water']
    if (!air(a) || !air(b)) continue
    if (!air(c) && bad.indexOf(c) < 0) return y
  }
  return -1
}
var seed = 12345
function rnd() { seed = (seed * 1103515245 + 12345) & 0x7fffffff; return seed / 0x7fffffff }
var bad = 0
for (var t = 0; t < 300; t++) {
  var spec = [[127, 255, 'minecraft:bedrock']]
  for (var k = 0; k < 6; k++) { var lo = Math.floor(rnd() * 120), hi = lo + Math.floor(rnd() * 20); spec.push([lo, hi, ['minecraft:netherrack', 'minecraft:lava', 'minecraft:magma_block', 'minecraft:soul_sand'][Math.floor(rnd() * 4)]]) }
  var col = column(spec)
  if (ccSafeY(col, 0, 0) !== ref(col, 0, 0)) bad++
}
checkEq(bad, 0, 'random columns that differ from the reference')
