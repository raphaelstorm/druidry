package net.tuboi.druidry.block.bumbleguardhive;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tuboi.druidry.entity.bumbleguard.Bumbleguard;
import net.tuboi.druidry.registries.DruidryBlockRegistry;
import net.tuboi.druidry.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;

public class BumbleguardBlockEntity extends BlockEntity {

    private static final Double DETECTION_DISTANCE = 30d; //How far enemies should be before bees are released from the hive
    private static final Double CHASE_DISTANCE = 60d; //Maximum distance from hive bees should follow enemy before giving up

    private Integer timeUntilNextBeeAllowedToLeave = 0;
    private Integer secondCountDown = 0;
    private static final Integer maxTimeUntilNextBeeAllowedToLeave = 20;
    private List<LivingEntity> targets = new ArrayList<>();
    private List<StoredBee> storedBees = new ArrayList<>();
    private Player owner;
    private int spellpower = 10;

    public BumbleguardBlockEntity(BlockEntityType<? extends BumbleguardBlockEntity> pEntityType, BlockPos pPos, BlockState pState) {
        super(pEntityType, pPos, pState);
    }

    public BumbleguardBlockEntity(BlockPos pPos, BlockState pState, Player owner, Integer spellpower) {
        super(DruidryBlockRegistry.BUMBLEGUARD_HIVE_BLOCKENTITY.get(), pPos, pState);
        this.owner = owner;
        this.spellpower = spellpower;
        this.generateBeeTags();
    }

    public BumbleguardBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(DruidryBlockRegistry.BUMBLEGUARD_HIVE_BLOCKENTITY.get(), blockPos, blockState);
        this.generateBeeTags();
        this.owner = null;
        this.spellpower = 1;
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, BumbleguardBlockEntity pBeehive) {

        //Actions to perform once every second
        if(pBeehive.secondCountDown<=0){

            //Gather new targets from the area
            pBeehive.gatherTargetsFromSurroundingArea();

            //Check for dead bees and set the respawn timer
            pBeehive.checkForKilledBeesAndUpdateStore();

            pBeehive.secondCountDown=20;
        }else{
            pBeehive.secondCountDown--;
        }

        //Advance all respawn timers
        pBeehive.advanceBeeRespawnTicks();

        //Advance spawn delay ticker
        pBeehive.decrementNextBeeSpawnCounter();

        //Check if there are enemies in the area
        List<LivingEntity> enemies = new ArrayList<>(pBeehive.getTargets());
        if(!enemies.isEmpty()) { //If there are enemies, deploy avaiable bees bees
            pBeehive.storedBees.forEach(StoredBee -> {
                if(StoredBee.status == "READY"){
                    if(pBeehive.spawnNewBumbleguard(StoredBee.getId())){
                        StoredBee.setStatus("ACTIVE"); //set as active is spawning was successful
                    };
                }
            });
        }
    }

    // #################################################################################################################
    // ACTIONS
    // #################################################################################################################

    private boolean spawnNewBumbleguard(String id){

        if(!beeCanSpawn()){
            return false;
        }

        resetSpawnDelay();

        Direction direction = getLevel().getBlockState(this.getBlockPos()).getValue(BumbleguardBlock.FACING);

        BlockPos blockpos = this.getBlockPos().offset(direction.getNormal());

        Bumbleguard newBumbleGuard = new Bumbleguard(
                this.getLevel(),
                this.owner,
                id,
                this.getBlockPos(),
                blockpos.getCenter().x,
                blockpos.getCenter().y,
                blockpos.getCenter().z
        );
        Vec3i dirVec = direction.getNormal(); //Get random direction out of the hive
        getLevel().addFreshEntity(newBumbleGuard);
        newBumbleGuard.setDeltaMovement(Vec3.atLowerCornerOf(dirVec)); //Give the bee a push out of the hive
        newBumbleGuard.getLookControl().setLookAt(Vec3.atCenterOf(dirVec)); //Make the bee look the direction of the launch
        getLevel().playSound(null, blockpos, SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    private void advanceBeeRespawnTicks(){
        storedBees.forEach(StoredBee::advanceRespawn);
    }

    private void checkForKilledBeesAndUpdateStore() {

        // Get all bumbleguards within the area
        Double r = 50d;
        AABB targetArea = AABB.ofSize(this.getBlockPos().getCenter(), r, r, r);
        List<Bumbleguard> bumblesInArea = getLevel().getEntitiesOfClass(Bumbleguard.class, targetArea);

        // Create a set of Bumbleguard IDs for fast lookup
        Set<String> bumbleguardIds = bumblesInArea.stream()
                .map(Bumbleguard::getBumbleId)
                .collect(Collectors.toSet());

        // Verify which bees are active and present in the area
        List<StoredBee> verifiedBees = storedBees.stream()
                .filter(bee -> "ACTIVE".equals(bee.getStatus()) && bumbleguardIds.contains(bee.getId()))
                .collect(Collectors.toList());

        // Set status of bees not found to "DEAD"
        storedBees.forEach(bee -> {
            if ("ACTIVE".equals(bee.getStatus()) && !verifiedBees.contains(bee)) {
                bee.setStatus("DEAD");
                bee.setTimeUntilRespawn(200); //10 seconds respawn timer
            }
        });
    }

    // #################################################################################################################
    // API
    // #################################################################################################################

    public List<LivingEntity> getTargets() {
        return this.targets;
    }

    public boolean Enter(Bumbleguard pBumbleguard) {
        boolean found = false;
        for (StoredBee storedBee : storedBees) {
            if (storedBee.getId().equals(pBumbleguard.getBumbleId())) { // Use equals() for string comparison
                storedBee.setStatus("READY");
                storedBee.setTimeUntilRespawn(0); //just in case
                pBumbleguard.remove(RemovalReason.DISCARDED);
                found = true;

                BlockPos blockpos = this.getBlockPos();
                this.level
                        .playSound(
                                null,
                                (double)blockpos.getX(),
                                (double)blockpos.getY(),
                                (double)blockpos.getZ(),
                                SoundEvents.BEEHIVE_ENTER,
                                SoundSource.BLOCKS,
                                1.0F,
                                1.0F
                        );
                break; // Exit loop once the bumbleguard is found
            }
        }

        return found;
    }

    public boolean IsMemberOf(String id){
        for (StoredBee bee : storedBees) {
            if (bee.getId().equals(id)) {
                return true;
            }
        };
        return false;
    };

    public Double getChaseDistance() {
        return CHASE_DISTANCE;
    }


    public boolean hasOwner() {
        return this.owner != null;
    }

    // #################################################################################################################
    // HELPERS
    // #################################################################################################################


    private void gatherTargetsFromSurroundingArea(){
        this.targets.clear();

        //Get enemies around hive
        Double r = DETECTION_DISTANCE;
        AABB targetArea = AABB.ofSize(this.getBlockPos().getCenter(),r,r,r);

        getLevel().getEntitiesOfClass(LivingEntity.class, targetArea).forEach(LivingEntity -> {
            if(isValidTarget(this, LivingEntity)){
                this.targets.add(LivingEntity);
            }
        });
    }

    public static boolean isValidTarget(BumbleguardBlockEntity pBumbleguardBlockEntity, LivingEntity entity){

        //Don't attack creative or spectating players
        if(entity instanceof Player && (((Player) entity).isCreative()) || entity.isSpectator()){
            return false;
        }

        //Don't attack bees or other bumbleguard
        if(entity instanceof Bee || entity instanceof Bumbleguard){
            return false;
        }

        //Don't attack whitelisted creatures
        if(pBumbleguardBlockEntity.hasOwner() && Utils.BumbleguardTagUtil.playerHasTaggedEntity(entity, pBumbleguardBlockEntity.owner.getUUID().toString())){
            return false;
        }

        //Don't attack passive mobs
        if(entity instanceof Animal && !(entity instanceof NeutralMob)){
            return false;
        }

        //Don't attack owner
        if(pBumbleguardBlockEntity.hasOwner() && pBumbleguardBlockEntity.owner != null && entity instanceof Player && (entity.getUUID().equals(pBumbleguardBlockEntity.owner.getUUID()))){
            return false;
        }

        //Don't attack pet's of owner
        if(pBumbleguardBlockEntity.hasOwner() && pBumbleguardBlockEntity.owner != null && entity instanceof TamableAnimal && ((TamableAnimal) entity).isTame() && ((TamableAnimal) entity).isOwnedBy(pBumbleguardBlockEntity.owner)){
            return false;
        }

        //Don't attack pets of whitelisted players
        if(pBumbleguardBlockEntity.hasOwner() && pBumbleguardBlockEntity.owner != null && entity instanceof TamableAnimal && ((TamableAnimal) entity).isTame()){
            LivingEntity petsOwner =((TamableAnimal) entity).getOwner();
            if (petsOwner instanceof Player) {
                return !Utils.BumbleguardTagUtil.playerHasTaggedEntity(petsOwner, pBumbleguardBlockEntity.owner.getUUID().toString());
            }
        }

        return true;
    }

    private void generateBeeTags(){
        this.storedBees.clear();

        //Generate X numbers of random strings the bees may be identified as
        for(int i = 0; i<calcNumberOfBeesFromSpellpower(this.spellpower); i++){
            this.storedBees.add(new StoredBee(
                    Utils.generateRandomString(8),
                    0,
                    "READY"
            ));
        }
    }

    private Integer calcNumberOfBeesFromSpellpower(Integer spellpower){
        return spellpower;
    };

    public class StoredBee {

        private String id;
        private int timeUntilRespawn;
        private String status; //READY, ACTIVE, DEAD

        // Constructor
        public StoredBee(String id, int timeUntilRespawn, String status) {
            this.id = id;
            this.timeUntilRespawn = timeUntilRespawn;
            this.status = status;
        }

        public String getId() {
            return id;
        }

        public int getTimeUntilRespawn() {
            return timeUntilRespawn;
        }

        public void setTimeUntilRespawn(int timeUntilRespawn) {
            this.timeUntilRespawn = timeUntilRespawn;
        }

        public boolean isAlive(){
            return timeUntilRespawn == 0;
        }

        public void advanceRespawn(){
            if(this.getStatus().equals("DEAD")){
                if(this.timeUntilRespawn==0){
                    this.status="READY";
                }else if(timeUntilRespawn > 0){
                        timeUntilRespawn--;
                }else{ //Something has gone wrong if this is used
                    timeUntilRespawn=0;
                }
            }
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    private void decrementNextBeeSpawnCounter(){
        if(this.timeUntilNextBeeAllowedToLeave > 0){
            this.timeUntilNextBeeAllowedToLeave--;
        }
    }

    private boolean beeCanSpawn(){
        return this.timeUntilNextBeeAllowedToLeave == 0;
    }

    private void resetSpawnDelay(){
        this.timeUntilNextBeeAllowedToLeave = (int)Math.ceil(io.redspace.ironsspellbooks.api.util.Utils.random.nextDouble()*maxTimeUntilNextBeeAllowedToLeave);
    }
}