package com.yiva.thejudgement.network;

import com.yiva.thejudgement.TheJudgement;
import com.yiva.thejudgement.item.JudgementStaffItem;
import com.yiva.thejudgement.network.packet.ChargingPoseS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端维护蓄力姿势状态，并把状态发送给正在追踪该玩家的客户端。
 */
@Mod.EventBusSubscriber(modid = TheJudgement.MOD_ID)
public final class ChargingPoseServerState {

    private static final Set<UUID> ACTIVE_PLAYERS = ConcurrentHashMap.newKeySet();

    private ChargingPoseServerState() {
    }

    public static void update(ServerPlayer player, boolean requestedActive) {
        boolean active = requestedActive
                && player.getMainHandItem().getItem() instanceof JudgementStaffItem;
        UUID playerUUID = player.getUUID();
        boolean changed = active ? ACTIVE_PLAYERS.add(playerUUID) : ACTIVE_PLAYERS.remove(playerUUID);

        if (changed) {
            ModMessages.sendPoseStateToTracking(player, active);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer tracker)
                || !(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }

        // 无论当前是否蓄力都发送一次，避免追踪者复用旧缓存时残留姿势。
        boolean active = ACTIVE_PLAYERS.contains(target.getUUID());
        ModMessages.sendPoseStateToPlayer(
                tracker,
                new ChargingPoseS2CPacket(target.getUUID(), active)
        );
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ACTIVE_PLAYERS.clear();
    }

    private static void clear(ServerPlayer player) {
        if (ACTIVE_PLAYERS.remove(player.getUUID())) {
            ModMessages.sendPoseStateToTracking(player, false);
        }
    }
}
