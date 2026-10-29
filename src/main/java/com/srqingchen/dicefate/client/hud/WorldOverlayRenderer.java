package com.srqingchen.dicefate.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.srqingchen.dicefate.client.ClientFateData;
import com.srqingchen.dicefate.network.CombatTextPayload;
import com.srqingchen.dicefate.network.PoiseSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import com.srqingchen.dicefate.DiceFate;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/**
 * 26.1 渲染管线下的世界空间叠加：
 * 头顶命势条（submitCustomGeometry）+ 飘字/命势数字（submitText）。
 * 0.2.0：按实体 ID 直查命势条目与飘字（替代原"距离匹配"的错挂/漏挂）；
 * 飘字与命势条解耦（无条目实体也渲染飘字）；条高取实体碰撞箱高；
 * 飘字计时改 gameTime（暂停不再"提前飘完"）；Miss/命破大字号。
 */
@EventBusSubscriber(modid = DiceFate.MODID, value = Dist.CLIENT)
public final class WorldOverlayRenderer {

    private static boolean renderProbeLogged = false;

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<net.minecraft.world.entity.LivingEntity, LivingEntityRenderState, ?> event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.level == null) return;
        LivingEntityRenderState state = event.getRenderState();
        long now = mc.level.getGameTime();

        PoiseSyncPayload.Entry entry = matchEntry(state, now);

        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        PoseStack pose = event.getPoseStack();
        if (!renderProbeLogged) {
            renderProbeLogged = true;
            com.srqingchen.dicefate.util.DFDebug.always("世界渲染事件到达（RenderLivingEvent.Post 活体，飘字/头顶条渲染器运行）");
        }

        // 头顶高度：渲染状态碰撞箱高 + 0.15（幼体按 ageScale 缩放）
        float top = Math.min(3.2F, state.boundingBoxHeight + 0.15F);
        if (state.ageScale != 1.0F) top *= state.ageScale;

        // —— 命势条（自定义几何；仅有条目时） ——
        if (entry != null) {
            float w = 0.62F, h = 0.075F;
            int color = entry.state() == 1 ? 0xFF9A5CD4 : entry.state() == 2 ? 0xFF5C7A9A : 0xFFD9A441;
            float frac = entry.maxPoise() <= 0 ? 0 : entry.poise() / (float) entry.maxPoise();
            pose.pushPose();
            pose.translate(0, top, 0);
            pose.mulPose(cameraOrientation());
            submitBar(collector, pose, -w / 2, -h / 2, w, h, frac, color);
            pose.popPose();

            // 命定细条
            if (entry.state() == 1 && entry.decreeThreshold() > 0) {
                pose.pushPose();
                pose.translate(0, top - 0.1F, 0);
                pose.mulPose(cameraOrientation());
                float dfrac = Math.min(1, entry.decree() / (float) entry.decreeThreshold());
                submitBar(collector, pose, -w / 2, -0.035F, w, 0.055F, dfrac, 0xFF8E4E9E);
                pose.popPose();
            }
        }

        // —— 飘字（类型+位置匹配；与命势条解耦） ——
        var texts = ClientFateData.textsNear(state.entityType, state.x, state.y, state.z, now);
        if (texts.isEmpty()) return;
        Font font = mc.font;
        int line = 0;
        for (ClientFateData.TimedText t : texts) {
            float age = Math.max(0, now - t.bornAt());
            if (age > 22) continue;
            float rise = age * 0.02F;
            int alpha = (int) (0xFF * Math.max(0.25F, 1.0F - age / 22.0F));
            CombatTextPayload.TextType type = CombatTextPayload.TextType.values()[t.payload().kind() & 0xFF];
            boolean big = type == CombatTextPayload.TextType.MISS
                || type == CombatTextPayload.TextType.FATEBREAK
                || type == CombatTextPayload.TextType.POISE_BREAK;
            String text = CombatTexts.format(t.payload());
            int argb = (alpha << 24) | (CombatTexts.color(t.payload()) & 0xFFFFFF);
            FormattedCharSequence seq = Component.literal(text).getVisualOrderText();
            pose.pushPose();
            pose.translate(0, top + 0.25F + rise + line * 0.2F, 0);
            pose.mulPose(cameraOrientation());
            // 0.4 修复：缩放符号对齐原版铭牌 (+s,-s,s)——原 (-,-,-) 造成镜像+背面剔除，飘字不可见
            float scale = big ? 0.032F : 0.02F;
            pose.scale(scale, -scale, scale);
            collector.submitText(pose,
                -font.width(seq) / 2f, 0, seq, true,
                Font.DisplayMode.SEE_THROUGH, state.lightCoords, argb, 0, 0);
            pose.popPose();
            line++;
        }
    }

    private static void submitBar(SubmitNodeCollector collector, PoseStack pose,
                                  float x0, float y0, float w, float h, float frac, int color) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        collector.submitCustomGeometry(pose, RenderTypes.debugQuads(), (p, buffer) -> {
            quad(p, buffer, x0 - 0.012F, y0 - 0.012F, x0 + w + 0.012F, y0 + h + 0.012F, 0.70F, 0.70F, 0.70F, 0.85F);
            quad(p, buffer, x0, y0, x0 + w, y0 + h, 0.08F, 0.08F, 0.12F, 0.9F);
            if (frac > 0) {
                quad(p, buffer, x0 + 0.004F, y0 + 0.004F, x0 + 0.004F + (w - 0.008F) * frac, y0 + h - 0.004F, r, g, b, 1.0F);
            }
        });
    }

    private static void quad(com.mojang.blaze3d.vertex.PoseStack.Pose pose, VertexConsumer buffer,
                             float x0, float y0, float x1, float y1, float r, float g, float b, float a) {
        buffer.addVertex(pose, x0, y1, 0).setColor(r, g, b, a);
        buffer.addVertex(pose, x1, y1, 0).setColor(r, g, b, a);
        buffer.addVertex(pose, x1, y0, 0).setColor(r, g, b, a);
        buffer.addVertex(pose, x0, y0, 0).setColor(r, g, b, a);
    }

    /** 26.1 渲染状态无实体 ID：按实体类型一致 + 位置 0.5 格内取最近条目（0.3.0：阈值 1.5→0.5，同种怪扎堆错挂率大幅下降）。 */
    private static PoiseSyncPayload.Entry matchEntry(LivingEntityRenderState state, long now) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        PoiseSyncPayload.Entry best = null;
        double bestD2 = 0.25;
        for (PoiseSyncPayload.Entry e : ClientFateData.poiseEntries.values()) {
            long age = now - e.gameTick();
            if (age < 0 || age >= 200) continue;
            var entity = mc.level.getEntity(e.entityId());
            if (entity == null || entity.getType() != state.entityType) continue;
            double dx = entity.getX() - state.x;
            double dy = entity.getY() - state.y;
            double dz = entity.getZ() - state.z;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < bestD2) {
                bestD2 = d2;
                best = e;
            }
        }
        return best;
    }

    private static org.joml.Quaternionf cameraOrientation() {
        Minecraft mc = Minecraft.getInstance();
        return new org.joml.Quaternionf(mc.gameRenderer.getMainCamera().rotation());
    }

    private WorldOverlayRenderer() {}
}
