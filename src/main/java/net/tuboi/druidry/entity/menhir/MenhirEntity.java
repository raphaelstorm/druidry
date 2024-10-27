package net.tuboi.druidry.entity.menhir;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

public class MenhirEntity extends LivingEntity implements GeoEntity {

    private int angleVariant;
    private float spellpower;
    private @Nullable LivingEntity owner;
    private int age;

    public MenhirEntity(EntityType<? extends MenhirEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.angleVariant = 1;
        this.spellpower = 1.0f;
        this.owner = null;
        this.age = 0;
    }

    public MenhirEntity(Level pLevel, Player pOwner, Float pSpellpower, int pAngleVariant) {
        this(DruidryEntityRegistry.MENHIR.get(), pLevel);
        this.owner = pOwner;
        this.spellpower = pSpellpower;
        this.angleVariant = pAngleVariant;
        this.age = 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
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

        if (this.age > 80) {
            this.remove(RemovalReason.DISCARDED);
        }
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

    // #################################################################################################################
    // # Geckolib animation stuff
    // #################################################################################################################

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController animationController = new AnimationController(this, "controller", 0, this::predicate);
    private final RawAnimation erect90Animation = RawAnimation.begin().thenPlay("erect_90");
    private final RawAnimation erect60Animation = RawAnimation.begin().thenPlay("erect_60");
    private final RawAnimation erect30Animation = RawAnimation.begin().thenPlay("erect_30");

    private Integer animationVersion = 1; //0 = no animation, 1 = 90°, 2 = 60°, 3 = 30°
    private PlayState predicate(software.bernie.geckolib.animation.AnimationState event) {
        if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
            if (animationVersion != 0) {
                event.getController().setAnimation(erect90Animation);
                animationVersion = 0;
            }
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
}
