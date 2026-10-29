package com.srqingchen.dicefate.item;

import com.srqingchen.dicefate.attachment.PlayerFateData;
import com.srqingchen.dicefate.dice.DFRarity;
import com.srqingchen.dicefate.dice.DieState;
import com.srqingchen.dicefate.registry.DiceItems;
import com.srqingchen.dicefate.registry.ModAttachments;
import com.srqingchen.dicefate.registry.ModDataComponents;
import com.srqingchen.dicefate.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 未鉴定骰（盲盒）：服务端按稀有度概率揭示；每 10 次未出珍品保底。
 * 需要 10 命砂（背包扣除）。
 */
public class UnidentifiedDieItem extends Item {

    public UnidentifiedDieItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        // 鉴定费：10 命砂；手持鉴定卷轴→免费（卷轴在 appraise 前消耗由下方标记）；
        // 背包鉴定镜→免费并消耗镜 1 耐久（64 次——0.2.0 验收补全）
        boolean hasLens = hasLens(sp);
        boolean free = hasLens;
        if (!free && !consume(sp, ModItems.FATE_SAND.get(), 10)) {
            sp.sendOverlayMessage(Component.translatable("message.dicefate.need_sand"));
            return InteractionResult.FAIL;
        }
        InteractionResult r = appraise(sp, stack, free, level);
        // 0.2.0 验收修复 BUG C：镜耐久在鉴定成功后才扣（64 次语义不变）
        if (r == InteractionResult.CONSUME && hasLens) damageLens(sp);
        return r;
    }

    /** 0.2.0：鉴定公共入口（鉴定台复用；free=true 不扣命砂）。 */
    public InteractionResult appraise(ServerPlayer sp, ItemStack stack, boolean free, Level level) {
        if (free && stack.getItem() == this) {
            // 卷轴路径已在上层消耗；此处不再扣费
        }
        PlayerFateData data = sp.getData(ModAttachments.PLAYER_FATE);
        DFRarity rarity = rollRarity(sp, data);
        ItemStack die = randomDieOf(sp, rarity);
        if (die == null) {
            if (!free) refund(sp, ModItems.FATE_SAND.get(), 10);
            return InteractionResult.FAIL;
        }
        stack.shrink(1);
        data.addUnidentifiedOpened();
        if (rarity.ordinal() >= DFRarity.RARE.ordinal()) data.resetUnidentifiedStreak();
        give(sp, die);
        data.learnDie(((DiceItem) die.getItem()).dieId);
        level.playSound(null, sp.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.6F, 1.6F);
        sp.sendSystemMessage(Component.translatable("message.dicefate.appraised",
            die.getHoverName()));
        com.srqingchen.dicefate.util.AdvancementGrant.grant(sp, "blind_box");   // 0.3.0 F1
        if (die.getItem() instanceof DiceItem d && d.spec.kind() == com.srqingchen.dicefate.dice.DieKind.UNIQUE) {
            com.srqingchen.dicefate.util.AdvancementGrant.grant(sp, "unique_die");
        }
        return InteractionResult.CONSUME;
    }

    /** 概率公示：凡60/良25/珍10/史诗4/传说1；10 次未出珍 → 保底珍。 */
    private DFRarity rollRarity(ServerPlayer sp, PlayerFateData data) {
        if (data.unidentifiedOpened() >= 9) return DFRarity.RARE;
        double r = sp.getRandom().nextDouble() * 100;
        if (r < 1) return DFRarity.LEGENDARY;
        if (r < 5) return DFRarity.EPIC;
        if (r < 15) return DFRarity.RARE;
        if (r < 40) return DFRarity.UNCOMMON;
        return DFRarity.COMMON;
    }

    private ItemStack randomDieOf(ServerPlayer sp, DFRarity rarity) {
        List<DiceItem> pool = new ArrayList<>();
        DiceItems.allDice().filter(d -> d.spec.rarity() == rarity).forEach(pool::add);
        // 15% 元素/技能骰已在池内自然出现
        if (pool.isEmpty()) pool.addAll(DiceItems.allDice().toList());
        DiceItem pick = pool.get(sp.getRandom().nextInt(pool.size()));
        ItemStack stack = new ItemStack(pick);
        stack.set(ModDataComponents.DIE_STATE.get(), DieState.fresh());
        return stack;
    }

    private static boolean consume(ServerPlayer sp, Item item, int count) {
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

    /** 背包是否有鉴定镜。 */
    private static boolean hasLens(ServerPlayer sp) {
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            if (sp.getInventory().getItem(i).is(ModItems.APPRAISAL_LENS.get())) return true;
        }
        return false;
    }

    /** 消耗鉴定镜 1 耐久（免费鉴定 64 次语义）。 */
    private static void damageLens(ServerPlayer sp) {
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            ItemStack s = sp.getInventory().getItem(i);
            if (s.is(ModItems.APPRAISAL_LENS.get())) {
                s.hurtAndBreak(1, sp, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                return;
            }
        }
    }

    private static void refund(ServerPlayer sp, Item item, int count) {
        sp.getInventory().add(new ItemStack(item, count));
    }

    private static void give(ServerPlayer sp, ItemStack stack) {
        if (!sp.getInventory().add(stack)) {
            sp.drop(stack, false);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.dicefate.unidentified").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
