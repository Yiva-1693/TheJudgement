package com.yiva.thejudgement.network.packet;

import com.yiva.thejudgement.network.ChargingPoseServerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端通知服务端：本地玩家是否正在进行审判蓄力姿势。
 * 该状态只用于同步渲染，不参与伤害或实体逻辑判定。
 */
public final class ChargingPoseC2SPacket {

    private final boolean active;

    public ChargingPoseC2SPacket(boolean active) {
        this.active = active;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(active);
    }

    public static ChargingPoseC2SPacket decode(FriendlyByteBuf buf) {
        return new ChargingPoseC2SPacket(buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                ChargingPoseServerState.update(sender, active);
            }
        });
        context.setPacketHandled(true);
    }
}
