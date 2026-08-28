package com.yiva.thejudgement.entity;

import com.yiva.thejudgement.Config;
import com.yiva.thejudgement.TheJudgement;
import com.yiva.thejudgement.item.JudgementStaffItem;
import com.yiva.thejudgement.sound.ModSounds;
import com.yiva.thejudgement.util.BeamMathHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import com.yiva.thejudgement.particle.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BeamEntity extends Entity {

    public enum Mode {
        BOMB,
        SWEEP
    }

    public enum Phase {
        CHARGING,
        CHARGED_WAIT,
        FIRING,
        DISSIPATING
    }

    // 同步字段
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS =
            SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TOTAL_TICKS =
            SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE_TIMER =
            SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.INT);

    // 静态按键状态存储
    private static final Map<UUID, Boolean> LEFT_KEY_DOWN = new HashMap<>();
    private static final Map<UUID, Boolean> RIGHT_KEY_DOWN = new HashMap<>();

    // 光束缓存
    public static final Map<UUID, BeamEntity> CHARGING_BEAMS = new HashMap<>();

    private UUID ownerUUID;
    private Mode mode;
    private Phase phase;
    private Vec3 centerPos;
    private Vec3 moveDirection;
    private double moveSpeed;
    private int phaseTimer;
    private int totalLifeTicks;
    private double currentRadius;
    @Nullable
    private Vec3 fixedTargetPos;

    public BeamEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.phase = Phase.CHARGING;
        this.phaseTimer = 0;
        this.totalLifeTicks = 0;
        this.currentRadius = Config.getBeamRadius();
        this.moveDirection = Vec3.ZERO;
        this.moveSpeed = 0;
        this.noPhysics = true;
        this.centerPos = Vec3.ZERO;
    }

    // ==================== 静态按键状态管理 ====================

    public static void setKeyState(UUID uuid, boolean isLeft, boolean isDown) {
        if (isLeft) {
            LEFT_KEY_DOWN.put(uuid, isDown);
        } else {
            RIGHT_KEY_DOWN.put(uuid, isDown);
        }
    }

    public static boolean isKeyDown(UUID uuid, boolean isLeft) {
        if (isLeft) {
            return LEFT_KEY_DOWN.getOrDefault(uuid, false);
        } else {
            return RIGHT_KEY_DOWN.getOrDefault(uuid, false);
        }
    }

    // ==================== 公共设置方法 ====================

    public void setOwnerUUID(UUID uuid) {
        this.ownerUUID = uuid;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void setCenterPos(Vec3 pos) {
        this.centerPos = pos;
        if (pos != null) {
            this.setPos(pos.x, pos.y, pos.z);
        }
    }

    public void setMoveDirection(Vec3 dir) {
        this.moveDirection = dir;
    }

    public void setFixedTargetPos(@Nullable Vec3 pos) {
        this.fixedTargetPos = pos;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
        this.entityData.set(PHASE, phase.ordinal());
    }

    public void setPhaseTimer(int ticks) {
        this.phaseTimer = ticks;
        this.entityData.set(PHASE_TIMER, ticks);
    }

    public void enterFiringPhase() {
        phase = Phase.FIRING;
        entityData.set(PHASE, phase.ordinal());
        phaseTimer = 0;
        entityData.set(PHASE_TIMER, 0);
        currentRadius = Config.getBeamRadius();
        entityData.set(RADIUS, (float) currentRadius);

        // 播放发射音效（服务端）
        if (!this.level().isClientSide) {
            this.level().playSound(null,
                    this.getX(), this.getY(), this.getZ(),
                    ModSounds.BEAM_SHOT.get(),
                    SoundSource.HOSTILE,
                    1.0f, 1.0f
            );
        }


        // 扫射模式：设置移动方向和速度
        if (mode == Mode.SWEEP && fixedTargetPos != null) {
            Vec3 dir = fixedTargetPos.subtract(centerPos);
            if (dir.horizontalDistanceSqr() > 0.01) {
                moveDirection = BeamMathHelper.getHorizontalDirection(dir);
            }
            moveSpeed = Config.getSweepFireSpeed();
        } else {
            moveSpeed = 0;
        }

        applyCooldownToOwner();
    }

//    private void spawnParticles() {
//        if (this.level() instanceof ServerLevel serverLevel) {
//            int particleCount = 3;
//            double radius = currentRadius * 0.8;
//            double halfHeight = Config.getBeamHeight();
//            RandomSource random = this.random; // 使用正确的类型
//
//            for (int i = 0; i < particleCount; i++) {
//                double angle = random.nextDouble() * 2 * Math.PI;
//                double distance = random.nextDouble() * radius;
//                double x = centerPos.x + Math.cos(angle) * distance;
//                double z = centerPos.z + Math.sin(angle) * distance;
//                double y = centerPos.y - halfHeight + random.nextDouble() * (halfHeight * 2);
//                serverLevel.sendParticles(ModParticles.ENERGY_SPARK.get(), x, y, z, 1, 0, 0, 0, 0);
//            }
//        }
//    }
    // ==================== 取消施法并冷却 ====================

    private void cancelAndCooldown() {
        if (!level().isClientSide) {
            applyCooldownToOwner();
            CHARGING_BEAMS.remove(ownerUUID);
            this.discard();
            TheJudgement.LOGGER.info("Beam cancelled by internal condition for " + ownerUUID);
        }
    }

    // ==================== 实体 Tick 逻辑 ====================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        totalLifeTicks++;
        entityData.set(TOTAL_TICKS, totalLifeTicks);

        if (!this.level().hasChunkAt(this.blockPosition())) {
            this.remove(RemovalReason.UNLOADED_WITH_PLAYER);
            CHARGING_BEAMS.remove(ownerUUID);
            return;
        }

        if (this.centerPos == null) {
            this.centerPos = this.position();
        }

        switch (phase) {
            case CHARGING -> tickCharging();
            case CHARGED_WAIT -> tickChargedWait();
            case FIRING -> tickFiring();
            case DISSIPATING -> tickDissipating();
        }

        entityData.set(PHASE_TIMER, phaseTimer);
        this.setPos(centerPos.x, centerPos.y, centerPos.z);
    }

    private void tickCharging() {
        phaseTimer++;

        // 扫射模式：核心向目标追踪移动，同时检查距离过近
        if (mode == Mode.SWEEP && fixedTargetPos != null) {
            double dist = centerPos.distanceTo(fixedTargetPos);
            if (dist < Config.getSweepMinDistance()) {
                cancelAndCooldown();
                return;
            }
            Vec3 delta = fixedTargetPos.subtract(centerPos);
            double distance = delta.horizontalDistance();
            if (distance > 0.1) {
                double speedPerTick = Config.getSweepTrackSpeed() * 0.05;
                Vec3 step = delta.normalize().scale(Math.min(speedPerTick, distance));
                centerPos = centerPos.add(step);
            }
        }

        int chargeTicks = (int) (Config.getChargeTime() * 20);
        if (phaseTimer >= chargeTicks) {
            phase = Phase.CHARGED_WAIT;
            entityData.set(PHASE, phase.ordinal());
            phaseTimer = 0;
            entityData.set(PHASE_TIMER, 0);
        }
    }

    private void tickChargedWait() {
        // 检查总蓄力时长是否超限
        if (totalLifeTicks > Config.getMaxChargeDuration() * 20) {
            cancelAndCooldown();
            return;
        }

        // 扫射模式继续追踪，同时检查距离过近
        if (mode == Mode.SWEEP && fixedTargetPos != null) {
            double dist = centerPos.distanceTo(fixedTargetPos);
            if (dist < Config.getSweepMinDistance()) {
                cancelAndCooldown();
                return;
            }
            Vec3 delta = fixedTargetPos.subtract(centerPos);
            double distance = delta.horizontalDistance();
            if (distance > 0.1) {
                double speedPerTick = Config.getSweepTrackSpeed() * 0.05;
                Vec3 step = delta.normalize().scale(Math.min(speedPerTick, distance));
                centerPos = centerPos.add(step);
            }
        }

        // 检查对应按键是否已松开
        boolean keyDown = (mode == Mode.BOMB) ? isKeyDown(ownerUUID, true) : isKeyDown(ownerUUID, false);
        if (!keyDown) {
            enterFiringPhase();
        }
    }

    private void tickFiring() {
        phaseTimer++;
        currentRadius = Config.getBeamRadius();
        entityData.set(RADIUS, (float) currentRadius);
        applyDamage();
//        spawnParticles();

        if (mode == Mode.SWEEP && moveDirection != Vec3.ZERO) {
            double speedPerTick = moveSpeed * 0.05;
            centerPos = centerPos.add(moveDirection.scale(speedPerTick));
        }

        int firingTicks = (int) (Config.getFiringTime() * 20);
        if (phaseTimer >= firingTicks) {
            phase = Phase.DISSIPATING;
            entityData.set(PHASE, phase.ordinal());
            phaseTimer = 0;
            entityData.set(PHASE_TIMER, 0);
        }
    }

    private void tickDissipating() {
        phaseTimer++;

        double progress = (double) phaseTimer / (int) (Config.getDissipateTime() * 20);
        double shrinkFactor = 1 - (progress * progress);
        currentRadius = Config.getBeamRadius() * Math.max(0, shrinkFactor);
        entityData.set(RADIUS, (float) currentRadius);

        if (currentRadius > 0.1) {
            applyDamage();
//            spawnParticles();
        }

        if (mode == Mode.SWEEP && moveDirection != Vec3.ZERO) {
            double speedPerTick = moveSpeed * 0.05;
            centerPos = centerPos.add(moveDirection.scale(speedPerTick));
        }

        int dissipateTicks = (int) (Config.getDissipateTime() * 20);
        if (phaseTimer >= dissipateTicks) {
            this.remove(RemovalReason.DISCARDED);
            CHARGING_BEAMS.remove(ownerUUID);
        }
    }

    // ==================== 伤害判定 ====================

    private void applyDamage() {
        if (this.level().isClientSide) return;
        if (this.centerPos == null || ownerUUID == null) return;

        Player owner = this.level().getPlayerByUUID(ownerUUID);
        if (owner == null) return;

        double height = Config.getBeamHeight();
        double cylinderHeight = height * 2;
        Vec3 cylinderBase = new Vec3(centerPos.x, centerPos.y - height, centerPos.z);

        AABB searchBox = new AABB(
                centerPos.x - currentRadius - 2,
                cylinderBase.y - 2,
                centerPos.z - currentRadius - 2,
                centerPos.x + currentRadius + 2,
                cylinderBase.y + cylinderHeight + 2,
                centerPos.z + currentRadius + 2
        );

        List<Entity> entities = this.level().getEntities(this, searchBox, e -> e instanceof LivingEntity);

        for (Entity entity : entities) {
            if (entity.getUUID().equals(ownerUUID)) continue;

            Vec3 entityPos = entity.position();
            if (BeamMathHelper.isPointInsideVerticalCylinder(entityPos, cylinderBase, cylinderHeight, currentRadius)
                    && entity instanceof LivingEntity living) {
                float damage;
                if (phase == Phase.FIRING) {
                    damage = Config.getBaseDamage();
                } else {
                    double ratio = currentRadius / Config.getBeamRadius();
                    damage = (float) (Config.getBaseDamage() * ratio);
                    if (damage < 0.5) damage = 0;
                }

                if (damage > 0) {
                    DamageSource source = this.level().damageSources().indirectMagic(this, owner);
                    living.hurt(source, damage);
                    living.invulnerableTime = 0;
                    living.hurtTime = 0;
                }
            }
        }
    }

    private void applyCooldownToOwner() {
        if (ownerUUID == null || this.level().isClientSide) return;

        Player owner = this.level().getPlayerByUUID(ownerUUID);
        if (owner == null) return;

        var mainHand = owner.getMainHandItem();
        var offHand = owner.getOffhandItem();

        if (mainHand.getItem() instanceof JudgementStaffItem staff) {
            owner.getCooldowns().addCooldown(staff, Config.getCooldown() * 20);
        } else if (offHand.getItem() instanceof JudgementStaffItem staff) {
            owner.getCooldowns().addCooldown(staff, Config.getCooldown() * 20);
        }
    }

    // ==================== Getter ====================

    public Phase getPhase() {
        return Phase.values()[entityData.get(PHASE)];
    }

    public double getCurrentRadius() {
        return entityData.get(RADIUS);
    }

    public Vec3 getCenterPos() {
        return centerPos != null ? centerPos : position();
    }

    public Mode getMode() {
        return mode;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public int getTotalLifeTicks() {
        return entityData.get(TOTAL_TICKS);
    }

    public int getPhaseTimer() {
        return entityData.get(PHASE_TIMER);
    }

    // ==================== 同步数据 ====================

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PHASE, Phase.CHARGING.ordinal());
        this.entityData.define(RADIUS, (float) Config.getBeamRadius());
        this.entityData.define(TOTAL_TICKS, 0);
        this.entityData.define(PHASE_TIMER, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}