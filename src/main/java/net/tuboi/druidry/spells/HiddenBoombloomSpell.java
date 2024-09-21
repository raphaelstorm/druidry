package net.tuboi.druidry.spells;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tuboi.druidry.Druidry;
import net.tuboi.druidry.entity.BoombloomEntity;
import net.tuboi.druidry.registries.DruidrySoundRegistry;
import net.tuboi.druidry.utils.Utils;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class HiddenBoombloomSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(Druidry.MODID, "hidden_boombloom");

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.radius", Utils.SetMaxDecimals((Math.sqrt(getSpellPower(spellLevel,caster))+2),1)), //Range is equal to power for this spell
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.SetMaxDecimals((Math.sqrt(getSpellPower(spellLevel,caster))*2),1))
                );
    }

    public HiddenBoombloomSpell() {
        this.manaCostPerLevel = 25;
        this.baseManaCost = 50;
        this.castTime = 40;
        this.spellPowerPerLevel = 4;
        this.baseSpellPower = 16;
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(5d)
            .build();

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

        var blockHitResult = Utils.getTargetBlock(level, entity, ClipContext.Fluid.NONE, 6);

        if(!level.getBlockState(blockHitResult.getBlockPos().above(1)).is(BlockTags.FLOWERS)){
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.tubois_druidry.boombloom_error_target_flower").withStyle(ChatFormatting.RED)));
            }
            return false;
        }

        //Check if another boombloom is already placed at that location
        List<BoombloomEntity> bloomsInBlock = level.getEntitiesOfClass(
                BoombloomEntity.class,
                AABB.ofSize(blockHitResult.getBlockPos().above(1).getCenter(),0.5,0.5,0.5) //Get volume of the block
        );

        if (!bloomsInBlock.isEmpty()) {
            if(entity instanceof ServerPlayer serverPlayer){
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.tubois_druidry.boombloom_error_occupied").withStyle(ChatFormatting.RED)));
            }
            return false;
        }

        return true;
    }


    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);

        //Get the blockpos the player is looking at
        var pos = Utils.getTargetBlock(level, entity, ClipContext.Fluid.NONE, 6);

        Vec3 position = Vec3.atCenterOf(pos.getBlockPos().above(1));

        //Create a new boombloom at location
        var newboombloom = new BoombloomEntity(
                level,
                entity,
                getSpellPower(spellLevel,entity),
                position.x,
                position.y,
                position.z
        );
        level.addFreshEntity(newboombloom);
    }
}
