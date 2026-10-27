package dev.councilcraft.advancement;

import com.google.gson.JsonObject;
import dev.councilcraft.CouncilCraftRules;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.AdvancementEntityPredicateSerializer;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

/**
 * PT42. {@code councilcraft:milestone}, for features with no vanilla or mod trigger. Conditions:
 * {@code {"milestone": "<name>"}}, one of {@link MilestoneRules#ALL}.
 */
public final class MilestoneCriterion extends AbstractCriterion<MilestoneCriterion.Conditions> {
    public static final Identifier ID = new Identifier(CouncilCraftRules.MOD_ID, "milestone");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    protected Conditions conditionsFromJson(
        JsonObject json, LootContextPredicate player, AdvancementEntityPredicateDeserializer deserializer
    ) {
        String milestone = JsonHelper.getString(json, "milestone");
        if (!MilestoneRules.ALL.contains(milestone)) {
            throw new IllegalArgumentException("Unknown councilcraft:milestone '" + milestone + "'");
        }
        return new Conditions(player, milestone);
    }

    public void trigger(ServerPlayerEntity player, String milestone) {
        trigger(player, conditions -> conditions.milestone.equals(milestone));
    }

    public static final class Conditions extends AbstractCriterionConditions {
        final String milestone;

        Conditions(LootContextPredicate player, String milestone) {
            super(ID, player);
            this.milestone = milestone;
        }

        @Override
        public JsonObject toJson(AdvancementEntityPredicateSerializer serializer) {
            JsonObject json = super.toJson(serializer);
            json.addProperty("milestone", milestone);
            return json;
        }
    }
}
