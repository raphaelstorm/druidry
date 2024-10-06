package net.tuboi.druidry.block.bumbleguardhive;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.Entity.RemovalReason;
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
    private UUID ownerUUID;
    private int spellpower = 10;

    public BumbleguardBlockEntity(BlockEntityType<? extends BumbleguardBlockEntity> pEntityType, BlockPos pPos, BlockState pState) {
        super(pEntityType, pPos, pState);
    }

    public BumbleguardBlockEntity(BlockPos pPos, BlockState pState, Player caster, Integer spellpower) {
        super(DruidryBlockRegistry.BUMBLEGUARD_HIVE_BLOCKENTITY.get(), pPos, pState);
        this.ownerUUID = caster != null ? caster.getUUID() : null;
        this.spellpower = spellpower;
        this.generateBeeTags();
    }

    public BumbleguardBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(DruidryBlockRegistry.BUMBLEGUARD_HIVE_BLOCKENTITY.get(), blockPos, blockState);
        this.generateBeeTags();
        this.ownerUUID = null;
        this.spellpower = 1;
    }


    // #################################################################################################################
    // NBT
    // #################################################################################################################

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(tag, pRegistries);

        // Save owner
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }

        // Save spellpower
        tag.putInt("Spellpower", this.spellpower);

        // Save stored bees
        ListTag beesList = new ListTag();
        for (StoredBee bee : this.storedBees) {
            CompoundTag beeTag = new CompoundTag();
            beeTag.putString("ID", bee.getId());
            beeTag.putInt("TimeUntilRespawn", bee.getTimeUntilRespawn());
            beeTag.putString("Status", bee.getStatus());
            beesList.add(beeTag);
        }
        tag.put("StoredBees", beesList);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(tag, pRegistries);

        // Load owner
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }

        // Load spellpower
        this.spellpower = tag.getInt("Spellpower");

        // Load stored bees
        this.storedBees.clear();
        ListTag beesList = tag.getList("StoredBees", 10); // 10 is the ID for CompoundTag
        for (int i = 0; i < beesList.size(); i++) {
            CompoundTag beeTag = beesList.getCompound(i);
            StoredBee bee = new StoredBee(
                    beeTag.getString("ID"),
                    beeTag.getInt("TimeUntilRespawn"),
                    beeTag.getString("Status")
            );
            this.storedBees.add(bee);
        }
    }

    // #################################################################################################################
    // TICKING
    // #################################################################################################################
    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, BumbleguardBlockEntity pBeehive) {

        //Actions to perform once every second
        if(pBeehive.secondCountDown<=0){

            //Check for dead bees and set the respawn timer
            pBeehive.checkForKilledBeesAndUpdateStore();

            //Distribute targets to bees
            pBeehive.distributeTargetsToBumbleguard();

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
                this.ownerUUID,
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
        Double r = 60d;
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
                bee.setTimeUntilRespawn((int)(900 + Math.ceil(Math.random() * 600d))); //1 minute +-15sec respawn timer
            }
        });
    }

    private void distributeTargetsToBumbleguard() {

        gatherTargetsFromSurroundingArea();

        /*todo: implement smart logic for distributing targets to bees, making sure that the target with the least
        amount of assigned bees gets a new bee assigned to it, and prioritizing targets that are close to the hive
        or close to the bee */

        // Fetch all active bees
        List<Bumbleguard> activeBees = getLevel().getEntitiesOfClass(Bumbleguard.class, AABB.ofSize(this.getBlockPos().getCenter(), 60, 60, 60));

        // Check if there are any active bees without a target
        List<Bumbleguard> beesWithoutTarget = activeBees.stream().filter(Bumbleguard -> !Bumbleguard.hasTarget()).collect(Collectors.toList());

        // If there are bees without a target, assign them one from the list of targets
        if (!beesWithoutTarget.isEmpty()) {
            Iterator<LivingEntity> targetIterator = this.targets.iterator();
            while (targetIterator.hasNext() && !beesWithoutTarget.isEmpty()) {
                LivingEntity target = targetIterator.next();
                if (target != null) {
                    Bumbleguard bumbleguard = beesWithoutTarget.getFirst();
                    bumbleguard.setTarget(target);
                    beesWithoutTarget.remove(bumbleguard);
                }
            }
        }
    }

    // #################################################################################################################
    // API
    // #################################################################################################################

    @Deprecated //Don't use this, make the hive give the bee a target instead
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
        return this.ownerUUID != null;
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

        //Don't attack creatures who are not hostile or players
        if(!(entity instanceof Player) && !(entity instanceof Mob && ((Mob) entity).getType().getCategory().equals(MobCategory.MONSTER))){
            return false;
        }

        //Don't attack creative or spectating players
        if(entity instanceof Player && (((Player) entity).isCreative()) || entity.isSpectator()){
            return false;
        }

        //Don't attack whitelisted creatures
        if(pBumbleguardBlockEntity.hasOwner() && Utils.BumbleguardTagUtil.playerHasTaggedEntity(entity, pBumbleguardBlockEntity.ownerUUID.toString())){
            return false;
        }

        //Don't attack owner
        if(pBumbleguardBlockEntity.hasOwner() && entity instanceof Player && (entity.getUUID().equals(pBumbleguardBlockEntity.ownerUUID))){
            return false;
        }

        //Don't attack pet's of owner
        if(pBumbleguardBlockEntity.hasOwner() && entity instanceof TamableAnimal && ((TamableAnimal) entity).isTame() && ((TamableAnimal) entity).getOwnerUUID() != null && ((TamableAnimal) entity).getOwnerUUID().equals(pBumbleguardBlockEntity.ownerUUID)){
            return false;
        }

        //Don't attack pets of whitelisted players
        if(pBumbleguardBlockEntity.hasOwner() && entity instanceof TamableAnimal && ((TamableAnimal) entity).isTame()){
            LivingEntity petsOwner =((TamableAnimal) entity).getOwner();
            if (petsOwner instanceof Player) {
                return !Utils.BumbleguardTagUtil.playerHasTaggedEntity(petsOwner, pBumbleguardBlockEntity.ownerUUID.toString());
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