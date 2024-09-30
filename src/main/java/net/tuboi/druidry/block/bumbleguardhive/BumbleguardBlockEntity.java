package net.tuboi.druidry.block.bumbleguardhive;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.tuboi.druidry.entity.BoombloomEntity;
import net.tuboi.druidry.registries.DruidryBlockRegistry;

import java.util.ArrayList;
import java.util.List;

public class BumbleguardBlockEntity extends BlockEntity {

    private static final int SPELLPOWER = 0;
    private static final int MAX_OCCUPANTS = 0;
    private static final Double DETECTION_DISTANCE = 15d; //How far enemies should be before bees are released from the hive
    private static final Double CHASE_DISTANCE = 30d; //Maximum distance from hive bees should follow enemy before giving up

    private List<LivingEntity> currentTargets = new ArrayList<LivingEntity>();
    private List<LivingEntity> nonAttackList = new ArrayList<LivingEntity>();

    public BumbleguardBlockEntity(BlockEntityType<? extends BumbleguardBlockEntity> pEntityType, BlockPos pPos, BlockState pState) {
        super(pEntityType, pPos, pState);
    }

    public BumbleguardBlockEntity(BlockPos pPos, BlockState pState) {
        super(DruidryBlockRegistry.BUMBLEGUARD_HIVE_BLOCKENTITY.get(), pPos, pState);
    }

}
