# -*- coding: utf-8 -*-
"""修复第 7 批：cooldown by Identifier、物品注册表查询、SnapshotFactory imports。"""

# ContestService: cooldown 用 Identifier 组
p = 'src/main/java/com/srqingchen/dicefate/combat/ContestService.java'
s = open(p, encoding='utf-8').read()
s = s.replace('''        player.getCooldowns().addCooldown(net.minecraft.world.item.Items.SHIELD, ticks);
        player.getCooldowns().addCooldown(com.srqingchen.dicefate.registry.GearItems.FATE_SHIELD_BONE.get(), ticks);
        player.getCooldowns().addCooldown(com.srqingchen.dicefate.registry.GearItems.FATE_SHIELD_MITHRIL.get(), ticks);
        player.getCooldowns().addCooldown(com.srqingchen.dicefate.registry.GearItems.FATE_SHIELD_FATEGOLD.get(), ticks);''',
'''        var cds = player.getCooldowns();
        cds.addCooldown(net.minecraft.resources.Identifier.withDefaultNamespace("minecraft:shield"), ticks);
        cds.addCooldown(com.srqingchen.dicefate.DiceFate.id("fate_shield_bone"), ticks);
        cds.addCooldown(com.srqingchen.dicefate.DiceFate.id("fate_shield_mithril"), ticks);
        cds.addCooldown(com.srqingchen.dicefate.DiceFate.id("fate_shield_fategold"), ticks);''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('cooldown ok')

# CombatEvents: 物品查询改 Optional<Reference> 处理
p = 'src/main/java/com/srqingchen/dicefate/event/CombatEvents.java'
s = open(p, encoding='utf-8').read()
s = s.replace('''        var rng = mob.getRandom();
        for (var drop : profile.drops()) {
            if (rng.nextDouble() < drop.chance()) {
                int count = drop.min() + rng.nextInt(drop.max() - drop.min() + 1);
                if (count <= 0) continue;
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.Identifier.parse(drop.item()));
                if (item == null || item == net.minecraft.core.registries.BuiltInRegistries.ITEM.byId(0) && !drop.item().equals("minecraft:air")) {
                    // 自定义物品走 dicefate 命名空间
                    var id = net.minecraft.resources.Identifier.parse(drop.item().startsWith("dicefate:") ? drop.item() : "dicefate:" + drop.item());
                    item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
                }
                if (item != null) {
                    event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(
                        level, mob.getX(), mob.getY() + 0.5, mob.getZ(), new ItemStack(item, count)));
                }
            }
        }''',
'''        var rng = mob.getRandom();
        var itemReg = net.minecraft.core.registries.BuiltInRegistries.ITEM;
        for (var drop : profile.drops()) {
            if (rng.nextDouble() < drop.chance()) {
                int count = drop.min() + rng.nextInt(Math.max(1, drop.max() - drop.min() + 1));
                if (count <= 0) continue;
                String raw = drop.item().contains(":") ? drop.item() : "dicefate:" + drop.item();
                var item = itemReg.get(net.minecraft.resources.Identifier.parse(raw));
                if (item.isPresent()) {
                    event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(
                        level, mob.getX(), mob.getY() + 0.5, mob.getZ(), new ItemStack(item.get(), count)));
                }
            }
        }''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('drops ok')

# SnapshotFactory 缺失导入
p = 'src/main/java/com/srqingchen/dicefate/combat/SnapshotFactory.java'
s = open(p, encoding='utf-8').read()
if 'import com.srqingchen.dicefate.dice.DieState;' not in s:
    s = s.replace('import com.srqingchen.dicefate.dice.DieDefinition;',
                  'import com.srqingchen.dicefate.dice.DieDefinition;\nimport com.srqingchen.dicefate.dice.DieState;')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('SnapshotFactory imports ok')
