package net.tuboi.druidry.registries;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tuboi.druidry.Druidry;
import net.tuboi.druidry.spells.MeadowMinefieldSpell;
import net.tuboi.druidry.spells.HiddenBoombloomSpell;
import net.tuboi.druidry.spells.FertilizeSpell;

public class DruidrySpellRegistry {

    //Create a new spell registry
    public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(
            SpellRegistry.SPELL_REGISTRY_KEY,
            Druidry.MODID
    );

    //method for registering new spells
    public static void register(IEventBus eventBus) {
        SPELLS.register(eventBus);
    }

    public static DeferredHolder<AbstractSpell, AbstractSpell> registerSpell(AbstractSpell spell){
        return SPELLS.register(spell.getSpellName(), () -> spell);
    }

    //Register the spells
    public static final DeferredHolder<AbstractSpell, AbstractSpell> FERTILIZE_SPELL = registerSpell(new FertilizeSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> HIDDEN_BOOMBLOOM_SPELL = registerSpell(new HiddenBoombloomSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MEADOW_MINEFIELD_SPELL = registerSpell(new MeadowMinefieldSpell());
}