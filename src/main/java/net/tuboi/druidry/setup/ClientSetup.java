package net.tuboi.druidry.setup;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.tuboi.druidry.Druidry;
import net.tuboi.druidry.particle.FertilizerParticle;
import net.tuboi.druidry.particle.FlowerParticle;
import net.tuboi.druidry.registries.DruidryEntityRegistry;
import net.tuboi.druidry.registries.DruidryParticleRegistry;

@EventBusSubscriber(modid = Druidry.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void rendererRegister(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DruidryEntityRegistry.FERTILIZE_PROJECTILE.get(), NoopRenderer::new);
        event.registerEntityRenderer(DruidryEntityRegistry.BOOMBLOOM_ENTITY.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(DruidryParticleRegistry.FERTILIZER_PARTICLE.get(), FertilizerParticle.Provider::new);
        event.registerSpriteSet(DruidryParticleRegistry.FLOWER_PARTICLE.get(), FlowerParticle.Provider::new);
    }
}
