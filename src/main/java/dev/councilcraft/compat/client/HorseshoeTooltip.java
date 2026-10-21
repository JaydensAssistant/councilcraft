package dev.councilcraft.compat.client;

import dev.councilcraft.rules.HorseshoeTiers;
import java.util.List;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * PPM12 / PPM20. Horseshoe tooltips: speed, armor, knockback (netherite), jump and step height, read from
 * {@link HorseshoeTiers} so they match the real stats. Jump and step use the singular key at exactly 1 block.
 */
public final class HorseshoeTooltip {
    private HorseshoeTooltip() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            HorseshoeTiers.Tier tier = HorseshoeTiers.tier(Registries.ITEM.getId(stack.getItem()).toString());
            if (tier != null) {
                add(tier, lines);
            }
        });
    }

    private static void add(HorseshoeTiers.Tier tier, List<Text> lines) {
        lines.add(Text.empty());
        lines.add(Text.translatable("councilcraft.horseshoes.tooltip.header").formatted(Formatting.GRAY));
        lines.add(line("councilcraft.horseshoes.tooltip.speed", HorseshoeTiers.plain(tier.speed())));
        lines.add(line("councilcraft.horseshoes.tooltip.armor", HorseshoeTiers.plain(tier.armor())));
        if (tier.knockbackResistance() > 0.0f) {
            // 0.1 -> "10" (the lang string adds the percent sign).
            lines.add(line("councilcraft.horseshoes.tooltip.knockback",
                HorseshoeTiers.plain(Math.round(tier.knockbackResistance() * 1000.0f) / 10.0f)));
        }
        lines.add(line(HorseshoeTiers.blocksKey("councilcraft.horseshoes.tooltip.jump", tier.jumpBlocks()),
            HorseshoeTiers.plain(tier.jumpBlocks())));
        lines.add(line(HorseshoeTiers.blocksKey("councilcraft.horseshoes.tooltip.step", tier.stepHeight()),
            HorseshoeTiers.plain(tier.stepHeight())));
    }

    private static Text line(String key, String value) {
        return Text.translatable(key, value).formatted(Formatting.BLUE);
    }
}
