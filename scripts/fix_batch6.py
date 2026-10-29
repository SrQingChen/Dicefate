# -*- coding: utf-8 -*-
"""修复第 6 批：PoolProfile.drops + kitGiven + 事件订阅注解。"""

# 1) PoolProfile 增加 drops 字段
p = 'src/main/java/com/srqingchen/dicefate/data/PoolProfile.java'
s = open(p, encoding='utf-8').read()
s = s.replace('''    List<Identifier> skillDice, // 空槽位即无
    Optional<Identifier> lootOverride
) {''',
'''    List<Identifier> skillDice, // 空槽位即无
    List<DropSpec> drops,
    Optional<Identifier> lootOverride
) {''')
s = s.replace('''    public record SlotSpec(int face, String element) {''',
'''    /** 额外掉落（命砂/命尘/精华等）。 */
    public record DropSpec(String item, int min, int max, double chance) {
        public static final Codec<DropSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("item").forGetter(DropSpec::item),
            Codec.intRange(0, 64).optionalFieldOf("min", 1).forGetter(DropSpec::min),
            Codec.intRange(0, 64).optionalFieldOf("max", 1).forGetter(DropSpec::max),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("chance", 1.0).forGetter(DropSpec::chance)
        ).apply(i, DropSpec::new));
    }

    public record SlotSpec(int face, String element) {''')
s = s.replace('''        Identifier.CODEC.listOf().optionalFieldOf("skill_dice", List.of()).forGetter(PoolProfile::skillDice),
        Identifier.CODEC.optionalFieldOf("loot_override").forGetter(PoolProfile::lootOverride)''',
'''        Identifier.CODEC.listOf().optionalFieldOf("skill_dice", List.<Identifier>of()).forGetter(PoolProfile::skillDice),
        DropSpec.CODEC.listOf().optionalFieldOf("drops", List.<DropSpec>of()).forGetter(PoolProfile::drops),
        Identifier.CODEC.optionalFieldOf("loot_override").forGetter(PoolProfile::lootOverride)''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('PoolProfile drops ok')

# 2) PlayerFateData kitGiven
p = 'src/main/java/com/srqingchen/dicefate/attachment/PlayerFateData.java'
s = open(p, encoding='utf-8').read()
s = s.replace('''    private int unidentifiedOpened;
''', '''    private int unidentifiedOpened;
    private boolean kitGiven;
''')
s = s.replace('''    public int unidentifiedOpened() { return unidentifiedOpened; }''',
'''    public int unidentifiedOpened() { return unidentifiedOpened; }
    public boolean isKitGiven() { return kitGiven; }
    public void markKitGiven() { kitGiven = true; }''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('kitGiven ok')

# 3) CombatEvents: @EventBusSubscriber + kit flag
p = 'src/main/java/com/srqingchen/dicefate/event/CombatEvents.java'
s = open(p, encoding='utf-8').read()
s = s.replace('public final class CombatEvents {',
'''@net.neoforged.fml.common.EventBusSubscriber(modid = com.srqingchen.dicefate.DiceFate.MODID)
public final class CombatEvents {''')
s = s.replace('''        if (!sp.getPersistentData().getBoolean("dicefate_init")) {
            sp.getPersistentData().putBoolean("dicefate_init", true);''',
'''        if (!data.isKitGiven()) {
            data.markKitGiven();''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('CombatEvents ok')
