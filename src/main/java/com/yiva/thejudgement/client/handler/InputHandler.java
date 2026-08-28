package com.yiva.thejudgement.client.handler;

import com.yiva.thejudgement.Config;
import com.yiva.thejudgement.TheJudgement;
import com.yiva.thejudgement.entity.BeamEntity;
import com.yiva.thejudgement.item.JudgementStaffItem;
import com.yiva.thejudgement.network.ModMessages;
import com.yiva.thejudgement.network.packet.BeamControlC2SPacket;
import com.yiva.thejudgement.util.BeamMathHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = TheJudgement.MOD_ID, value = Dist.CLIENT)
public class InputHandler {

    private static long leftPressTime = 0;
    private static long rightPressTime = 0;
    private static double rightPressY = 0;
    private static boolean leftCharging = false;
    private static boolean rightCharging = false;

    // ---------- 鼠标事件 ----------
    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return;

        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof JudgementStaffItem staff)) return;

        int button = event.getButton();
        int action = event.getAction();

        // ========== 左键：轰炸模式 ==========
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (action == GLFW.GLFW_PRESS) {
                if (rightCharging) {
                    cancelRight(player);
                    return;
                }
                if (player.getCooldowns().isOnCooldown(mainHand.getItem())) return;
                if (leftCharging) return;

                leftPressTime = System.currentTimeMillis();
                leftCharging = true;

                Vec3 eyePos = player.getEyePosition(1.0f);
                ModMessages.sendToServer(BeamControlC2SPacket.startCharge(BeamEntity.Mode.BOMB, eyePos, null, player.getUUID()));
                ModMessages.sendToServer(BeamControlC2SPacket.keyState(BeamEntity.Mode.BOMB, true, player.getUUID()));
            } else if (action == GLFW.GLFW_RELEASE) {
                if (leftCharging) {
                    leftCharging = false;
                    leftPressTime = 0;
                    // 松开按键不停止音效
                    ModMessages.sendToServer(BeamControlC2SPacket.keyState(BeamEntity.Mode.BOMB, false, player.getUUID()));
                }
            }
        }

        // ========== 右键：扫射模式 ==========
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (action == GLFW.GLFW_PRESS) {
                if (leftCharging) {
                    cancelLeft(player);
                    return;
                }
                if (player.getCooldowns().isOnCooldown(mainHand.getItem())) return;
                if (rightCharging) return;

                rightPressTime = System.currentTimeMillis();
                rightPressY = player.position().y;
                rightCharging = true;

                Vec3 core = new Vec3(player.position().x, rightPressY, player.position().z);
                Vec3 horizontalDir = BeamMathHelper.getHorizontalDirection(player.getLookAngle());
                Vec3 target = core.add(horizontalDir.scale(Config.getSweepCircleRadius()));
                target = new Vec3(target.x, rightPressY, target.z);

                ModMessages.sendToServer(BeamControlC2SPacket.startCharge(BeamEntity.Mode.SWEEP, core, target, player.getUUID()));
                ModMessages.sendToServer(BeamControlC2SPacket.keyState(BeamEntity.Mode.SWEEP, true, player.getUUID()));
            } else if (action == GLFW.GLFW_RELEASE) {
                if (rightCharging) {
                    rightCharging = false;
                    rightPressTime = 0;
                    // 松开按键不停止音效
                    ModMessages.sendToServer(BeamControlC2SPacket.keyState(BeamEntity.Mode.SWEEP, false, player.getUUID()));
                }
            }
        }
    }

    // ---------- 客户端 Tick ----------
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        ItemStack mainHand = player.getMainHandItem();
        boolean holdingStaff = mainHand.getItem() instanceof JudgementStaffItem;
        boolean guiOpen = mc.screen != null;

        // 如果切换物品或打开 GUI，取消所有蓄力（但不停止音效）
        if ((!holdingStaff || guiOpen) && (leftCharging || rightCharging)) {
            if (leftCharging) cancelLeft(player);
            if (rightCharging) cancelRight(player);
            return;
        }

        // 左键轰炸模式更新
        if (leftCharging) {
            float chargeTime = (System.currentTimeMillis() - leftPressTime) / 1000.0f;
            double sphereRadius = Config.getBombInitRadius() + chargeTime * Config.getBombGrowRate();
            sphereRadius = Math.min(sphereRadius, 85.0);

            Vec3 eyePos = player.getEyePosition(1.0f);
            Vec3 lookVec = player.getLookAngle();
            var hitOpt = BeamMathHelper.intersectRayWithSphere(eyePos, lookVec, eyePos, sphereRadius);
            if (hitOpt.isPresent()) {
                Vec3 core = hitOpt.get();
                ModMessages.sendToServer(BeamControlC2SPacket.updateCharge(BeamEntity.Mode.BOMB, core, null, player.getUUID()));
            }
        }

        // 右键扫射模式更新
        if (rightCharging) {
            Vec3 horizontalDir = BeamMathHelper.getHorizontalDirection(player.getLookAngle());
            Vec3 core = new Vec3(player.position().x, rightPressY, player.position().z);
            Vec3 target = core.add(horizontalDir.scale(Config.getSweepCircleRadius()));
            target = new Vec3(target.x, rightPressY, target.z);
            ModMessages.sendToServer(BeamControlC2SPacket.updateCharge(BeamEntity.Mode.SWEEP, core, target, player.getUUID()));
        }
    }

    // ---------- 取消方法 ----------
    private static void cancelLeft(LocalPlayer player) {
        leftCharging = false;
        leftPressTime = 0;
        // 不停止音效，让它自然播放或被发射音效打断
        ModMessages.sendToServer(BeamControlC2SPacket.cancelCharge(BeamEntity.Mode.BOMB, player.getUUID()));
    }

    private static void cancelRight(LocalPlayer player) {
        rightCharging = false;
        rightPressTime = 0;
        // 不停止音效
        ModMessages.sendToServer(BeamControlC2SPacket.cancelCharge(BeamEntity.Mode.SWEEP, player.getUUID()));
    }

    // ---------- 状态查询 ----------
    public static boolean isLeftCharging() { return leftCharging; }
    public static boolean isRightCharging() { return rightCharging; }
}
