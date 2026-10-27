// PT44 regression: councilcraft_recipes.js woodLookKey. A `const v` inside the loop bound 'oak' only, so only the oak
// Nifty Carts recipes got wood-look tags. Needs prelude.js + councilcraft_recipes.js first.
var woods = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'bamboo', 'crimson', 'warped']
woods.forEach(function (w) {
  var stripped = 'minecraft:stripped_' + w + (w === 'bamboo' ? '_block' : (w === 'crimson' || w === 'warped') ? '_stem' : '_log')
  checkEq(woodLookKey({ item: 'minecraft:' + w + '_planks' }), { tag: 'councilcraft:wood_look/' + w + '_planks' }, w + ' planks')
  checkEq(woodLookKey({ item: stripped }), { tag: 'councilcraft:wood_look/' + w + '_stripped_logs' }, w + ' stripped')
})
checkEq(woodLookKey({ item: 'minecraft:stick' }), { item: 'minecraft:stick' }, 'non-wood item unchanged')
checkEq(woodLookKey({ tag: 'minecraft:planks' }), { tag: 'minecraft:planks' }, 'tag ingredient unchanged')
checkEq(woodLookKey(null), null, 'null unchanged')
