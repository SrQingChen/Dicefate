package com.srqingchen.dicefate.client.screen;

import net.minecraft.network.chat.Component;

/** 标签按钮文案（单独小类便于本地化复用）。 */
final class TabButton {
    static Component label() {
        return Component.translatable("screen.dicefate.tab");
    }

    private TabButton() {}
}
