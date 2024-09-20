package net.tuboi.druidry.entity;

import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.tuboi.druidry.registries.DruidryEntityRegistry;
import net.tuboi.druidry.registries.DruidrySpellRegistry;
import net.tuboi.druidry.utils.ParticleHelper;
import net.tuboi.druidry.utils.Utils;

import javax.annotation.Nullable;
import java.util.List;

public class BoombloomEntity extends Entity {

    private LivingEntity owner; //Player that placed the boombloom
    private Double fuse;

    //Phase can one of the following: STANDBY, TRIGGERED, IGNITED, TICKING, EXPLOSION, DEAD, REMOVED
    private static final EntityDataAccessor<String> PHASE = SynchedEntityData.defineId(BoombloomEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> SPELLPOWER = SynchedEntityData.defineId(BoombloomEntity.class, EntityDataSerializers.FLOAT);

    public BoombloomEntity(EntityType<? extends BoombloomEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public BoombloomEntity(Level pLevel, LivingEntity pOwner, Float pSpellpower, double pX, double pY, double pZ) {
        this(DruidryEntityRegistry.BOOMBLOOM_ENTITY.get(), pLevel); //Run entity constructor
        this.owner = pOwner;
        this.entityData.set(SPELLPOWER, pSpellpower);
        this.setPos(pX, pY, pZ);
        this.xo = pX;
        this.yo = pY;
        this.zo = pZ;
        this.fuse = 0d;
        this.setSilent(false);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        pBuilder.define(SPELLPOWER, 0f);
        pBuilder.define(PHASE, "STANDBY");
    }

    public void tick(){
        super.tick();

        //Kill on serverside if dead
        if(!level().isClientSide && this.entityData.get(PHASE) == "DEAD"){
            this.kill();
        }

        //Check if flower block is still present on serverside
        if(!level().isClientSide() && !level().getBlockState(blockPosition()).is(BlockTags.FLOWERS)){
            this.entityData.set(PHASE, "REMOVED");
        }

        //Check if a player or hostile entity that is not the caster exists within X blocks
        if(!level().isClientSide() && entityDetected()){
            if(this.entityData.get(PHASE).equals("STANDBY")){
                this.entityData.set(PHASE, "TRIGGERED");
            }
        }

        //Handle phase changes on server side
        if(!level().isClientSide() && !this.entityData.get(PHASE).equals("STANDBY")&&!level().isClientSide() && !this.entityData.get(PHASE).equals("REMOVED")){
            this.fuse++;
            //Bloom explodes 40ticks / 2 seconds after being set
            double fusetime = 20d;
            fusetime += Math.ceil(random.nextDouble()*20); //Add random diviation to fuse time
            if(this.fuse == 1){ //Upon fuse countdown start, change phase from TRIGGERED to IGNITED
                this.entityData.set(PHASE, "IGNITED");
            }else if (this.fuse > 1 && this.fuse < fusetime && !this.entityData.get(PHASE).equals("TICKING")){ //On later ticks, set phase to TICKING if not already set
                this.entityData.set(PHASE, "TICKING");
            }else if(this.fuse.equals(fusetime) && !this.entityData.get(PHASE).equals("EXPLOSION")){
                this.entityData.set(PHASE, "EXPLOSION");
                explodeLogic();
            }else if(this.fuse > fusetime && !this.entityData.get(PHASE).equals("DEAD")){
                this.entityData.set(PHASE, "DEAD");
            }
        }

        //Play effects
        if(level().isClientSide){
            SpawnParticles(); //Play particles for client
        }else{
            PlaySounds(); //Play sounds server side (for some reason)
        }

        if(!level().isClientSide() && this.entityData.get(PHASE).equals("REMOVED")){
            this.entityData.set(PHASE, "DEAD");
        }
    }

    public void Detonate(){
        if(!level().isClientSide && this.entityData.get(PHASE).equals("STANDBY")){
            this.entityData.set(PHASE,"TRIGGERED");
        }
    }

    protected void PlaySounds(){
        if(this.entityData.get(PHASE).equals("REMOVED")){ //Effects when flower is removed / defused
            this.playSound(SoundEvents.LAVA_EXTINGUISH);
        }else if(this.entityData.get(PHASE).equals("IGNITED")){ //Effects right after flower was triggered
            //todo: create custom sound event for ignition
            this.playSound(SoundEvents.TNT_PRIMED);
        }else if (this.entityData.get(PHASE).equals("TICKING")){ //Particle effects while flower is ticking down
            //no sound for now
        }else if(this.entityData.get(PHASE).equals("EXPLOSION")){
            //Play explosion sound
            this.playSound(SoundEvents.GENERIC_EXPLODE.value());
        }
    }

    protected void SpawnParticles(){
        if(this.entityData.get(PHASE).equals("REMOVED")){ //Effects when flower is removed / defused
            removedParticles();
        }else if(this.entityData.get(PHASE).equals("IGNITED")){ //Effects right after flower was triggered
            ignitedParticles();
        }else if (this.entityData.get(PHASE).equals("TICKING")){ //Particle effects while flower is ticking down
            tickingParticles();
        }else if(this.entityData.get(PHASE).equals("EXPLOSION")){
            explodeParticles();
        }
    }

    private void removedParticles(){
        for(int i = 0; i < 30; i++){
            this.level().addParticle(ParticleTypes.WHITE_SMOKE, randomVariation(this.xo,0.2), randomVariation(this.yo,0.2), randomVariation(this.zo,0.2),
                    0,
                    (random.nextDouble()*0.1)-0.05,
                    0);
        }
    }

    private void ignitedParticles(){
        //Throw some sparkles in the air
        for(int i = 0; i < 30; i++){
            this.level().addParticle(ParticleHelper.FERTILIZER_EMITTER, randomVariation(this.xo,0.05), randomVariation(this.yo,0.05), randomVariation(this.zo,0.05),
                    (random.nextDouble()*0.3)-0.15,
                    (random.nextDouble()*0.3)-0.15,
                    (random.nextDouble()*0.3)-0.15);
        }
    }

    private void tickingParticles(){
        //create particle flower fountain
        this.level().addParticle(ParticleHelper.FLOWER_EMITTER, randomVariation(this.xo,0.05), randomVariation(this.yo,0.05), randomVariation(this.zo,0.05),
                (random.nextDouble()*0.2)-0.1,
                random.nextDouble(),
                (random.nextDouble()*0.2)-0.1);
    }

    private void explodeParticles(){
        float explosionradius = getExplosionRadius(this.entityData.get(SPELLPOWER)); //Get explosion radius
        double particleamount = getParticleCount(explosionradius,100); //Get amount of particles
        float baseParticleSpeed = Utils.GetParticleSpeedNeededForTravelDistance(explosionradius, 0.1f,20);
        float particleSpeedVariationScalar = baseParticleSpeed*0.1f; //Scale speed by +-10 percent

        //Create particles
        for(int i = 0; i<particleamount; i++){

            float particleSpeed = baseParticleSpeed - particleSpeedVariationScalar + (particleSpeedVariationScalar*random.nextFloat());

            double[] speeds = Utils.GetVectorSpeeds(particleSpeed); //Get speeds for sphere of particles

            //create particle flower fountain
            this.level().addParticle(ParticleHelper.FLOWER_EMITTER, randomVariation(this.xo,0.05), randomVariation(this.yo,0.05), randomVariation(this.zo,0.05),
                    speeds[0],
                    speeds[1],
                    speeds[2]);
        }
    }

    private void explodeLogic(){

        //Get all blooms within explosionradius and trigger them as well
        float r = getExplosionRadius(this.entityData.get(SPELLPOWER)); //Get detection radius

        //Fetch all other boomblooms around this one
        List<BoombloomEntity> nearbyboomblooms = level().getEntitiesOfClass(
                BoombloomEntity.class,
                AABB.ofSize(this.blockPosition().getCenter(),r+0.5,r+0.5,r+0.5) //Get volume of the block
        );

        //Detonate all nearby boomblooms
        nearbyboomblooms.forEach(boombloomEntity -> boombloomEntity.Detonate());

        var flowerBlockstate = level().getBlockState(this.blockPosition());
        if (this.owner instanceof Player p) {
            BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level(), this.blockPosition(), level().getBlockState(this.blockPosition()),p);
            NeoForge.EVENT_BUS.post(event);
        }

        //Fetch all players in explosion radius
        List<LivingEntity> nearbyEntities = level().getEntitiesOfClass(
                LivingEntity.class,
                AABB.ofSize(this.blockPosition().getCenter(),r+0.5,r+0.5,r+0.5)
        );
        nearbyEntities.forEach(nearbyEntity -> {
            //todo: apply force to creature

            DamageSources.applyDamage(nearbyEntity, getDamage(), DruidrySpellRegistry.BOOMBLOOM_SPELL.get().getDamageSource(this,getOwner()));
        });
    }

    private boolean entityDetected(){
        float dr = getDetectionRadius(this.entityData.get(SPELLPOWER)); //Get detection radius

        //Fetch all living entities within an area around the boombloom
        List<LivingEntity> entitiesWithinDetectionRange = level().getEntitiesOfClass(
                LivingEntity.class,
                AABB.ofSize(this.blockPosition().getCenter(),dr+0.5,dr+0.5,dr+0.5) //Get volume of the block
        );

        return !entitiesWithinDetectionRange.isEmpty();
    }

    private double getParticleCount(float radius, int baseParticles) {
        // baseParticles is the number of particles you want at radius 1
        return Math.round(baseParticles * radius * radius);
    }

    private float getExplosionRadius(float spellpower){
        return Math.round(Math.sqrt(spellpower)); //Radius starts out at 4, and slowly increase with the square of the spell power.
    }

    private float getDetectionRadius(float spellpower){
        return 2 + (float)(Math.sqrt(spellpower))/4;
    }

    private double randomVariation(double number, double variation){
        return number - variation + (random.nextDouble() * 2 * variation);
    };

    private float getDamage(){
        //Damage is spellpower squared multiplied by four
        return (float)Math.sqrt(this.entityData.get(SPELLPOWER))*4;
    };

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
