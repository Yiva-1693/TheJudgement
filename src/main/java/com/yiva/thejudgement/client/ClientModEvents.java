package com.yiva.thejudgement.client;

import com.yiva.thejudgement.TheJudgement;
import com.yiva.thejudgement.client.particle.AbyssMoteParticle;
import com.yiva.thejudgement.client.particle.EnergySparkParticle;
import com.yiva.thejudgement.client.particle.RiftShardParticle;
import com.yiva.thejudgement.client.renderer.BeamRenderer;
import com.yiva.thejudgement.entity.ModEntities;
import com.yiva.thejudgement.particle.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TheJudgement.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BEAM.get(), BeamRenderer::new);
        TheJudgement.LOGGER.info("BeamRenderer registered!");
    }

    // 新增：注册粒子工厂
    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.ENERGY_SPARK.get(), EnergySparkParticle.Provider::new);
        event.registerSpriteSet(ModParticles.ABYSS_MOTE.get(), AbyssMoteParticle.Provider::new);
        event.registerSpriteSet(ModParticles.RIFT_SHARD.get(), RiftShardParticle.Provider::new);
        TheJudgement.LOGGER.info("Particle providers registered!");
    }
}
