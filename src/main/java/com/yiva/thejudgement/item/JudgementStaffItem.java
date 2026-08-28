package com.yiva.thejudgement.item;

import com.yiva.thejudgement.Config;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class JudgementStaffItem extends Item {

    public JudgementStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        // 阻止原版交互（如吃东西、放置方块等）
        return InteractionResultHolder.pass(stack);
    }

    public void applyCooldown(Player player) {
        if (!player.level().isClientSide) {
            player.getCooldowns().addCooldown(this, Config.getCooldown() * 20);
        }
    }

    public void clearCooldown(Player player) {
        if (!player.level().isClientSide) {
            player.getCooldowns().removeCooldown(this);
        }
    }
}