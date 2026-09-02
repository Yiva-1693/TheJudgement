package com.yiva.thejudgement.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * Shared implementation for the two Judgement Staff energy enchantments.
 */
public final class StaffEnergyEnchantment extends Enchantment {

    private static final int LEVEL_ONE_MIN_COST = 27;
    private static final int LEVEL_TWO_MIN_COST = 36;
    private static final int UNAVAILABLE_AT_TABLE_COST = 1000;

    public StaffEnergyEnchantment(Rarity rarity, EnchantmentCategory category) {
        super(rarity, category, new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
    }

    @Override
    public int getMinCost(int level) {
        return switch (level) {
            case 1 -> LEVEL_ONE_MIN_COST;
            case 2 -> LEVEL_TWO_MIN_COST;
            default -> UNAVAILABLE_AT_TABLE_COST;
        };
    }

    @Override
    public int getMaxCost(int level) {
        return switch (level) {
            case 1 -> LEVEL_TWO_MIN_COST - 1;
            case 2 -> 45;
            default -> UNAVAILABLE_AT_TABLE_COST;
        };
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        if (stack.is(Items.BOOK)) {
            return EnchantmentAcquisitionEvents.isMaxLevelTableItem(stack);
        }
        return super.canApplyAtEnchantingTable(stack);
    }

    @Override
    public boolean isAllowedOnBooks() {
        // 附魔书改由上面的上下文检查放行，避免未满级附魔台绕过限制。
        return false;
    }

    @Override
    public boolean isTradeable() {
        // 禁用原版图书管理员的随机附魔交易，改由模组交易固定提供 I、II 级。
        return false;
    }
}
