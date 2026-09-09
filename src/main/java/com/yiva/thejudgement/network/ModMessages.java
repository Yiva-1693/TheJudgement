package com.yiva.thejudgement.network;

import com.yiva.thejudgement.TheJudgement;
import com.yiva.thejudgement.network.packet.BeamControlC2SPacket;
import com.yiva.thejudgement.network.packet.ChargingPoseC2SPacket;
import com.yiva.thejudgement.network.packet.ChargingPoseS2CPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {

    private static final String PROTOCOL_VERSION = "1.1";
    public static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    ResourceLocation.bySeparator("thejudgement:main", ':'),
                    () -> PROTOCOL_VERSION,
                    PROTOCOL_VERSION::equals,
                    PROTOCOL_VERSION::equals
            );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(
                packetId++,
                BeamControlC2SPacket.class,
                BeamControlC2SPacket::encode,
                BeamControlC2SPacket::decode,
                BeamControlC2SPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                ChargingPoseC2SPacket.class,
                ChargingPoseC2SPacket::encode,
                ChargingPoseC2SPacket::decode,
                ChargingPoseC2SPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                ChargingPoseS2CPacket.class,
                ChargingPoseS2CPacket::encode,
                ChargingPoseS2CPacket::decode,
                ChargingPoseS2CPacket::handle
        );
        TheJudgement.LOGGER.info("Network messages registered!");
    }

    public static void sendToServer(BeamControlC2SPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendPoseToServer(ChargingPoseC2SPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendPoseStateToTracking(ServerPlayer player, boolean active) {
        CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new ChargingPoseS2CPacket(player.getUUID(), active)
        );
    }

    public static void sendPoseStateToPlayer(ServerPlayer player, ChargingPoseS2CPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
