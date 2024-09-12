package net.tuboi.druidry.utils;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.tuboi.druidry.registries.DruidryParticleRegistry;
import org.joml.Vector3f;

public class ParticleHelper {

    public static final ParticleOptions FERTILIZER_EMITTER = DruidryParticleRegistry.FERTILIZER_PARTICLE.get();
    public static final ParticleOptions FLOWER_EMITTER = DruidryParticleRegistry.FLOWER_PARTICLE.get();
}
