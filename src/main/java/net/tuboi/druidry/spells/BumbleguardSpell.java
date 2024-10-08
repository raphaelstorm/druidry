package net.tuboi.druidry.spells;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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
import net.tuboi.druidry.entity.bumbleguard.Bumbleguard;
import net.tuboi.druidry.registries.DruidryBlockRegistry;
import net.tuboi.druidry.registries.DruidrySoundRegistry;
import net.tuboi.druidry.utils.ParticleHelper;
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
        return checkBlockValid(level, entity) || getHitEntity(level, entity) != null;
    }


    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);

        //If spell is cast on a living entity, tag or untag it to whitelist it from bumbleguard attacks
        LivingEntity hitEntity = getHitEntity(level, entity);
        if(hitEntity != null) {

            //Get the name of the entity type, or the custom name if it has one. If its a player, get the playername
            String nameToDisplay;
            if(hitEntity.hasCustomName()){
                nameToDisplay = hitEntity.getCustomName().getString();
            }else if(hitEntity instanceof Player){
                nameToDisplay = ((Player) hitEntity).getGameProfile().getName();
            }else{
                nameToDisplay = hitEntity.getType().getDescriptionId();
            }

            if (Utils.BumbleguardTagUtil.playerHasTaggedEntity(hitEntity, entity.getStringUUID())) {
                //Remove the tag from the entity
                Utils.BumbleguardTagUtil.removedPlayerFromTaggedEntity(hitEntity, entity.getStringUUID());

                //Notify the player
                if (entity instanceof ServerPlayer serverPlayer) {
                    serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.tubois_druidry.bumbleguard_untagged_entity", nameToDisplay).withStyle(ChatFormatting.RED)));
                }

            }else{
                //Add the tag to the entity
                Utils.BumbleguardTagUtil.addPlayerToTaggedEntity(hitEntity, entity.getStringUUID());

                //Notify the player
                if (entity instanceof ServerPlayer serverPlayer) {
                    serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.tubois_druidry.bumbleguard_tagged_entity", nameToDisplay).withStyle(ChatFormatting.GREEN)));
                }
            }
            return;
        }

        if (!checkBlockValid(level, entity) || !(entity instanceof Player)) {
            return;
        }

        var pos = Utils.getTargetBlock(level, entity, ClipContext.Fluid.NONE, 6);
        BlockState hitblock = level.getBlockState(pos.getBlockPos());

        Direction facing;
        var existingBeehive = level.getBlockEntity(pos.getBlockPos());
        if (existingBeehive instanceof BeehiveBlockEntity) {
            ((BeehiveBlockEntity) existingBeehive).emptyAllLivingFromHive((Player) entity, hitblock, BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED);
            facing = hitblock.getValue(BeehiveBlock.FACING);
            existingBeehive.setRemoved(); //remove the old beehive
        }else if (existingBeehive instanceof BumbleguardBlockEntity) {
            facing = hitblock.getValue(BumbleguardBlock.FACING);
        }else{
            return;
        }

        //Create blockstate for new hive
        Block hive = DruidryBlockRegistry.BUMBLEGUARD_HIVE_BLOCK.get();
        BlockState bumbleguardHive = hive.defaultBlockState().setValue(BumbleguardBlock.FACING, facing);

        level.removeBlockEntity(pos.getBlockPos()); //in case of remaining blockentities from previous hive
        level.setBlockAndUpdate(pos.getBlockPos(), bumbleguardHive);
        level.removeBlockEntity(pos.getBlockPos());

        // Schedule the creation of the block entity to the next tick
        level.getServer().execute(() -> {
            // Create the block entity
            if (hive instanceof BumbleguardBlock && level.getBlockState(pos.getBlockPos()).is(DruidryBlockRegistry.BUMBLEGUARD_HIVE_BLOCK)) {
                BumbleguardBlockEntity bumbleguardBlockEntity = new BumbleguardBlockEntity(
                        pos.getBlockPos(),
                        bumbleguardHive,
                        (Player) entity,
                        (int) Math.ceil(getSpellPower(spellLevel, entity)
                        )
                );
                level.setBlockEntity(bumbleguardBlockEntity);
            }
        });
    }

    @Override
    public void onClientCast(Level level, int spellLevel, LivingEntity entity, ICastData castData) {
        super.onClientCast(level, spellLevel, entity, castData);
        spawnParticlesAroundHive(level, Utils.getTargetBlock(level,entity, ClipContext.Fluid.NONE, 6).getBlockPos().getCenter(), 50);
    }

    private boolean checkBlockValid(Level level, LivingEntity caster){
        var blockHitResult = Utils.getTargetBlock(level, caster, ClipContext.Fluid.NONE, 6);
        BlockState hitblock = level.getBlockState(blockHitResult.getBlockPos());

        if(!(hitblock.is(Blocks.BEEHIVE)||hitblock.is(Blocks.BEE_NEST)||hitblock.is(DruidryBlockRegistry.BUMBLEGUARD_HIVE_BLOCK))){
            if (caster instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.tubois_druidry.bumbleguard_error_target_block").withStyle(ChatFormatting.RED)));
            }
            return false;
        }

        if(!hitblock.is(DruidryBlockRegistry.BUMBLEGUARD_HIVE_BLOCK) && !checkNoOtherHiveWithin30Blocks(level, blockHitResult.getBlockPos().getCenter())){
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

        if(containsBlock(level, aabb, DruidryBlockRegistry.BUMBLEGUARD_HIVE_BLOCK.get())){
            return false;
        }

        return true;
    }

    private static boolean containsBlock(Level level, AABB area, Block targetBlock) {
        BlockPos min = new BlockPos((int)area.minX, (int)area.minY, (int)area.minZ);
        BlockPos max = new BlockPos((int)area.maxX, (int)area.maxY, (int)area.maxZ);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            Block currentBlock = level.getBlockState(pos).getBlock();
            if (currentBlock.equals(targetBlock)) {
                return true;
            }
        }
        return false;
    }

    private static void spawnParticlesAroundHive(Level level, Vec3 position, int count){
        for (int i = 0; i < count; i++) {
            double[] randomVec = Utils.GetVectorSpeeds(0.05d);
            level.addParticle(ParticleHelper.FERTILIZER_EMITTER, position.x+Math.random()-0.5d, position.y+Math.random()-0.5d, position.z+Math.random()-0.5d, randomVec[0], randomVec[1], randomVec[2]);
        }
    }

    private static LivingEntity getHitEntity(Level level, LivingEntity caster) {
        Vec3 start = caster.getEyePosition();
        Vec3 end = start.add(caster.getLookAngle().scale(6));
        AABB aabb = new AABB(start, end).inflate(1.0); // Inflate to ensure we catch entities near the ray

        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, aabb, entity -> entity != caster && !(entity instanceof Bumbleguard) && entity.isAlive());
        LivingEntity closestEntity = null;
        double closestDistance = Double.MAX_VALUE;

        for (LivingEntity entity : entities) {
            AABB entityAABB = entity.getBoundingBox().inflate(0.3); // Inflate to make it easier to hit
            Optional<Vec3> optionalHit = entityAABB.clip(start, end);

            if (optionalHit.isPresent()) {
                double distance = start.distanceTo(optionalHit.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEntity = entity;
                }
            }
        }

        return closestEntity;
    }
}
