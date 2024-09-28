package net.tuboi.druidry.registries;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.block.portal_frame.PortalFrameBlock;
import io.redspace.ironsspellbooks.block.scroll_forge.ScrollForgeTile;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tuboi.druidry.Druidry;
import net.tuboi.druidry.block.bumbleguardhive.BumbleguardBlock;
import net.tuboi.druidry.block.bumbleguardhive.BumbleguardBlockEntity;

import java.util.Collection;

public class DruidryBlockRegistry {


    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Druidry.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Druidry.MODID);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
    }

    public static final DeferredHolder<Block, Block> BUMBLEGUARD_HIVE_BLOCK = BLOCKS.register("bumbleguard_hive", BumbleguardBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BumbleguardBlockEntity>> BUMBLEGUARD_HIVE_BLOCKENTITY = BLOCK_ENTITIES.register("bumbleguard_hive", () -> BlockEntityType.Builder.of(BumbleguardBlockEntity::new, BUMBLEGUARD_HIVE_BLOCK.get()).build(null));


    public static Collection<DeferredHolder<Block, ? extends Block>> blocks() {
        return BLOCKS.getEntries();
    }

}
