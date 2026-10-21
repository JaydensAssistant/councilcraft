package dev.councilcraft.compat.client;

import dev.councilcraft.compat.Zebras;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.HorseEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.item.DyeableHorseArmorItem;
import net.minecraft.item.HorseArmorItem;
import net.minecraft.item.ItemStack;

/**
 * PPPM3. Zebras render horse armor with the vanilla horse armor model and textures, like vanilla's
 * HorseArmorFeatureRenderer (which is typed to HorseEntity). Naturalist's ZebraModel extends the vanilla
 * HorseEntityModel, so the armor model follows the zebra's pose. The server puts the armor in the chest
 * equipment slot, which is synced, so the client reads it from there.
 */
public final class ZebraArmorFeatureRenderer
    extends FeatureRenderer<AbstractHorseEntity, HorseEntityModel<AbstractHorseEntity>> {
    private final HorseEntityModel<AbstractHorseEntity> model;

    private ZebraArmorFeatureRenderer(FeatureRendererContext<AbstractHorseEntity, HorseEntityModel<AbstractHorseEntity>> context,
                                      EntityModelLoader loader) {
        super(context);
        this.model = new HorseEntityModel<>(loader.getModelPart(EntityModelLayers.HORSE_ARMOR));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            if (Zebras.isZebra(entityType) && renderer.getModel() instanceof HorseEntityModel) {
                helper.register(new ZebraArmorFeatureRenderer((FeatureRendererContext) renderer, context.getModelLoader()));
            }
        });
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractHorseEntity entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        ItemStack stack = entity.getEquippedStack(EquipmentSlot.CHEST);
        if (!(stack.getItem() instanceof HorseArmorItem armor)) {
            return;
        }
        this.getContextModel().copyStateTo(this.model);
        this.model.animateModel(entity, limbAngle, limbDistance, tickDelta);
        this.model.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        float red = 1.0f;
        float green = 1.0f;
        float blue = 1.0f;
        if (armor instanceof DyeableHorseArmorItem dyeable) {
            int color = dyeable.getColor(stack);
            red = (color >> 16 & 0xFF) / 255.0f;
            green = (color >> 8 & 0xFF) / 255.0f;
            blue = (color & 0xFF) / 255.0f;
        }
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(armor.getEntityTexture()));
        this.model.render(matrices, consumer, light, OverlayTexture.DEFAULT_UV, red, green, blue, 1.0f);
    }
}
