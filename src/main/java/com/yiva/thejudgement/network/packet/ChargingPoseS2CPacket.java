package com.yiva.thejudgement.network.packet;

import com.yiva.thejudgement.client.handler.ChargingPoseClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 服务端广播某位玩家当前是否处于蓄力姿势。
 */
public final class ChargingPoseS2CPacket {

    private final UUID playerUUID;
    private final boolean active;

    public ChargingPoseS2CPacket(UUID playerUUID, boolean active) {
        this.playerUUID = playerUUID;
        this.active = active;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeBoolean(active);
    }

    public static ChargingPoseS2CPacket decode(FriendlyByteBuf buf) {
        return new ChargingPoseS2CPacket(buf.readUUID(), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ChargingPoseClientState.setFromNetwork(playerUUID, active)
        ));
        context.setPacketHandled(true);
    }
}
