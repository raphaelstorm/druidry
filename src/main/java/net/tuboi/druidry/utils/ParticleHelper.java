package net.tuboi.druidry.utils;

import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.tuboi.druidry.registries.DruidryParticleRegistry;
import org.joml.Vector3f;

public class ParticleHelper {

    public static final Vector3f FERTILIZERCOLOR = Vec3.fromRGB24(65280).toVector3f();

    public static final ParticleOptions LEAF_EMITTER = DruidryParticleRegistry.LEAF_PARTICLE.get();
    public static final ParticleOptions WHITE_FLOWER_EMITTER = DruidryParticleRegistry.WHITE_FLOWER_PARTICLE.get();
    public static final ParticleOptions PINK_FLOWER_EMITTER = DruidryParticleRegistry.PINK_FLOWER_PARTICLE.get();
}
