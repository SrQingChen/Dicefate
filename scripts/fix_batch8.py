# -*- coding: utf-8 -*-
"""客户端 API 修复：KeyMapping.Category、Matrix4fc、accessor、import。"""

# 1) DFClient KeyMapping
p = 'src/main/java/com/srqingchen/dicefate/client/DFClient.java'
s = open(p, encoding='utf-8').read()
s = s.replace('''    public static final KeyMapping CASHOUT_KEY = new KeyMapping(
        "key.dicefate.cashout", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.dicefate");
    public static final KeyMapping SPIRIT_KEY = new KeyMapping(
        "key.dicefate.spirit", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.dicefate");
    public static final KeyMapping BOARD_KEY = new KeyMapping(
        "key.dicefate.board", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.dicefate");''',
'''    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(DiceFate.id("main"));

    public static final KeyMapping CASHOUT_KEY = new KeyMapping(
        "key.dicefate.cashout", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping SPIRIT_KEY = new KeyMapping(
        "key.dicefate.spirit", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping BOARD_KEY = new KeyMapping(
        "key.dicefate.board", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('KeyMapping ok')

# 2) CombatTextOverlay drawInBatch: Pose → pose() Matrix4f（兼容 Matrix4fc 参数）+ float coords
p = 'src/main/java/com/srqingchen/dicefate/client/hud/CombatTextOverlay.java'
s = open(p, encoding='utf-8').read()
s = s.replace('pose.last(), buffers, Font.DisplayMode.SEE_THROUGH, 0, light);',
              'pose.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0, light);')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('CombatTextOverlay ok')

# PoiseBarOverlay 同样
p = 'src/main/java/com/srqingchen/dicefate/client/hud/PoiseBarOverlay.java'
s = open(p, encoding='utf-8').read()
s = s.replace('matrix, buffers, Font.DisplayMode.SEE_THROUGH, 0, light);',
              'matrix.pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0, light);')
s = s.replace('private static void fill(com.mojang.blaze3d.vertex.PoseStack.Pose pose, MultiBufferSource buffers,',
              'private static void fill(com.mojang.blaze3d.vertex.PoseStack.Pose pose, MultiBufferSource buffers,')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('PoiseBarOverlay ok')

# 3) FateStatusHud accessor
p = 'src/main/java/com/srqingchen/dicefate/client/hud/FateStatusHud.java'
s = open(p, encoding='utf-8').read()
s = s.replace('pool.base.get(', 'pool.base().get(')
s = s.replace('pool.add.isEmpty()', 'pool.add().isEmpty()')
s = s.replace('pool.mul.isEmpty()', 'pool.mul().isEmpty()')
s = s.replace('pool.skill0.isEmpty()', 'pool.skill0().isEmpty()')
s = s.replace('pool.skill1.isEmpty()', 'pool.skill1().isEmpty()')
s = s.replace('g.item(pool.add, x, sy);', 'g.item(pool.add(), x, sy);')
s = s.replace('g.item(pool.mul, x, sy);', 'g.item(pool.mul(), x, sy);')
s = s.replace('g.item(pool.skill0, x, sy);', 'g.item(pool.skill0(), x, sy);')
s = s.replace('g.item(pool.skill1, x, sy);', 'g.item(pool.skill1(), x, sy);')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('FateStatusHud ok')

# 4) FateBoardScreen import
p = 'src/main/java/com/srqingchen/dicefate/client/screen/FateBoardScreen.java'
s = open(p, encoding='utf-8').read()
s = s.replace('import com.srqingchen.dicefate.client.ClientFateData;',
              'import com.srqingchen.dicefate.client.ClientFateData;\nimport com.srqingchen.dicefate.network.PoolSyncPayload;')
s = s.replace('ClientFateData.PoolSyncPayload pool', 'PoolSyncPayload pool')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('FateBoardScreen ok')
