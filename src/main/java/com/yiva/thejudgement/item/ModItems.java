package com.yiva.thejudgement.item;

import com.yiva.thejudgement.TheJudgement;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    // 创建物品注册表
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TheJudgement.MOD_ID);

    // 注册"圣光"法杖物品
    public static final RegistryObject<Item> JUDGEMENT_STAFF =
            ITEMS.register("the_judgement",
                    () -> new JudgementStaffItem(new Item.Properties()
                            .stacksTo(1)                // 不可堆叠
//                            .durability(500)            // 耐久度500，后续可通过配置调整
                            .setNoRepair()              // 暂不可修复，后续可添加修复材料
                    ));

    // 外部调用注册方法
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        TheJudgement.LOGGER.info("Items registered for The Judgement!");
    }
}