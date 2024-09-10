package net.tuboi.druidry.spells;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.spells.EntityCastData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.tuboi.druidry.Druidry;
import net.tuboi.druidry.entity.FertilizeProjectile;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class FertilizeSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(Druidry.MODID, "fertilize");

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.cooldown", GetCoolDown(getSpellPower(spellLevel, caster)), 1)
        );
    }

    //Set the basic properties of the spell
    public FertilizeSpell(){
        this.manaCostPerLevel = 1;
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 1;
        this.castTime = 100; //Cast time in ticks, 5 seconds
        this.baseManaCost = 5;
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(GetCoolDown(1))
            .build();


    //Get the cooldown of the spell based on it's level and the casters power
    private Float GetCoolDown(float spellpower){
        Float cooldown = 6000f;

        //Retract 1 minute from cooldown per spell level above first
        cooldown -= 1200*(spellpower-1);

        //If cooldown is below 20, set to 20 (1 second)
        if (cooldown < 20){
            cooldown = 20f;
        }

        return cooldown;
    }

    @Override
    public ResourceLocation getSpellResource(){
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig(){
        return defaultConfig;
    }

    @Override
    public CastType getCastType(){
        return CastType.CONTINUOUS;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.FIRE_BREATH_LOOP.get());
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData){
        if ( //If projectile already exists
                playerMagicData.isCasting()
                && playerMagicData.getCastingSpellId().equals(this.getSpellId())
                && playerMagicData.getAdditionalCastData() instanceof EntityCastData entityCastData
                && entityCastData.getCastingEntity() instanceof AbstractConeProjectile cone
        ) { //Tick damage
            cone.setDealDamageActive();
        } else { //Create a new projectile
            FertilizeProjectile fertilizeProjectile = new FertilizeProjectile(world, entity);
            fertilizeProjectile.setPos(entity.position().add(0, entity.getEyeHeight() * .7, 0));
            world.addFreshEntity(fertilizeProjectile);

            playerMagicData.setAdditionalCastData(new EntityCastData(fertilizeProjectile));
        }
        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

}
