package net.tuboi.druidry.entity.menhir;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.entity.spells.spectral_hammer.SpectralHammer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.tuboi.druidry.Druidry;
import net.tuboi.druidry.renderer.GeoLivingEntityRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import java.util.logging.Logger;

public class MenhirRenderer extends GeoLivingEntityRenderer<MenhirEntity> {

    public static final ResourceLocation MENHIR_TEXTURE = ResourceLocation.fromNamespaceAndPath(Druidry.MODID, "textures/entity/menhir/menhir.png");

    public MenhirRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MenhirModel());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(MenhirEntity pEntity) {
        return MENHIR_TEXTURE;
    }


    @Override
    public void preRender(PoseStack poseStack, MenhirEntity animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
