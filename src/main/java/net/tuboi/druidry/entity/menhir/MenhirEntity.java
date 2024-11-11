package net.tuboi.druidry.entity.menhir;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
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
import net.tuboi.druidry.registries.DruidrySoundRegistry;
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
import java.util.Objects;
import java.util.Random;

public class MenhirEntity extends LivingEntity implements GeoEntity {

    private @Nullable LivingEntity owner;
    private float spellpower;
    private int age;
    private boolean ejectedCreatures = false;
    private boolean erectParticlesSpawned = false;
    private final static Double menhirHeight = 3.0;

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

        if(this.age > 20 && this.age <= 60){// One second idle period after creation
            this.entityData.set(PHASE, "erect");

            if(!ejectedCreatures){
                ejectCreatures();
                ejectedCreatures = true;
            }
        }else if(this.age > 60){
            this.entityData.set(PHASE, "crumble");
        }

        if(level().isClientSide){
            spawnParticles();
        }else{
            playSound();
        }
    }

    private void ejectCreatures(){
        var targets = getTargets();

        double forceModifier = getForceFromSpellpower(this.spellpower);
        var lookAngle = this.getLookAngle().normalize();


        Vec3 forceDirection = switch (this.entityData.get(ANGLE_VARIANT)) {
            case 1 -> new Vec3(0, (double)6/6, 0).scale(forceModifier); //straight up
            case 2 -> new Vec3(lookAngle.x(), (double) 4/6, lookAngle.z()).scale(forceModifier); //60°
            case 3 -> new Vec3(lookAngle.x(), (double) 2/6, lookAngle.z()).scale(forceModifier); //30°
            default -> this.getLookAngle();
        };

        //Apply upwards force to all targets
        for (LivingEntity target : targets) {
            target.push(forceDirection.x(), forceDirection.y(), forceDirection.z());
        }
    }

    // #################################################################################################################
    // # Sound stuff
    // #################################################################################################################

    private boolean rumbleSoundPlayed = false;
    private boolean explodeSoundPlayed = false;
    private boolean crumbleSoundPlayed = false;

    private void playSound(){

        var phase = this.entityData.get(PHASE);

        if(phase.equals("idle") && !rumbleSoundPlayed){
            level().playSound(
                    this,
                    this.blockPosition(),
                    DruidrySoundRegistry.MENHIR_RUMBLE.get(),
                    SoundSource.BLOCKS,
                    1.0f,
                    0.95f + (float) Math.random() * 0.1f
            );
            rumbleSoundPlayed = true;

        }else if(phase.equals("erect") && !explodeSoundPlayed){
            level().playSound(
                    this,
                    this.blockPosition(),
                    DruidrySoundRegistry.MENHIR_ERUPT.get(),
                    SoundSource.BLOCKS,
                    1.0f,
                    0.95f + (float) Math.random() * 0.1f
            );
            explodeSoundPlayed = true;

        }else if(phase.equals("crumble") && !crumbleSoundPlayed){
            level().playSound(
                    this,
                    this.blockPosition(),
                    DruidrySoundRegistry.MENHIR_RUMBLE.get(),
                    SoundSource.BLOCKS,
                    1.0f,
                    0.95f + (float) Math.random() * 0.1f
            );
            crumbleSoundPlayed = true;
        }
    }

    // #################################################################################################################
    // # Client side
    // #################################################################################################################

    private void spawnParticles(){
        String phase = this.entityData.get(PHASE);

        if(phase.equals("idle")){
            spawnIdleParticles();
        }else if(phase.equals("erect") && !erectParticlesSpawned){
            spawnErectParticles((int)Math.pow(Objects.requireNonNull(this.getAttribute(Attributes.SCALE)).getValue()*2,3)*5);
            erectParticlesSpawned = true;
        }else if(phase.equals("erect") || phase.equals("crumble")){
            spawnErectParticles(1);
        }
    }

    private void spawnIdleParticles(){

        var scale = Objects.requireNonNull(this.getAttribute(Attributes.SCALE)).getValue()*2;

        //Create particles at the bottom of the menhir equal to 2x the scale
        for (int i = 0; i < Math.ceil(scale*scale); i++) {
            double x = this.position().x + Math.random() * scale - scale/2;
            double z = this.position().z + Math.random() * scale - scale/2;
            double y = this.position().y + Math.random() * 0.1;

            ParticleOptions particle = new BlockParticleOption(
                    ParticleTypes.BLOCK,
                    this.level().getBlockState(this.getOnPos())
            );

            this.level().addParticle(particle, x, y, z, 0, 0, 0);
        }
    }

    private void spawnErectParticles(int amount){

        var scale = Objects.requireNonNull(this.getAttribute(Attributes.SCALE)).getValue();
        var randomSource = new Random();

        for(int i = 0; i < amount; i++){
            ParticleOptions particle = new BlockParticleOption(
                    ParticleTypes.BLOCK,
                    this.level().getBlockState(this.getOnPos())
            );

            var verticalBlockDistanceFromBase = randomSource.nextDouble()*menhirHeight*scale;
            var randomOffset = randomSource.nextDouble()*scale - scale/2;

            Double radians = switch ((int) Math.ceil(randomSource.nextDouble() * 4)) {
                case 2 -> Math.toRadians(90d);
                case 3 -> Math.toRadians(180d);
                case 4 -> Math.toRadians(270d);
                default -> Math.toRadians(0d); //case 1
            };
            Vec3 surfacePosition = MenhirEntity.getCylinderSurfacePosition(verticalBlockDistanceFromBase,this, scale / 2, radians, randomOffset);

            Double tiltAngle = switch (this.entityData.get(ANGLE_VARIANT)) {
                case 2 -> 30d;
                case 3 -> 60d;
                default -> 0d; //case 1
            };
            Vec3 tiltedPosition = MenhirEntity.rotateAroundPoint(this.position(), surfacePosition, tiltAngle, this.getYRot());

            this.level().addParticle(
                    particle,
                    tiltedPosition.x(),
                    tiltedPosition.y(),
                    tiltedPosition.z(),
                    0,
                    0,
                    0
            );
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

    public static Double getScaleFromSpellpower(float spellpower){
        return 0.8 + spellpower / 5.0;
    }

    public static Double getForceFromSpellpower(float spellpower){
        return 1 + spellpower / 2.0;
    }

    private List<LivingEntity> getTargets(){

        var scale = this.getAttribute(Attributes.SCALE).getValue();

        //Expand scale to make it easier to hit creatures
        scale *= 2;

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

    //##################################################################################################################
    // Helpers
    //##################################################################################################################

    public static Vec3 getCylinderSurfacePosition(Double yMod, LivingEntity entity, double distance, double radians, double randomOffset) {
        var originalPosition = entity.position().add(0,yMod,0);
        double totalAngle = Math.toRadians(entity.getYRot()) + radians; // Add the input angle to the base angle

        double newX = originalPosition.x() + distance * Math.cos(totalAngle);
        double newZ = originalPosition.z() + distance * Math.sin(totalAngle);

        var adjustedPos = new Vec3(newX, originalPosition.y(), newZ);

        //Apply random offset to the position along the x and z axis in a 2d plane relative to the menhir
        return randomOffset != 0 ? offsetAlongPerpendicular(originalPosition, adjustedPos, randomOffset) : adjustedPos;
    }

    public static Vec3 offsetAlongPerpendicular(Vec3 center, Vec3 edgeCenter, double offsetScale) {
        Vec3 direction = new Vec3(edgeCenter.x - center.x, 0, edgeCenter.z - center.z).normalize();
        Vec3 perpendicular = new Vec3(-direction.z, 0, direction.x);

        return new Vec3(
                edgeCenter.x + perpendicular.x * offsetScale,
                edgeCenter.y,
                edgeCenter.z + perpendicular.z * offsetScale
        );
    }

    public static Vec3 rotateAroundPoint(Vec3 originPoint, Vec3 actualPoint, double angle, double yRot) {
        // Calculate the vector from the origin to the actual point
        Vec3 direction = actualPoint.subtract(originPoint);

        // Convert angle and yRot to radians
        double angleRad = Math.toRadians(-angle); //Flip the angle to make the rotation go forwards
        double yRotRad = Math.toRadians(yRot);

        // Calculate the rotation axis based on the yRot
        double sinYRot = Math.sin(yRotRad);
        double cosYRot = Math.cos(yRotRad);

        // Create a rotation axis in the x-z plane (tipping direction based on yRot)
        Vec3 rotationAxis = new Vec3(cosYRot, 0, sinYRot);

        // Calculate the new x, y, z coordinates after tipping by 'angle' around the rotation axis
        double rotatedX = direction.x * Math.cos(angleRad) + (rotationAxis.z * direction.y - rotationAxis.y * direction.z) * Math.sin(angleRad);
        double rotatedY = direction.y * Math.cos(angleRad) + (rotationAxis.x * direction.z - rotationAxis.z * direction.x) * Math.sin(angleRad);
        double rotatedZ = direction.z * Math.cos(angleRad) + (rotationAxis.y * direction.x - rotationAxis.x * direction.y) * Math.sin(angleRad);

        // Adjust the point back to the originPoint reference frame
        Vec3 rotatedPoint = new Vec3(
                originPoint.x + rotatedX,
                originPoint.y + rotatedY,
                originPoint.z + rotatedZ
        );

        return rotatedPoint;
    }

}
