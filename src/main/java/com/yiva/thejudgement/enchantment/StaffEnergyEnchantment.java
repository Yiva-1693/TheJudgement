package com.yiva.thejudgement.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * Shared implementation for the two Judgement Staff energy enchantments.
 */
public final class StaffEnergyEnchantment extends Enchantment {

    public StaffEnergyEnchantment(Rarity rarity, EnchantmentCategory category) {
        super(rarity, category, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND);
    }

    @Override
    public int getMinCost(int level) {
        return 5 + (level - 1) * 6;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 15;
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }
}
