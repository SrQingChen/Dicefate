# -*- coding: utf-8 -*-
"""26.1 API 重命名批量修复。"""
import glob

# ---- 1. DiceItem: appendHoverText 新签名 + specId→dieId ----
p = 'src/main/java/com/srqingchen/dicefate/item/DiceItem.java'
s = open(p, encoding='utf-8').read()
s = s.replace('.specId())', '.dieId)')
old_head = '''    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        DieState state = stack.get(ModDataComponents.DIE_STATE.get());
        DieDefinition def = ctx instanceof Level level
            ? definition(level)
            : DiceLookup.fromStatic(DiceFate.id(dieId));'''
new_head = '''    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.core.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> builder, TooltipFlag flag) {
        DieState state = stack.get(ModDataComponents.DIE_STATE.get());
        DieDefinition def = ctx instanceof Level level
            ? definition(level)
            : DiceLookup.fromStatic(dieId);'''
s = s.replace(old_head, new_head)
start = s.index('public void appendHoverText')
end = s.index('public boolean isFoil')
body = s[start:end]
body = body.replace('lines.add(', 'builder.accept(')
s = s[:start] + body + s[end:]
s = s.replace('import java.util.List;\n', '')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('DiceItem ok')

# ---- 2. InteractionResultHolder → InteractionResult ----
for p in glob.glob('src/main/java/com/srqingchen/dicefate/item/*.java'):
    s = open(p, encoding='utf-8').read()
    o = s
    s = s.replace('import net.minecraft.world.InteractionResultHolder;\n', 'import net.minecraft.world.InteractionResult;\n')
    s = s.replace('InteractionResultHolder<ItemStack> use(', 'InteractionResult use(')
    s = s.replace('return InteractionResultHolder.success(stack);', 'return InteractionResult.SUCCESS;')
    s = s.replace('return InteractionResultHolder.consume(stack);', 'return InteractionResult.CONSUME;')
    s = s.replace('return InteractionResultHolder.fail(stack);', 'return InteractionResult.FAIL;')
    s = s.replace('return InteractionResultHolder.pass(stack);', 'return InteractionResult.PASS;')
    if s != o:
        open(p, 'w', encoding='utf-8', newline='\n').write(s)
        print('use() fixed:', p)
