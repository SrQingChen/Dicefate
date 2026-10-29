# -*- coding: utf-8 -*-
"""修复第 9 批：Util 包位置、tooltip、MouseButtonEvent、super.render。"""

# 1) WorldOverlayRenderer: Util 位置
p = 'src/main/java/com/srqingchen/dicefate/client/hud/WorldOverlayRenderer.java'
s = open(p, encoding='utf-8').read()
s = s.replace('import net.minecraft.Util;', 'import net.minecraft.util.Util;')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('Util ok')

# 2) FateBoardScreen: 适配 26.1 输入与渲染
p = 'src/main/java/com/srqingchen/dicefate/client/screen/FateBoardScreen.java'
s = open(p, encoding='utf-8').read()
s = s.replace('        super.render(g, mouseX, mouseY, partialTick);',
              '        super.extractRenderState(g, mouseX, mouseY, partialTick);')
s = s.replace('g.renderTooltip(this.font, s, mouseX, mouseY);',
              'g.tooltip(this.font, net.minecraft.client.gui.screens.Screen.getTooltipFromItem(minecraft, s), mouseX, mouseY);')
s = s.replace('''    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ClientFateData.PoolSyncPayload pool = ClientFateData.pool;''',
'''    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();''')
s = s.replace('PoolSyncPayload pool = ClientFateData.pool;\n        FateStatePayload st',
              'PoolSyncPayload pool = ClientFateData.pool;\n        FateStatePayload st')
# mouseClicked 内首个 pool 引用修复
s = s.replace('''        int baseSlots = st == null ? 6 : st.meta().baseSlots();

        // 点击背包中的骰 → 装入选中的骰位''',
'''        PoolSyncPayload pool = ClientFateData.pool;
        FateStatePayload st = ClientFateData.fateState;
        if (pool == null) return super.mouseClicked(event, doubleClick);
        int baseSlots = st == null ? 6 : st.meta().baseSlots();

        // 点击背包中的骰 → 装入选中的骰位''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('FateBoardScreen ok')
