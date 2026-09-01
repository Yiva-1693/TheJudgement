package com.yiva.thejudgement.entity;

import com.yiva.thejudgement.Config;
import com.yiva.thejudgement.TheJudgement;
import com.yiva.thejudgement.item.JudgementStaffItem;
import com.yiva.thejudgement.util.BeamGeometry;
import com.yiva.thejudgement.util.BeamMathHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import com.yiva.thejudgement.particle.ModParticles;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BeamEntity extends Entity {

    public enum Mode {
        BOMB,
        SWEEP,
        DIRECT_BOMB    // 直接轰炸（非潜行左键）
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
    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS =
            SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TOTAL_TICKS =
            SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE_TIMER =
            SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BOTTOM_Y =
            SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.FLOAT);

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
    private int phaseTimer;
    private int totalLifeTicks;
    private double currentRadius;
    private boolean hasRefundedEnergy = false;
    @Nullable
    private UUID sourceStaffId;
    @Nullable
    private Vec3 fixedTargetPos;

    public BeamEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.mode = Mode.BOMB;
        this.phase = Phase.CHARGING;
        this.phaseTimer = 0;
        this.totalLifeTicks = 0;
        this.currentRadius = Config.getBeamRadius();
        this.moveDirection = Vec3.ZERO;
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
        this.entityData.set(MODE, mode.ordinal());
    }

    public void setCenterPos(Vec3 pos) {
        this.centerPos = pos;
        if (pos != null) {
            this.setPos(pos.x, pos.y, pos.z);
            if (!level().isClientSide) {
                updateBottomY();
            }
        }
    }

    public void setMoveDirection(Vec3 dir) {
        this.moveDirection = dir;
    }

    public void setFixedTargetPos(@Nullable Vec3 pos) {
        this.fixedTargetPos = pos;
    }

    public void setSourceStaffId(UUID sourceStaffId) {
        this.sourceStaffId = sourceStaffId;
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
        updateBottomY();

        // 扫射模式：设置移动方向和速度
        if (mode == Mode.SWEEP && fixedTargetPos != null) {
            Vec3 dir = fixedTargetPos.subtract(centerPos);
            if (dir.horizontalDistanceSqr() > 0.01) {
                moveDirection = BeamMathHelper.getHorizontalDirection(dir);
            }
        }

    }

    // ==================== 取消施法 ====================

    private void cancelBeam() {
        if (!level().isClientSide) {
            CHARGING_BEAMS.remove(ownerUUID, this);
            this.discard();
            TheJudgement.LOGGER.info("Beam cancelled by internal condition for " + ownerUUID);
        }
    }

    private void updateBottomY() {
        if (centerPos != null && !level().isClientSide) {
            entityData.set(BOTTOM_Y, (float) BeamGeometry.findBottomY(level(), centerPos, this));
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
            CHARGING_BEAMS.remove(ownerUUID, this);
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

        if (!(BeamEntity.isKeyDown(ownerUUID, true)||BeamEntity.isKeyDown(ownerUUID, false)) )
            CHARGING_BEAMS.remove(ownerUUID, this);

        // 扫射模式：核心向目标追踪移动，同时检查距离过近
        if (mode == Mode.SWEEP && fixedTargetPos != null) {
            double dist = centerPos.distanceTo(fixedTargetPos);
            if (dist < Config.getSweepMinDistance()) {
                cancelBeam();
                return;
            }
            Vec3 delta = fixedTargetPos.subtract(centerPos);
            double distance = delta.horizontalDistance();
            if (distance > 0.1) {
                double speedPerTick = Config.blocksPerSecondToBlocksPerTick(Config.getSweepTrackSpeed());
                Vec3 step = delta.normalize().scale(Math.min(speedPerTick, distance));
                centerPos = centerPos.add(step);
            }
        }

        int chargeTicks = (int) (Config.getChargeTime() * 20);
        if (phaseTimer >= chargeTicks && mode != Mode.BOMB) {
            phase = Phase.CHARGED_WAIT;
            entityData.set(PHASE, phase.ordinal());
            phaseTimer = 0;
            entityData.set(PHASE_TIMER, 0);
        }

        // 扫射模式或潜行轰炸模式：核心向目标追踪移动
        if (mode == Mode.BOMB && fixedTargetPos != null) {
            double dist = centerPos.distanceTo(fixedTargetPos);
            if (dist > 0.1) {
                double speed = (mode == Mode.SWEEP) ? Config.getSweepTrackSpeed() : Config.getChaseSpeed();
                double speedPerTick = Config.blocksPerSecondToBlocksPerTick(speed);
                Vec3 delta = fixedTargetPos.subtract(centerPos);
                Vec3 step = delta.normalize().scale(Math.min(speedPerTick, dist));
                centerPos = centerPos.add(step);
            }
        }

        int chargeTick = (int) (Config.getChargeTime() * 20);
        if (phaseTimer >= chargeTick && ((mode == Mode.BOMB) || (mode == Mode.DIRECT_BOMB))) {
            // 潜行轰炸模式没有等待阶段，直接进入发射
            enterFiringPhase();
        }
    }

    private void tickChargedWait() {
        if (!(BeamEntity.isKeyDown(ownerUUID, true)||BeamEntity.isKeyDown(ownerUUID, false)) )
            CHARGING_BEAMS.remove(ownerUUID, this);
        // 检查总蓄力时长是否超限
        if (totalLifeTicks > Config.getMaxChargeDuration() * 20) {
            cancelBeam();
            return;
        }

        // 扫射模式继续追踪，同时检查距离过近
        if (mode == Mode.SWEEP && fixedTargetPos != null) {
            double dist = centerPos.distanceTo(fixedTargetPos);
            if (dist < Config.getSweepMinDistance()) {
                cancelBeam();
                return;
            }
            Vec3 delta = fixedTargetPos.subtract(centerPos);
            double distance = delta.horizontalDistance();
            if (distance > 0.1) {
                double speedPerTick = Config.blocksPerSecondToBlocksPerTick(Config.getSweepTrackSpeed());
                Vec3 step = delta.normalize().scale(Math.min(speedPerTick, distance));
                centerPos = centerPos.add(step);
            }
        }

        // 检查对应按键是否已松开
        boolean keyDown = (mode == Mode.BOMB || mode == Mode.DIRECT_BOMB) ? isKeyDown(ownerUUID, true) : isKeyDown(ownerUUID, false);
        if (!keyDown) {
            enterFiringPhase();
        }
    }

    private void tickFiring() {
        if (!(BeamEntity.isKeyDown(ownerUUID, true)||BeamEntity.isKeyDown(ownerUUID, false)) )
            CHARGING_BEAMS.remove(ownerUUID, this);
        phaseTimer++;
        currentRadius = Config.getBeamRadius();
        entityData.set(RADIUS, (float) currentRadius);
        // 潜行轰炸模式：继续向最后目标移动
        if (mode == Mode.BOMB && fixedTargetPos != null) {
            double dist = centerPos.distanceTo(fixedTargetPos);
            if (dist > 0.1) {
                double speedPerTick = Config.blocksPerSecondToBlocksPerTick(Config.getChaseSpeed());
                Vec3 delta = fixedTargetPos.subtract(centerPos);
                Vec3 step = delta.normalize().scale(Math.min(speedPerTick, dist));
                centerPos = centerPos.add(step);
            }
        }

        if (mode == Mode.SWEEP && moveDirection != Vec3.ZERO) {
            double speedPerTick = Config.blocksPerSecondToBlocksPerTick(Config.getSweepFireSpeed());
            centerPos = centerPos.add(moveDirection.scale(speedPerTick));
        }

        updateBottomY();
        applyDamage();

        int firingTicks = (int) (Config.getFiringTime() * 20);
        if (phaseTimer >= firingTicks) {
            phase = Phase.DISSIPATING;
            entityData.set(PHASE, phase.ordinal());
            phaseTimer = 0;
            entityData.set(PHASE_TIMER, 0);
        }
    }

    private void tickDissipating() {
        if (!(BeamEntity.isKeyDown(ownerUUID, true)||BeamEntity.isKeyDown(ownerUUID, false)) )
            CHARGING_BEAMS.remove(ownerUUID, this);
        phaseTimer++;
        double progress = (double) phaseTimer / (int) (Config.getDissipateTime() * 20);
        double shrinkFactor = (progress-1)*(progress-1)*(1-progress);
        currentRadius = Config.getBeamRadius() * Math.max(0, shrinkFactor);
        entityData.set(RADIUS, (float) currentRadius);

        // 潜行轰炸模式：继续向最后目标移动
        if (mode == Mode.BOMB && fixedTargetPos != null) {
            double dist = centerPos.distanceTo(fixedTargetPos);
            if (dist > 0.1) {
                double speedPerTick = Config.blocksPerSecondToBlocksPerTick(Config.getChaseSpeed());
                Vec3 delta = fixedTargetPos.subtract(centerPos);
                Vec3 step = delta.normalize().scale(Math.min(speedPerTick, dist));
                centerPos = centerPos.add(step);
            }
        }

        if (mode == Mode.SWEEP && moveDirection != Vec3.ZERO) {
            double speedPerTick = Config.blocksPerSecondToBlocksPerTick(Config.getSweepFireSpeed());
            centerPos = centerPos.add(moveDirection.scale(speedPerTick));
        }

        updateBottomY();
        if (currentRadius > 0) {
            applyDamage();
        }

        int dissipateTicks = (int) (Config.getDissipateTime() * 20);
        if (phaseTimer >= dissipateTicks) {
            this.remove(RemovalReason.DISCARDED);
            CHARGING_BEAMS.remove(ownerUUID, this);
        }
    }

    // ==================== 伤害判定 ====================

    private void applyDamage() {
        if (this.level().isClientSide) return;
        if (this.centerPos == null || ownerUUID == null) return;

        Player owner = this.level().getPlayerByUUID(ownerUUID);
        if (owner == null) return;

        double bottomY = getBeamBottomY();
        double riftY = getRiftY();
        double cylinderHeight = Math.max(0.05, riftY - bottomY);
        Vec3 cylinderBase = new Vec3(centerPos.x, bottomY, centerPos.z);

        AABB searchBox = new AABB(
                centerPos.x - currentRadius - 3,
                cylinderBase.y - 3,
                centerPos.z - currentRadius - 3,
                centerPos.x + currentRadius + 3,
                cylinderBase.y + cylinderHeight + 3,
                centerPos.z + currentRadius + 3
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
                    boolean hurt = living.hurt(source, damage);
                    living.invulnerableTime = 0;
                    living.hurtTime = 0;
                    if (hurt) {
                        spawnHitParticles(living);
                    }

                    // 直接轰炸模式：命中生物时向实际发动本次攻击的法杖返还100点能量。
                    if (mode == Mode.DIRECT_BOMB && phase == Phase.FIRING && !hasRefundedEnergy) {
                        hasRefundedEnergy = restoreSourceStaffEnergy(owner);
                    }
                }
            }
        }
    }

    private void spawnHitParticles(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel) || totalLifeTicks % 4 != 0) {
            return;
        }

        serverLevel.sendParticles(
                ModParticles.ENERGY_SPARK.get(),
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                5,
                target.getBbWidth() * 0.35, target.getBbHeight() * 0.25, target.getBbWidth() * 0.35,
                0.08
        );
    }

    private boolean restoreSourceStaffEnergy(Player owner) {
        if (sourceStaffId == null) {
            return false;
        }
        for (int slot = 0; slot < owner.getInventory().getContainerSize(); slot++) {
            ItemStack stack = owner.getInventory().getItem(slot);
            if (JudgementStaffItem.hasEnergyId(stack, sourceStaffId)) {
                JudgementStaffItem.restoreEnergy(stack, JudgementStaffItem.ENERGY_COST);
                return true;
            }
        }
        return false;
    }

    // ==================== Getter ====================

    public Phase getPhase() {
        return Phase.values()[entityData.get(PHASE)];
    }

    public double getCurrentRadius() {
        return entityData.get(RADIUS);
    }

    public Vec3 getCenterPos() {
        return level().isClientSide ? position() : (centerPos != null ? centerPos : position());
    }

    public Mode getMode() {
        int ordinal = entityData.get(MODE);
        return ordinal >= 0 && ordinal < Mode.values().length ? Mode.values()[ordinal] : Mode.BOMB;
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

    public double getBeamBottomY() {
        return entityData.get(BOTTOM_Y);
    }

    public double getRiftY() {
        return BeamGeometry.getRiftY(level(), getCenterPos());
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        Vec3 center = position();
        double bottomY = Math.min(getBeamBottomY(), center.y);
        double topY = Math.max(getRiftY(), center.y);
        double horizontalPadding = Math.max(16.0, Config.getBeamRadius() + 2.0);
        return new AABB(
                center.x - horizontalPadding, bottomY - 1.0, center.z - horizontalPadding,
                center.x + horizontalPadding, topY + 1.0, center.z + horizontalPadding
        );
    }

    // ==================== 同步数据 ====================

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PHASE, Phase.CHARGING.ordinal());
        this.entityData.define(MODE, Mode.BOMB.ordinal());
        this.entityData.define(RADIUS, (float) Config.getBeamRadius());
        this.entityData.define(TOTAL_TICKS, 0);
        this.entityData.define(PHASE_TIMER, 0);
        this.entityData.define(BOTTOM_Y, 0.0f);
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
