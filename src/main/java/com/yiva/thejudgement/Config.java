package com.yiva.thejudgement;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;

@Mod.EventBusSubscriber(modid = TheJudgement.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    // 通用配置实例
    public static final CommonConfig COMMON;
    public static final ForgeConfigSpec SPEC;

    static {
        // 使用 Pair 将配置对象与 Spec 绑定
        final Pair<CommonConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(CommonConfig::new);
        COMMON = specPair.getLeft();
        SPEC = specPair.getRight();
    }

    // 内部静态类，存放具体配置字段
    public static class CommonConfig {
        public final ForgeConfigSpec.DoubleValue maxChargeDuration;

        // ---------- 激光本体基础属性 ----------
        public final ForgeConfigSpec.DoubleValue beamRadius;          // 光柱半径（格）
        public final ForgeConfigSpec.DoubleValue beamHeight;          // 光柱总高度（格）
        public final ForgeConfigSpec.IntValue baseDamagePerTick;      // 第二阶段每tick伤害（生命值）

        // ---------- 三阶段时间配置（单位：秒） ----------
        public final ForgeConfigSpec.DoubleValue chargeTime;          // 第一阶段前摇时长
        public final ForgeConfigSpec.DoubleValue firingTime;          // 第二阶段持续时长
        public final ForgeConfigSpec.DoubleValue dissipateTime;       // 第三阶段消散时长

        // ---------- 左键轰炸模式 ----------
        public final ForgeConfigSpec.DoubleValue bombSphereInitRadius; // 瞄准球初始半径
        public final ForgeConfigSpec.DoubleValue bombSphereGrowRate;   // 瞄准球每秒增长半径
        public final ForgeConfigSpec.DoubleValue bombMaxChargeTime;    // 最大蓄力时间（超时自动取消）

        // ---------- 右键扫射模式 ----------
        public final ForgeConfigSpec.DoubleValue sweepCircleRadius;    // 水平参考圆半径
        public final ForgeConfigSpec.DoubleValue sweepTrackSpeed;      // 第一阶段追踪速度（格/秒）
        public final ForgeConfigSpec.DoubleValue sweepFireSpeed;       // 第二阶段飞行速度（格/秒）
        public final ForgeConfigSpec.DoubleValue sweepMinDistance;     // 最小安全距离（小于则取消）

        // ---------- 冷却与限制 ----------
        public final ForgeConfigSpec.IntValue cooldownSeconds;         // 技能冷却时间（秒）

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.push("beam_core");
            beamRadius = builder
                    .comment("光柱半径（格）")
                    .defineInRange("beamRadius", 3.0, 0.5, 10.0);
            beamHeight = builder
                    .comment("光柱总高度（格）")
                    .defineInRange("beamHeight", 40.0, 10.0, 100.0);
            baseDamagePerTick = builder
                    .comment("第二阶段每tick基础伤害（生命值）")
                    .defineInRange("baseDamagePerTick", 10, 1, 100);
            builder.pop();

            builder.push("timing");
            chargeTime = builder
                    .comment("第一阶段前摇持续时间（秒）")
                    .defineInRange("chargeTime", 1.75, 0.5, 10.0);
            firingTime = builder
                    .comment("第二阶段持续输出时间（秒）")
                    .defineInRange("firingTime", 1.5, 0.5, 10.0);
            dissipateTime = builder
                    .comment("第三阶段消散时间（秒）")
                    .defineInRange("dissipateTime", 1.5, 0.5, 10.0);
            builder.pop();

            builder.push("bomb_mode");
            bombSphereInitRadius = builder
                    .comment("轰炸模式瞄准球初始半径（格）")
                    .defineInRange("bombSphereInitRadius", 5.0, 1.0, 20.0);
            bombSphereGrowRate = builder
                    .comment("轰炸模式瞄准球每秒半径增长率（格/秒）")
                    .defineInRange("bombSphereGrowRate", 12.0, 1.0, 50.0);
            bombMaxChargeTime = builder
                    .comment("轰炸模式最大蓄力时间（秒），超时自动取消")
                    .defineInRange("bombMaxChargeTime", 4.0, 2.0, 15.0);
            builder.pop();

            builder.push("sweep_mode");
            sweepCircleRadius = builder
                    .comment("扫射模式水平参考圆半径（格）")
                    .defineInRange("sweepCircleRadius", 80.0, 10.0, 100.0);
            sweepTrackSpeed = builder
                    .comment("扫射模式第一阶段追踪速度（格/秒）")
                    .defineInRange("sweepTrackSpeed", 18.0, 1.0, 30.0);
            sweepFireSpeed = builder
                    .comment("扫射模式第二阶段飞行速度（格/秒）")
                    .defineInRange("sweepFireSpeed", 30.0, 5.0, 80.0);
            sweepMinDistance = builder
                    .comment("扫射模式最小安全距离（格），小于此值取消施法")
                    .defineInRange("sweepMinDistance", 18.0, 1.0, 30.0);
            builder.pop();

            builder.push("cooldown");
            cooldownSeconds = builder
                    .comment("施法后/取消后的冷却时间（秒）")
                    .defineInRange("cooldownSeconds", 5, 1, 30);
            builder.pop();

            builder.push("timing");
            maxChargeDuration = builder
                    .comment("最大蓄力总时长（秒），超过则自动取消并冷却")
                    .defineInRange("maxChargeDuration", 4.0, 1.0, 30.0);
            builder.pop();

        }
    }

    // 为了方便调用，封装静态获取方法（缩减代码行数）
    public static double getMaxChargeDuration() { return COMMON.maxChargeDuration.get(); }
    public static double getBeamRadius() { return COMMON.beamRadius.get(); }
    public static double getBeamHeight() { return COMMON.beamHeight.get(); }
    public static int getBaseDamage() { return COMMON.baseDamagePerTick.get(); }
    public static double getChargeTime() { return COMMON.chargeTime.get(); }
    public static double getFiringTime() { return COMMON.firingTime.get(); }
    public static double getDissipateTime() { return COMMON.dissipateTime.get(); }
    public static double getBombInitRadius() { return COMMON.bombSphereInitRadius.get(); }
    public static double getBombGrowRate() { return COMMON.bombSphereGrowRate.get(); }
    public static double getBombMaxCharge() { return COMMON.bombMaxChargeTime.get(); }
    public static double getSweepCircleRadius() { return COMMON.sweepCircleRadius.get(); }
    public static double getSweepTrackSpeed() { return COMMON.sweepTrackSpeed.get(); }
    public static double getSweepFireSpeed() { return COMMON.sweepFireSpeed.get(); }
    public static double getSweepMinDistance() { return COMMON.sweepMinDistance.get(); }
    public static int getCooldown() { return COMMON.cooldownSeconds.get(); }
}