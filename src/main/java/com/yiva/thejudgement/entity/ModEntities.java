package com.yiva.thejudgement.entity;

import com.yiva.thejudgement.TheJudgement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    // 创建实体类型注册表
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TheJudgement.MOD_ID);

    // 注册"圣光光束"实体
    public static final RegistryObject<EntityType<BeamEntity>> BEAM =
            ENTITIES.register("beam",
                    () -> EntityType.Builder.<BeamEntity>of(BeamEntity::new, MobCategory.MISC)
                            .sized(3.0f, 40.0f)              // 碰撞箱：直径3格，高40格（视觉参考，实际用数学碰撞）
                            .setTrackingRange(64)            // 客户端同步距离（格）
                            .setUpdateInterval(1)            // 每 tick 同步一次位置
                            .setShouldReceiveVelocityUpdates(false) // 不需要接收速度更新
                            .build(ResourceLocation.bySeparator("thejudgement:beam", ':').toString())            );

    // 外部调用注册方法
    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        TheJudgement.LOGGER.info("Entities registered for The Judgement!");
    }
}