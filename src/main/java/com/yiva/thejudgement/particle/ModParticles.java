package com.yiva.thejudgement.particle;

import com.yiva.thejudgement.TheJudgement;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, TheJudgement.MOD_ID);

    // 能量飘散粒子（持续阶段）
    public static final RegistryObject<SimpleParticleType> ENERGY_SPARK =
            PARTICLES.register("energy_spark", () -> new SimpleParticleType(false));

    // 深渊残留微粒（蓄力与消散阶段）
    public static final RegistryObject<SimpleParticleType> ABYSS_MOTE =
            PARTICLES.register("abyss_mote", () -> new SimpleParticleType(false));

    // 迸裂碎片粒子（消散阶段）
    public static final RegistryObject<SimpleParticleType> RIFT_SHARD =
            PARTICLES.register("rift_shard", () -> new SimpleParticleType(false));

    public static void register(IEventBus modEventBus) {
        PARTICLES.register(modEventBus);
        TheJudgement.LOGGER.info("Particles registered!");
    }
}
