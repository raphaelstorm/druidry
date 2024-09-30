package net.tuboi.druidry.registries;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tuboi.druidry.Druidry;
import net.tuboi.druidry.entity.BoombloomEntity;
import net.tuboi.druidry.entity.bumbleguard.Bumbleguard;
import net.tuboi.druidry.entity.FertilizeProjectile;

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

    public static final DeferredHolder<EntityType<?>, EntityType<BoombloomEntity>> BOOMBLOOM_ENTITY =
            ENTITIES.register("boombloom", () -> EntityType.Builder.<BoombloomEntity>of(BoombloomEntity::new, MobCategory.MISC)
                    .sized(1f,1f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(Druidry.MODID, "boombloom_entity").toString()));


    public static final DeferredHolder<EntityType<?>, EntityType<Bumbleguard>> BUMBLEGUARD =
            ENTITIES.register("bumbleguard", () -> EntityType.Builder.<Bumbleguard>of(Bumbleguard::new, MobCategory.CREATURE)
                    .sized(0.5f,0.5f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(Druidry.MODID, "bumbleguard").toString()));

}
