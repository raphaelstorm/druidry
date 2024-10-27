package net.tuboi.druidry.spells;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tuboi.druidry.Druidry;
import net.tuboi.druidry.entity.menhir.MenhirEntity;
import net.tuboi.druidry.utils.Utils;

import java.util.List;

@AutoSpellConfig
public class ErectMenhirSpell extends AbstractSpell {

    @Override
    public ResourceLocation getSpellResource() {
        return ResourceLocation.fromNamespaceAndPath(Druidry.MODID, "erect_menhir");
    }

    public ErectMenhirSpell(){
        this.manaCostPerLevel = 20;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 40;
    }

    private final DefaultConfig defaultConfig = new DefaultConfig().setMinRarity(SpellRarity.UNCOMMON).setSchoolResource(SchoolRegistry.NATURE_RESOURCE).setMaxLevel(5).setCooldownSeconds(10d).build();


    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);

        var lookPos = Utils.getTargetBlock(level, entity, ClipContext.Fluid.NONE, 30);

        if (lookPos != null) {
            var menhir = new MenhirEntity(
                    level,
                    entity,
                    getSpellPower(spellLevel, entity),
                    getMenhirAngleVariant(entity.getEyePosition(), lookPos.getLocation())
            );

            menhir.setPos(lookPos.getLocation().x, lookPos.getLocation().y, lookPos.getLocation().z);
            level.addFreshEntity(menhir);
        }
    }

    // Based on the distance between the player and the menhir, return the angle variant
    private static int getMenhirAngleVariant(Vec3 eyePos, Vec3 targetPos){
        var distance = eyePos.distanceTo(targetPos);

        if (distance <= 5) {
            return 1;
        } else if (distance <= 12) {
            return 2;
        } else if (distance > 12) {
            return 3;
        } else {
            return 0;
        }
    }
}
