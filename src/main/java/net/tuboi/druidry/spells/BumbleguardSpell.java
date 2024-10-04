package net.tuboi.druidry.spells;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tuboi.druidry.Druidry;
import net.tuboi.druidry.block.bumbleguardhive.BumbleguardBlock;
import net.tuboi.druidry.block.bumbleguardhive.BumbleguardBlockEntity;
import net.tuboi.druidry.entity.BoombloomEntity;
import net.tuboi.druidry.registries.DruidryBlockRegistry;
import net.tuboi.druidry.registries.DruidrySoundRegistry;
import net.tuboi.druidry.utils.Utils;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class BumbleguardSpell extends AbstractSpell {

    private final static Integer MINIMUM_DISTANCE_BETWEEN_HIVES = 30;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(Druidry.MODID, "summon_bumbleguard");

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.tubois_druidry.number_of_bumbleguard", (int)Math.ceil(getSpellPower(spellLevel, caster)))
        );
    };

    public BumbleguardSpell() {
        this.manaCostPerLevel = 100;
        this.baseManaCost = 100;
        this.castTime = 100;
        this.spellPowerPerLevel = 1;
        this.baseSpellPower = 1;
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(60d)
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
        return Optional.of(DruidrySoundRegistry.NATURE_CAST_END.get());
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(DruidrySoundRegistry.NATURE_CAST_START.get());
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {

        return checkBlockValid(level, entity);
    }


    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);

        var pos = Utils.getTargetBlock(level, entity, ClipContext.Fluid.NONE, 6);
        BlockState hitblock = level.getBlockState(pos.getBlockPos());

        Vec3 position = Vec3.atCenterOf(pos.getBlockPos());

        if (!(entity instanceof Player)) {
            return;
        }

        //Verify again that the block is a beehive or bee nest
        if (!checkBlockValid(level, entity)) {
            return;
        }

        //Expel the bees from previous hive
        var existingBeehive = level.getBlockEntity(pos.getBlockPos());
        if (!(existingBeehive instanceof BeehiveBlockEntity)) {
            return;
        }

        ((BeehiveBlockEntity) existingBeehive).emptyAllLivingFromHive((Player) entity, hitblock, BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED);
        existingBeehive.setRemoved(); //remove the old beehive

        //Create blockstate for new hive
        Block hive = DruidryBlockRegistry.BUMBLEGUARD_HIVE_BLOCK.get();
        BlockState bumbleguardHive = hive.defaultBlockState();
        bumbleguardHive.setValue(BumbleguardBlock.FACING, hitblock.getValue(BeehiveBlock.FACING));

        level.removeBlockEntity(pos.getBlockPos()); //remove the old beehive
        level.setBlockAndUpdate(pos.getBlockPos(), bumbleguardHive); //Spawn the bumbleguard hive
        level.removeBlockEntity(pos.getBlockPos()); //in case one already exists for some reason

        // Schedule the creation of the block entity to the next tick
        level.getServer().execute(() -> {
            // Create the block entity
            if (hive instanceof BumbleguardBlock && level.getBlockState(pos.getBlockPos()).is(DruidryBlockRegistry.BUMBLEGUARD_HIVE_BLOCK)) {
                BumbleguardBlockEntity bumbleguardBlockEntity = new BumbleguardBlockEntity(pos.getBlockPos(), bumbleguardHive, (Player) entity, (int) Math.ceil(getSpellPower(spellLevel, entity)));
                level.setBlockEntity(bumbleguardBlockEntity);
            }
        });
    }

    private boolean checkBlockValid(Level level, LivingEntity caster){
        var blockHitResult = Utils.getTargetBlock(level, caster, ClipContext.Fluid.NONE, 6);
        BlockState hitblock = level.getBlockState(blockHitResult.getBlockPos());

        if(!(hitblock.is(Blocks.BEEHIVE)||hitblock.is(Blocks.BEE_NEST))){
            if (caster instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.tubois_druidry.bumbleguard_error_target_block").withStyle(ChatFormatting.RED)));
            }
            return false;
        }

        if(!checkNoOtherHiveWithin30Blocks(level, blockHitResult.getBlockPos().getCenter())){
            if (caster instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.tubois_druidry.bumbleguard_error_too_close").withStyle(ChatFormatting.RED)));
            }
            return false;
        }

        return true;
    }

    private boolean checkNoOtherHiveWithin30Blocks(Level level, Vec3 position){
        Integer r = MINIMUM_DISTANCE_BETWEEN_HIVES*2;
        AABB aabb = AABB.ofSize(position, r, r, r);

        if(containsBlockState(level, aabb, DruidryBlockRegistry.BUMBLEGUARD_HIVE_BLOCK.get().defaultBlockState())){
            return false;
        }

        return true;
    }

    private static boolean containsBlockState(Level level, AABB area, BlockState targetState) {
        BlockPos min = new BlockPos((int)area.minX, (int)area.minY, (int)area.minZ);
        BlockPos max = new BlockPos((int)area.maxX, (int)area.maxY, (int)area.maxZ);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState currentState = level.getBlockState(pos);
            if (currentState.equals(targetState)) {
                return true;
            }
        }
        return false;
    }



}
