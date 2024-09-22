package net.tuboi.druidry.spells;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Position;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.tuboi.druidry.Druidry;
import net.tuboi.druidry.entity.BoombloomEntity;
import net.tuboi.druidry.registries.DruidrySoundRegistry;
import net.tuboi.druidry.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BoombloomCascadeSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(Druidry.MODID, "boombloom_cascade");

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.radius", Utils.SetMaxDecimals((Math.sqrt(getSpellPower(spellLevel,caster))+2),1)), //Range is equal to power for this spell
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.SetMaxDecimals((Math.sqrt(getSpellPower(spellLevel,caster))*2),1)),
                Component.translatable("ui.irons_spellbooks.cast_range", Utils.SetMaxDecimals((Math.sqrt(getSpellPower(spellLevel,caster))/2),1))
        );
    }

    public BoombloomCascadeSpell() {
        this.manaCostPerLevel = 100;
        this.baseManaCost = 100;
        this.castTime = 100;
        this.spellPowerPerLevel = 8;
        this.baseSpellPower = 16;
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
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
        int scanHeight = 10;
        Double maxDistBetweenBoomblooms = 2.5d;
        //Get radius of spell (not actually radius but whatever)
        int radius = (int)Math.ceil(getRadius(getSpellPower(spellLevel, entity)));

        BlockPos playerPos = entity.blockPosition();

        List<BlockPos> validPositions = new ArrayList<>();

        //Get a square of blocks 10 blocks above the player
        BlockPos seCorner = playerPos.above(scanHeight).south(radius).east(radius);

        for(int i=0;i<radius*2;i++){
            BlockPos yPos = seCorner.north(i);          //iterate over each row on the north/south axis
            for(int j=0;j<radius*2;j++){
                BlockPos xPos = yPos.west(j);           //iterate over each block on the west/east axis
                for(int k=0;k<scanHeight*2;k++){        //Check all blocks 20 blocks downwards
                    BlockPos zPos = xPos.below(k);
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
            }
        }

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

            //Create a new boombloom at location
            BoombloomEntity newboombloom = new BoombloomEntity(
                    level,
                    (Player)entity,
                    getSpellPower(spellLevel,entity),
                    position.x,
                    position.y,
                    position.z,
                    true,
                    2100d + Math.ceil(io.redspace.ironsspellbooks.api.util.Utils.random.nextDouble()*600), //Alive for 2 minutes
                    5d + Math.ceil(io.redspace.ironsspellbooks.api.util.Utils.random.nextDouble()*15),
                    40d+Math.ceil(io.redspace.ironsspellbooks.api.util.Utils.random.nextDouble()*160),
                    1d+Math.ceil(io.redspace.ironsspellbooks.api.util.Utils.random.nextDouble()*7)
            );
            level.addFreshEntity(newboombloom);
        });
    }

    private static Double getRadius(Float spellpower){
        return (double)spellpower/2;
    }
}
