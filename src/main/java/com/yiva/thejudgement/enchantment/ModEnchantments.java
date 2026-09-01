package com.yiva.thejudgement.enchantment;

import com.yiva.thejudgement.TheJudgement;
import com.yiva.thejudgement.item.JudgementStaffItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEnchantments {

    private static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, TheJudgement.MOD_ID);

    public static final EnchantmentCategory JUDGEMENT_STAFF =
            EnchantmentCategory.create("THE_JUDGEMENT_STAFF", item -> item instanceof JudgementStaffItem);

    public static final RegistryObject<Enchantment> ENERGY_CAPACITY = ENCHANTMENTS.register(
            "energy_capacity",
            () -> new StaffEnergyEnchantment(Enchantment.Rarity.RARE, JUDGEMENT_STAFF)
    );

    public static final RegistryObject<Enchantment> ENERGY_REGENERATION = ENCHANTMENTS.register(
            "energy_regeneration",
            () -> new StaffEnergyEnchantment(Enchantment.Rarity.RARE, JUDGEMENT_STAFF)
    );

    private ModEnchantments() {
    }

    public static void register(IEventBus modEventBus) {
        ENCHANTMENTS.register(modEventBus);
    }
}
