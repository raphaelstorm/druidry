package net.tuboi.druidry.entity;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import io.redspace.ironsspellbooks.entity.spells.AbstractShieldEntity;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.tuboi.druidry.registries.DruidryEntityRegistry;
import net.tuboi.druidry.utils.ParticleHelper;

public class FertilizeProjectile extends AbstractConeProjectile {

    public FertilizeProjectile(EntityType<? extends AbstractConeProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public FertilizeProjectile(Level level, LivingEntity entity) {
        super(DruidryEntityRegistry.FERTILIZE_PROJECTILE.get(),level,entity);
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
                float range = 15 * Mth.DEG_TO_RAD;
                for (int i = 0; i < 2; i++) { //Numbers of fired rays per tick
                    Vec3 cast = getOwner().getLookAngle().normalize().xRot(Utils.random.nextFloat() * range * 2 - range).yRot(Utils.random.nextFloat() * range * 2 - range);
                    HitResult hitResult = level().clip(new ClipContext(getOwner().getEyePosition(), getOwner().getEyePosition().add(cast.scale(10)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                    Vec3 pos = hitResult.getLocation().subtract(cast.scale(.5));
                    BlockPos blockPos = BlockPos.containing(pos.x, pos.y, pos.z);

                    //Create bonemeal item stack
                    ItemStack bonemealstack = new ItemStack(Items.BONE_MEAL);

                    //If the hit block is air, apply bonemeal to the block below instead
                    if (level().getBlockState(blockPos).isAir()) {
                        blockPos = blockPos.offset(0,-1,0);
                    }

                    //Apply bonemeal to ray impact
                    BoneMealItem.applyBonemeal(bonemealstack, level(), blockPos, null);
                    BoneMealItem.growWaterPlant(bonemealstack, level(), blockPos, null);

                    /* This is what is normally done to display bonemeal effect on the block in question, but we chosen to not use it here because it sounds like ass.
                    if (!level().isClientSide) {
                        level().levelEvent(1505, blockPos, 15);
                    } */

                    //Instead, we only use the bonemealparticles without the sound
                    BoneMealItem.addGrowthParticles(level(), blockPos, 15);
            }
        }
        super.tick();
    }

    @Override
    public void spawnParticles() {
        var owner = getOwner();
        if (!level().isClientSide || owner == null) {
            return;
        }
        Vec3 rotation = owner.getLookAngle().normalize();
        var pos = owner.position().add(rotation.scale(1.6));

        double x = pos.x;
        double y = pos.y + owner.getEyeHeight() * .9f;
        double z = pos.z;

        double speed = random.nextDouble() * .2 + .30;
        for (int i = 0; i < 20; i++) { //Create particles

            /* STOLEN CODE FROM IRONS
            //Create random speed vectors for particle beams
            double angularness = .5;
            Vec3 randomVec = new Vec3(Math.random() * 2 * angularness - angularness, Math.random() * 2 * angularness - angularness, Math.random() * 2 * angularness - angularness).normalize();
            Vec3 result = (rotation.scale(3).add(randomVec)).normalize().scale(speed);
            */

            double offset = .15;
            double ox = Math.random() * 2 * offset - offset; // Slight position offset
            double oy = Math.random() * 2 * offset - offset;
            double oz = Math.random() * 2 * offset - offset;

            // Define the degree range for x (horizontal) and y (vertical)
            double xDegreeRange = 30; // Horizontal spread in degrees
            double yDegreeRange = 15; // Vertical spread in degrees

            // Generate random angles within the range for x and y
            double randomXAngle = (Math.random() * 2 * xDegreeRange) - xDegreeRange; // -30 to +30 degrees
            double randomYAngle = (Math.random() * 2 * yDegreeRange) - yDegreeRange; // -15 to +15 degrees

            // Convert degrees to radians
            double randomXRad = Math.toRadians(randomXAngle);
            double randomYRad = Math.toRadians(randomYAngle);

            // Calculate new direction by rotating the player's look direction
            double cosY = Math.cos(randomYRad); // Vertical rotation
            double sinY = Math.sin(randomYRad);
            double cosX = Math.cos(randomXRad); // Horizontal rotation
            double sinX = Math.sin(randomXRad);

            // Rotation applied to look direction (rotation is normalized already)
            Vec3 rotatedDirection = new Vec3(
                    rotation.x * cosX - rotation.z * sinX,      // Horizontal rotation
                    rotation.y * cosY + sinY,                   // Vertical rotation
                    rotation.z * cosX + rotation.x * sinX       // Maintain forward direction
            ).normalize();

            Vec3 result = rotatedDirection.scale(speed); // Scale by speed


            ParticleOptions ParticleToEmit = null;
            if(i%3==1){ //One in three particles are flowers
                ParticleToEmit = ParticleHelper.FLOWER_EMITTER;
            }else{
                ParticleToEmit = ParticleHelper.FERTILIZER_EMITTER;
            }
            //Display particle
            level().addParticle(ParticleToEmit, x + ox, y + oy, z + oz, result.x, result.y, result.z);

        }
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        var entity = entityHitResult.getEntity();
        //Todo: add random flowers and shrubbery to inventory and maybe heal a bit
    }
}
