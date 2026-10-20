// Tooltips (S244 F5): books above max level -> "Apply with a Blaze Forger" (the vanilla anvil caps them).
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
      if (ench && tag.getShort('lvl') > ench.getMaxLevel()) { text.add(Text.gold('Apply with a Blaze Forger')); return }
    }
  })
})
