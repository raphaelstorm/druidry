package net.tuboi.druidry.utils;

import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ParticleHelper {

    public static final Vector3f FERTILIZERCOLOR = Vec3.fromRGB24(65280).toVector3f();

    //Todo: make actual particles and a particle registry, for now this is just stolen
    public static final ParticleOptions FERTILIZER_EMITTER = ParticleTypes.CLOUD;

}
