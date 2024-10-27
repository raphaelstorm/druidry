package net.tuboi.druidry.entity.menhir;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.tuboi.druidry.Druidry;
import net.tuboi.druidry.renderer.GeoLivingEntityRenderer;
import org.jetbrains.annotations.NotNull;

public class MenhirRenderer extends GeoLivingEntityRenderer<MenhirEntity> {

    public static final ResourceLocation MENHIR_TEXTURE = ResourceLocation.fromNamespaceAndPath(Druidry.MODID, "textures/entity/menhir/menhir.png");

    public MenhirRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MenhirModel());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(MenhirEntity pEntity) {
        return MENHIR_TEXTURE;
    }

}
