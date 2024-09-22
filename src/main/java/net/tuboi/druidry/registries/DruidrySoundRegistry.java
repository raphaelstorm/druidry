package net.tuboi.druidry.registries;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tuboi.druidry.Druidry;

public class DruidrySoundRegistry {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, Druidry.MODID);

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

    public static DeferredHolder<SoundEvent, SoundEvent> WINDY_LEAVES = registerSoundEvent("spell.windy_leaves.loop");
    public static DeferredHolder<SoundEvent, SoundEvent> BOOMBLOOM_TRIGGER = registerSoundEvent("entity.boombloom.trigger");
    public static DeferredHolder<SoundEvent, SoundEvent> BOOMBLOOM_ARMED = registerSoundEvent("entity.boombloom.armed");
    public static DeferredHolder<SoundEvent, SoundEvent> NATURE_CAST_START = registerSoundEvent("spell.boombloom.start");
    public static DeferredHolder<SoundEvent, SoundEvent> NATURE_CAST_END = registerSoundEvent("spell.boombloom.end");



    private static DeferredHolder<SoundEvent, SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Druidry.MODID, name)));
    }

}