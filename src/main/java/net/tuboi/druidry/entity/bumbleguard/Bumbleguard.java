package net.tuboi.druidry.entity.bumbleguard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.LookControl;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.tuboi.druidry.block.bumbleguardhive.BumbleguardBlockEntity;
import net.tuboi.druidry.registries.DruidryEntityRegistry;
import net.tuboi.druidry.utils.ParticleHelper;
import org.jetbrains.annotations.NotNull;

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

    private final Integer minimumTicksOutOfHive = 800;
    private Double ticksOutOfHiveCounter = 0d;
    private UUID ownerUUID;
    private String bumbleId;

    @Nullable
    private BlockPos hivePos;

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

    public Bumbleguard(Level world, UUID ownerUUID, String bumbleID, BlockPos hivePos, double pX, double pY, double pZ) {
        super(DruidryEntityRegistry.BUMBLEGUARD.get(), world);
        this.ownerUUID = ownerUUID;
        this.bumbleId = bumbleID;
        this.hivePos = hivePos;
        this.setPos(pX, pY, pZ);
        this.xo = pX;
        this.yo = pY;
        this.zo = pZ;
        this.ticksOutOfHiveCounter+=Math.ceil(getRandom().nextDouble()*600);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.lookControl = new Bumbleguard.BeeLookControl(this);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 16.0F);
        this.setPathfindingMalus(PathType.COCOA, -1.0F);
        this.setPathfindingMalus(PathType.FENCE, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.FLYING_SPEED, 1.0F)
                .add(Attributes.MOVEMENT_SPEED, 0.5F)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.SCALE, 1.3);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new Bumbleguard.BeeAttackGoal(this, 1.4f, true));
        this.goalSelector.addGoal(1, new Bumbleguard.EnterHiveGoal());
        this.goalSelector.addGoal(2, new Bumbleguard.ReturnToHiveGoal());
        this.goalSelector.addGoal(3, new BeeWanderGoal());
        this.goalSelector.addGoal(4, new Bumbleguard.SuicideGoal());
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        // Save ticksOutOfHiveCounter
        tag.putDouble("TicksOutOfHiveCounter", this.ticksOutOfHiveCounter);

        // Save owner UUID
        if (hasOwner()) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }

        // Save bumbleId
        if (this.bumbleId != null) {
            tag.putString("BumbleId", this.bumbleId);
        }

        // Save hivePos
        if (this.hivePos != null) {
            tag.putInt("HivePosX", this.hivePos.getX());
            tag.putInt("HivePosY", this.hivePos.getY());
            tag.putInt("HivePosZ", this.hivePos.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        // Load ticksOutOfHiveCounter
        this.ticksOutOfHiveCounter = tag.getDouble("TicksOutOfHiveCounter");

        // Load owner UUID
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }

        // Load bumbleId
        if (tag.contains("BumbleId")) {
            this.bumbleId = tag.getString("BumbleId");
        }

        // Load hivePos
        if (tag.contains("HivePosX") && tag.contains("HivePosY") && tag.contains("HivePosZ")) {
            this.hivePos = new BlockPos(tag.getInt("HivePosX"), tag.getInt("HivePosY"), tag.getInt("HivePosZ"));
        }
    }

    /*
              _   _
             ( | / )
           \\ \|/,'_
           (")(_)()))=-
              <\\
     */
    // #################################################################################################################
    // API
    // #################################################################################################################

    public boolean hasHive(){
        return this.hivePos != null;
    }

    public boolean hasTarget(){
        return Bumbleguard.this.getTarget() != null && Bumbleguard.this.getTarget().isAlive();
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
    protected void checkFallDamage(double pY, boolean pOnGround, BlockState pState, BlockPos pPos) {
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    protected void playStepSound(BlockPos pPos, BlockState pBlock) {
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BEE_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.BEE_HURT;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
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
        return this.ownerUUID;
    }

    public Player getOwner(){
        if(hasOwner()){
            return this.level().getPlayerByUUID(this.ownerUUID);
        }
        return null;
    }

    public String getBumbleId(){
        return this.bumbleId;
    }

    public Boolean hiveIsValid(){
        return hasHive() && level().getBlockEntity(this.hivePos) instanceof BumbleguardBlockEntity && ((BumbleguardBlockEntity) level().getBlockEntity(this.hivePos)).IsMemberOf(Bumbleguard.this.getBumbleId());
    }

    @Nullable
    public BlockEntity getHive(){
        if(hiveIsValid()){
            return level().getBlockEntity(this.hivePos);
        }
        return null;
    }

    public Boolean hasOwner(){
        return this.ownerUUID != null;
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
    protected @NotNull PathNavigation createNavigation(@NotNull Level pLevel) {
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

        if(level().isClientSide){
            if(this.random.nextDouble()>0.9){
                spawnParticles(1);
            }
        }else{
            advanceTimeOutOfHiveCounter();
        }
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
            return super.canContinueToUse()
                    && Bumbleguard.this.hasTarget()
                    && isWithinChaseDistance(Bumbleguard.this.getTarget()) //todo: make chase distance be stored in the bee, constantly fetching it from the hive is not efficient
                    && (!hasOwner()
                    || (Bumbleguard.this.hiveIsValid() && BumbleguardBlockEntity.isValidTarget((BumbleguardBlockEntity)getHive(), Bumbleguard.this.getTarget())));
        }

        @Override
        public void stop() {
            super.stop();
            if(Bumbleguard.this.hasTarget() && Bumbleguard.this.hiveIsValid() && !BumbleguardBlockEntity.isValidTarget((BumbleguardBlockEntity)getHive(), Bumbleguard.this.getTarget())){
                Bumbleguard.this.setTarget(null);
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
            return hiveIsValid() && getMinTimeOutOfHivePercentageProgress() == 1 && Bumbleguard.this.hivePos.closerThan(new Vec3i(Bumbleguard.this.getBlockX(), Bumbleguard.this.getBlockY(), Bumbleguard.this.getBlockZ()), 1.5);
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

    private Double getMinTimeOutOfHivePercentageProgress(){
        return this.ticksOutOfHiveCounter/ minimumTicksOutOfHive;
    }

    private void advanceTimeOutOfHiveCounter(){
        if(this.ticksOutOfHiveCounter < minimumTicksOutOfHive){
            this.ticksOutOfHiveCounter++;
        }
    }

    //Check if the target is within the chase distance of the hive
    private boolean isWithinChaseDistance(LivingEntity target){
        return this.hiveIsValid() && this.hivePos.getCenter().closerThan(target.position(), ((BumbleguardBlockEntity) level().getBlockEntity(this.hivePos)).getChaseDistance());
    }

    private void spawnParticles(Integer count) {

        //todo: make new particle thats a bit smaller, like magic dust or something

        for (int i = 0; i < count; i++) {
            double x = this.getX() + (this.getRandom().nextDouble() - 0.5D) * (double)this.getBbWidth();
            double y = this.getY() + this.getRandom().nextDouble() * (double)this.getBbHeight();
            double z = this.getZ() + (this.getRandom().nextDouble() - 0.5D) * (double)this.getBbWidth();
            level().addParticle(ParticleHelper.FERTILIZER_EMITTER, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }


}
