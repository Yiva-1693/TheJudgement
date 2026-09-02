package com.yiva.thejudgement.client.handler;

import com.yiva.thejudgement.item.JudgementStaffItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "thejudgement", value = Dist.CLIENT)
public class PlayerTickHandler {

    private static final UUID CHARGING_SLOWDOWN_ID = UUID.fromString("be350ee7-7746-4c78-883e-c6db561bb193");
    private static final AttributeModifier CHARGING_SLOWDOWN = new AttributeModifier(
            CHARGING_SLOWDOWN_ID,
            "Judgement staff charging slowdown",
            -0.6D,
            AttributeModifier.Operation.MULTIPLY_TOTAL
    );

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        ItemStack mainHand = player.getMainHandItem();
        boolean holdingStaff = mainHand.getItem() instanceof JudgementStaffItem;
        boolean isUsingStaff = holdingStaff
                && (InputHandler.isLeftCharging() || InputHandler.isRightCharging());
        boolean shouldSlowDown = holdingStaff
                && InputHandler.shouldApplySlowdown(player.level().getGameTime());

        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) return;

        if (shouldSlowDown) {
            // MULTIPLY_TOTAL 的 -0.6 会保留当前移动速度的 40%，兼容其他速度效果。
            if (movementSpeed.getModifier(CHARGING_SLOWDOWN_ID) == null) {
                movementSpeed.addTransientModifier(CHARGING_SLOWDOWN);
            }
            player.swingTime = 10;
        } else {
            movementSpeed.removeModifier(CHARGING_SLOWDOWN_ID);
        }

        if (isUsingStaff) {
            player.swingTime = 10;
        }
    }
}
