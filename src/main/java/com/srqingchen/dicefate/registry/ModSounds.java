package com.srqingchen.dicefate.registry;

import com.srqingchen.dicefate.DiceFate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 0.2.0 音效（策划 8.9）：落定金属冲击、Miss、破势、命破、兑现、牌型、掷骰。
 * ogg 由 scripts/gen_sounds_020.py 合成，sounds.json 引用。
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(Registries.SOUND_EVENT, DiceFate.MODID);

    public static final Holder<SoundEvent> SETTLE_IMPACT = register("settle_impact");
    public static final Holder<SoundEvent> SETTLE_IMPACT_GREAT = register("settle_impact_great");
    public static final Holder<SoundEvent> CONTEST_MISS = register("contest_miss");
    public static final Holder<SoundEvent> POISE_BREAK = register("poise_break");
    public static final Holder<SoundEvent> FATEBREAK = register("fatebreak");
    public static final Holder<SoundEvent> CASHOUT = register("cashout");
    public static final Holder<SoundEvent> PATTERN = register("pattern");
    public static final Holder<SoundEvent> DICE_ROLL = register("dice_roll");

    private static Holder<SoundEvent> register(String name) {
        Supplier<SoundEvent> sup = () -> SoundEvent.createVariableRangeEvent(DiceFate.id(name));
        DeferredHolder<SoundEvent, SoundEvent> holder = SOUNDS.register(name, sup);
        return holder;
    }

    private ModSounds() {}
}
