package net.tuboi.druidry.block.bumbleguardhive;

import com.mojang.serialization.MapCodec;
import io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile;
import io.redspace.ironsspellbooks.registries.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.NoteBlockEvent;
import net.tuboi.druidry.Druidry;
import net.tuboi.druidry.registries.DruidryBlockRegistry;
import net.tuboi.druidry.utils.ParticleHelper;
import net.tuboi.druidry.utils.Utils;

import javax.annotation.Nullable;

public class BumbleguardBlock extends BaseEntityBlock {

    public static final MapCodec<BumbleguardBlock> CODEC = simpleCodec((properties) -> new BumbleguardBlock());
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    @Override
    public MapCodec<BumbleguardBlock> codec() { return CODEC; }

    public BumbleguardBlock() {
        super(Properties.ofFullCopy(Blocks.BEE_NEST));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        int count = 3;
        Vec3 position = pPos.getCenter();
        for (int i = 0; i < count; i++) {
            double[] randomVec = Utils.GetVectorSpeeds(0.75d);
            pLevel.addParticle(ParticleHelper.FERTILIZER_EMITTER, position.x+randomVec[0], position.y+randomVec[1], position.z+randomVec[2], randomVec[0]*0.01, randomVec[1]*0.1, randomVec[2]*0.01);
        }
    }

        //##################################################################################################################
    // Blockentity stuff
    //##################################################################################################################

    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState, Player owner, int spellpower) {
        return new BumbleguardBlockEntity(pPos, pState, owner, spellpower);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new BumbleguardBlockEntity(pPos, pState, null, 1);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return createTicker(pLevel, pBlockEntityType, DruidryBlockRegistry.BUMBLEGUARD_HIVE_BLOCKENTITY.get());
    }

    @javax.annotation.Nullable
    protected static <T extends BlockEntity> BlockEntityTicker<T> createTicker(Level pLevel, BlockEntityType<T> pServerType, BlockEntityType<? extends BumbleguardBlockEntity> pClientType) {
        return pLevel.isClientSide ? null : createTickerHelper(pServerType, pClientType, BumbleguardBlockEntity::serverTick);
    }

    //##################################################################################################################
    // Blockstate stuff
    //##################################################################################################################

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }
}
