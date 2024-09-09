package net.tuboi.druidry.registries;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tuboi.druidry.Druidry;
import net.tuboi.druidry.entity.FertilizeProjectile;
import net.minecraft.resources.ResourceLocation;

public class DruidryEntityRegistry  {

    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(
            Registries.ENTITY_TYPE,
            Druidry.MODID);

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }

    public static final DeferredHolder<EntityType<?>, EntityType<FertilizeProjectile>> FERTILIZE_PROJECTILE =
            ENTITIES.register("fertilizer_projectile", () -> EntityType.Builder.<FertilizeProjectile>of(FertilizeProjectile::new, MobCategory.MISC)
                    .sized(1f, 1f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(Druidry.MODID, "fertilizer_projectile").toString()));
}
