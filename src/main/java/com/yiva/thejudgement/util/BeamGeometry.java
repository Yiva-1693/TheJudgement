package com.yiva.thejudgement.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared server/client geometry for the piercing beam.
 */
public final class BeamGeometry {

    public static final double FIXED_RIFT_Y = 180.0;
    public static final int MAX_SURFACE_EFFECTS = 16;
    private static final double SURFACE_OFFSET = 0.03;
    private static final double DAMAGE_FLOOR_MARGIN = 0.05;
    private static final int AIR_MEDIUM = 0;
    private static final int FLUID_MEDIUM = 1;
    private static final int SOLID_MEDIUM = 2;

    private BeamGeometry() {
    }

    public static double getRiftY(Level level, Vec3 core) {
        double worldTop = level.getMaxBuildHeight() - 0.05;
        double fixedY = Math.min(FIXED_RIFT_Y, worldTop);
        return fixedY > core.y ? fixedY : Math.min(worldTop, core.y + 1.0);
    }

    /**
     * Finds the first collision or fluid surface below the core.
     */
    public static double findBottomY(Level level, Vec3 core, Entity context) {
        Vec3 start = core.add(0, 0.25, 0);
        Vec3 end = new Vec3(core.x, level.getMinBuildHeight() - 1.0, core.z);
        BlockHitResult hit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                context
        ));
        if (hit.getType() == HitResult.Type.BLOCK) {
            // Keep the cylinder slightly below the visible surface so entities standing on it remain inside.
            return hit.getLocation().y - DAMAGE_FLOOR_MARGIN;
        }
        return level.getMinBuildHeight();
    }

    public static double getVisualBottomY(double damageBottomY) {
        return damageBottomY + DAMAGE_FLOOR_MARGIN + SURFACE_OFFSET;
    }

    /**
     * Finds every solid/fluid boundary above the core so each pierced shell can receive a flare.
     */
    public static List<Vec3> findUpperSurfacePositions(Level level, Vec3 core, double riftY) {
        List<Vec3> surfaces = new ArrayList<>();
        int startY = Math.max(level.getMinBuildHeight(), Mth.floor(core.y));
        int endY = Math.min(level.getMaxBuildHeight() - 1, Mth.ceil(riftY));
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
                Mth.floor(core.x), startY, Mth.floor(core.z));
        int previousMedium = getMedium(level, cursor);

        for (int y = startY + 1; y <= endY && surfaces.size() < MAX_SURFACE_EFFECTS; y++) {
            cursor.setY(y);
            int medium = getMedium(level, cursor);
            boolean isBlockUndersideExposedToAir = previousMedium == AIR_MEDIUM
                    && medium == SOLID_MEDIUM;
            if (medium != previousMedium && !isBlockUndersideExposedToAir) {
                surfaces.add(new Vec3(core.x, y + SURFACE_OFFSET, core.z));
            }
            previousMedium = medium;
        }
        return List.copyOf(surfaces);
    }

    private static int getMedium(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return AIR_MEDIUM;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.getCollisionShape(level, pos).isEmpty()) {
            return SOLID_MEDIUM;
        }
        return state.getFluidState().isEmpty() ? AIR_MEDIUM : FLUID_MEDIUM;
    }
}
