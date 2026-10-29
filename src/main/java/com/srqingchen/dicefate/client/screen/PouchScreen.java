package com.srqingchen.dicefate.client.screen;

import com.srqingchen.dicefate.client.ClientFateData;
import com.srqingchen.dicefate.item.DiceItem;
import com.srqingchen.dicefate.network.ClientActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 0.2.0 骰袋界面：6 格备用骰 + 背包网格。
 * 点击背包骰→存入选中格；点击已存格→取回背包。服务端全量校验（需佩戴骰袋）。
 */
public class PouchScreen extends Screen {

    private static final int SLOT = 18;
    private int leftPos, topPos;
    private int selectedSlot = -1;

    public static void open() {
        Minecraft.getInstance().setScreen(new PouchScreen());
    }

    public PouchScreen() {
        super(Component.translatable("screen.dicefate.pouch"));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - 176) / 2;
        this.topPos = (this.height - 166) / 2;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.text(this.font, this.title, leftPos, topPos - 12, 0xFF7FE3B2, false);
        var pouch = ClientFateData.pouch;
        for (int i = 0; i < 6; i++) {
            int x = leftPos + 8 + (i % 6) * SLOT;
            int y = topPos + 6;
            ItemStack s = pouch == null || i >= pouch.size() ? ItemStack.EMPTY : pouch.get(i);
            g.fill(x - 1, y - 1, x + SLOT - 1, y + SLOT - 1, selectedSlot == i ? 0xFF7FE3B2 : 0xFF3A3A4A);
            g.fill(x, y, x + SLOT - 2, y + SLOT - 2, 0xFF1B2620);
            if (!s.isEmpty()) {
                g.item(s, x + 1, y + 1);
                if (isHovering(mouseX, mouseY, x, y)) {
                    g.setComponentTooltipForNextFrame(this.font, Screen.getTooltipFromItem(minecraft, s), mouseX, mouseY);
                }
            }
        }
        renderInventory(g, mouseX, mouseY);
    }

    private void renderInventory(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Inventory inv = minecraft.player.getInventory();
        int bx = leftPos + 8;
        int by = topPos + 34;
        g.text(this.font, Component.translatable("screen.dicefate.backpack"), bx, by - 10, 0xFFB8C7D9, false);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = 9 + row * 9 + col;
                int x = bx + col * SLOT;
                int y = by + row * SLOT;
                g.fill(x - 1, y - 1, x + SLOT - 1, y + SLOT - 1, 0xFF2A2A38);
                g.fill(x, y, x + SLOT - 2, y + SLOT - 2, 0xFF15151E);
                ItemStack s = inv.getItem(idx);
                if (!s.isEmpty()) {
                    g.item(s, x + 1, y + 1);
                    if (s.getItem() instanceof DiceItem && isHovering(mouseX, mouseY, x, y)) {
                        g.setComponentTooltipForNextFrame(this.font, Screen.getTooltipFromItem(minecraft, s), mouseX, mouseY);
                    }
                }
            }
        }
        for (int col = 0; col < 9; col++) {
            int x = bx + col * SLOT;
            int y = by + 3 * SLOT + 6;
            ItemStack s = inv.getItem(col);
            g.fill(x - 1, y - 1, x + SLOT - 1, y + SLOT - 1, 0xFF2A2A38);
            g.fill(x, y, x + SLOT - 2, y + SLOT - 2, 0xFF15151E);
            if (!s.isEmpty()) g.item(s, x + 1, y + 1);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        // 骰袋格：选中/取回
        for (int i = 0; i < 6; i++) {
            int x = leftPos + 8 + i * SLOT;
            int y = topPos + 6;
            if (isHovering((int) mouseX, (int) mouseY, x, y)) {
                if (selectedSlot == i) {
                    sendOp(ClientActionPayload.Op.POUCH_TAKE, (byte) i);
                    selectedSlot = -1;
                } else {
                    selectedSlot = i;
                }
                return true;
            }
        }
        // 背包骰：存入选中格
        Inventory inv = minecraft.player.getInventory();
        int bx = leftPos + 8;
        int by = topPos + 34;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = 9 + row * 9 + col;
                int x = bx + col * SLOT;
                int y = by + row * SLOT;
                if (isHovering((int) mouseX, (int) mouseY, x, y)) {
                    store(inv.getItem(idx));
                    return true;
                }
            }
        }
        for (int col = 0; col < 9; col++) {
            int x = bx + col * SLOT;
            int y = by + 3 * SLOT + 6;
            if (isHovering((int) mouseX, (int) mouseY, x, y)) {
                store(inv.getItem(col));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void store(ItemStack stack) {
        if (selectedSlot < 0 || !(stack.getItem() instanceof DiceItem)) return;
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().send(new ClientActionPayload(
                (byte) ClientActionPayload.Op.POUCH_STORE.ordinal(), (byte) selectedSlot, stack.copy()));
        }
        selectedSlot = -1;
    }

    private void sendOp(ClientActionPayload.Op op, byte slot) {
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().send(new ClientActionPayload((byte) op.ordinal(), slot, ItemStack.EMPTY));
        }
    }

    private boolean isHovering(int mx, int my, int x, int y) {
        return mx >= x - 1 && mx < x + SLOT - 1 && my >= y - 1 && my < y + SLOT - 1;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
