package net.tuboi.druidry.entity.bumbleguard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
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
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.tuboi.druidry.block.bumbleguardhive.BumbleguardBlockEntity;
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

    private Integer minimumTicksOutOfHive = 600;
    private Double ticksOutOfHiveCounter = 0d;
    private Player owner;
    private String bumbleId;

    @Nullable
    private BlockPos hivePos;
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

    public Bumbleguard(Level world, Player owner, String bumbleID, BlockPos hivePos, double pX, double pY, double pZ) {
        super(DruidryEntityRegistry.BUMBLEGUARD.get(), world);
        this.owner = owner;
        this.bumbleId = bumbleID;
        this.hivePos = hivePos;
        this.setPos(pX, pY, pZ);
        this.xo = pX;
        this.yo = pY;
        this.zo = pZ;
        this.ticksOutOfHiveCounter+=Math.ceil(getRandom().nextDouble()*600); //add between 0 and 30 seconds
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
        this.goalSelector.addGoal(2, new Bumbleguard.EnterHiveGoal());
        this.goalSelector.addGoal(3, new Bumbleguard.ReturnToHiveGoal());
        this.goalSelector.addGoal(4, new BeeWanderGoal());
        this.goalSelector.addGoal(5, new Bumbleguard.SuicideGoal());
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
    // API
    // #################################################################################################################

    public boolean hasHive(){
        return this.hivePos != null;
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

    public UUID getOwnerUUID(){
        return this.owner.getUUID();
    }

    public String getBumbleId(){
        return this.bumbleId;
    }

    public Boolean hiveIsValid(){
        return hasHive() && level().getBlockEntity(this.hivePos) instanceof BumbleguardBlockEntity && ((BumbleguardBlockEntity) level().getBlockEntity(this.hivePos)).IsMemberOf(Bumbleguard.this.getBumbleId());
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
        advanceTimeOutOfHiveCounter();
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
                Bumbleguard.this.enemyList.clear();
                Bumbleguard.this.enemyList.addAll(getNewEnemies());

                //From the avaliable targets given by the hive, select the closest one
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
            return Bumbleguard.this.navigation.isDone() && Bumbleguard.this.random.nextInt(10) == 0 && Bumbleguard.this.getMinTimeOutOfHivePercentageProgress() < 1;
        }

        @Override
        public boolean canContinueToUse() {
            return Bumbleguard.this.navigation.isInProgress() && Bumbleguard.this.getMinTimeOutOfHivePercentageProgress() < 1;
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

    class ReturnToHiveGoal extends BaseBeeGoal {

        ReturnToHiveGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canBeeUse() {
            return Bumbleguard.this.navigation.isDone() && hiveIsValid() && getMinTimeOutOfHivePercentageProgress() == 1;
        }

        @Override
        public boolean canBeeContinueToUse() {
            return Bumbleguard.this.navigation.isInProgress() && hiveIsValid();
        }

        @Override
        public void start() {
            BlockPos hivePos = Bumbleguard.this.hivePos;
            Vec3 vec3 = hivePos.getCenter();
            Bumbleguard.this.navigation.moveTo(Bumbleguard.this.navigation.createPath(BlockPos.containing(vec3), 1), 1.0);
        }
    }

    class EnterHiveGoal extends BaseBeeGoal{
        EnterHiveGoal() {
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canBeeUse() {
            return hiveIsValid() && getMinTimeOutOfHivePercentageProgress() == 1 && Bumbleguard.this.hivePos.closerThan(new Vec3i(Bumbleguard.this.getBlockX(), Bumbleguard.this.getBlockY(), Bumbleguard.this.getBlockZ()), 2);
        }

        @Override
        public boolean canBeeContinueToUse() {
            return canBeeUse();
        }

        @Override
        public void start() {
            BlockEntity hive = level().getBlockEntity(Bumbleguard.this.hivePos);
            if(hive instanceof BumbleguardBlockEntity){
                ((BumbleguardBlockEntity) hive).Enter(Bumbleguard.this);
            }
        }
    }

    class SuicideGoal extends BaseBeeGoal{
        SuicideGoal() {
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canBeeUse() {
            return !hiveIsValid();
        }

        @Override
        public boolean canBeeContinueToUse() {
            return !hiveIsValid();
        }

        @Override
        public void start() {
            Bumbleguard.this.kill(); // :( poor little bee
        }
    }

    // #################################################################################################################
    // HELPERS
    // #################################################################################################################


    List<LivingEntity> getNewEnemies(){

        //Create new list
        List<LivingEntity> enemies = new ArrayList<>();

        //If beehive is present, get the enemy list
        if(this.hasHive()){
            var hive = level().getBlockEntity(this.hivePos);
            if (hive instanceof BumbleguardBlockEntity){
                enemies.addAll(((BumbleguardBlockEntity) hive).getTargets());
            }
        }

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

    private Double getMinTimeOutOfHivePercentageProgress(){
        return this.ticksOutOfHiveCounter/ minimumTicksOutOfHive;
    }

    private void advanceTimeOutOfHiveCounter(){
        if(this.ticksOutOfHiveCounter < minimumTicksOutOfHive){
            this.ticksOutOfHiveCounter++;
        }
    }
}
