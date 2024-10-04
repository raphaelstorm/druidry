package net.tuboi.druidry.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
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

    public static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder randomString = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            randomString.append(chars.charAt(index));
        }

        return randomString.toString();
    }

    public static Vec3 directionToVec3(Direction direction) {
        Random random = new Random();

        // Convert the direction to a unit vector
        Vec3 baseVec = switch (direction) {
            case NORTH -> new Vec3(0, 0, -1);
            case SOUTH -> new Vec3(0, 0, 1);
            case EAST  -> new Vec3(1, 0, 0);
            case WEST  -> new Vec3(-1, 0, 0);
            case UP    -> new Vec3(0, 1, 0);
            case DOWN  -> new Vec3(0, -1, 0);
        };

        // Add a small amount of randomization to the vector
        double randomX = (random.nextDouble() - 0.5) * 0.1;  // Random value between -0.05 and 0.05
        double randomY = (random.nextDouble() - 0.5) * 0.1;
        double randomZ = (random.nextDouble() - 0.5) * 0.1;

        // Return the new vector with randomization
        return baseVec.add(randomX, randomY, randomZ);
    }

    public static void createParticleBurst(Level level, BlockPos pos, Vec3 direction, int particleCount, ParticleOptions particle) {
        Random random = new Random();
        for (int i = 0; i < particleCount; i++) {
            // Randomize the angle within the cone
            double angle = random.nextDouble() * Math.PI * 2;
            double offset = random.nextDouble() * 0.5; // Adjust the spread of the particles

            // Calculate the particle's velocity
            double x = direction.x + offset * Math.cos(angle);
            double y = direction.y + offset * Math.sin(angle);
            double z = direction.z + offset * Math.sin(angle);

            // Create the particle
            level.addParticle(particle, pos.getCenter().x, pos.getCenter().y, pos.getCenter().z, x, y, z);
        }
    }

    public static class BumbleguardTagUtil {

        private static final String TAG_KEY = "BUMBLEGUARD_NO_ATTACK_LIST";

        // Method to add a tag to an entity
        public static void addTag(Entity entity, String tag) {
            CompoundTag compound = entity.getPersistentData();
            compound.putString(TAG_KEY, tag);
        }

        // Method to check if an entity has a specific tag
        public static String getTag(Entity entity, String tag) {
            CompoundTag compound = entity.getPersistentData();
            return compound.getString(TAG_KEY);
        }

        public static boolean playerHasTaggedEntity(Entity entity, String playerUUIDasString) {
            return getTag(entity, playerUUIDasString).contains(playerUUIDasString);
        }

        public static void addPlayerToTaggedEntity(Entity entity, String playerUUIDasString) {
            if(playerHasTaggedEntity(entity, playerUUIDasString)) return;

            if(getTag(entity, playerUUIDasString).isEmpty()){
                addTag(entity, playerUUIDasString);
                return;
            }

            String tag = getTag(entity, playerUUIDasString);
            tag += "," + playerUUIDasString;

            removeTag(entity);
            addTag(entity, tag);

        }

        // Method to remove a tag from an entity
        public static void removeTag(Entity entity) {
            CompoundTag compound = entity.getPersistentData();
            compound.remove(TAG_KEY);
        }
    }

}
