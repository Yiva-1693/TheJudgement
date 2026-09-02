package com.yiva.thejudgement.client.handler;

import com.yiva.thejudgement.item.JudgementStaffItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "thejudgement", value = Dist.CLIENT)
public class PlayerTickHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof JudgementStaffItem)) {
            player.setSpeed(0.1f); // 恢复默认速度
            return;
        }

        // 如果左右键任一正在蓄力，则减速
        boolean isCharging = InputHandler.isLeftCharging() || InputHandler.isRightCharging();
        if (isCharging) {
            player.setSpeed(0.4f); // 减速60%
            player.swingTime = 10;
        } else {
            player.setSpeed(0.1f);
        }
    }
}