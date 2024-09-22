package net.tuboi.druidry.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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

    // Function to limit the decimal places using Math library
    public static double SetMaxDecimals(double value, int maxDecimals) {
        double scale = Math.pow(10, maxDecimals);
        return Math.round(value * scale) / scale;
    }

    public static List<Block> GetFlowerBlocks() {
        TagKey<Block> flowerTag = BlockTags.SMALL_FLOWERS; // Assuming there's already a FLOWERS tag
        return BuiltInRegistries.BLOCK.getTag(flowerTag)
                .map(tag -> tag.stream().map(Holder::value).collect(Collectors.toList()))
                .orElse(List.of()); // Return empty list if no blocks found
    }

    public static Block GetRandomNormalFlower(){
        List<Block> flowers = new ArrayList<>();

        flowers.add(Blocks.POPPY);
        flowers.add(Blocks.ORANGE_TULIP);
        flowers.add(Blocks.RED_TULIP);
        flowers.add(Blocks.WHITE_TULIP);
        flowers.add(Blocks.DANDELION);
        flowers.add(Blocks.PINK_TULIP);
        flowers.add(Blocks.ALLIUM);
        flowers.add(Blocks.AZURE_BLUET);
        flowers.add(Blocks.BLUE_ORCHID);
        flowers.add(Blocks.CORNFLOWER);
        flowers.add(Blocks.LILY_OF_THE_VALLEY);
        flowers.add(Blocks.OXEYE_DAISY);

        int randomIndex = (int)Math.floor(io.redspace.ironsspellbooks.api.util.Utils.random.nextDouble()*flowers.size());

        return flowers.get(randomIndex);
    }

    /**
     * Returns a list of BlockPos within a given horizontal radius around a center block.
     *
     * @param center The center BlockPos
     * @param radius The radius around the center block (horizontal distance)
     * @return List of BlockPos within the radius (including the perimeter)
     */
    public static List<BlockPos> GetBlocksInRadius(BlockPos center, int radius) {
        List<BlockPos> blocksInRadius = new ArrayList<>();
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();

        // Iterate over a square grid centered at the input block
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Use Pythagoras' theorem to check if the block is within the radius
                if (x * x + z * z <= radius * radius) {
                    blocksInRadius.add(new BlockPos(centerX + x, centerY, centerZ + z));
                }
            }
        }

        return blocksInRadius;
    }
}
