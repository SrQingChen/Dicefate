package com.srqingchen.dicefate.client.screen;

import com.srqingchen.dicefate.DiceFate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * 0.2.0 命盘背包标签页（规划 8.8）：
 * ScreenEvent.Init.Post 向原版 InventoryScreen 注入"命盘"标签按钮（样式类似 Curios），
 * 点击切换到 FateBoardScreen；FateBoardScreen 内有对称的返回按钮。
 * Init.Post 的 addListener → Screen.addEventWidget 会同时加入 renderables，按钮可渲染可交互
 * （已核对 26.1.2 patched Screen 反编译）。
 */
@EventBusSubscriber(modid = DiceFate.MODID, value = Dist.CLIENT)
public final class InventoryTabInjector {

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen invScreen)) return;
        Minecraft mc = invScreen.getMinecraft();
        if (mc.player == null) return;
        // 0.3.0：位置改为面板左缘竖标签（原 width/2+88 半悬面板外且配方书展开时压书；
        // 策划 8.8"角色预览左侧/配方书按钮对侧"）
        int panelX = (invScreen.width - 176) / 2;
        int panelY = (invScreen.height - 166) / 2;
        event.addListener(Button.builder(
                com.srqingchen.dicefate.client.screen.TabButton.label(),
                b -> mc.setScreen(new FateBoardScreen()))
            .bounds(panelX - 24, panelY + 6, 22, 24)
            .build());
    }

    private InventoryTabInjector() {}
}
