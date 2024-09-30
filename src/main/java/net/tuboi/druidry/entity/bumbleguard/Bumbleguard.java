package net.tuboi.druidry.entity.bumbleguard;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.AirRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tuboi.druidry.registries.DruidryEntityRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Bumbleguard extends Animal implements FlyingAnimal {


    // #################################################################################################################
    // FIELDS
    // #################################################################################################################

    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(Bumbleguard.class, EntityDataSerializers.BYTE);

    @Nullable
    private BlockPos hivePos;
    private UUID persistentAngerTarget;

    private Double detectionRadius = 8.0;
    private List<LivingEntity> enemyList = new ArrayList<>();

    // #################################################################################################################
    // INITIATION
    // #################################################################################################################

    //Constructor
    public Bumbleguard(EntityType<? extends Bumbleguard> type, Level world) {
        super(type, world);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.lookControl = new Bumbleguard.BeeLookControl(this);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 16.0F);
        this.setPathfindingMalus(PathType.COCOA, -1.0F);
        this.setPathfindingMalus(PathType.FENCE, -1.0F);
    }

    public Bumbleguard(Level world) {
        super(DruidryEntityRegistry.BUMBLEGUARD.get(), world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.FLYING_SPEED, 0.6F)
                .add(Attributes.MOVEMENT_SPEED, 0.3F)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new Bumbleguard.BeeAttackGoal(this, 1.4f, true));
        this.goalSelector.addGoal(1, new Bumbleguard.SearchForEnemiesGoal());
    }

    //Synced data
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(DATA_FLAGS_ID, (byte)0);
    }

    // #################################################################################################################
    // NBT
    // #################################################################################################################

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {

    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {

    }

    // #################################################################################################################
    // MISC
    // #################################################################################################################

    public boolean hasHive(){
        if(this.hivePos != null) {
            return true;
        }else{
            return false;
        }
    }

    public boolean hasTarget(){
        return Bumbleguard.this.getTarget() == null || Bumbleguard.this.getTarget() != null && !Bumbleguard.this.getTarget().isAlive();
    }

    @Override
    public boolean isFood(ItemStack pStack) {
        return pStack.is(ItemTags.BEE_FOOD);
    }

    @Nullable
    public Bee getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        return EntityType.BEE.create(pLevel);
    }

    @Override
    public boolean isFlying() {
        return !this.onGround();
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (this.isInvulnerableTo(pSource)) {
            return false;
        } else {
            return super.hurt(pSource, pAmount);
        }
    }

    class BeeLookControl extends LookControl {
        BeeLookControl(Mob pMob) {
            super(pMob);
        }

        @Override
        public void tick() {
            super.tick();

        }
    }

    // #################################################################################################################
    // NAVIGATION
    // #################################################################################################################

    void pathfindRandomlyTowards(BlockPos pPos) {
        Vec3 vec3 = Vec3.atBottomCenterOf(pPos);
        int i = 0;
        BlockPos blockpos = this.blockPosition();
        int j = (int)vec3.y - blockpos.getY();
        if (j > 2) {
            i = 4;
        } else if (j < -2) {
            i = -4;
        }

        int k = 6;
        int l = 8;
        int i1 = blockpos.distManhattan(pPos);
        if (i1 < 15) {
            k = i1 / 2;
            l = i1 / 2;
        }

        Vec3 vec31 = AirRandomPos.getPosTowards(this, k, l, i, vec3, (float) (Math.PI / 10));
        if (vec31 != null) {
            this.navigation.setMaxVisitedNodesMultiplier(0.5F);
            this.navigation.moveTo(vec31.x, vec31.y, vec31.z, 1.0);
        }
    }

    @Override
    protected PathNavigation createNavigation(Level pLevel) {
        FlyingPathNavigation flyingpathnavigation = new FlyingPathNavigation(this, pLevel) {
            @Override
            public boolean isStableDestination(BlockPos p_27947_) {
                return !this.level.getBlockState(p_27947_.below()).isAir();
            }

            @Override
            public void tick() {
                super.tick();
            }
        };
        flyingpathnavigation.setCanOpenDoors(false);
        flyingpathnavigation.setCanFloat(false);
        flyingpathnavigation.setCanPassDoors(true);
        return flyingpathnavigation;
    }

    // #################################################################################################################
    // TICKING
    // #################################################################################################################

    @Override
    public void tick() {
        super.tick();
    }

    // #################################################################################################################
    // GOALS
    // #################################################################################################################

    public GoalSelector getGoalSelector() {
        return this.goalSelector;
    }

    abstract class BaseBeeGoal extends Goal {
        public abstract boolean canBeeUse();
        public abstract boolean canBeeContinueToUse();

        @Override
        public boolean canUse() {
            return this.canBeeUse();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canBeeContinueToUse();
        }
    }

    class BeeAttackGoal extends MeleeAttackGoal {
        BeeAttackGoal(PathfinderMob pMob, double pSpeedModifier, boolean pFollowingTargetEvenIfNotSeen) {
            super(pMob, pSpeedModifier, pFollowingTargetEvenIfNotSeen);
        }

        @Override
        public boolean canUse() {
            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse();
        }
    }

    class SearchForEnemiesGoal extends BaseBeeGoal {
        public boolean canBeeUse() {
            return super.canUse() && Bumbleguard.this.getTarget() == null || Bumbleguard.this.getTarget() != null && !Bumbleguard.this.getTarget().isAlive();
        }

        public boolean canBeeContinueToUse() {
            return super.canContinueToUse() && Bumbleguard.this.getTarget() == null || Bumbleguard.this.getTarget() != null && !Bumbleguard.this.getTarget().isAlive();
        }

        @Override
        public void start() {

        }

        @Override
        public void stop() {

        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if(Bumbleguard.this.getTarget() == null || Bumbleguard.this.getTarget() != null && !Bumbleguard.this.getTarget().isAlive()){
                //Look for new enemies
                getNewEnemies().forEach(LivingEntity -> {
                    if(!Bumbleguard.this.enemyList.contains(LivingEntity)){
                        Bumbleguard.this.enemyList.add(LivingEntity);
                    }
                });
                Bumbleguard.this.setTarget(getClosestEnemyWithinLineOfSight());
            }
        }

    }

    // #################################################################################################################
    // HELPERS
    // #################################################################################################################


    List<LivingEntity> getNewEnemies(){

        //Create new list
        List<LivingEntity> enemies = new ArrayList<>();

        //Get enemies around beehive

        //Get enemies around self
        Double r = this.detectionRadius;
        AABB targetArea = AABB.ofSize(this.blockPosition().getCenter(),r,r,r);
        enemies.addAll(level().getEntitiesOfClass(LivingEntity.class, targetArea));

        return enemies;
    }

    private LivingEntity getClosestEnemyWithinLineOfSight(){
        LivingEntity closest = null;

        for(int i = 0;i<enemyList.size();i++){
            if(hasLineOfSight(enemyList.get(i))){
                if(closest == null){
                    closest = enemyList.get(i);
                }else{
                    if (this.distanceTo(closest) > this.distanceTo(enemyList.get(i))){
                        closest = enemyList.get(i);
                    };
                }
            }
        }

        return closest;
    }

}
