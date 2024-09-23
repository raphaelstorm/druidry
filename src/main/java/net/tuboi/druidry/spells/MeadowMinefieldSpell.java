package net.tuboi.druidry.spells;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tuboi.druidry.Druidry;
import net.tuboi.druidry.entity.BoombloomEntity;
import net.tuboi.druidry.registries.DruidrySoundRegistry;
import net.tuboi.druidry.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class MeadowMinefieldSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(Druidry.MODID, "meadow_minefield");

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.tubois_druidry.boombloom_explosion_radius", Utils.SetMaxDecimals(Math.sqrt(getSpellPower(spellLevel,caster)-8),1)), //Range is equal to power for this spell
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.SetMaxDecimals((Math.sqrt(getSpellPower(1,caster)-8)*2),1)),
                Component.translatable("ui.tubois_druidry.boombloom_radius", Utils.SetMaxDecimals(getRadius(getSpellPower(1,caster)),1)),
                Component.translatable("ui.tubois_druidry.boombloom_detection_radius", Utils.SetMaxDecimals(Math.sqrt(getSpellPower(1,caster)-8)/4,1))
        );
    }

    public MeadowMinefieldSpell() {
        this.manaCostPerLevel = 100;
        this.baseManaCost = 100;
        this.castTime = 80;
        this.spellPowerPerLevel = 8;
        this.baseSpellPower = 16;
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(180d)
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
        return Optional.of(DruidrySoundRegistry.NATURE_CAST_END.get());
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(DruidrySoundRegistry.NATURE_CAST_START.get());
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        int scanHeight = 10;
        Double maxDistBetweenBoomblooms = 2.5d;

        //Get radius of spell
        int radius = (int)Math.ceil(getRadius(getSpellPower(spellLevel, entity)));

        //Array to store valid positions to summon boomblooms
        List<BlockPos> validPositions = new ArrayList<>();

        //Get a circle of blocks 10 blocks above the player
        List<BlockPos> blockCircle = Utils.GetBlocksInRadius(entity.blockPosition().above(10),radius);


        //Iterate over each block in the circle
        blockCircle.forEach(blockPos -> {
            for(int k=0;k<scanHeight*2;k++){        //Check all blocks 20 blocks downwards
                BlockPos zPos = blockPos.below(k);
                if( //Check if the block is a flower, leaf, air or crop and has a dirt type block below it
                        (level.getBlockState(zPos).isAir()
                                || level.getBlockState(zPos).is(BlockTags.FLOWERS)
                                || level.getBlockState(zPos).is(BlockTags.LEAVES))
                                && level.getBlockState(zPos.below(1)).is(BlockTags.DIRT)
                ){
                    validPositions.add(zPos); //Save position if it's valid for a flower placement
                    continue;
                };
            }
        });

        //Remove all valid positions that are within 2 blocks from another valid position
        for (int i = 0; i < validPositions.size(); i++) {
            BlockPos blockPos1 = validPositions.get(i);
            for (int j = i + 1; j < validPositions.size(); j++) {
                BlockPos blockPos2 = validPositions.get(j);
                if (blockPos1.getCenter().closerThan(blockPos2.getCenter(), maxDistBetweenBoomblooms)) {
                    validPositions.remove(j);
                    j--; // Decrement j since we removed an element, adjusting the index
                }
            }
        }

        if(!(entity instanceof Player)){
            return;
        }

        //Place a boombloom on each location
        validPositions.forEach(blockPos -> {
            //Create a boombloom entity
            Vec3 position = Vec3.atCenterOf(blockPos);

            //Calculate spawndelay based on distance to caster. 1 second per 4 blocks away from caster.
            Double spawnDelay = position.distanceTo(entity.position())*5;


            //Create a new boombloom at location
            BoombloomEntity newboombloom = new BoombloomEntity(
                    level,
                    (Player)entity,
                    getSpellPower(1,entity), //Created boomblooms are always level 1
                    position.x,
                    position.y,
                    position.z,
                    true,
                    2100d + Math.ceil(io.redspace.ironsspellbooks.api.util.Utils.random.nextDouble()*600),
                    80d+Math.ceil(io.redspace.ironsspellbooks.api.util.Utils.random.nextDouble()*160),
                    1d+Math.ceil(io.redspace.ironsspellbooks.api.util.Utils.random.nextDouble()*7),
                    (int)Math.ceil((spawnDelay*0.9)+(io.redspace.ironsspellbooks.api.util.Utils.random.nextDouble()*spawnDelay*0.2)) //Apply +-10 percent variation in spawndelay
            );
            level.addFreshEntity(newboombloom);
        });
    }

    private static Double getRadius(Float spellpower){
        return (double)spellpower/4 + Math.sqrt(spellpower);
    }
}
