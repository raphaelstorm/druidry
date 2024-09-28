package net.tuboi.druidry.registries;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.registries.BlockRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;

public class DruidryItemRegistry {

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, IronsSpellbooks.MODID);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }


    public static final DeferredHolder<Item, Item> BUMBLEGUARD_BLOCK_ITEM = ITEMS.register("bumbleguard_hive", () -> new BlockItem(DruidryBlockRegistry.BUMBLEGUARD_HIVE_BLOCK.get(), new Item.Properties()));


    public static Collection<DeferredHolder<Item, ? extends Item>> getDruidryItems() {
        return ITEMS.getEntries();
    }

}

