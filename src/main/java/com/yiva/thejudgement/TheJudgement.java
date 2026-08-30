package com.yiva.thejudgement;

import com.yiva.thejudgement.entity.ModEntities;
import com.yiva.thejudgement.item.ModCreativeTabs;
import com.yiva.thejudgement.item.ModItems;
import com.yiva.thejudgement.network.ModMessages;
import com.yiva.thejudgement.sound.ModSounds;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import com.yiva.thejudgement.particle.ModParticles;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TheJudgement.MOD_ID)
public class TheJudgement {

    // 模组ID（资源路径根目录，对应 assets/thejudgement）
    public static final String MOD_ID = "thejudgement";
    // 日志记录器
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public TheJudgement() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册配置文件
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON,
                Config.SPEC,
                MOD_ID + "-common.toml"
        );

        // ---- 注册所有模组内容 ----
        // 物品
        ModItems.register(modEventBus);
        // 创造模式页签
        ModCreativeTabs.register(modEventBus);
        // 实体
        ModEntities.register(modEventBus);
        // 音效
        ModSounds.register(modEventBus);
        // 网络（不依赖modEventBus，直接静态调用）
        ModMessages.register();
        ModParticles.register(modEventBus);
        // 注册通用事件总线（服务端/客户端事件监听）
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("The Judgement mod has been initialized!");
    }
}
