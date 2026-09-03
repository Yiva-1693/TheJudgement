package com.yiva.thejudgement.network.packet;

import com.yiva.thejudgement.Config;
import com.yiva.thejudgement.TheJudgement;
import com.yiva.thejudgement.entity.BeamEntity;
import com.yiva.thejudgement.entity.ModEntities;
import com.yiva.thejudgement.item.JudgementStaffItem;
import com.yiva.thejudgement.util.BeamMathHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class BeamControlC2SPacket {

    public enum Action {
        START_CHARGE,
        UPDATE_CHARGE,
        CANCEL_CHARGE,
        KEY_STATE
    }

    private final Action action;
    private final BeamEntity.Mode mode;
    private final double centerX, centerY, centerZ;
    private final double targetX, targetY, targetZ;
    private final float chargeTime;
    private final UUID playerUUID;
    private final boolean keyDown;

    public BeamControlC2SPacket(Action action, BeamEntity.Mode mode, Vec3 centerPos, Vec3 targetPos,
                                float chargeTime, UUID playerUUID, boolean keyDown) {
        this.action = action;
        this.mode = mode;
        this.centerX = centerPos != null ? centerPos.x : 0;
        this.centerY = centerPos != null ? centerPos.y : 0;
        this.centerZ = centerPos != null ? centerPos.z : 0;
        this.targetX = targetPos != null ? targetPos.x : 0;
        this.targetY = targetPos != null ? targetPos.y : 0;
        this.targetZ = targetPos != null ? targetPos.z : 0;
        this.chargeTime = chargeTime;
        this.playerUUID = playerUUID;
        this.keyDown = keyDown;
    }

    public BeamControlC2SPacket(Action action, BeamEntity.Mode mode, Vec3 centerPos, Vec3 targetPos,
                                float chargeTime, UUID playerUUID) {
        this(action, mode, centerPos, targetPos, chargeTime, playerUUID, false);
    }

    // 工厂方法
    public static BeamControlC2SPacket startCharge(BeamEntity.Mode mode, Vec3 center, Vec3 target, UUID playerUUID) {
        return new BeamControlC2SPacket(Action.START_CHARGE, mode, center, target, 0, playerUUID);
    }

    public static BeamControlC2SPacket updateCharge(BeamEntity.Mode mode, Vec3 center, Vec3 target, UUID playerUUID) {
        return new BeamControlC2SPacket(Action.UPDATE_CHARGE, mode, center, target, 0, playerUUID);
    }

    public static BeamControlC2SPacket cancelCharge(BeamEntity.Mode mode, UUID playerUUID) {
        return new BeamControlC2SPacket(Action.CANCEL_CHARGE, mode, null, null, 0, playerUUID);
    }

    public static BeamControlC2SPacket keyState(BeamEntity.Mode mode, boolean isDown, UUID playerUUID) {
        return new BeamControlC2SPacket(Action.KEY_STATE, mode, null, null, 0, playerUUID, isDown);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeEnum(mode);
        buf.writeDouble(centerX);
        buf.writeDouble(centerY);
        buf.writeDouble(centerZ);
        buf.writeDouble(targetX);
        buf.writeDouble(targetY);
        buf.writeDouble(targetZ);
        buf.writeFloat(chargeTime);
        buf.writeUUID(playerUUID);
        buf.writeBoolean(keyDown);
    }

    public static BeamControlC2SPacket decode(FriendlyByteBuf buf) {
        Action action = buf.readEnum(Action.class);
        BeamEntity.Mode mode = buf.readEnum(BeamEntity.Mode.class);
        double cx = buf.readDouble();
        double cy = buf.readDouble();
        double cz = buf.readDouble();
        double tx = buf.readDouble();
        double ty = buf.readDouble();
        double tz = buf.readDouble();
        float charge = buf.readFloat();
        UUID uuid = buf.readUUID();
        boolean keyDown = buf.readBoolean();
        Vec3 center = new Vec3(cx, cy, cz);
        Vec3 target = new Vec3(tx, ty, tz);
        return new BeamControlC2SPacket(action, mode, center, target, charge, uuid, keyDown);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                TheJudgement.LOGGER.warn("BeamControlC2SPacket.handle: sender is null");
                return;
            }
            if (!player.getUUID().equals(playerUUID)) {
                TheJudgement.LOGGER.warn("BeamControlC2SPacket.handle: UUID mismatch!");
                return;
            }

            ServerLevel level = (ServerLevel) player.level();

            switch (action) {
                case START_CHARGE -> {
                    var mainHand = player.getMainHandItem();
                    if (!(mainHand.getItem() instanceof JudgementStaffItem)
                            || !JudgementStaffItem.hasEnoughEnergy(mainHand)) {
                        return;
                    }

                    Vec3 startCenter;
                    Vec3 fixedTarget = null;
                    if (mode == BeamEntity.Mode.BOMB) {
                        // 潜行左键：初始 Core 在玩家脚下（取整）
                        BlockPos playerPos = player.blockPosition();
                        startCenter = new Vec3(playerPos.getX() + 0.5, playerPos.getY(), playerPos.getZ() + 0.5);
                    }  else if (mode == BeamEntity.Mode.SWEEP) {
                        startCenter = player.position();
                        Vec3 horizontalDir = BeamMathHelper.getHorizontalDirection(player.getLookAngle());
                        fixedTarget = startCenter.add(horizontalDir.scale(Config.getSweepCircleRadius()));
                    } else {
                        HitResult hitResult = player.pick(Config.getBombTargetMaxRange(), 1.0F, false);
                        if (!(hitResult instanceof BlockHitResult blockHit)) return;
                        BlockPos blockPos = blockHit.getBlockPos();
                        startCenter = new Vec3(blockPos.getX() + 0.5, blockPos.getY() - 1.0, blockPos.getZ() + 0.5);
                    }

                    BeamEntity oldBeam = BeamEntity.CHARGING_BEAMS.remove(playerUUID);
                    if (oldBeam != null && !oldBeam.isRemoved()) {
                        oldBeam.discard();
                    }

                    BeamEntity beam = new BeamEntity(ModEntities.BEAM.get(), level);
                    beam.setOwnerUUID(playerUUID);
                    beam.setMode(mode);
                    beam.setPhase(BeamEntity.Phase.CHARGING);
                    beam.setPhaseTimer(0);
                    beam.setSourceStaffId(JudgementStaffItem.getOrCreateEnergyId(mainHand));
                    beam.setDamagePerTick(JudgementStaffItem.getDamagePerTick(mainHand));

                    beam.setCenterPos(startCenter);

                    if (mode == BeamEntity.Mode.SWEEP) {
                        beam.setFixedTargetPos(fixedTarget);
                        BeamEntity.setKeyState(playerUUID, false, true);
                    } else {
                        BeamEntity.setKeyState(playerUUID, true, true);
                    }

                    beam.setPos(startCenter.x, startCenter.y, startCenter.z);
                    if (!JudgementStaffItem.consumeEnergy(mainHand, JudgementStaffItem.ENERGY_COST)) {
                        return;
                    }
                    if (!level.addFreshEntity(beam)) {
                        JudgementStaffItem.restoreEnergy(mainHand, JudgementStaffItem.ENERGY_COST);
                        return;
                    }
                    BeamEntity.CHARGING_BEAMS.put(playerUUID, beam);
                }

                case UPDATE_CHARGE -> {
                    BeamEntity beam = BeamEntity.CHARGING_BEAMS.get(playerUUID);
                    if (beam == null || beam.isRemoved()) return;
                    if (beam.getMode() != mode) {
                        TheJudgement.LOGGER.warn("BeamControlC2SPacket.handle: mode mismatch!");
                        return;
                    }
                    if (mode == BeamEntity.Mode.BOMB) {
                        Vec3 start = player.getEyePosition(1.0F);
                        Vec3 end = start.add(player.getLookAngle().scale(Config.getBombTargetMaxRange()));
                        BeamMathHelper.findEntityOnLine(level, start, end, player)
                                .ifPresent(beam::setFixedTargetPos);
                    } else if (mode == BeamEntity.Mode.SWEEP) {
                        Vec3 horizontalDir = BeamMathHelper.getHorizontalDirection(player.getLookAngle());
                        Vec3 horizontalTarget = player.position().add(horizontalDir.scale(Config.getSweepCircleRadius()));
                        Vec3 target = new Vec3(horizontalTarget.x, beam.getCenterPos().y, horizontalTarget.z);
                        beam.setFixedTargetPos(target);
                    }
                }

                case CANCEL_CHARGE -> {
                    BeamEntity beam = BeamEntity.CHARGING_BEAMS.get(playerUUID);
                    if (beam != null && beam.getMode() != mode) return;
                    BeamEntity.CHARGING_BEAMS.remove(playerUUID, beam);
                    if (beam != null && !beam.isRemoved()) {
                        beam.discard();
                    }
                    BeamEntity.setKeyState(playerUUID, isLeftMode(mode), false);
                }

                case KEY_STATE -> {
                    BeamEntity beam = BeamEntity.CHARGING_BEAMS.get(playerUUID);
                    if (beam != null && beam.getMode() != mode) return;
                    BeamEntity.setKeyState(playerUUID, isLeftMode(mode), keyDown);
                }
            }
        });
        context.setPacketHandled(true);
    }

    private static boolean isLeftMode(BeamEntity.Mode mode) {
        return mode == BeamEntity.Mode.BOMB || mode == BeamEntity.Mode.DIRECT_BOMB;
    }
}
