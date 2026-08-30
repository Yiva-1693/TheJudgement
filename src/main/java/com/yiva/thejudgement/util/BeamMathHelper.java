package com.yiva.thejudgement.util;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BeamMathHelper {

    /**
     * 计算射线与球体的交点（取离起点最近的交点）
     * @param origin 射线起点
     * @param direction 射线方向（必须为单位向量）
     * @param sphereCenter 球心坐标
     * @param radius 球体半径
     * @return 交点坐标，若无交点则返回空 Optional
     */

    public static Optional<Vec3> intersectRayWithSphere(Vec3 origin, Vec3 direction, Vec3 sphereCenter, double radius) {
        Vec3 oc = origin.subtract(sphereCenter);
        double a = direction.dot(direction);
        double b = 2.0 * oc.dot(direction);
        double c = oc.dot(oc) - radius * radius;
        double discriminant = b * b - 4 * a * c;

        if (discriminant < 0) {
            return Optional.empty();
        }

        double sqrtDisc = Math.sqrt(discriminant);
        double t1 = (-b - sqrtDisc) / (2 * a);
        double t2 = (-b + sqrtDisc) / (2 * a);

        // 取最近的正交点（t > 0 表示在射线前方）
        double t = Double.MAX_VALUE;
        if (t1 > 0) t = Math.min(t, t1);
        if (t2 > 0) t = Math.min(t, t2);

        if (t == Double.MAX_VALUE) {
            return Optional.empty();
        }
        return Optional.of(origin.add(direction.scale(t)));
    }

    /**
     * 计算射线与水平圆的交点（忽略方向向量的竖直分量）
     * 适用于扫射模式：玩家视线与地面参考圆的交点
     * @param origin 射线起点（玩家眼睛位置）
     * @param direction 射线方向（必须为单位向量）
     * @param circleCenter 圆心坐标（XZ平面有效，Y轴为圆所在高度）
     * @param radius 圆的半径
     * @return 交点坐标，若无交点则返回空 Optional
     */
    public static Optional<Vec3> intersectRayWithHorizontalCircle(Vec3 origin, Vec3 direction, Vec3 circleCenter, double radius) {
        // 如果视线完全水平（没有竖直分量），则永远不会与水平圆相交（忽略竖直分量后为平行线）
        if (Math.abs(direction.y) < 1e-7) {
            return Optional.empty();
        }

        // 计算射线与 circleCenter.y 所在水平面的交点
        double t = (circleCenter.y - origin.y) / direction.y;
        if (t < 0) {
            return Optional.empty(); // 交点在视线后方
        }

        Vec3 hitPoint = origin.add(direction.scale(t));
        // 忽略高度，只检查水平距离
        double dx = hitPoint.x - circleCenter.x;
        double dz = hitPoint.z - circleCenter.z;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        if (horizontalDist <= radius) {
            return Optional.of(hitPoint);
        }
        return Optional.empty();
    }

    /**
     * 判断一个点是否位于垂直圆柱体内部
     * @param point 待检测点坐标
     * @param baseCenter 圆柱底面中心坐标（Y轴为底面高度）
     * @param height 圆柱总高度（向上延伸）
     * @param radius 圆柱半径
     * @return 是否在圆柱体内（包含边界）
     */
    public static boolean isPointInsideVerticalCylinder(Vec3 point, Vec3 baseCenter, double height, double radius) {
        // 检查竖直范围：底面 Y 到 底面 Y + height
        double bottomY = baseCenter.y;
        double topY = baseCenter.y + height;
        if (point.y < bottomY || point.y > topY) {
            return false;
        }

        // 检查水平距离
        double dx = point.x - baseCenter.x;
        double dz = point.z - baseCenter.z;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        return horizontalDist <= radius;
    }

    /**
     * 将视线方向投影到水平面（忽略竖直分量），并返回单位向量
     * 用于扫射模式确定水平方向
     */
    public static Vec3 getHorizontalDirection(Vec3 direction) {
        Vec3 horizontal = new Vec3(direction.x, 0, direction.z).normalize();
        // 如果水平分量几乎为零，返回正Z方向作为保底
        if (horizontal.lengthSqr() < 1e-7) {
            return new Vec3(0, 0, 1);
        }
        return horizontal;
    }

    /**
     * 获取视线与动态球相交处的“虚拟方块”坐标（向下取整）
     * 轰炸模式用：无视实际方块碰撞，只取交点所在的方块整数坐标
     */
    public static Vec3 getBlockPosFromVec3(Vec3 pos) {
        return new Vec3(Math.floor(pos.x), Math.floor(pos.y), Math.floor(pos.z));
    }
    public static Optional<Vec3> findEntityOnLine(Level level, Vec3 start, Vec3 end, LivingEntity exclude) {
        // 1. 检测方块，得到方块碰撞距离
        BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, exclude));
        double blockDist = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation().distanceTo(start) : Double.MAX_VALUE;

        // 2. 搜索射线附近的生物
        AABB searchBox = new AABB(start, end).inflate(1.0);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != exclude && e.isAlive());

        LivingEntity closest = null;
        double closestDist = blockDist; // 只有生物距离小于方块距离才有效

        for (LivingEntity entity : entities) {
            Optional<Vec3> hit = entity.getBoundingBox().inflate(0.1).clip(start, end);
            if (hit.isPresent()) {
                double dist = hit.get().distanceTo(start);
                if (dist < closestDist) {
                    closest = entity;
                    closestDist = dist;
                }
            }
        }

        if (closest != null) {
            Vec3 pos = closest.position();
            return Optional.of(new Vec3(Math.floor(pos.x), Math.floor(pos.y), Math.floor(pos.z)));
        }
        return Optional.empty();
    }
}