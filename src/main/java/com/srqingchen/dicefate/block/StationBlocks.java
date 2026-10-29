package com.srqingchen.dicefate.block;

import com.srqingchen.dicefate.attachment.PlayerFateData;
import com.srqingchen.dicefate.dice.DieRune;
import com.srqingchen.dicefate.dice.DieState;
import com.srqingchen.dicefate.item.DiceItem;
import com.srqingchen.dicefate.registry.ModAttachments;
import com.srqingchen.dicefate.registry.ModDataComponents;
import com.srqingchen.dicefate.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 六个工作站方块的右键交互实现（规划 5.4 功能）。
 * 全屏 GUI 属于打磨项；核心功能以手持物品右键直接触达。
 */
public final class StationBlocks {
    private StationBlocks() {}

    /** 骰运台：手持骰右键 → 消耗命砂+祭品 → 施加随机骰纹；空手右键 → 随机命途。 */
    public static final class FateAltarBlock extends Block {
        public FateAltarBlock(Properties p) { super(p); }

        @Override
        public InteractionResult useWithoutItem(net.minecraft.world.level.block.state.BlockState state,
                net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                net.minecraft.world.entity.player.Player player,
                net.minecraft.world.phys.BlockHitResult hit) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            ItemStack held = sp.getMainHandItem();

            if (held.getItem() instanceof DiceItem) {
                // 骰纹洗练：20 命砂（背包有命纹墨 → 1 墨 + 10 命砂）
                boolean ink = hasItem(sp, ModItems.RUNE_INK.get());
                int sandCost = ink ? 10 : 20;
                if (!consume(sp, ModItems.FATE_SAND.get(), sandCost)) {
                    sp.sendOverlayMessage(Component.translatable("message.dicefate.need_sand"));
                    return InteractionResult.FAIL;
                }
                if (ink) consumeOne(sp, ModItems.RUNE_INK.get());
                DieState st = held.getOrDefault(ModDataComponents.DIE_STATE.get(), DieState.fresh());
                // 祭品决定纹池：命核→高价值纹；星屑→中；骨→基础（0.2.0 祭品落地）
                String offering = consumeOne(sp, ModItems.OFFERING_CORE.get()) ? "core"
                    : consumeOne(sp, ModItems.OFFERING_STAR.get()) ? "star"
                    : consumeOne(sp, ModItems.OFFERING_BONE.get()) ? "bone" : "none";
                DieRune[] runes = switch (offering) {
                    case "core" -> new DieRune[]{DieRune.FIERCE, DieRune.TOUGH, DieRune.SWIFT, DieRune.GREEDY};
                    case "star" -> new DieRune[]{DieRune.EDGE, DieRune.WALL, DieRune.FIERCE, DieRune.TOUGH};
                    default -> new DieRune[]{DieRune.EDGE, DieRune.WALL, DieRune.STEADY, DieRune.FIERCE,
                        DieRune.TOUGH, DieRune.SWIFT, DieRune.GREEDY};
                };
                DieRune rune = runes[sp.getRandom().nextInt(runes.length)];
                held.set(ModDataComponents.DIE_STATE.get(), st.withRune(rune.getSerializedName()));
                level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.2F);
                sp.sendOverlayMessage(Component.translatable("message.dicefate.rune_applied",
                    Component.translatable("tooltip.dicefate.rune." + rune.getSerializedName())));
                com.srqingchen.dicefate.util.AdvancementGrant.grant(sp, "rune");   // 0.3.0 F1
                return InteractionResult.CONSUME;
            }

            // 玩家命途（空手）：50 命砂随机
            PlayerFateData data = sp.getData(ModAttachments.PLAYER_FATE);
            if (!consume(sp, ModItems.FATE_SAND.get(), 50)) {
                sp.sendOverlayMessage(Component.translatable("message.dicefate.need_sand"));
                return InteractionResult.FAIL;
            }
            String[] runes = {"rune_guard", "rune_hunt", "rune_sacrifice", "rune_pity", "rune_greedy"};
            String pick = runes[sp.getRandom().nextInt(runes.length)];
            data.setPlayerRune(com.srqingchen.dicefate.DiceFate.id(pick));
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8F, 0.8F);
            sp.sendOverlayMessage(Component.translatable("message.dicefate.fate_rune_applied"));
            return InteractionResult.CONSUME;
        }
    }

    /** 刻骰台：手持骰胚右键 → 按背包最高刻片 + 材料造骰。 */
    public static final class DieEngraverBlock extends Block {
        public DieEngraverBlock(Properties p) { super(p); }

        @Override
        public InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level,
                net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player,
                net.minecraft.world.phys.BlockHitResult hit) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            ItemStack held = sp.getMainHandItem();

            int blankFace = blankFace(held);
            if (blankFace < 0) {
                sp.sendOverlayMessage(Component.translatable("message.dicefate.need_blank"));
                return InteractionResult.FAIL;
            }
            // 找背包内最高面数刻片
            int bestPlate = 0;
            for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
                ItemStack s = sp.getInventory().getItem(i);
                int f = plateFace(s);
                if (f > bestPlate) bestPlate = f;
            }
            if (bestPlate == 0) {
                sp.sendOverlayMessage(Component.translatable("message.dicefate.need_plate"));
                return InteractionResult.FAIL;
            }
            if (!consume(sp, ModItems.FATE_DUST.get(), 2)) {
                sp.sendOverlayMessage(Component.translatable("message.dicefate.need_dust"));
                return InteractionResult.FAIL;
            }
            // 产出：材质按胚（bone/iron/mithril/fategold/starlight）+ 面数按刻片
            String mat = switch (held.getItem().builtInRegistryHolder().getRegisteredName()) {
                case "dicefate:blank_die_iron" -> "iron";
                case "dicefate:blank_die_mithril" -> "mithril";
                case "dicefate:blank_die_fategold" -> "fategold";
                case "dicefate:blank_die_starlight" -> "starlight";
                default -> "bone";
            };
            var id = com.srqingchen.dicefate.DiceFate.id("die_" + mat + "_d" + bestPlate);
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
            if (item.isEmpty()) {
                sp.sendOverlayMessage(Component.translatable("message.dicefate.need_plate"));
                return InteractionResult.FAIL;
            }
            held.shrink(1);
            ItemStack die = new ItemStack(item.get());
            die.set(ModDataComponents.DIE_STATE.get(), DieState.fresh());
            if (!sp.getInventory().add(die)) sp.drop(die, false);
            sp.getData(ModAttachments.PLAYER_FATE).learnDie(((DiceItem) die.getItem()).dieId);
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.4F);
            sp.sendOverlayMessage(Component.translatable("message.dicefate.die_forged", die.getHoverName()));
            return InteractionResult.CONSUME;
        }

        private int blankFace(ItemStack s) {
            return switch (s.getItem().builtInRegistryHolder().getRegisteredName()) {
                case "dicefate:blank_die_bone", "dicefate:blank_die_iron" -> 1;
                case "dicefate:blank_die_mithril" -> 2;
                case "dicefate:blank_die_fategold" -> 3;
                case "dicefate:blank_die_starlight" -> 4;
                default -> -1;
            };
        }

        private int plateFace(ItemStack s) {
            return switch (s.getItem().builtInRegistryHolder().getRegisteredName()) {
                case "dicefate:face_plate_d4" -> 4;
                case "dicefate:face_plate_d6" -> 6;
                case "dicefate:face_plate_d8" -> 8;
                case "dicefate:face_plate_d10" -> 10;
                case "dicefate:face_plate_d12" -> 12;
                case "dicefate:face_plate_d20" -> 20;
                case "dicefate:face_plate_d24" -> 24;
                default -> 0;
            };
        }
    }

    /** 鉴定台：手持未鉴定骰右键 → 鉴定（10 命砂；背包有鉴定镜→免费；手持卷轴→消耗卷轴免费）。
     *  空手右键 → 提示。0.2.0 从 PASS（无功能）落地为实际实现。 */
    public static final class AppraisalTableBlock extends Block {
        public AppraisalTableBlock(Properties p) { super(p); }

        @Override
        public InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level,
                net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player,
                net.minecraft.world.phys.BlockHitResult hit) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            ItemStack held = sp.getMainHandItem();
            if (!(held.getItem() instanceof com.srqingchen.dicefate.item.UnidentifiedDieItem unid)) {
                sp.sendOverlayMessage(Component.translatable("message.dicefate.need_unidentified"));
                return InteractionResult.PASS;
            }
            boolean free = damageLens(sp);
            if (!free && !consume(sp, ModItems.FATE_SAND.get(), 10)) {
                sp.sendOverlayMessage(Component.translatable("message.dicefate.need_sand"));
                return InteractionResult.FAIL;
            }
            InteractionResult r = unid.appraise(sp, held, free, level);
            if (r == InteractionResult.CONSUME) {
                level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.7F, 1.1F);
            }
            return r;
        }
    }

    /** 修复台：手持磨损骰右键 → 修复（0.2.0：修复锉-20/命胶-50/完美油-全，无材料回落命砂）。 */
    public static final class RepairBenchBlock extends Block {
        public RepairBenchBlock(Properties p) { super(p); }

        @Override
        public InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level,
                net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player,
                net.minecraft.world.phys.BlockHitResult hit) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            ItemStack held = sp.getMainHandItem();
            if (!(held.getItem() instanceof DiceItem dice)) return InteractionResult.PASS;
            var def = dice.definition(level);
            if (def == null) return InteractionResult.PASS;
            DieState st = held.getOrDefault(ModDataComponents.DIE_STATE.get(), DieState.fresh());
            int max = def.material().baseWear();
            if (st.wear() <= 0 || st.wear() >= max) return InteractionResult.PASS;

            int repaired;
            if (consumeOne(sp, ModItems.PERFECT_OIL.get())) {
                repaired = max;
                sp.sendOverlayMessage(Component.translatable("message.dicefate.repair_oil"));
            } else if (consumeOne(sp, ModItems.FATE_GLUE.get())) {
                repaired = 50;
                sp.sendOverlayMessage(Component.translatable("message.dicefate.repair_glue"));
            } else if (consumeOne(sp, ModItems.REPAIR_FILE.get())) {
                repaired = 20;
                sp.sendOverlayMessage(Component.translatable("message.dicefate.repair_file"));
            } else {
                // 0.3.0 B13：命砂按 20 点/档计费（原一份钱修 50 点）
                int cost = 10 + def.face();
                if (!consume(sp, ModItems.FATE_SAND.get(), cost)) {
                    sp.sendOverlayMessage(Component.translatable("message.dicefate.need_sand"));
                    return InteractionResult.FAIL;
                }
                repaired = 20;
            }
            held.set(ModDataComponents.DIE_STATE.get(), st.withWear(Math.max(0, st.wear() - repaired)));
            level.playSound(null, pos, SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, 0.6F, 1.2F);
            sp.sendOverlayMessage(Component.translatable("message.dicefate.repaired"));
            com.srqingchen.dicefate.util.AdvancementGrant.grant(sp, "repair_1");   // 0.3.0 F1
            return InteractionResult.CONSUME;
        }
    }

    /** 命线织机：右键循环加载预设 1-3；潜行右键保存到下一预设。 */
    public static final class FateLoomBlock extends Block {
        public FateLoomBlock(Properties p) { super(p); }

        @Override
        public InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level,
                net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player,
                net.minecraft.world.phys.BlockHitResult hit) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            PlayerFateData data = sp.getData(ModAttachments.PLAYER_FATE);
            if (sp.isShiftKeyDown()) {
                int cursor = data.loomCursor();
                data.presets().get(cursor).copyFrom(data.pool());
                sp.sendOverlayMessage(Component.translatable("message.dicefate.preset_saved", cursor + 1));
                data.setLoomCursor(cursor + 1);
            } else {
                int cursor = data.loomCursor();
                data.pool().copyFrom(data.presets().get(cursor));
                com.srqingchen.dicefate.network.DFNetwork.sendPool(sp, data);
                sp.sendOverlayMessage(Component.translatable("message.dicefate.preset_loaded", cursor + 1));
                data.setLoomCursor(cursor + 1);
            }
            return InteractionResult.CONSUME;
        }
    }

    /** 供奉龛：手持香右键 → 生命恢复/抗性 30s（10 分钟冷却简化为饱食消耗）。 */
    public static final class OfferingShrineBlock extends Block {
        public OfferingShrineBlock(Properties p) { super(p); }

        @Override
        public InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level,
                net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player,
                net.minecraft.world.phys.BlockHitResult hit) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            if (!sp.getMainHandItem().is(ModItems.INCENSE.get())) return InteractionResult.PASS;
            sp.getMainHandItem().shrink(1);
            sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0));
            sp.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 600, 0));
            level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.7F);
            sp.sendOverlayMessage(Component.translatable("message.dicefate.shrine_blessed"));
            return InteractionResult.CONSUME;
        }
    }

    /** 骰商摊位：工作站（村民 POI），无右键功能。 */
    public static final class MerchantStallBlock extends Block {
        public MerchantStallBlock(Properties p) { super(p); }
    }

    public static boolean consume(ServerPlayer sp, Item item, int count) {
        int remaining = count;
        for (int i = 0; i < sp.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack s = sp.getInventory().getItem(i);
            if (s.is(item)) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
        return remaining == 0;
    }

    /** 0.2.0：消耗 1 个指定物品（找到才消耗）。 */
    public static boolean consumeOne(ServerPlayer sp, Item item) {
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            ItemStack s = sp.getInventory().getItem(i);
            if (s.is(item)) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    /** 鉴定镜：消耗 1 耐久（64 次免费鉴定——0.2.0 验收补全）。 */
    private static boolean damageLens(ServerPlayer sp) {
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            ItemStack s = sp.getInventory().getItem(i);
            if (s.is(ModItems.APPRAISAL_LENS.get())) {
                s.hurtAndBreak(1, sp, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                return true;
            }
        }
        return false;
    }

    /** 0.2.0：背包是否有指定物品（不消耗）。 */
    public static boolean hasItem(ServerPlayer sp, Item item) {
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            if (sp.getInventory().getItem(i).is(item)) return true;
        }
        return false;
    }
}
