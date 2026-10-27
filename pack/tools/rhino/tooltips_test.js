// PT44 regression: client_scripts/councilcraft_tooltips.js. `const tag` / `const ench` inside the loop made the
// "Apply with a Super Blaze Forger" tooltip look only at a book's first stored enchantment. Needs prelude.js, with
// the Java classes registered, and councilcraft_tooltips.js first.
var maxLevels = { 'minecraft:sharpness': 5, 'minecraft:mending': 1, 'minecraft:unbreaking': 3 }
var handlers = {}
events['item.tooltip']({ addAdvanced: function (id, fn) { handlers[id] = fn }, add: function () {} })
check(typeof handlers['minecraft:enchanted_book'] === 'function', 'enchanted book tooltip registered')
function book(list) {
  if (!list) return { nbt: null }
  var tags = list.map(function (e) { return { getString: function () { return e[0] }, getShort: function () { return e[1] } } })
  var l = { size: function () { return tags.length }, getCompound: function (i) { return tags[i] } }
  return { nbt: { contains: function (k) { return k === 'StoredEnchantments' }, getList: function () { return l } } }
}
function lines(list) { var out = []; handlers['minecraft:enchanted_book'](book(list), true, { add: function (t) { out.push(t.text) } }); return out }
var tip = 'Apply with a Super Blaze Forger (Experience Cake)'
checkEq(lines([['minecraft:sharpness', 5], ['minecraft:unbreaking', 3]]), [], 'every level within max: no tip')
checkEq(lines([['minecraft:sharpness', 6]]), [tip], 'single above-max enchant')
checkEq(lines([['minecraft:sharpness', 5], ['minecraft:unbreaking', 4]]), [tip], 'second enchant above max (first within max)')
checkEq(lines([['minecraft:mending', 1], ['minecraft:sharpness', 5], ['minecraft:unbreaking', 9]]), [tip], 'third enchant above max')
checkEq(lines([['minecraft:sharpness', 7], ['minecraft:unbreaking', 9]]), [tip], 'two above max: one tip line only')
checkEq(lines([['minecraft:mending', 2], ['minecraft:unbreaking', 3]]), [tip], 'first enchant above max')
checkEq(lines(null), [], 'no NBT')
checkEq(lines([['minecraft:unknown_enchant', 99], ['minecraft:unbreaking', 3]]), [], 'unknown enchant id ignored')
