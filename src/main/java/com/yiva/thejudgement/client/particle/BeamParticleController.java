package com.yiva.thejudgement.client.particle;

import com.yiva.thejudgement.Config;
import com.yiva.thejudgement.TheJudgement;
import com.yiva.thejudgement.entity.BeamEntity;
import com.yiva.thejudgement.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Generates beam particles from synced entity state, independently of frame rate.
 */
@Mod.EventBusSubscriber(modid = TheJudgement.MOD_ID, value = Dist.CLIENT)
public final class BeamParticleController {

    private static final double FULL_DISTANCE_SQR = 64.0 * 64.0;
    private static final double MAX_DISTANCE_SQR = 128.0 * 128.0;
    private static final Map<Integer, BeamVisualState> ACTIVE_BEAMS = new HashMap<>();
    private static ClientLevel activeLevel;

    private BeamParticleController() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            clearState();
            return;
        }
        if (activeLevel != level) {
            ACTIVE_BEAMS.clear();
            activeLevel = level;
        }

        Set<Integer> seen = new HashSet<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof BeamEntity beam)) {
                continue;
            }

            int id = beam.getId();
            seen.add(id);
            Vec3 center = beam.position();
            double scale = particleScale(minecraft, center);
            BeamVisualState state = ACTIVE_BEAMS.get(id);
            if (state == null) {
                state = new BeamVisualState(beam.getPhase(), center, level.getGameTime());
                ACTIVE_BEAMS.put(id, state);
                if (beam.getPhase() == BeamEntity.Phase.FIRING && scale > 0) {
                    spawnFiringBurst(level, center, scale);
                }
            } else if (state.phase != beam.getPhase()) {
                spawnPhaseTransition(level, beam.getPhase(), center, scale);
            }

            if (scale > 0) {
                spawnContinuousParticles(level, beam, state, center, scale);
            }
            state.update(beam, center, level.getGameTime());
        }

        removeMissingBeams(level, seen, minecraft.player.position());
    }

    private static void spawnPhaseTransition(ClientLevel level, BeamEntity.Phase current,
                                             Vec3 center, double scale) {
        if (scale <= 0) {
            return;
        }
        if (current == BeamEntity.Phase.FIRING) {
            spawnFiringBurst(level, center, scale);
        } else if (current == BeamEntity.Phase.DISSIPATING) {
            spawnDissipatingBurst(level, center, scale);
        }
    }

    private static void spawnContinuousParticles(ClientLevel level, BeamEntity beam,
                                                 BeamVisualState state, Vec3 center, double scale) {
        switch (beam.getPhase()) {
            case CHARGING, CHARGED_WAIT -> spawnChargingParticles(level, center, scale);
            case FIRING -> {
                spawnBeamEdgeParticles(level, beam, center, scale);
                spawnMovementTrail(level, beam, state.center, center, scale);
            }
            case DISSIPATING -> {
                spawnDissipatingParticles(level, beam, center, scale);
                spawnMovementTrail(level, beam, state.center, center, scale * 0.65);
            }
        }
    }

    private static void spawnChargingParticles(ClientLevel level, Vec3 center, double scale) {
        RandomSource random = level.random;
        long time = level.getGameTime();
        double height = Config.getBeamHeight();

        if (time % 2 == 0) {
            int count = scaledCount(level, 2, scale);
            for (int i = 0; i < count; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double distance = 4.0 + random.nextDouble() * 7.0;
                double x = center.x + Math.cos(angle) * distance;
                double z = center.z + Math.sin(angle) * distance;
                double y = center.y + height + 0.1 + (random.nextDouble() - 0.5) * 0.5;
                Vec3 velocity = center.add(0, height, 0).subtract(x, y, z).normalize()
                        .scale(0.04 + random.nextDouble() * 0.05);
                level.addParticle(ModParticles.ABYSS_MOTE.get(), x, y, z,
                        velocity.x, velocity.y, velocity.z);
            }
        }

        if (time % 3 == 0) {
            int count = scaledCount(level, 2, scale);
            for (int i = 0; i < count; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double radius = 2.5 + random.nextDouble() * 0.7;
                level.addParticle(ModParticles.ENERGY_SPARK.get(),
                        center.x + Math.cos(angle) * radius,
                        center.y + 0.08 + random.nextDouble() * 0.12,
                        center.z + Math.sin(angle) * radius,
                        -Math.cos(angle) * 0.015, 0.012, -Math.sin(angle) * 0.015);
            }
        }
    }

    private static void spawnFiringBurst(ClientLevel level, Vec3 center, double scale) {
        RandomSource random = level.random;
        int count = scaledCount(level, 28, scale);
        double height = Config.getBeamHeight();
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 0.08 + random.nextDouble() * 0.2;
            double y = i % 2 == 0 ? center.y : center.y + height;
            level.addParticle(ModParticles.ENERGY_SPARK.get(), center.x, y, center.z,
                    Math.cos(angle) * speed,
                    (random.nextDouble() - 0.25) * speed,
                    Math.sin(angle) * speed);
        }
    }

    private static void spawnBeamEdgeParticles(ClientLevel level, BeamEntity beam,
                                               Vec3 center, double scale) {
        RandomSource random = level.random;
        int baseCount = beam.getMode() == BeamEntity.Mode.DIRECT_BOMB ? 5 : 4;
        int count = scaledCount(level, baseCount, scale);
        double radius = Math.max(0.25, beam.getCurrentRadius());
        double height = Config.getBeamHeight();
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double edge = radius * (0.72 + random.nextDouble() * 0.35);
            double y = center.y - height + random.nextDouble() * height * 2.0;
            double outward = 0.015 + random.nextDouble() * 0.045;
            level.addParticle(ModParticles.ENERGY_SPARK.get(),
                    center.x + Math.cos(angle) * edge, y, center.z + Math.sin(angle) * edge,
                    Math.cos(angle) * outward,
                    (random.nextDouble() - 0.5) * 0.05,
                    Math.sin(angle) * outward);
        }
    }

    private static void spawnMovementTrail(ClientLevel level, BeamEntity beam, Vec3 previous,
                                           Vec3 center, double scale) {
        Vec3 movement = center.subtract(previous);
        if (movement.horizontalDistanceSqr() < 0.0025) {
            return;
        }

        RandomSource random = level.random;
        int baseCount = beam.getMode() == BeamEntity.Mode.SWEEP ? 3 : 1;
        int count = scaledCount(level, baseCount, scale);
        double radius = Math.max(0.3, beam.getCurrentRadius());
        double height = Config.getBeamHeight();
        Vec3 direction = movement.normalize();
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double spread = random.nextDouble() * radius;
            level.addParticle(ModParticles.ABYSS_MOTE.get(),
                    previous.x + Math.cos(angle) * spread,
                    center.y - height + random.nextDouble() * height * 2.0,
                    previous.z + Math.sin(angle) * spread,
                    -direction.x * 0.035,
                    (random.nextDouble() - 0.5) * 0.02,
                    -direction.z * 0.035);
        }
    }

    private static void spawnDissipatingBurst(ClientLevel level, Vec3 center, double scale) {
        RandomSource random = level.random;
        int count = scaledCount(level, 18, scale);
        double height = Config.getBeamHeight();
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 0.06 + random.nextDouble() * 0.16;
            level.addParticle(ModParticles.RIFT_SHARD.get(),
                    center.x + (random.nextDouble() - 0.5) * 2.0,
                    center.y + height + random.nextDouble() * 0.8,
                    center.z + (random.nextDouble() - 0.5) * 2.0,
                    Math.cos(angle) * speed,
                    0.05 + random.nextDouble() * 0.12,
                    Math.sin(angle) * speed);
        }
    }

    private static void spawnDissipatingParticles(ClientLevel level, BeamEntity beam,
                                                  Vec3 center, double scale) {
        RandomSource random = level.random;
        int count = scaledCount(level, 3, scale);
        double radius = Math.max(0.4, beam.getCurrentRadius());
        double height = Config.getBeamHeight();
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = random.nextDouble() * radius;
            double outward = 0.01 + random.nextDouble() * 0.035;
            level.addParticle(ModParticles.ABYSS_MOTE.get(),
                    center.x + Math.cos(angle) * distance,
                    center.y - height + random.nextDouble() * height * 2.0,
                    center.z + Math.sin(angle) * distance,
                    Math.cos(angle) * outward,
                    0.015 + random.nextDouble() * 0.025,
                    Math.sin(angle) * outward);
        }
    }

    private static void removeMissingBeams(ClientLevel level, Set<Integer> seen, Vec3 playerPos) {
        long now = level.getGameTime();
        Iterator<Map.Entry<Integer, BeamVisualState>> iterator = ACTIVE_BEAMS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, BeamVisualState> entry = iterator.next();
            BeamVisualState state = entry.getValue();
            if (seen.contains(entry.getKey())) {
                continue;
            }
            iterator.remove();
            boolean wasCharging = state.phase == BeamEntity.Phase.CHARGING
                    || state.phase == BeamEntity.Phase.CHARGED_WAIT;
            if (wasCharging && now - state.lastSeen <= 2 && playerPos.distanceToSqr(state.center) <= MAX_DISTANCE_SQR) {
                spawnCancelCollapse(level, state.center, particleScale(Minecraft.getInstance(), state.center));
            }
        }
    }

    private static void spawnCancelCollapse(ClientLevel level, Vec3 center, double scale) {
        RandomSource random = level.random;
        int count = scaledCount(level, 16, scale);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 2.0 + random.nextDouble() * 4.0;
            double x = center.x + Math.cos(angle) * distance;
            double y = center.y + (random.nextDouble() - 0.5) * 1.5;
            double z = center.z + Math.sin(angle) * distance;
            Vec3 velocity = center.subtract(x, y, z).normalize().scale(0.08 + random.nextDouble() * 0.08);
            level.addParticle(ModParticles.ABYSS_MOTE.get(), x, y, z,
                    velocity.x, velocity.y, velocity.z);
        }
    }

    private static double particleScale(Minecraft minecraft, Vec3 center) {
        double distanceSqr = minecraft.player == null
                ? Double.MAX_VALUE
                : minecraft.player.position().distanceToSqr(center);
        if (distanceSqr > MAX_DISTANCE_SQR) {
            return 0;
        }

        double distanceScale = distanceSqr <= FULL_DISTANCE_SQR ? 1.0 : 0.5;
        ParticleStatus status = minecraft.options.particles().get();
        double optionScale = switch (status) {
            case ALL -> 1.0;
            case DECREASED -> 0.55;
            case MINIMAL -> 0.2;
        };
        return distanceScale * optionScale;
    }

    private static int scaledCount(ClientLevel level, int baseCount, double scale) {
        double exact = baseCount * scale;
        int count = (int) exact;
        return level.random.nextDouble() < exact - count ? count + 1 : count;
    }

    private static void clearState() {
        ACTIVE_BEAMS.clear();
        activeLevel = null;
    }

    private static final class BeamVisualState {
        private BeamEntity.Phase phase;
        private Vec3 center;
        private long lastSeen;

        private BeamVisualState(BeamEntity.Phase phase, Vec3 center, long lastSeen) {
            this.phase = phase;
            this.center = center;
            this.lastSeen = lastSeen;
        }

        private void update(BeamEntity beam, Vec3 center, long lastSeen) {
            this.phase = beam.getPhase();
            this.center = center;
            this.lastSeen = lastSeen;
        }
    }
}
