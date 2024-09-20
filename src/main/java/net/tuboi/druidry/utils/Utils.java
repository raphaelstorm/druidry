package net.tuboi.druidry.utils;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class Utils {

    //Stolen from irons getTargetBlock, but modified to detect non collision blocks
    public static BlockHitResult getTargetBlock(Level level, LivingEntity entity, ClipContext.Fluid clipContext, double reach) {
        var rotation = entity.getLookAngle().normalize().scale(reach);
        var pos = entity.getEyePosition();
        var dest = rotation.add(pos);
        return level.clip(new ClipContext(pos, dest, ClipContext.Block.COLLIDER, clipContext, entity));
    }


    public static float GetParticleSpeedNeededForTravelDistance(double travelDist, float particleFriction, double tickAmount) {
        // Variable to store the total distance covered
        double totalDistance = 0;
        // Variable to store the initial speed, which we are solving for
        float initialSpeed = 0;

        // Increment initial speed until the total distance covered over the ticks is equal or greater than travelDist
        while (totalDistance < travelDist) {
            initialSpeed += 0.01f;  // Increment the speed in small steps
            totalDistance = 0;      // Reset total distance for each test

            // Calculate the total distance covered with the current initial speed and friction
            float currentSpeed = initialSpeed;
            for (int i = 0; i < tickAmount; i++) {
                totalDistance += currentSpeed;
                currentSpeed *= (1 - particleFriction);  // Decrease the speed based on friction each tick
            }
        }

        return initialSpeed;
    }

    public static Vec3 GetRandomScaledVector(double scale) {
        Random random = new Random();

        // Generate random unit vector
        double x = random.nextDouble() * 2.0 - 1.0;
        double y = random.nextDouble() * 2.0 - 1.0;
        double z = random.nextDouble() * 2.0 - 1.0;

        // Create the vector
        Vec3 randomVector = new Vec3(x, y, z).normalize(); // Normalize to make it a unit vector

        // Scale the vector to the desired magnitude
        Vec3 scaledVector = randomVector.scale(scale);

        return scaledVector;
    }

    // Extract x, y, z components of the scaled vector
    public static double[] GetVectorSpeeds(double scale) {
        Vec3 vector = GetRandomScaledVector(scale);

        // Get individual speed components
        double speedX = vector.x;
        double speedY = vector.y;
        double speedZ = vector.z;

        // Return speeds as an array
        return new double[]{speedX, speedY, speedZ};
    }
}
