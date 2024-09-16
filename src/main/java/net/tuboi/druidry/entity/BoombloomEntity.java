package net.tuboi.druidry.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.tuboi.druidry.registries.DruidryEntityRegistry;
import net.tuboi.druidry.utils.ParticleHelper;

import javax.annotation.Nullable;

public class BoombloomEntity extends Entity {

    private LivingEntity owner; //Player that placed the boombloom
    private Float spellpower;


    public BoombloomEntity(EntityType<? extends BoombloomEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public BoombloomEntity(Level pLevel, LivingEntity pOwner, Float pSpellpower, double pX, double pY, double pZ) {
        this(DruidryEntityRegistry.BOOMBLOOM_ENTITY.get(), pLevel); //Run entity constructor
        this.owner = pOwner;
        this.spellpower = pSpellpower;
        this.setPos(pX, pY, pZ);
        this.xo = pX;
        this.yo = pY;
        this.zo = pZ;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {

    }

    public void tick(){
        super.tick();

        //For testing, create particle
        this.level().addParticle(ParticleHelper.FLOWER_EMITTER, this.xo, this.yo, this.zo,
                random.nextDouble()*0.1,
                random.nextDouble(),
                random.nextDouble()*0.1);

        //Check if flower is still present


        //If not, fizzleout


        //Check if a player or hostile entity that is not the caster exists within X blocks

        //If yes, explode


    }

    private void fizzleOut(){

    }

    private void detonate(){

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {

    }

    @Nullable
    public LivingEntity getOwner() {
        return this.owner;
    }
}
