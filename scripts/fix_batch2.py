# -*- coding: utf-8 -*-
"""26.1 API 重命名批量修复（第 2 批）。"""

NEW_TOOLTIP_SIG = '''    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.core.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> builder, TooltipFlag flag) {'''

# MiscUseItem tooltip
p = 'src/main/java/com/srqingchen/dicefate/item/MiscUseItem.java'
s = open(p, encoding='utf-8').read()
s = s.replace('    @Override\n    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {',
              NEW_TOOLTIP_SIG)
s = s.replace('''        lines.add(Component.translatable("tooltip.dicefate.item." + key).withStyle(
                net.minecraft.ChatFormatting.DARK_AQUA));''',
'''        builder.accept(Component.translatable("tooltip.dicefate.item." + key).withStyle(
                net.minecraft.ChatFormatting.DARK_AQUA));''')
s = s.replace('import java.util.List;\n', '')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('MiscUseItem tooltip ok')

# UnidentifiedDieItem tooltip
p = 'src/main/java/com/srqingchen/dicefate/item/UnidentifiedDieItem.java'
s = open(p, encoding='utf-8').read()
s = s.replace('    @Override\n    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {',
              NEW_TOOLTIP_SIG)
s = s.replace('lines.add(Component.translatable("tooltip.dicefate.unidentified").withStyle(net.minecraft.ChatFormatting.GRAY));',
              'builder.accept(Component.translatable("tooltip.dicefate.unidentified").withStyle(net.minecraft.ChatFormatting.GRAY));')
s = s.replace('import java.util.List;\n', '')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('UnidentifiedDieItem tooltip ok')

# PackItem tooltip
p = 'src/main/java/com/srqingchen/dicefate/item/PackItem.java'
s = open(p, encoding='utf-8').read()
s = s.replace('    @Override\n    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {',
              NEW_TOOLTIP_SIG)
s = s.replace('lines.add(Component.translatable("tooltip.dicefate.pack").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));',
              'builder.accept(Component.translatable("tooltip.dicefate.pack").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));')
s = s.replace('import java.util.List;\n', '')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('PackItem tooltip ok')

# DiceRoller: exhausted 参数
p = 'src/main/java/com/srqingchen/dicefate/combat/DiceRoller.java'
s = open(p, encoding='utf-8').read()
s = s.replace('DieRollView view = rollSlot(sv, rng);', 'DieRollView view = rollSlot(sv, snap.exhausted, rng);')
s = s.replace('public static DieRollView rollSlot(SlotView sv, RandomSource rng) {',
              'public static DieRollView rollSlot(SlotView sv, boolean poolExhausted, RandomSource rng) {')
s = s.replace('if (sv.exhausted || sv.frozen) {', 'if (poolExhausted || sv.frozen) {')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('DiceRoller ok')

# PoiseService
p = 'src/main/java/com/srqingchen/dicefate/combat/PoiseService.java'
s = open(p, encoding='utf-8').read()
s = s.replace('.registryOrThrow(Registries.DAMAGE_TYPE)', '.lookupOrThrow(Registries.DAMAGE_TYPE)')
s = s.replace('CombatTextPayload.TextType.POISE_BREAK.ordinal()', '(byte) CombatTextPayload.TextType.POISE_BREAK.ordinal()')
s = s.replace('CombatTextPayload.TextType.FATEBREAK.ordinal()', '(byte) CombatTextPayload.TextType.FATEBREAK.ordinal()')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('PoiseService ok')

# DiceLookup import DiceItem
p = 'src/main/java/com/srqingchen/dicefate/dice/DiceLookup.java'
s = open(p, encoding='utf-8').read()
s = s.replace('import com.srqingchen.dicefate.registry.DiceItems;',
              'import com.srqingchen.dicefate.item.DiceItem;\nimport com.srqingchen.dicefate.registry.DiceItems;')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('DiceLookup ok')

# DiceFate import DFNetwork
p = 'src/main/java/com/srqingchen/dicefate/DiceFate.java'
s = open(p, encoding='utf-8').read()
if 'import com.srqingchen.dicefate.network.DFNetwork;' not in s:
    s = s.replace('import com.srqingchen.dicefate.data.DFRegistries;',
                  'import com.srqingchen.dicefate.data.DFRegistries;\nimport com.srqingchen.dicefate.network.DFNetwork;')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('DiceFate ok')

# DieProjectile pickup 字段
p = 'src/main/java/com/srqingchen/dicefate/entity/projectile/DieProjectile.java'
s = open(p, encoding='utf-8').read()
s = s.replace('this.setPickup(Pickup.DISALLOWED);', 'this.pickup = Pickup.DISALLOWED;')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('DieProjectile ok')

# BossEntity 存档 API + profile 查询
p = 'src/main/java/com/srqingchen/dicefate/entity/boss/BossEntity.java'
s = open(p, encoding='utf-8').read()
old_save = '''    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("PhaseTwo", phaseTwo);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        phaseTwo = tag.getBoolean("PhaseTwo");
        if (this.hasCustomName()) this.bossEvent.setName(this.getDisplayName());
    }'''
new_save = '''    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("PhaseTwo", phaseTwo);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        phaseTwo = input.getBooleanOr("PhaseTwo", false);
        if (this.hasCustomName()) this.bossEvent.setName(this.getDisplayName());
    }'''
s = s.replace(old_save, new_save)
s = s.replace('import net.minecraft.nbt.CompoundTag;\n', '')
old_lookup = '''        Optional<HolderLookup.RegistryLookup<PoolProfile>> reg = provider.lookup(DFRegistries.POOL_PROFILE);
        if (reg.isPresent()) {
            var held = reg.get().get(net.minecraft.resources.ResourceKey.create(DFRegistries.POOL_PROFILE, profileId()));
            if (held.isPresent()) {
                cachedProfile = held.get().value();
                return cachedProfile;
            }
        }
        return null;'''
new_lookup = '''        var key = net.minecraft.resources.ResourceKey.create(DFRegistries.POOL_PROFILE, profileId());
        PoolProfile p = provider.lookup(DFRegistries.POOL_PROFILE)
            .flatMap(reg -> reg.get(key))
            .map(java.util.function.Supplier::get)
            .orElse(null);
        cachedProfile = p;
        return p;'''
s = s.replace(old_lookup, new_lookup)
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('BossEntity ok')
