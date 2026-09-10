package com.yiva.thejudgement.network;

import com.yiva.thejudgement.TheJudgement;
import com.yiva.thejudgement.network.packet.BeamControlC2SPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {

    private static final String PROTOCOL_VERSION = "1.0";
    public static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    new ResourceLocation(TheJudgement.MOD_ID, "main"),
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
        TheJudgement.LOGGER.info("Network messages registered!");
    }

    public static void sendToServer(BeamControlC2SPacket packet) {
        CHANNEL.sendToServer(packet);
    }
}
