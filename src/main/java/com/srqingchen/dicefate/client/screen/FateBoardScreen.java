package com.srqingchen.dicefate.client.screen;

import com.srqingchen.dicefate.client.ClientFateData;
import com.srqingchen.dicefate.network.PoolSyncPayload;
import com.srqingchen.dicefate.item.DiceItem;
import com.srqingchen.dicefate.network.ClientActionPayload;
import com.srqingchen.dicefate.network.FateStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 命盘界面（背包顶部标签页 / B 键）：
 * 左侧角色概览 + 基础骰位网格 + 加算/乘算/技能×2 专属槽；下方背包网格点击装配。
 * 客户端展示 + 意图包；服务端全量校验。
 * 0.2.0：技能槽协议 20/21→0/1 修复（原永远装不上）；专属槽二连击卸下；
 * 槽位间交换（SWAP_BASE a*10+b）；战斗中灰化只读；返回背包按钮；左侧概览。
 */
public class FateBoardScreen extends Screen {

    private static final int SLOT = 18;
    private int leftPos, topPos;
    private int selectedSlot = -1;   // -1=无选择；0-5 基础；10 加算；11 乘算；20/21 技能（显示用）

    public FateBoardScreen() {
        super(Component.translatable("screen.dicefate.board"));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - 220) / 2;
        this.topPos = (this.height - 166) / 2;
        // 返回背包（与标签页注入对称）
        addRenderableWidget(Button.builder(Component.translatable("screen.dicefate.back_to_inventory"),
                b -> minecraft.setScreen(new InventoryScreen(minecraft.player)))
            .bounds(this.width / 2 - 60, this.topPos - 22, 120, 16)
            .build());

        // 0.3.0：姿态四选一（服务端校验 CD/硬直；原 SET_STANCE 死协议无 UI 入口）
        int sy0 = this.topPos + 118;
        String[] stances = {"均衡", "背水", "守势", "诡道"};
        for (int i = 0; i < 4; i++) {
            final int ord = i;
            addRenderableWidget(Button.builder(Component.literal(stances[i]), b -> {
                sendOp(ClientActionPayload.Op.SET_STANCE, (byte) ord);
            }).bounds(this.leftPos + 6 + i * 54, sy0, 50, 14).build());
        }
        // 0.3.0：预设 保存/读取 ×3（原 SAVE/LOAD_PRESET 死协议无 UI 入口）
        int py0 = this.topPos + 136;
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            addRenderableWidget(Button.builder(Component.literal("存" + (i + 1)), b ->
                sendOp(ClientActionPayload.Op.SAVE_PRESET, (byte) idx))
                .bounds(this.leftPos + 6 + i * 74, py0, 34, 14).build());
            addRenderableWidget(Button.builder(Component.literal("取" + (i + 1)), b ->
                sendOp(ClientActionPayload.Op.LOAD_PRESET, (byte) idx))
                .bounds(this.leftPos + 6 + i * 74 + 36, py0, 34, 14).build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.text(this.font, this.title, leftPos, topPos - 12, 0xFFD9A441, false);

        PoolSyncPayload pool = ClientFateData.pool;
        FateStatePayload st = ClientFateData.fateState;
        if (pool == null) return;
        int baseSlots = st == null ? 6 : st.meta().baseSlots();
        boolean inCombat = st != null && st.meta().inCombat();
        if (inCombat) {
            g.fill(leftPos - 4, topPos - 2, leftPos + 224, topPos + 168, 0x60202028);
            g.text(this.font, Component.translatable("screen.dicefate.combat_locked"),
                leftPos + 60, topPos + 78, 0xFFFF9A6A, false);
        }

        // —— 左侧角色概览（0.3.0：clamp 防小分辨率出屏） ——
        int ox = Math.max(2, leftPos - 86);
        g.text(this.font, Component.translatable("screen.dicefate.overview"), ox, topPos + 6, 0xFFD9A441, false);
        int tier = baseSlots;   // 命盘阶=骰位数
        g.text(this.font, Component.translatable("screen.dicefate.pool_tier", tier), ox, topPos + 20, 0xFFE8E8E8, false);
        String stance = stanceName(st == null ? 0 : st.meta().stanceOrdinal());
        g.text(this.font, Component.translatable("screen.dicefate.stance_line", stance), ox, topPos + 31, 0xFFB8C7D9, false);
        int equipped = 0;
        for (ItemStack s : pool.base()) if (!s.isEmpty()) equipped++;
        g.text(this.font, Component.translatable("screen.dicefate.equipped_line", equipped, baseSlots),
            ox, topPos + 42, 0xFFB8C7D9, false);
        if (st != null && st.extras() != null && !st.extras().pack().isEmpty()) {
            g.text(this.font, Component.translatable("screen.dicefate.pack_line", st.extras().packRemaining()),
                ox, topPos + 53, 0xFF7FE3B2, false);
        }

        // —— 基础骰位 ——
        for (int i = 0; i < 6; i++) {
            int x = leftPos + 8 + (i % 3) * SLOT;
            int y = topPos + 6 + (i / 3) * SLOT;
            drawDieSlot(g, x, y, i < baseSlots ? pool.base().get(i) : ItemStack.EMPTY, i < baseSlots, i == selectedSlot, inCombat);
            // 0.3.0：定命中的骰显示金色"定"角标
            if (st != null && st.extras() != null && st.extras().lockSlot() == i) {
                g.text(this.font, "定", x + 11, y + 1, 0xFFFFD966, false);
            }
        }
        // —— 专属槽 ——
        int sy = topPos + 6 + 2 * SLOT + 8;
        drawDieSlot(g, leftPos + 8, sy, pool.add(), st != null && st.meta().addUnlocked(), selectedSlot == 10, inCombat);
        drawDieSlot(g, leftPos + 8 + SLOT, sy, pool.mul(), st != null && st.meta().mulUnlocked(), selectedSlot == 11, inCombat);
        int skillSlots = st == null ? 0 : st.meta().skillSlots();
        drawDieSlot(g, leftPos + 8 + 2 * SLOT, sy, pool.skill0(), skillSlots > 0, selectedSlot == 20, inCombat);
        drawDieSlot(g, leftPos + 8 + 3 * SLOT, sy, pool.skill1(), skillSlots > 1, selectedSlot == 21, inCombat);
        // 专属槽小标签
        g.text(this.font, "加", leftPos + 8 + 13, sy + 4, 0xFF7A7A8A, false);
        g.text(this.font, "乘", leftPos + 8 + SLOT + 13, sy + 4, 0xFF7A7A8A, false);

        // —— 背包网格 ——
        renderInventory(g, mouseX, mouseY);
    }

    private void drawDieSlot(GuiGraphicsExtractor g, int x, int y, ItemStack stack, boolean unlocked, boolean selected, boolean locked) {
        g.fill(x - 1, y - 1, x + SLOT - 1, y + SLOT - 1, selected ? 0xFFD9A441 : locked ? 0xFF2E2E3A : 0xFF3A3A4A);
        g.fill(x, y, x + SLOT - 2, y + SLOT - 2, unlocked ? 0xFF1B1B26 : 0xFF121218);
        if (!stack.isEmpty()) {
            g.item(stack, x + 1, y + 1);
        } else if (!unlocked) {
            g.text(this.font, "×", x + 5, y + 5, 0xFF666677, false);
        }
    }

    private void renderInventory(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Inventory inv = minecraft.player.getInventory();
        int bx = leftPos + 70;
        int by = topPos + 6;
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
        // 热键栏
        for (int col = 0; col < 9; col++) {
            int x = bx + col * SLOT;
            int y = by + 3 * SLOT + 6;
            ItemStack s = inv.getItem(col);
            g.fill(x - 1, y - 1, x + SLOT - 1, y + SLOT - 1, 0xFF2A2A38);
            g.fill(x, y, x + SLOT - 2, y + SLOT - 2, 0xFF15151E);
            if (!s.isEmpty()) {
                g.item(s, x + 1, y + 1);
                if (s.getItem() instanceof DiceItem && isHovering(mouseX, mouseY, x, y)) {
                    g.setComponentTooltipForNextFrame(this.font, Screen.getTooltipFromItem(minecraft, s), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        PoolSyncPayload pool = ClientFateData.pool;
        FateStatePayload st = ClientFateData.fateState;
        if (pool == null) return super.mouseClicked(event, doubleClick);
        int baseSlots = st == null ? 6 : st.meta().baseSlots();
        boolean inCombat = st != null && st.meta().inCombat();

        if (!inCombat && event.button() == 1) {
            // 0.3.0：右键基础骰位 → 定命（策划 3.6"命盘界面右键一颗骰子→定命"）
            for (int i = 0; i < 6; i++) {
                int x = leftPos + 8 + (i % 3) * SLOT;
                int y = topPos + 6 + (i / 3) * SLOT;
                if (isHovering((int) mouseX, (int) mouseY, x, y) && i < baseSlots) {
                    sendOp(ClientActionPayload.Op.LOCK_DIE, (byte) i);
                    return true;
                }
            }
        }

        if (!inCombat) {
            // 点击背包中的骰 → 装入选中的骰位
            Inventory inv = minecraft.player.getInventory();
            int bx = leftPos + 70;
            int by = topPos + 6;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    int idx = 9 + row * 9 + col;
                    int x = bx + col * SLOT;
                    int y = by + row * SLOT;
                    if (isHovering((int) mouseX, (int) mouseY, x, y)) {
                        tryEquip(inv.getItem(idx));
                        return true;
                    }
                }
            }
            for (int col = 0; col < 9; col++) {
                int x = bx + col * SLOT;
                int y = by + 3 * SLOT + 6;
                if (isHovering((int) mouseX, (int) mouseY, x, y)) {
                    tryEquip(inv.getItem(col));
                    return true;
                }
            }
        }

        // 点击基础骰位 → 选中/交换/卸下
        for (int i = 0; i < 6; i++) {
            int x = leftPos + 8 + (i % 3) * SLOT;
            int y = topPos + 6 + (i / 3) * SLOT;
            if (isHovering((int) mouseX, (int) mouseY, x, y) && i < baseSlots) {
                if (inCombat) return true;
                if (selectedSlot == i) {
                    sendOp(ClientActionPayload.Op.UNEQUIP_BASE, (byte) i);
                    selectedSlot = -1;
                } else if (selectedSlot >= 0 && selectedSlot < 6) {
                    // 槽位交换（0.2.0：SWAP_BASE a*10+b）
                    sendOp(ClientActionPayload.Op.SWAP_BASE, (byte) (selectedSlot * 10 + i));
                    selectedSlot = -1;
                } else {
                    selectedSlot = i;
                }
                return true;
            }
        }
        // 专属/技能槽：选中→再点卸下（0.2.0 补 UNEQUIP_*）
        int sy = topPos + 6 + 2 * SLOT + 8;
        int[][] special = {{leftPos + 8, 10}, {leftPos + 8 + SLOT, 11},
                           {leftPos + 8 + 2 * SLOT, 20}, {leftPos + 8 + 3 * SLOT, 21}};
        for (int[] spec : special) {
            if (isHovering((int) mouseX, (int) mouseY, spec[0], sy)) {
                if (inCombat) return true;
                if (selectedSlot == spec[1]) {
                    ClientActionPayload.Op unequip = switch (spec[1]) {
                        case 10 -> ClientActionPayload.Op.UNEQUIP_ADD;
                        case 11 -> ClientActionPayload.Op.UNEQUIP_MUL;
                        default -> ClientActionPayload.Op.UNEQUIP_SKILL;
                    };
                    sendOp(unequip, (byte) spec[1]);
                    selectedSlot = -1;
                } else {
                    selectedSlot = spec[1];
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void tryEquip(ItemStack stack) {
        if (!(stack.getItem() instanceof DiceItem dice)) return;
        if (selectedSlot < 0) {
            // 0.3.0：未选槽位给反馈（原静默返回）
            if (minecraft.player != null) {
                minecraft.gui.setOverlayMessage(
                    Component.translatable("message.dicefate.select_slot_first"), true);
            }
            return;
        }
        var def = dice.definition(minecraft.level);
        if (def == null) return;
        ClientActionPayload.Op op = switch (selectedSlot) {
            case 10 -> ClientActionPayload.Op.EQUIP_ADD;
            case 11 -> ClientActionPayload.Op.EQUIP_MUL;
            case 20, 21 -> ClientActionPayload.Op.EQUIP_SKILL;
            default -> ClientActionPayload.Op.EQUIP_BASE;
        };
        // 0.2.0：技能槽显示编号 20/21 → 协议编号 0/1（原协议错位致永远装不上）
        byte slot = (byte) (selectedSlot >= 20 ? selectedSlot - 20 : selectedSlot);
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().send(new ClientActionPayload((byte) op.ordinal(), slot, stack.copy()));
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

    private static String stanceName(int ordinal) {
        return switch (ordinal) {
            case 1 -> "背水";
            case 2 -> "守势";
            case 3 -> "诡道";
            default -> "均衡";
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
