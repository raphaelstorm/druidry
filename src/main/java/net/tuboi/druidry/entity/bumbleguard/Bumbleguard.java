package net.tuboi.druidry.entity.bumbleguard;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.AirRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tuboi.druidry.registries.DruidryEntityRegistry;
import net.tuboi.druidry.utils.SendMessage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
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
    private List<Entity> individualWhitelist = new ArrayList<>();
    private List<Class> classWhitelist = new ArrayList<>();
    private Player owner;

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
        this.individualWhitelist.add(this);
        this.classWhitelist.add(Animal.class);
    }

    public Bumbleguard(Level world, Player owner) {
        super(DruidryEntityRegistry.BUMBLEGUARD.get(), world);
        this.owner = owner;
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
        this.goalSelector.addGoal(2, new Bumbleguard.BeeWanderGoal());
        this.goalSelector.addGoal(3, new FloatGoal(this));
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

    boolean closerThan(BlockPos pPos, int pDistance) {
        return pPos.closerThan(this.blockPosition(), (double)pDistance);
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
            return Bumbleguard.this.getTarget() == null || (Bumbleguard.this.getTarget() != null && !Bumbleguard.this.getTarget().isAlive());
        }

        public boolean canBeeContinueToUse() {
            return Bumbleguard.this.getTarget() == null || (Bumbleguard.this.getTarget() != null && !Bumbleguard.this.getTarget().isAlive());
        }

        @Override
        public void start() {
            new SendMessage().Send("Starting search for enemies!");
        }

        @Override
        public void stop() {
            new SendMessage().Send("Stopping search!");
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if(Bumbleguard.this.getTarget() == null || Bumbleguard.this.getTarget() != null && !Bumbleguard.this.getTarget().isAlive()){

                new SendMessage().Send("Searching for enemies!");
                //Look for new enemies
                getNewEnemies().forEach(LivingEntity -> {
                    if(!Bumbleguard.this.enemyList.contains(LivingEntity) && Bumbleguard.this.isValidTarget(LivingEntity)){
                        Bumbleguard.this.enemyList.add(LivingEntity);
                    }
                });
                Bumbleguard.this.setTarget(getClosestEnemyWithinLineOfSight());
            }
        }

    }

    class BeeWanderGoal extends Goal {
        private static final int WANDER_THRESHOLD = 22;

        BeeWanderGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Bumbleguard.this.navigation.isDone() && Bumbleguard.this.random.nextInt(10) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return Bumbleguard.this.navigation.isInProgress();
        }

        @Override
        public void start() {
            Vec3 vec3 = this.findPos();
            if (vec3 != null) {
                Bumbleguard.this.navigation.moveTo(Bumbleguard.this.navigation.createPath(BlockPos.containing(vec3), 1), 1.0);
            }
        }

        @Nullable
        private Vec3 findPos() {
            Vec3 vec3;
            if (Bumbleguard.this.hasHive() && !Bumbleguard.this.closerThan(Bumbleguard.this.hivePos, 22)) {
                Vec3 vec31 = Vec3.atCenterOf(Bumbleguard.this.hivePos);
                vec3 = vec31.subtract(Bumbleguard.this.position()).normalize();
            } else {
                vec3 = Bumbleguard.this.getViewVector(0.0F);
            }

            int i = 8;
            Vec3 vec32 = HoverRandomPos.getPos(Bumbleguard.this, 8, 7, vec3.x, vec3.z, (float) (Math.PI / 2), 3, 1);
            return vec32 != null ? vec32 : AirAndWaterRandomPos.getPos(Bumbleguard.this, 8, 4, -2, vec3.x, vec3.z, (float) (Math.PI / 2));
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

    private boolean isValidTarget(Entity entity){

        //Don't attack creative or spectating players
        if(entity instanceof Player && (((Player) entity).isCreative()) || entity.isSpectator()){
            return false;
        }

        //Don't attack bees or other bumbleguard
        if(entity instanceof Bee || entity instanceof Bumbleguard){
            return false;
        }

        //Don't attack whitelisted creatures
        if(individualWhitelist.contains(entity)){
            return false;
        }

        //Don't attack passive mobs
        if(entity instanceof Animal && !(entity instanceof NeutralMob)){
            return false;
        }

        //Don't attack owner
        if(entity instanceof Player && (entity.getUUID().equals(this.owner.getUUID()))){
            return false;
        }

        //Don't attack pet's of owner
        if(entity instanceof TamableAnimal && ((TamableAnimal) entity).isTame() && ((TamableAnimal) entity).isOwnedBy(this.owner)){
            return false;
        }

        return true;
    }

}
