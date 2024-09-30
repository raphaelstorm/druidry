package net.tuboi.druidry.setup;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.tuboi.druidry.Druidry;
import net.tuboi.druidry.entity.bumbleguard.Bumbleguard;
import net.tuboi.druidry.registries.DruidryEntityRegistry;

@EventBusSubscriber(modid = Druidry.MODID, bus = EventBusSubscriber.Bus.MOD)
public class CommonSetup {

    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(DruidryEntityRegistry.BUMBLEGUARD.get(), Bumbleguard.createAttributes().build());
    }
}
