// Tooltips (S244 F5): books above max level -> "Apply with a Super Blaze Forger (Experience Cake)" (PT30: above-max levels only apply in Super Forging — SEETHING blaze on Experience Cake; normal forging clamps to max) (the vanilla anvil caps them).
// PPM106: each coin states what it melts to in Create crushing (councilcraft:melt_* in councilcraft_recipes.js).
const CC_Registries = Java.loadClass('net.minecraft.core.registries.BuiltInRegistries')
const CC_RL = Java.loadClass('net.minecraft.resources.ResourceLocation')
ItemEvents.tooltip(event => {
  event.addAdvanced('minecraft:enchanted_book', (item, advanced, text) => {
    const nbt = item.nbt
    if (!nbt || !nbt.contains('StoredEnchantments')) return
    const list = nbt.getList('StoredEnchantments', 10)
    for (let i = 0; i < list.size(); i++) {
      const tag = list.getCompound(i)
      const ench = CC_Registries.ENCHANTMENT.get(new CC_RL(tag.getString('id')))
      if (ench && tag.getShort('lvl') > ench.getMaxLevel()) { text.add(Text.gold('Apply with a Super Blaze Forger (Experience Cake)')); return }
    }
  })
  ;[['spur', '1 diamond (80% chance)'], ['bevel', '4 diamonds'], ['sprocket', '8 diamonds'], ['cog', '20 diamonds'],
    ['crown', '40 diamonds'], ['sun', '80 diamonds']].forEach(c => event.add('numismatics:' + c[0], Text.gold('Melts to: ' + c[1])))
})
