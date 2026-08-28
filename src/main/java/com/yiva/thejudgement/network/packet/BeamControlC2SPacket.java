package com.yiva.thejudgement.network.packet;

import com.yiva.thejudgement.TheJudgement;
import com.yiva.thejudgement.entity.BeamEntity;
import com.yiva.thejudgement.entity.ModEntities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
                    BeamEntity oldBeam = BeamEntity.CHARGING_BEAMS.remove(playerUUID);
                    if (oldBeam != null && !oldBeam.isRemoved()) {
                        oldBeam.discard();
                    }

                    BeamEntity beam = new BeamEntity(ModEntities.BEAM.get(), level);
                    beam.setOwnerUUID(playerUUID);
                    beam.setMode(mode);
                    beam.setPhase(BeamEntity.Phase.CHARGING);
                    beam.setPhaseTimer(0);

                    Vec3 startCenter = new Vec3(centerX, centerY, centerZ);
                    beam.setCenterPos(startCenter);

                    if (mode == BeamEntity.Mode.SWEEP) {
                        if (targetX != 0 || targetY != 0 || targetZ != 0) {
                            beam.setFixedTargetPos(new Vec3(targetX, targetY, targetZ));
                        }
                        BeamEntity.setKeyState(playerUUID, false, true);
                    } else {
                        BeamEntity.setKeyState(playerUUID, true, true);
                    }

                    beam.setPos(startCenter.x, startCenter.y, startCenter.z);
                    level.addFreshEntity(beam);
                    BeamEntity.CHARGING_BEAMS.put(playerUUID, beam);
                }

                case UPDATE_CHARGE -> {
                    BeamEntity beam = BeamEntity.CHARGING_BEAMS.get(playerUUID);
                    if (beam == null || beam.isRemoved()) return;
                    if (mode == BeamEntity.Mode.BOMB) {
                        beam.setCenterPos(new Vec3(centerX, centerY, centerZ));
                    } else if (mode == BeamEntity.Mode.SWEEP) {
                        if (targetX != 0 || targetY != 0 || targetZ != 0) {
                            beam.setFixedTargetPos(new Vec3(targetX, targetY, targetZ));
                        }
                    }
                }

                case CANCEL_CHARGE -> {
                    BeamEntity beam = BeamEntity.CHARGING_BEAMS.remove(playerUUID);
                    if (beam != null && !beam.isRemoved()) {
                        beam.discard();
                    }
                    applyCooldown(player);
                    if (mode == BeamEntity.Mode.BOMB) {
                        BeamEntity.setKeyState(playerUUID, true, false);
                    } else {
                        BeamEntity.setKeyState(playerUUID, false, false);
                    }
                }

                case KEY_STATE -> {
                    boolean isLeft = (mode == BeamEntity.Mode.BOMB);
                    BeamEntity.setKeyState(playerUUID, isLeft, keyDown);
                }
            }
        });
        context.setPacketHandled(true);
    }

    private void applyCooldown(ServerPlayer player) {
        var mainHand = player.getMainHandItem();
        var offHand = player.getOffhandItem();
        if (mainHand.getItem() instanceof com.yiva.thejudgement.item.JudgementStaffItem staff) {
            player.getCooldowns().addCooldown(staff, com.yiva.thejudgement.Config.getCooldown() * 20);
        } else if (offHand.getItem() instanceof com.yiva.thejudgement.item.JudgementStaffItem staff) {
            player.getCooldowns().addCooldown(staff, com.yiva.thejudgement.Config.getCooldown() * 20);
        }
    }
}