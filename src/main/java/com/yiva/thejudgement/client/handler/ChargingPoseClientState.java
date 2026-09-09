package com.yiva.thejudgement.client.handler;

import com.yiva.thejudgement.TheJudgement;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 客户端缓存所有可见玩家的蓄力姿势状态，并提供进出姿势的平滑过渡。
 */
@Mod.EventBusSubscriber(modid = TheJudgement.MOD_ID, value = Dist.CLIENT)
public final class ChargingPoseClientState {

    private static final int TRANSITION_TICKS = 5;
    private static final Map<UUID, PoseState> STATES = new HashMap<>();

    private ChargingPoseClientState() {
    }

    public static void setFromInput(UUID playerUUID, boolean active) {
        set(playerUUID, active);
    }

    public static void setFromNetwork(UUID playerUUID, boolean active) {
        set(playerUUID, active);
    }

    /**
     * Returns a value in [0, 1], including the short enter/exit transition.
     */
    public static float getBlend(UUID playerUUID, long gameTime, float partialTick) {
        PoseState state = STATES.get(playerUUID);
        if (state == null) {
            return 0.0F;
        }

        float progress = Mth.clamp(
                (float) ((gameTime + partialTick - state.transitionStartTick) / TRANSITION_TICKS),
                0.0F,
                1.0F
        );
        float blend = Mth.lerp(progress, state.fromBlend, state.targetBlend);

        if (!state.active && progress >= 1.0F) {
            STATES.remove(playerUUID);
            return 0.0F;
        }
        return blend;
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        STATES.clear();
    }

    private static void set(UUID playerUUID, boolean active) {
        Minecraft minecraft = Minecraft.getInstance();
        long gameTime = minecraft.level != null ? minecraft.level.getGameTime() : 0L;
        PoseState state = STATES.get(playerUUID);

        if (state == null) {
            STATES.put(playerUUID, new PoseState(
                    0.0F,
                    active ? 1.0F : 0.0F,
                    active,
                    gameTime
            ));
            return;
        }
        if (state.active == active) {
            return;
        }

        float progress = Mth.clamp(
                (float) ((gameTime - state.transitionStartTick) / (float) TRANSITION_TICKS),
                0.0F,
                1.0F
        );
        float currentBlend = Mth.lerp(progress, state.fromBlend, state.targetBlend);
        state.fromBlend = currentBlend;
        state.targetBlend = active ? 1.0F : 0.0F;
        state.active = active;
        state.transitionStartTick = gameTime;
    }

    private static final class PoseState {
        private float fromBlend;
        private float targetBlend;
        private boolean active;
        private long transitionStartTick;

        private PoseState(float fromBlend, float targetBlend, boolean active, long transitionStartTick) {
            this.fromBlend = fromBlend;
            this.targetBlend = targetBlend;
            this.active = active;
            this.transitionStartTick = transitionStartTick;
        }
    }
}
