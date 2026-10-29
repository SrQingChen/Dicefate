package com.srqingchen.dicefate.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.srqingchen.dicefate.DiceFate;
import com.srqingchen.dicefate.client.hud.ContestTrackHud;
import com.srqingchen.dicefate.client.hud.FateStatusHud;
import com.srqingchen.dicefate.client.screen.FateBoardScreen;
import com.srqingchen.dicefate.network.ClientActionPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.DeltaTracker;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端装配：HUD 图层、按键（V 兑现 / G 骰灵 / B 命盘）、头顶条带与飘字。
 */
@EventBusSubscriber(modid = DiceFate.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class DFClient {

    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(DiceFate.id("main"));

    public static final KeyMapping CASHOUT_KEY = new KeyMapping(
        "key.dicefate.cashout", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping SPIRIT_KEY = new KeyMapping(
        "key.dicefate.spirit", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping BOARD_KEY = new KeyMapping(
        "key.dicefate.board", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);   // 0.3.0：注册分类（否则控制列表排序 indexOf=-1 显示原始键）
        event.register(CASHOUT_KEY);
        event.register(SPIRIT_KEY);
        event.register(BOARD_KEY);
    }

    @SubscribeEvent
    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(DiceFate.id("fate_status"), DFClient::renderStatus);
        event.registerAboveAll(DiceFate.id("contest_tracks"), DFClient::renderTracks);
    }

    private static void renderStatus(GuiGraphicsExtractor g, DeltaTracker delta) {
        FateStatusHud.render(g, delta);
    }

    private static void renderTracks(GuiGraphicsExtractor g, DeltaTracker delta) {
        ContestTrackHud.render(g, delta);
    }

    /** 0.2.0：退世界/换服清空客户端缓存（实体 ID 复用会错挂）。 */
    @SubscribeEvent
    public static void onLoggingOut(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        ClientFateData.clearAll();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        while (CASHOUT_KEY.consumeClick()) {
            send(ClientActionPayload.Op.CASHOUT, (byte) 0);
        }
        while (SPIRIT_KEY.consumeClick()) {
            send(ClientActionPayload.Op.SPIRIT_TOGGLE, (byte) 0);
        }
        while (BOARD_KEY.consumeClick()) {
            mc.setScreen(new FateBoardScreen());
        }
    }

    private static void send(ClientActionPayload.Op op, byte slot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            mc.getConnection().send(new ClientActionPayload((byte) op.ordinal(), slot,
                net.minecraft.world.item.ItemStack.EMPTY));
        }
    }

    private DFClient() {}
}
