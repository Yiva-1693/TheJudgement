package com.yiva.thejudgement.loot;

import com.mojang.serialization.Codec;
import com.yiva.thejudgement.TheJudgement;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModLootModifiers {

    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, TheJudgement.MOD_ID);

    public static final RegistryObject<Codec<ClampEnergyEnchantmentsLootModifier>> CLAMP_ENERGY_ENCHANTMENTS =
            LOOT_MODIFIER_SERIALIZERS.register(
                    "clamp_energy_enchantments",
                    () -> ClampEnergyEnchantmentsLootModifier.CODEC
            );

    private ModLootModifiers() {
    }

    public static void register(IEventBus modEventBus) {
        LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
    }
}
