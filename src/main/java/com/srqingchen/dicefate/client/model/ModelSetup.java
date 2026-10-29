package com.srqingchen.dicefate.client.model;

import com.srqingchen.dicefate.DiceFate;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/** 自定义模型加载器注册（dicefate:mesh）。 */
@EventBusSubscriber(modid = DiceFate.MODID, value = Dist.CLIENT)
public final class ModelSetup {

    @SubscribeEvent
    public static void onRegisterLoaders(ModelEvent.RegisterLoaders event) {
        event.register(DiceFate.id("mesh"), new MeshModelLoader());
    }

    private ModelSetup() {}
}
