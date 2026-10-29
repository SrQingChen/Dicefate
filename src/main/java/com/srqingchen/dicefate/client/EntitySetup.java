package com.srqingchen.dicefate.client;

import com.srqingchen.dicefate.DiceFate;
import com.srqingchen.dicefate.registry.ModEntities;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** 客户端实体渲染器装配。 */
@EventBusSubscriber(modid = DiceFate.MODID, value = Dist.CLIENT)
public final class EntitySetup {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.TWO_FACED_GAMBLER.get(),
            ctx -> new BossRenderer(ctx, DiceFate.id("textures/entity/two_faced_gambler.png")));
        event.registerEntityRenderer(ModEntities.MOLTEN_DIE_KING.get(),
            ctx -> new BossRenderer(ctx, DiceFate.id("textures/entity/molten_die_king.png")));
        event.registerEntityRenderer(ModEntities.FROST_SILENCE_QUEEN.get(),
            ctx -> new BossRenderer(ctx, DiceFate.id("textures/entity/frost_silence_queen.png")));
        event.registerEntityRenderer(ModEntities.NULLFACE.get(),
            ctx -> new BossRenderer(ctx, DiceFate.id("textures/entity/nullface.png")));
        event.registerEntityRenderer(ModEntities.DIE_PROJECTILE.get(), DieProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.FATE_ARROW.get(), FateArrowRenderer::new);
    }

    /** 通用人形 Boss 渲染。 */
    public static class BossRenderer
            extends net.minecraft.client.renderer.entity.HumanoidMobRenderer<Mob, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
        private final Identifier texture;

        public BossRenderer(EntityRendererProvider.Context ctx, Identifier texture) {
            super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.6F);
            this.texture = texture;
        }

        @Override
        public HumanoidRenderState createRenderState() {
            return new HumanoidRenderState();
        }

        @Override
        public Identifier getTextureLocation(HumanoidRenderState state) {
            return texture;
        }
    }

    /** 骰弹渲染（0.2.0：ThrownItemRenderer 渲染真实骰子物品模型）。 */
    public static class DieProjectileRenderer
            extends ThrownItemRenderer<com.srqingchen.dicefate.entity.projectile.DieProjectile> {
        public DieProjectileRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, 0.8F, false);
        }
    }

    /** 命矢渲染（Arrow 具体态）。 */
    public static class FateArrowRenderer extends ArrowRenderer<com.srqingchen.dicefate.entity.projectile.FateArrow,
            net.minecraft.client.renderer.entity.state.ArrowRenderState> {
        public FateArrowRenderer(EntityRendererProvider.Context ctx) {
            super(ctx);
        }

        @Override
        public net.minecraft.client.renderer.entity.state.ArrowRenderState createRenderState() {
            return new net.minecraft.client.renderer.entity.state.ArrowRenderState();
        }

        @Override
        public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.ArrowRenderState state) {
            return Identifier.withDefaultNamespace("textures/entity/projectiles/arrow.png");
        }
    }

    private EntitySetup() {}
}
