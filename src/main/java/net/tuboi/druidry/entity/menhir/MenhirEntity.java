package net.tuboi.druidry.entity.menhir;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tuboi.druidry.registries.DruidryEntityRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Collections;
import java.util.List;

public class MenhirEntity extends LivingEntity implements GeoEntity {

    private @Nullable LivingEntity owner;
    private float spellpower;
    private int age;
    private boolean ejectedCreatures = false;

    private static final EntityDataAccessor<Integer> ANGLE_VARIANT = SynchedEntityData.defineId(MenhirEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> PHASE = SynchedEntityData.defineId(MenhirEntity.class, EntityDataSerializers.STRING);

    public MenhirEntity(EntityType<? extends MenhirEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.entityData.set(ANGLE_VARIANT, 0);
        this.entityData.set(PHASE, "idle");
        this.spellpower = 1.0f;
        this.owner = null;
        this.age = 0;
    }

    public MenhirEntity(Level pLevel, LivingEntity pOwner, Float pSpellpower, int pAngleVariant) {
        this(DruidryEntityRegistry.MENHIR.get(), pLevel);
        this.owner = pOwner;
        this.spellpower = pSpellpower;
        this.entityData.set(ANGLE_VARIANT, pAngleVariant);
        this.entityData.set(PHASE, "idle");
        this.age = 0;
        this.setYRot(pOwner.getYRot());
        this.setYHeadRot(pOwner.getYHeadRot());
        this.getAttribute(Attributes.SCALE).setBaseValue(getScaleFromSpellpower(pSpellpower));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(ANGLE_VARIANT, 0);
        pBuilder.define(PHASE, "idle");
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.SCALE, 1.0D);
    }

    // #################################################################################################################
    // # Game mechanics
    // #################################################################################################################

    @Override
    public void tick() {
        super.tick();

        if (this.age > 100) {
            this.remove(RemovalReason.DISCARDED);
        }else{
            this.age++;
        }

        if(this.age > 20){// One second idle period after creation
            this.entityData.set(PHASE, "erect");

            if(!ejectedCreatures){
                ejectCreatures();
                ejectedCreatures = true;
            }
        }
    }

    private void ejectCreatures(){
        var targets = getTargets();

        double forceModifier = getForceFromSpellpower(this.spellpower);
        var lookAngle = this.getLookAngle().normalize();
        Vec3 forceDirection;

        //get vector of force direction
        if(this.entityData.get(ANGLE_VARIANT) == 1){
            forceDirection = new Vec3(0, 1, 0).scale(forceModifier); //straight up
        }else if(this.entityData.get(ANGLE_VARIANT) == 2){
            forceDirection = new Vec3(lookAngle.x(), 1, lookAngle.z()).scale(forceModifier); //60°
        }else if(this.entityData.get(ANGLE_VARIANT) == 3){
            forceDirection = new Vec3(lookAngle.x(), 0.5, lookAngle.z()).scale(forceModifier); //30°
        }else{
            forceDirection = this.getLookAngle();
        }

        //Apply upwards force to all targets
        for (LivingEntity target : targets) {
            target.push(forceDirection.x(), forceDirection.y(), forceDirection.z());
        }
    }

    // #################################################################################################################
    // # Geckolib animation stuff
    // #################################################################################################################

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController animationController = new AnimationController(this, "controller", 0, this::predicate);
    private final RawAnimation erect90Animation = RawAnimation.begin().thenPlay("animation.menhir.erect_90");
    private final RawAnimation erect60Animation = RawAnimation.begin().thenPlay("animation.menhir.erect_60");
    private final RawAnimation erect30Animation = RawAnimation.begin().thenPlay("animation.menhir.erect_30");
    private final RawAnimation idleAnimation = RawAnimation.begin().thenPlay("animation.menhir.idle");
    private boolean animationPlayed = false;

    //0 = no animation, 1 = 90°, 2 = 60°, 3 = 30°
    private PlayState predicate(software.bernie.geckolib.animation.AnimationState event) {
        boolean noAnimationRunning = event.getController().getAnimationState() == AnimationController.State.STOPPED;
        int anim = this.entityData.get(ANGLE_VARIANT);
        String phase = this.entityData.get(PHASE);

        if(!animationPlayed && phase.equals("erect")){
            if (anim == 1) {
                event.getController().setAnimation(erect90Animation);
            }else if(anim == 2){
                event.getController().setAnimation(erect60Animation);
            }else if(anim == 3){
                event.getController().setAnimation(erect30Animation);
            }
            animationPlayed = true;
        }else if (noAnimationRunning && !animationPlayed && phase.equals("idle")){
            event.getController().setAnimation(idleAnimation);
        }

        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(animationController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // #################################################################################################################
    // # Helpers
    // #################################################################################################################

    private Double getScaleFromSpellpower(float spellpower){
        return 1.0 + spellpower / 10.0;
    }

    private Double getForceFromSpellpower(float spellpower){
        return 1.0 + spellpower / 10.0;
    }

    private List<LivingEntity> getTargets(){

        var scale = this.getAttribute(Attributes.SCALE).getValue();

        //Expand scale to make it easier to hit creatures
        scale *= 1.5;

        //Dirty but can be done as the menhir entity is exactly 1 block wide and 3 blocks tall
        var aabb = new AABB(
                this.position().x-scale/2,
                this.position().y,
                this.position().z-scale/2,
                this.position().x+scale/2,
                this.position().y+scale*3,
                this.position().z+scale/2
        );

        return level().getEntitiesOfClass(LivingEntity.class, aabb).stream().filter(e -> e != this).toList();
    }

    // #################################################################################################################
    // API
    // #################################################################################################################

    public static AttributeSupplier.Builder prepareAttributes() {
        return LivingEntity.createLivingAttributes();
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity pEntity) {
        return false; //No collision with other entities
    }

    @Override
    public boolean isColliding(BlockPos pPos, BlockState pState) {
        return false;
    }

    @Override
    protected boolean isHorizontalCollisionMinor(Vec3 pDeltaMovement) {
        return false;
    }

    @Override
    public boolean isInWall() {
        return false; //Prevent menhir from appearing gray as it will always be inside a wall
    }

    @Override
    public @NotNull Iterable<ItemStack> getArmorSlots() {
        return Collections.singleton(ItemStack.EMPTY);
    }

    @Override
    public @NotNull ItemStack getItemBySlot(EquipmentSlot pSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot pSlot, ItemStack pStack) {

    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.LEFT;
    }

}
