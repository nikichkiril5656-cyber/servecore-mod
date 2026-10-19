package com.example.servercore.event;

import com.example.servercore.Config;
import com.example.servercore.ServerCore;
import com.example.servercore.command.ModCommands;
import com.example.servercore.data.AuctionStore;
import com.example.servercore.data.PlayerData;
import com.example.servercore.data.PlayerStore;
import com.example.servercore.pet.Pet;
import com.example.servercore.pet.PetManager;
import com.example.servercore.skill.Skill;
import com.example.servercore.skill.SkillManager;
import com.example.servercore.auction.AuctionManager;
import com.example.servercore.util.Fmt;
import com.example.servercore.util.Inv;
import com.example.servercore.util.PlacedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Все игровые события мода: опыт навыков, бонусы, питомцы, жизненный цикл аукциона. */
@EventBusSubscriber(modid = ServerCore.MODID)
public final class GameEvents {
    private GameEvents() {
    }

    // ---------- команды ----------

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        ModCommands.register(e.getDispatcher());
    }

    // ---------- вход / респавн ----------

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        PlayerData d = PlayerStore.get(p.server).data(p);
        PetManager.applyAttributes(p);

        if (d.offlineSales > 0) {
            Fmt.ok(p, "Пока вас не было, на аукционе продано лотов: " + d.offlineSales
                    + ". Заработано: " + Fmt.coins(d.offlineEarnings));
            d.offlineSales = 0;
            d.offlineEarnings = 0;
            PlayerStore.get(p.server).setDirty();
        }
        int claims = AuctionStore.get(p.server).claimCount(p.getUUID());
        if (claims > 0) {
            Fmt.info(p, "В хранилище аукциона вас ждут предметы (" + claims + "): /ah claim");
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) PetManager.applyAttributes(p);
    }

    // ---------- аукцион ----------

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 600 == 0) {
            AuctionManager.expire(e.getServer());
        }
    }

    // ---------- блоки: добыча, ферма, лес ----------

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getEntity() instanceof Player && e.getLevel() instanceof Level level && !level.isClientSide) {
            PlacedBlocks.mark(level, e.getPos());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p) || p instanceof FakePlayer || p.isCreative()) return;
        if (!(e.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = e.getPos();
        // блок, поставленный игроком, опыта не даёт
        if (PlacedBlocks.consume(level, pos)) return;

        BlockState state = e.getState();

        // Добыча
        double mining = miningXp(state);
        if (mining > 0 && p.hasCorrectToolForDrops(state)) {
            SkillManager.addXp(p, Skill.MINING, mining);
            if (state.is(Tags.Blocks.ORES)) {
                doubleDrop(p, level, pos, state, SkillManager.level(p, Skill.MINING) * SkillManager.MINING_DOUBLE);
            }
            return;
        }

        // Лесорубство
        if (state.is(BlockTags.LOGS) && p.getMainHandItem().is(ItemTags.AXES)) {
            SkillManager.addXp(p, Skill.FORAGING, 8);
            doubleDrop(p, level, pos, state, SkillManager.level(p, Skill.FORAGING) * SkillManager.FORAGING_DOUBLE);
            return;
        }

        // Фермерство
        double farming = farmingXp(state);
        if (farming > 0) {
            SkillManager.addXp(p, Skill.FARMING, farming);
            doubleDrop(p, level, pos, state, SkillManager.level(p, Skill.FARMING) * SkillManager.FARMING_DOUBLE);
        }
    }

    private static double miningXp(BlockState s) {
        if (s.is(Tags.Blocks.ORES_NETHERITE_SCRAP) || s.is(Blocks.ANCIENT_DEBRIS)) return 150;
        if (s.is(Tags.Blocks.ORES_EMERALD)) return 80;
        if (s.is(Tags.Blocks.ORES_DIAMOND)) return 60;
        if (s.is(Tags.Blocks.ORES_GOLD)) return 25;
        if (s.is(Tags.Blocks.ORES_LAPIS)) return 20;
        if (s.is(Tags.Blocks.ORES_IRON) || s.is(Tags.Blocks.ORES_REDSTONE)) return 15;
        if (s.is(Tags.Blocks.ORES_COPPER) || s.is(Tags.Blocks.ORES_QUARTZ)) return 12;
        if (s.is(Tags.Blocks.ORES_COAL)) return 10;
        if (s.is(Tags.Blocks.ORES)) return 15;
        if (s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.BASE_STONE_NETHER)) return 1;
        return 0;
    }

    private static double farmingXp(BlockState s) {
        if (s.getBlock() instanceof CropBlock crop) return crop.isMaxAge(s) ? 6 : 0;
        if (s.is(Blocks.NETHER_WART)) return s.getValue(NetherWartBlock.AGE) >= 3 ? 8 : 0;
        if (s.is(Blocks.MELON) || s.is(Blocks.PUMPKIN)) return 8;
        return 0;
    }

    /** С шансом повторно выбрасывает дроп ломаемого блока. */
    private static void doubleDrop(ServerPlayer p, ServerLevel level, BlockPos pos, BlockState state, double chance) {
        if (chance <= 0 || p.getRandom().nextDouble() >= chance) return;
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) return;
        for (ItemStack drop : Block.getDrops(state, level, pos, level.getBlockEntity(pos), p, p.getMainHandItem())) {
            Block.popResource(level, pos, drop);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer) return;
        BlockState s = e.getState();
        double bonus = 0;
        if (s.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            bonus += SkillManager.level(p, Skill.MINING) * SkillManager.MINING_SPEED;
        }
        if (s.is(BlockTags.LOGS)) {
            bonus += SkillManager.level(p, Skill.FORAGING) * SkillManager.FORAGING_SPEED;
        }
        if (bonus > 0) e.setNewSpeed((float) (e.getNewSpeed() * (1.0 + bonus)));
    }

    // ---------- бой ----------

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent e) {
        if (e.getSource().getEntity() instanceof ServerPlayer p && !(p instanceof FakePlayer)) {
            int lvl = SkillManager.level(p, Skill.COMBAT);
            if (lvl > 0) e.setAmount((float) (e.getAmount() * (1.0 + lvl * SkillManager.COMBAT_DAMAGE)));
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide || victim instanceof Player || victim instanceof ArmorStand) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer) return;

        double hp = victim.getMaxHealth();
        boolean hostile = victim instanceof Enemy;
        SkillManager.addXp(p, Skill.COMBAT, Math.max(1, hostile ? hp : hp * 0.25));
        if (hostile) {
            PetManager.tryDrop(p, Config.PET_DROP_KILL.get() * (1.0 + hp / 50.0), "убийство моба");
        }
    }

    // ---------- рыбалка ----------

    @SubscribeEvent
    public static void onFished(ItemFishedEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer) return;
        int lvl = SkillManager.level(p, Skill.FISHING);

        SkillManager.addXp(p, Skill.FISHING, 20);

        long coins = 1 + lvl * SkillManager.FISHING_COINS_PER_LEVEL;
        PlayerData d = PlayerStore.get(p.server).data(p);
        d.balance = Fmt.satAdd(d.balance, coins);
        PlayerStore.get(p.server).setDirty();

        if (lvl > 0 && p.getRandom().nextDouble() < lvl * SkillManager.FISHING_DOUBLE) {
            for (ItemStack drop : e.getDrops()) Inv.give(p, drop.copy());
            Fmt.ok(p, "Двойной улов!");
        }
        PetManager.tryDrop(p, Config.PET_DROP_FISH.get(), "рыбалка");
    }

    // ---------- предмет-питомец ----------

    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ServerPlayer p)) return;
        ItemStack stack = e.getItemStack();
        Pet pet = PetManager.fromItem(stack);
        if (pet == null) return;
        e.setCanceled(true);
        PetManager.redeem(p, stack, pet);
    }
}
