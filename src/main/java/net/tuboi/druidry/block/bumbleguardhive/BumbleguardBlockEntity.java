package net.tuboi.druidry.block.bumbleguardhive;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.tuboi.druidry.entity.BoombloomEntity;
import net.tuboi.druidry.entity.bumbleguard.Bumbleguard;
import net.tuboi.druidry.registries.DruidryBlockRegistry;

import java.util.ArrayList;
import java.util.List;

public class BumbleguardBlockEntity extends BlockEntity {

    private static final int SPELLPOWER = 20;
    private static final int MAX_OCCUPANTS = 0;
    private static final Double DETECTION_DISTANCE = 15d; //How far enemies should be before bees are released from the hive
    private static final Double CHASE_DISTANCE = 30d; //Maximum distance from hive bees should follow enemy before giving up

    private List<String> beeTags = new ArrayList<>();
    private List<LivingEntity> currentTargets = new ArrayList<LivingEntity>();
    private List<Entity> individualWhitelist = new ArrayList<>();
    private List<Class> classWhitelist = new ArrayList<>();
    private Player owner;


    public BumbleguardBlockEntity(BlockEntityType<? extends BumbleguardBlockEntity> pEntityType, BlockPos pPos, BlockState pState) {
        super(pEntityType, pPos, pState);
    }

    public BumbleguardBlockEntity(BlockPos pPos, BlockState pState) {
        super(DruidryBlockRegistry.BUMBLEGUARD_HIVE_BLOCKENTITY.get(), pPos, pState);
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, BumbleguardBlockEntity pBeehive) {

    }

    // #################################################################################################################
    // ACTIONS
    // #################################################################################################################

    private void spawnNewBumbleguard(){

    }

    // #################################################################################################################
    // API
    // #################################################################################################################

    public List<LivingEntity> getTargets() {
        return currentTargets;
    }

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


}