package net.tuboi.druidry.entity;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.tuboi.druidry.registries.DruidryEntityRegistry;
import net.tuboi.druidry.utils.Constants;
import net.tuboi.druidry.utils.ParticleHelper;

public class FertilizeProjectile extends AbstractConeProjectile {
    private static final EntityDataAccessor<Float> SPELLPOWER = SynchedEntityData.defineId(FertilizeProjectile.class, EntityDataSerializers.FLOAT);


    public FertilizeProjectile(EntityType<? extends AbstractConeProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public FertilizeProjectile(Level level, LivingEntity entity, Float spellpower) {
        super(DruidryEntityRegistry.FERTILIZE_PROJECTILE.get(),level,entity);
        this.entityData.set(SPELLPOWER, spellpower);;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(SPELLPOWER, 0f);
    }

    /*
     _._     _,-'""`-._
    (,-.`._,'(       |\`-/|
         `-.-' \ )-`( , o o)
               `-    \`_`"'-
     */
    @Override
    public void tick() {
        if (!level().isClientSide) {
                float rotRange = 15 * Mth.DEG_TO_RAD; //How many degrees to either side spell can deviate
                float range = this.entityData.get(SPELLPOWER)*1.5f; //Should realisticly be between 6 and 30 blocks
                if(range>30){range=30;}
                else if(range<6){range=6;}

                Double raycastcount = Math.floor(Math.pow((this.entityData.get(SPELLPOWER) / 3),1.5d));
                if (raycastcount < 1){raycastcount=1d;}
                else if(raycastcount > 50){raycastcount=50d;}

                for (int i = 0; i < raycastcount; i++) { //Numbers of fired rays per tick

                    Vec3 cast = getOwner()
                            .getLookAngle()
                            .normalize()
                            .xRot(Utils.random.nextFloat() * rotRange * 2 - rotRange)
                            .yRot(Utils.random.nextFloat() * rotRange * 2 - rotRange);

                    HitResult hitResult = level()
                            .clip(
                                    new ClipContext(
                                            getOwner().getEyePosition(),
                                            getOwner().getEyePosition().add(cast.scale(range)),
                                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this
                                    )
                            );


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

        double speed = random.nextDouble() * .3 + 0.2 + (Math.pow(this.entityData.get(SPELLPOWER),2f))/200;

        Integer particleCount = (int) Math.pow(this.entityData.get(SPELLPOWER),1.5f);

        for (int i = 0; i < particleCount; i++) { //Create particles scaling with sp

            /* STOLEN CODE FROM IRONS
            //Create random speed vectors for particle beams
            double angularness = .5;
            Vec3 randomVec = new Vec3(Math.random() * 2 * angularness - angularness, Math.random() * 2 * angularness - angularness, Math.random() * 2 * angularness - angularness).normalize();
            Vec3 result = (rotation.scale(3).add(randomVec)).normalize().scale(speed);
            */

            double offset = .15; // Slight position offset
            double ox = Math.random() * 2 * offset - offset;
            double oy = Math.random() * 2 * offset - offset;
            double oz = Math.random() * 2 * offset - offset;

            // Define the degree range for x (horizontal) and y (vertical)
            double yDegreeRange = 15; // Vertical spread in degrees
            double xDegreeRange = 30;

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

        //Create regeneration effect
        MobEffectInstance regenEffect = new MobEffectInstance(MobEffects.REGENERATION, (int)Math.floor(this.entityData.get(SPELLPOWER)*20),1);

        //If entity is alive, apply regeneration
        if(entity instanceof LivingEntity){
            ((LivingEntity) entity).addEffect(regenEffect);
        }

        //Check if the head slot of their inventory is empty
        if(entity.getSlot(Constants.HEADSLOT).get().isEmpty()){
            ItemStack leaf = new ItemStack(Items.OAK_LEAVES);
            entity.getSlot(Constants.HEADSLOT).set(leaf);
        }
    }
}
