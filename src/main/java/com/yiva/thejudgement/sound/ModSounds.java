package com.yiva.thejudgement.sound;

import com.yiva.thejudgement.TheJudgement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TheJudgement.MOD_ID);

    // 蓄力音效（4秒，可被打断）
    public static final RegistryObject<SoundEvent> BEAM_CHARGE =
            registerSound("beam_charge");

    // 发射/持续音效（3秒，覆盖第二+第三阶段）
    public static final RegistryObject<SoundEvent> BEAM_SHOT =
            registerSound("beam_shot");

    private static RegistryObject<SoundEvent> registerSound(String name) {
        // 使用 1.20.1 原有构造方法，兼容没有 bySeparator 的旧版 Forge。
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(TheJudgement.MOD_ID, name)
        ));
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
        TheJudgement.LOGGER.info("Sounds registered!");
    }
}
