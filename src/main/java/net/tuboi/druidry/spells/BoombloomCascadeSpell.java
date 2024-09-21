package net.tuboi.druidry.spells;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.tuboi.druidry.Druidry;
import net.tuboi.druidry.registries.DruidrySoundRegistry;
import net.tuboi.druidry.utils.Utils;

import java.util.List;
import java.util.Optional;

public class BoombloomCascadeSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(Druidry.MODID, "boombloom_cascade");

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.radius", Utils.SetMaxDecimals((Math.sqrt(getSpellPower(spellLevel,caster))+2),1)), //Range is equal to power for this spell
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.SetMaxDecimals((Math.sqrt(getSpellPower(spellLevel,caster))*2),1))
        );
    }

    public BoombloomCascadeSpell() {
        this.manaCostPerLevel = 100;
        this.baseManaCost = 100;
        this.castTime = 100;
        this.spellPowerPerLevel = 1;
        this.baseSpellPower = 1;
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(120d)
            .build();


    @Override
    public ResourceLocation getSpellResource(){
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig(){
        return defaultConfig;
    }

    public CastType getCastType(){
        return CastType.LONG;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        //todo: make proper sound
        return Optional.of(DruidrySoundRegistry.WINDY_LEAVES.get());
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(DruidrySoundRegistry.WINDY_LEAVES.get());
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {

    }
}
