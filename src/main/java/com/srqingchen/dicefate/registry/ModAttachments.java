package com.srqingchen.dicefate.registry;

import com.srqingchen.dicefate.DiceFate;
import com.srqingchen.dicefate.attachment.MobFateData;
import com.srqingchen.dicefate.attachment.PlayerFateData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 附件注册：玩家命数数据（持久化+死亡保留）、怪物命势（运行时）。
 */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, DiceFate.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerFateData>> PLAYER_FATE =
        ATTACHMENTS.register("player_fate", () -> AttachmentType.builder(PlayerFateData::new)
            .serialize(PlayerFateData.MAP_CODEC)
            .copyOnDeath()
            .build());

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<MobFateData>> MOB_FATE =
        ATTACHMENTS.register("mob_fate", () -> AttachmentType.builder(MobFateData::new)
            // 无 serialize：运行时数据，不落盘
            .build());

    private ModAttachments() {}
}
