package net.tuboi.druidry.entity;

import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tuboi.druidry.registries.DruidryEntityRegistry;
import net.tuboi.druidry.registries.DruidrySoundRegistry;
import net.tuboi.druidry.registries.DruidrySpellRegistry;
import net.tuboi.druidry.utils.ParticleHelper;
import net.tuboi.druidry.utils.Utils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BoombloomEntity extends Entity {

    //Local fields for server side
    private Double fuse = 0d;
    private Double age= 0d;
    private Double timeToKill = 0d;
    private Double armingTickCounterServer = 0d;

    //Local fields for client side
    private boolean removedParticlesDisplayedClientSide = false;
    private Double armingTickCounterClient = 0d;

    //Synced fields
    private static final EntityDataAccessor<String> PHASE = SynchedEntityData.defineId(BoombloomEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> SPELLPOWER = SynchedEntityData.defineId(BoombloomEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> EMITPARTICLES = SynchedEntityData.defineId(BoombloomEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ARMINGTIME = SynchedEntityData.defineId(BoombloomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FUSETIME = SynchedEntityData.defineId(BoombloomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(BoombloomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(BoombloomEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> SPAWN_PARTICLE_HEIGHT = SynchedEntityData.defineId(BoombloomEntity.class, EntityDataSerializers.FLOAT);


    // #################################################################################################################
    // CONSTRUCTOR STUFF
    // #################################################################################################################

    public BoombloomEntity(EntityType<? extends BoombloomEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public BoombloomEntity(Level pLevel, Player pOwner, Float pSpellpower, double pX, double pY, double pZ, boolean emitParticlesOnStandby, Double lifetime, Double fusetime, Double armingTime, Double spawnParticleHeight) {
        this(DruidryEntityRegistry.BOOMBLOOM_ENTITY.get(), pLevel); //Run entity constructor
        this.entityData.set(OWNER_UUID, Optional.of(pOwner.getUUID()));
        this.entityData.set(SPELLPOWER, pSpellpower);
        this.setPos(pX, pY, pZ);
        this.xo = pX;
        this.yo = pY;
        this.zo = pZ;
        this.entityData.set(EMITPARTICLES, emitParticlesOnStandby);
        this.entityData.set(LIFETIME, lifetime.intValue());
        this.entityData.set(FUSETIME, fusetime.intValue());
        this.entityData.set(ARMINGTIME, armingTime.intValue());
        this.entityData.set(SPAWN_PARTICLE_HEIGHT, spawnParticleHeight.floatValue());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        pBuilder.define(SPELLPOWER, 0f);
        pBuilder.define(PHASE, Phases.UNARMED);
        pBuilder.define(EMITPARTICLES, false);
        pBuilder.define(ARMINGTIME, 40);
        pBuilder.define(FUSETIME, 40);
        pBuilder.define(LIFETIME, 2400);
        pBuilder.define(OWNER_UUID, Optional.empty());
        pBuilder.define(SPAWN_PARTICLE_HEIGHT, 3f);
    }

    // #################################################################################################################
    // SERVER SIDE LOGIC
    // #################################################################################################################

    public void tick(){
        super.tick();

        //Kill on serverside if dead
        if(!level().isClientSide && this.entityData.get(PHASE).equals(Phases.DEAD)){
            if(timeToKill <5d){ //wait before killing entity, giving client side time to display particles
                timeToKill++;
            }else{
                this.kill();
            }
        }

        //Countdown to natural death if a age limit is set
        Integer lifeTime = this.entityData.get(LIFETIME);
        if(!level().isClientSide && lifeTime != 0){
            if(this.age >= lifeTime){
                this.entityData.set(PHASE, Phases.DEFUSED);
            }else{
                this.age++;
            }
        }

        //Check if flower block is still present on serverside
        if(!level().isClientSide() && !level().getBlockState(blockPosition()).is(BlockTags.FLOWERS) && !this.entityData.get(PHASE).equals(Phases.DEFUSED) && !this.entityData.get(PHASE).equals(Phases.UNARMED)&& !this.entityData.get(PHASE).equals(Phases.DEAD)){
            this.entityData.set(PHASE, Phases.DEFUSED);
        }

        //Check if a player or hostile entity that is not the caster exists within X blocks
        if(!level().isClientSide() && entityDetected() && this.entityData.get(PHASE).equals(Phases.ARMED)){
            this.entityData.set(PHASE, Phases.TRIGGERED);
        }

        //Handle arming period
        if(this.entityData.get(PHASE).equals(Phases.UNARMED)){
            if(!level().isClientSide){
                if(this.armingTickCounterServer > this.entityData.get(ARMINGTIME)){ //Check if arming sequence is over
                    //Check if there is a flower on the boomblooms location, if not, create one
                    if(!level().getBlockState(this.blockPosition()).is(BlockTags.FLOWERS)){

                        //Check that the block below is dirt and that this block is air or grass
                        if(!level().getBlockState(this.blockPosition().below()).is(BlockTags.DIRT) || !level().getBlockState(this.blockPosition()).isEmpty())
                        {
                            this.entityData.set(PHASE, Phases.DEFUSED);
                        }

                        BlockState newFlower = Utils.GetRandomNormalFlower().defaultBlockState();
                        level().setBlockAndUpdate(this.blockPosition(), newFlower);
                    }
                    this.entityData.set(PHASE, Phases.ARMED); //If yes, arm flower
                }else{
                    this.armingTickCounterServer++; //If not, increase timer
                }
            }
        }

        //Handle phase changes on server side
        if(!level().isClientSide() && !this.entityData.get(PHASE).equals(Phases.ARMED) && !this.entityData.get(PHASE).equals(Phases.DEFUSED) && !this.entityData.get(PHASE).equals(Phases.DEAD)&& !this.entityData.get(PHASE).equals(Phases.UNARMED)){
            String phase = this.entityData.get(PHASE);
            Integer fuseTime = this.entityData.get(FUSETIME);
            if(this.fuse == 0 || phase.equals(Phases.TRIGGERED)){ //Upon fuse countdown start, change phase from TRIGGERED to IGNITED
                this.entityData.set(PHASE, Phases.IGNITED);
                this.fuse++;
            }else if (this.fuse > 0 && this.fuse < fuseTime || phase.equals(Phases.IGNITED)){ //On later ticks, set phase to TICKING if not already set
                if(phase.equals(Phases.IGNITED)){
                    this.entityData.set(PHASE, Phases.TICKING);
                }
                this.fuse++;
            }else if(this.fuse >= fuseTime && phase.equals(Phases.TICKING)){
                this.entityData.set(PHASE, Phases.EXPLOSION);
                explodeLogic();
                this.fuse++;
            }else if(this.fuse >= fuseTime && phase.equals(Phases.EXPLOSION)){
                this.entityData.set(PHASE, Phases.DEAD);
                level().setBlockAndUpdate(this.blockPosition(),Blocks.AIR.defaultBlockState()); //Break the flower
            }
        }

        //Play effects
        if(level().isClientSide){
            SpawnParticles(); //Play particles for client
        }else{
            PlaySounds(); //Play sounds server side (for some reason)
        }

        if(!level().isClientSide() && this.entityData.get(PHASE).equals(Phases.DEFUSED)){
            this.entityData.set(PHASE, Phases.DEAD);
        }
    }

    private void explodeLogic(){

        //Get all blooms within explosionradius and trigger them as well
        double r = getExplosionRadius(this.entityData.get(SPELLPOWER)); //Get detection radius

        //Fetch all other boomblooms around this one
        List<BoombloomEntity> nearbyboomblooms = level().getEntitiesOfClass(
                BoombloomEntity.class,
                AABB.ofSize(this.blockPosition().getCenter(),r+0.5,r+0.5,r+0.5) //Get volume of the block
        );

        //Detonate all nearby boomblooms (currently disabled)
        //nearbyboomblooms.forEach(boombloomEntity -> boombloomEntity.Detonate());

        //Fetch all players in explosion radius
        List<LivingEntity> nearbyEntities = level().getEntitiesOfClass(
                LivingEntity.class,
                AABB.ofSize(this.blockPosition().getCenter(),r+0.5,r+0.5,r+0.5)
        );
        nearbyEntities.forEach(nearbyEntity -> {

            //todo: the force doesn't scale right, don't know whats wrong but it seems to set the same output force regardless of spellpower
            //Create a vector between the boombloom and the struck entity
            Vec3 originToEntityVec = this.position().vectorTo(nearbyEntity.position()).normalize();

            //Get force from distance
            double pushforce = calcInverseDistanceForce(
                    this.position().distanceTo(nearbyEntity.position()),
                    this.entityData.get(SPELLPOWER)*100,
                    getExplosionRadius(entityData.get(SPELLPOWER))); //Multiply output force by 4

            //Scale the vector to be equal to the spellpower squared and squared again by the distance
            originToEntityVec.scale(pushforce);

            //Apply force to entity
            nearbyEntity.setDeltaMovement(originToEntityVec);

            if(getOwner().isPresent()) {
                DamageSources.applyDamage(nearbyEntity, getDamage(), DruidrySpellRegistry.HIDDEN_BOOMBLOOM_SPELL.get().getDamageSource(this, getOwner().get()));
            }
            });
    }

    private boolean entityDetected(){
        float dr = getDetectionRadius(this.entityData.get(SPELLPOWER)); //Get detection radius

        //Fetch all living entities within an area around the boombloom
        List<LivingEntity> entitiesWithinDetectionRange = level().getEntitiesOfClass(
                LivingEntity.class,
                AABB.ofSize(this.blockPosition().getCenter(),dr+0.5,dr+0.5,dr+0.5) //Get volume of the block
        );

        //Remove the owner from the list
        for(int i=0;i<entitiesWithinDetectionRange.size();i++){
            if(getOwner().isPresent() && entitiesWithinDetectionRange.get(i).equals(getOwner().get())){
                entitiesWithinDetectionRange.remove(i);
                i--;
            }
        };

        return !entitiesWithinDetectionRange.isEmpty();
    }

    // #################################################################################################################
    // SOUND EFFECTS
    // #################################################################################################################

    protected void PlaySounds(){
        if(this.entityData.get(PHASE).equals(Phases.DEFUSED)){ //Effects when flower is removed / defused
            this.playSound(SoundEvents.LAVA_EXTINGUISH);
            this.playSound(DruidrySoundRegistry.WINDY_LEAVES.value());
        }else if(this.entityData.get(PHASE).equals(Phases.IGNITED)){ //Effects right after flower was triggered
            //todo: create custom sound event for ignition
            this.playSound(SoundEvents.TNT_PRIMED);
        }else if (this.entityData.get(PHASE).equals(Phases.TICKING)){ //Particle effects while flower is ticking down
            //no sound for now
        }else if(this.entityData.get(PHASE).equals(Phases.EXPLOSION)){
            //Play explosion sound
            this.playSound(SoundEvents.GENERIC_EXPLODE.value());
            this.playSound(DruidrySoundRegistry.WINDY_LEAVES.value());
        }
    }

    // #################################################################################################################
    // PARTICLES
    // #################################################################################################################

    protected void SpawnParticles(){
        if(this.entityData.get(PHASE).equals(Phases.DEFUSED) || this.entityData.get(PHASE).equals(Phases.DEAD) && !this.removedParticlesDisplayedClientSide){ //Effects when flower is removed / defused
            removedParticles();
            removedParticlesDisplayedClientSide = true;
        }else if(this.entityData.get(PHASE).equals(Phases.IGNITED)){ //Effects right after flower was triggered
            ignitedParticles();
        }else if (this.entityData.get(PHASE).equals(Phases.TICKING)){ //Particle effects while flower is ticking down
            tickingParticles();
        }else if(this.entityData.get(PHASE).equals(Phases.EXPLOSION)){
            explodeParticles();
        }else if(this.entityData.get(PHASE).equals(Phases.ARMED) && this.entityData.get(EMITPARTICLES).equals(true)){ //only emit passive particles if on standby and passive emission is enabled
            passiveParticles();
        }else if(this.entityData.get(PHASE).equals(Phases.UNARMED) && armingTickCounterClient <= this.entityData.get(ARMINGTIME)) { //Check if arming sequence is over
            armingParticles(armingTickCounterClient, (double)this.entityData.get(ARMINGTIME));
            armingTickCounterClient++;
        }
    }

    private void passiveParticles(){
        if(this.random.nextDouble() > 0.95){
            this.level().addParticle(ParticleTypes.FLAME, randomVariation(this.xo,0.2), randomVariation(this.yo,0.2), randomVariation(this.zo,0.2),
                    (random.nextDouble()*0.1)-0.05,
                    (random.nextDouble()*0.2),
                    (random.nextDouble()*0.1)-0.05);
        }
    }

    private void removedParticles(){
        for(int i = 0; i < 30; i++){
            this.level().addParticle(ParticleTypes.CLOUD, randomVariation(this.xo,0.2), randomVariation(this.yo,0.2), randomVariation(this.zo,0.2),
                    (random.nextDouble()*0.1)-0.05,
                    (random.nextDouble()*0.5),
                    (random.nextDouble()*0.1)-0.05);
        }
        for(int i = 0; i < 10; i++){
            this.level().addParticle(ParticleHelper.FLOWER_EMITTER, randomVariation(this.xo,0.2), randomVariation(this.yo,0.2), randomVariation(this.zo,0.2),
                    (random.nextDouble()*0.1)-0.03,
                    (random.nextDouble()*0.3),
                    (random.nextDouble()*0.1)-0.03);
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
        double explosionradius = getExplosionRadius(this.entityData.get(SPELLPOWER)); //Get explosion radius
        double particleamount = getParticleCount(explosionradius,50); //Get amount of particles
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

            if(random.nextDouble() >= 0.75){
                this.level().addParticle(ParticleHelper.FERTILIZER_EMITTER, randomVariation(this.xo,0.05), randomVariation(this.yo,0.05), randomVariation(this.zo,0.05),
                        speeds[0],
                        speeds[1],
                        speeds[2]);
            }

            if(random.nextDouble() >= 0.75){
                Double rand = random.nextDouble()*0.5;
                this.level().addParticle(ParticleTypes.CLOUD, randomVariation(this.xo,1), randomVariation(this.yo,1), randomVariation(this.zo,1),
                        speeds[0]*rand,
                        speeds[1]*rand,
                        speeds[2]*rand);
            }
        }
    }

    private void armingParticles(Double currentTick, Double totalTicks){

        //Desired effect:
        //particles start out in a sphere around the flower, moving quickly away from it.
        //As timer reaches 100%, the flowers will move more slowly away, as well as spawning closer to the center
        //As the timer completes, the flowers spawn in the center and does not move

        Double maxDist = this.entityData.get(SPAWN_PARTICLE_HEIGHT).doubleValue();
        Double progress = (totalTicks-currentTick)/totalTicks;
        Double particleCount = 2d;

        //Get particle speed and spawn distance
        Double speed = maxDist*progress;

        if (getOwner().isPresent() && getOwner().get() instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.irons_spellbooks.radius",particleCount).withStyle(ChatFormatting.RED)));
        }

        for(int i = 0; i<particleCount; i++){
            Double randomScalar = 0.1*((random.nextDouble()*0.25)+0.75);
            this.level().addParticle(ParticleHelper.FERTILIZER_EMITTER, this.xo+(random.nextDouble()*0.1)-0.05, this.yo+speed, this.zo+(random.nextDouble()*0.1)-0.05,
                    (random.nextDouble()*0.1)-0.05,
                    speed*randomScalar,
                    (random.nextDouble()*0.1)-0.05);
        }

        //Create burst of particles on final tick
        if(currentTick.equals(totalTicks)){
            for(int i = 0; i < 20; i++){
                this.level().addParticle(ParticleHelper.FLOWER_EMITTER, randomVariation(this.xo,0.05), randomVariation(this.yo,0.05), randomVariation(this.zo,0.05),
                        (random.nextDouble()*0.5)-0.25,
                        random.nextDouble()*0.25,
                        (random.nextDouble()*0.5)-0.25);
            }
            for(int i = 0; i < 10; i++){
                this.level().addParticle(ParticleTypes.FLAME, randomVariation(this.xo,0.05), randomVariation(this.yo,0.05), randomVariation(this.zo,0.05),
                        (random.nextDouble()*0.3)-0.15,
                        random.nextDouble()*0.4,
                        (random.nextDouble()*0.3)-0.15);
            }
        }
    }

    // #################################################################################################################
    // HELPERS
    // #################################################################################################################

    public interface Phases{
        String UNARMED = "UNARMED";
        String ARMED = "ARMED";
        String TRIGGERED = "TRIGGERED";
        String IGNITED = "IGNITED";
        String TICKING = "TICKING";
        String EXPLOSION = "EXPLOSION";
        String DEAD = "DEAD";
        String DEFUSED = "DEFUSED";
    }

    private double getParticleCount(double radius, int baseParticles) {
        // baseParticles is the number of particles you want at radius 1
        return Math.round(baseParticles * radius * radius);
    }

    private double getExplosionRadius(float spellpower){
        return Math.sqrt(spellpower); //Radius starts out at 4, and slowly increase with the square of the spell power.
    }

    private float getDetectionRadius(float spellpower){
        return 2 + (float)(Math.sqrt(spellpower))/4;
    }

    private double randomVariation(double number, double variation){
        return number - variation + (random.nextDouble() * 2 * variation);
    };

    private float getDamage(){
        //Damage is spellpower squared multiplied by four
        return (float)Math.sqrt(this.entityData.get(SPELLPOWER))*2;
    };

    private static double calcInverseDistanceForce(double distance, double spellPower, double explosionRadius) {
        // If the entity is outside the explosion radius, apply no force
        if (distance > explosionRadius) {
            return 0;
        }

        if(distance < 1){ //Prevent crazy application of force from standing in the same spot as the explosion
            distance = 1;
        }

        // Calculate force using inverse square law (force decreases with square of distance)
        return spellPower / (distance * distance);
    }

    // #################################################################################################################
    // MICS
    // #################################################################################################################

    public void Detonate(){
        if(!level().isClientSide && this.entityData.get(PHASE).equals(Phases.ARMED)){
            this.entityData.set(PHASE,Phases.TRIGGERED);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {

    }

    @Nullable
    public Optional<LivingEntity> getOwner() {
        if(this.entityData.get(OWNER_UUID).isPresent()){
            if(level().getPlayerByUUID(this.entityData.get(OWNER_UUID).get()) != null){
                return Optional.ofNullable(level().getPlayerByUUID(this.entityData.get(OWNER_UUID).get()));
            }else{
                return Optional.empty();
            }
        }else{
            return Optional.empty();
        }
    }
}
