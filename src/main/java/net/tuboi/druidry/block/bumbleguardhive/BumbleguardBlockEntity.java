package net.tuboi.druidry.block.bumbleguardhive;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.tuboi.druidry.entity.BoombloomEntity;
import net.tuboi.druidry.entity.bumbleguard.Bumbleguard;
import net.tuboi.druidry.registries.DruidryBlockRegistry;
import net.tuboi.druidry.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BumbleguardBlockEntity extends BlockEntity {

    private static final int SPELLPOWER = 20;
    private static final int MAX_OCCUPANTS = 0;
    private static final Double DETECTION_DISTANCE = 30d; //How far enemies should be before bees are released from the hive
    private static final Double CHASE_DISTANCE = 30d; //Maximum distance from hive bees should follow enemy before giving up

    private String hiveId;

    private List<StoredBee> bees = new ArrayList<>(); //List of all bee tags
    private List<Entity> individualWhitelist = new ArrayList<>();
    private Player owner;

    public BumbleguardBlockEntity(BlockEntityType<? extends BumbleguardBlockEntity> pEntityType, BlockPos pPos, BlockState pState) {
        super(pEntityType, pPos, pState);
    }

    public BumbleguardBlockEntity(BlockPos pPos, BlockState pState) {
        super(DruidryBlockRegistry.BUMBLEGUARD_HIVE_BLOCKENTITY.get(), pPos, pState);
        this.generateBeeTags();
        this.generateHiveTag();
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, BumbleguardBlockEntity pBeehive) {

        //Check for dead bees and set the respawn timer
        pBeehive.checkForKilledBeesAndUpdateStore();

        //Advance all respawn timers
        pBeehive.advanceBeeRespawnTicks();

        //Check if there are enemies in the area
        List<LivingEntity> enemies = new ArrayList<>(pBeehive.getTargets());
        if(!enemies.isEmpty()) { //If there are enemies, deploy avaiable bees bees
            pBeehive.bees.forEach(StoredBee -> {
                if(StoredBee.status == "READY"){
                    StoredBee.setStatus("ACTIVE");
                    pBeehive.spawnNewBumbleguard(StoredBee.getId());
                }
            });
        }
    }

    // #################################################################################################################
    // ACTIONS
    // #################################################################################################################

    private void spawnNewBumbleguard(String id){

        Direction direction = getLevel().getBlockState(this.getBlockPos()).getValue(BumbleguardBlock.FACING);
        BlockPos blockpos = this.getBlockPos().relative(direction);

        Bumbleguard newBumbleGuard = new Bumbleguard(
                this.getLevel(),
                this.owner,
                id,
                this.getBlockPos(),
                blockpos.getX(),
                blockpos.getY(),
                blockpos.getZ()
        );
        level.addFreshEntity(newBumbleGuard);
    }

    private void advanceBeeRespawnTicks(){
        bees.forEach(StoredBee -> {
            StoredBee.advanceRespawn();
        });

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
        List<StoredBee> verifiedBees = bees.stream()
                .filter(bee -> "ACTIVE".equals(bee.getStatus()) && bumbleguardIds.contains(bee.getId()))
                .collect(Collectors.toList());

        // Set status of bees not found to "DEAD"
        bees.forEach(bee -> {
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

        List<LivingEntity> enemies = new ArrayList<>();

        //Get enemies around hive
        Double r = this.DETECTION_DISTANCE;
        AABB targetArea = AABB.ofSize(this.getBlockPos().getCenter(),r,r,r);

        getLevel().getEntitiesOfClass(LivingEntity.class, targetArea).forEach(LivingEntity -> {
            if(this.isValidTarget(LivingEntity)){
                enemies.add(LivingEntity);
            }
        });

        return enemies;
    }

    public boolean Enter(Bumbleguard pBumbleguard) {
        boolean found = false;
        for (StoredBee storedBee : bees) {
            if (storedBee.getId().equals(pBumbleguard.getBumbleId())) { // Use equals() for string comparison
                storedBee.setStatus("READY");
                storedBee.setTimeUntilRespawn(0); //just in case
                pBumbleguard.kill();
                found = true;
                break; // Exit loop once the bumbleguard is found
            }
        }
        return found;
    }

    public boolean IsMemberOf(String id){
        for (StoredBee bee : bees) {
            if (bee.getId().equals(id)) {
                return true;
            }
        };
        return false;
    };

    // #################################################################################################################
    // HELPERS
    // #################################################################################################################

    private boolean isValidTarget(Entity entity){

        //Don't attack creative or spectating players
        if(entity instanceof Player && (((Player) entity).isCreative()) || entity.isSpectator()){
            return false;
        }

        //Don't attack bees or other bumbleguard
        if(entity instanceof Bee || entity instanceof Bumbleguard){
            return false;
        }

        //Don't attack whitelisted creatures
        if(individualWhitelist.contains(entity)){
            return false;
        }

        //Don't attack passive mobs
        if(entity instanceof Animal && !(entity instanceof NeutralMob)){
            return false;
        }

        //Don't attack owner
        if(this.owner != null && entity instanceof Player && (entity.getUUID().equals(this.owner.getUUID()))){
            return false;
        }

        //Don't attack pet's of owner
        if(this.owner != null && entity instanceof TamableAnimal && ((TamableAnimal) entity).isTame() && ((TamableAnimal) entity).isOwnedBy(this.owner)){
            return false;
        }

        return true;
    }

    private void generateBeeTags(){
        this.bees.clear();

        //Generate X numbers of random strings the bees may be identified as
        for(int i=0;i<calcNumberOfBeesFromSpellpower(this.SPELLPOWER);i++){
            this.bees.add(new StoredBee(
                    Utils.generateRandomString(8),
                    0,
                    "READY"
            ));
        }
    }

    private void generateHiveTag(){
        this.hiveId = Utils.generateRandomString(8);
    }

    private Integer calcNumberOfBeesFromSpellpower(Integer spellpower){
        return (int)Math.floor(1 + (double) spellpower /2);
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
}