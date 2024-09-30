package net.tuboi.druidry.entity.bumbleguard;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.tuboi.druidry.Druidry;

@OnlyIn(Dist.CLIENT)
public class BumbleguardRenderer extends MobRenderer<Bumbleguard, BumbleguardModel<Bumbleguard>> {
    private static final ResourceLocation BUMBLEGUARD_TEXTURE = ResourceLocation.fromNamespaceAndPath(Druidry.MODID, "textures/entity/bumbleguard/bumbleguard.png");
    private static final ResourceLocation BUMBLEGUARD_ANGRY_TEXTURE = ResourceLocation.fromNamespaceAndPath(Druidry.MODID, "textures/entity/bumbleguard/bumbleguard_angry.png");

    public BumbleguardRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BumbleguardModel<>(ctx.bakeLayer(ModelLayers.BEE)), 0.4F);
    }

    public ResourceLocation getTextureLocation(Bumbleguard pEntity) {
        if (pEntity.hasTarget()) {
            return  BUMBLEGUARD_ANGRY_TEXTURE;
        } else {
            return  BUMBLEGUARD_TEXTURE;
        }
    }
}
