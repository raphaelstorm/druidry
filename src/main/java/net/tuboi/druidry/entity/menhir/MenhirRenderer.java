package net.tuboi.druidry.entity.menhir;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.tuboi.druidry.Druidry;

public class MenhirRenderer extends MobRenderer<MenhirEntity, MenhirModel<MenhirEntity>> {

    private static final ResourceLocation MENHIR_TEXTURE = ResourceLocation.fromNamespaceAndPath(Druidry.MODID, "textures/entity/menhir/menhir.png");


    public MenhirRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MenhirModel<>(ctx.bakeLayer(MenhirModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(MenhirEntity pEntity) {
        return MENHIR_TEXTURE;
    }
}
