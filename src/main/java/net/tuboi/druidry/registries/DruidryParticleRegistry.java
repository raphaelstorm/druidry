package net.tuboi.druidry.registries;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tuboi.druidry.Druidry;

import java.util.function.Supplier;

public class DruidryParticleRegistry {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, Druidry.MODID);

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }

    /*
    To Create Particle:
    - textures + json
    - particle class
    - register it here
    - add it to particle helper
    - register it in client setup
     */
    public static final Supplier<SimpleParticleType> LEAF_PARTICLE = PARTICLE_TYPES.register("leaf", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> PINK_FLOWER_PARTICLE = PARTICLE_TYPES.register("pinkflower", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> WHITE_FLOWER_PARTICLE = PARTICLE_TYPES.register("whiteflower", () -> new SimpleParticleType(false));
}
