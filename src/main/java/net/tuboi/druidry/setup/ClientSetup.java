package net.tuboi.druidry.setup;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.particle.*;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.tuboi.druidry.Druidry;
import net.tuboi.druidry.particle.LeafParticle;
import net.tuboi.druidry.particle.PinkFlowerParticle;
import net.tuboi.druidry.particle.WhiteFlowerParticle;
import net.tuboi.druidry.registries.DruidryEntityRegistry;
import net.tuboi.druidry.registries.DruidryParticleRegistry;

@EventBusSubscriber(modid = Druidry.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void rendererRegister(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DruidryEntityRegistry.FERTILIZE_PROJECTILE.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(DruidryParticleRegistry.LEAF_PARTICLE.get(), LeafParticle.Provider::new);
        event.registerSpriteSet(DruidryParticleRegistry.WHITE_FLOWER_PARTICLE.get(), WhiteFlowerParticle.Provider::new);
        event.registerSpriteSet(DruidryParticleRegistry.PINK_FLOWER_PARTICLE.get(), PinkFlowerParticle.Provider::new);
    }
}
