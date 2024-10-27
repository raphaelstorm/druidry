package net.tuboi.druidry.entity.menhir;// Made with Blockbench 4.11.1
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import net.minecraft.resources.ResourceLocation;
import net.tuboi.druidry.Druidry;
import software.bernie.geckolib.model.GeoModel;

public class MenhirModel extends GeoModel<MenhirEntity> {
	public static final ResourceLocation modelResource = ResourceLocation.fromNamespaceAndPath(Druidry.MODID, "geo/menhir.geo.json");
	public static final ResourceLocation textureResource = MenhirRenderer.MENHIR_TEXTURE;
	public static final ResourceLocation animationResource = ResourceLocation.fromNamespaceAndPath(Druidry.MODID, "animations/menhir.animation.json");

	@Override
	public ResourceLocation getModelResource(MenhirEntity menhirEntity) {
		return modelResource;
	}

	@Override
	public ResourceLocation getTextureResource(MenhirEntity menhirEntity) {
		return textureResource;
	}

	@Override
	public ResourceLocation getAnimationResource(MenhirEntity menhirEntity) {
		return animationResource;
	}
}