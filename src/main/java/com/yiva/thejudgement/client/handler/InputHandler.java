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
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

@Mod.EventBusSubscriber(modid = TheJudgement.MOD_ID, value = Dist.CLIENT)
public class InputHandler {

    private static long leftPressTime = 0;
    private static long rightPressTime = 0;
    private static double rightPressY = 0;
    private static boolean leftCharging = false;
    private static boolean rightCharging = false;
    private static BeamEntity.Mode leftMode = null;

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

        // ========== 左键：潜行蓄力轰炸 / 非潜行直接轰炸 ==========
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (action == GLFW.GLFW_PRESS) {
                if (rightCharging) {
                    cancelRight(player);
                    return;
                }
                if (player.getCooldowns().isOnCooldown(mainHand.getItem())) return;
                if (leftCharging) return;

                if (player.isShiftKeyDown()) {
                    leftPressTime = System.currentTimeMillis();
                    leftMode = BeamEntity.Mode.BOMB;
                    leftCharging = true;

                    Vec3 eyePos = player.getEyePosition(1.0f);
                    ModMessages.sendToServer(BeamControlC2SPacket.startCharge(leftMode, eyePos, null, player.getUUID()));
                    ModMessages.sendToServer(BeamControlC2SPacket.keyState(leftMode, true, player.getUUID()));
                } else {

                    // 射线检测，忽略流体
                    double maxRange = Config.getDirectBombMaxRange();
                    HitResult hitResult = player.pick(maxRange, 1.0F, false);
                    if (hitResult instanceof BlockHitResult blockHit) {
                        BlockPos blockPos = blockHit.getBlockPos();
                        Vec3 core = new Vec3(blockPos.getX() + 0.5, blockPos.getY() - 1.0, blockPos.getZ() + 0.5);

                        leftMode = BeamEntity.Mode.DIRECT_BOMB;
                        leftCharging = true;
                        ModMessages.sendToServer(BeamControlC2SPacket.startCharge(leftMode, core, null, player.getUUID()));
                    } else {
                        // 未命中方块则取消
                        return;
                    }
                }
            } else if (action == GLFW.GLFW_RELEASE) {
                if (leftCharging) {
                    BeamEntity.Mode releasedMode = leftMode;
                    resetLeftState();
                    ModMessages.sendToServer(BeamControlC2SPacket.keyState(releasedMode, false, player.getUUID()));
                }
            }
            return;
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
        if (leftCharging && leftMode == BeamEntity.Mode.BOMB) {
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
        BeamEntity.Mode cancelledMode = leftMode;
        resetLeftState();
        // 不停止音效，让它自然播放或被发射音效打断
        if (cancelledMode != null) {
            ModMessages.sendToServer(BeamControlC2SPacket.cancelCharge(cancelledMode, player.getUUID()));
        }
    }

    private static void resetLeftState() {
        leftCharging = false;
        leftPressTime = 0;
        leftMode = null;
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
