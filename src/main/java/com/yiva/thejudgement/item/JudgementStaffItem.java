package com.yiva.thejudgement.item;

import com.yiva.thejudgement.enchantment.ModEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class JudgementStaffItem extends Item {

    public static final int BASE_MAX_ENERGY = 100;
    public static final int ENERGY_COST = 100;
    public static final int BASE_REGEN_PER_SECOND = 10;
    public static final int REGEN_PER_LEVEL = 2;
    private static final String ENERGY_ID_TAG = "JudgementEnergyId";

    public JudgementStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!hasEnoughEnergy(stack)) {
            return InteractionResultHolder.fail(stack);
        }

        // 消耗本次使用结果，阻止原版方块/实体交互且不触发挥手动画。
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        // 能量使用耐久值同步；仅耐久变化时不应反复播放手部重新装备动画。
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        int capacityLevel = EnchantmentHelper.getItemEnchantmentLevel(
                ModEnchantments.ENERGY_CAPACITY.get(), stack);
        return BASE_MAX_ENERGY + capacityLevel * 100;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide || !(entity instanceof Player) || level.getGameTime() % 20 != 0) {
            return;
        }

        int regenerationLevel = EnchantmentHelper.getItemEnchantmentLevel(
                ModEnchantments.ENERGY_REGENERATION.get(), stack);
        restoreEnergy(stack, BASE_REGEN_PER_SECOND + regenerationLevel * REGEN_PER_LEVEL);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment == ModEnchantments.ENERGY_CAPACITY.get()
                || enchantment == ModEnchantments.ENERGY_REGENERATION.get();
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 15;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable(
                "tooltip.thejudgement.energy",
                getEnergy(stack), stack.getMaxDamage()
        ).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    public static int getEnergy(ItemStack stack) {
        return Math.max(0, stack.getMaxDamage() - Math.min(stack.getDamageValue(), stack.getMaxDamage()));
    }

    public static boolean hasEnoughEnergy(ItemStack stack) {
        return stack.getItem() instanceof JudgementStaffItem && getEnergy(stack) >= ENERGY_COST;
    }

    public static boolean consumeEnergy(ItemStack stack, int amount) {
        if (!(stack.getItem() instanceof JudgementStaffItem) || getEnergy(stack) < amount) {
            return false;
        }
        stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + amount));
        return true;
    }

    public static void restoreEnergy(ItemStack stack, int amount) {
        if (!(stack.getItem() instanceof JudgementStaffItem) || amount <= 0) {
            return;
        }
        int clampedDamage = Math.min(stack.getDamageValue(), stack.getMaxDamage());
        stack.setDamageValue(Math.max(0, clampedDamage - amount));
    }

    public static UUID getOrCreateEnergyId(ItemStack stack) {
        if (!stack.getOrCreateTag().hasUUID(ENERGY_ID_TAG)) {
            stack.getOrCreateTag().putUUID(ENERGY_ID_TAG, UUID.randomUUID());
        }
        return stack.getOrCreateTag().getUUID(ENERGY_ID_TAG);
    }

    public static boolean hasEnergyId(ItemStack stack, UUID energyId) {
        return stack.getItem() instanceof JudgementStaffItem
                && stack.hasTag()
                && stack.getTag().hasUUID(ENERGY_ID_TAG)
                && stack.getTag().getUUID(ENERGY_ID_TAG).equals(energyId);
    }
}
