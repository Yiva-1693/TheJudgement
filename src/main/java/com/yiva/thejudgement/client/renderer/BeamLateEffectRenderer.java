package com.yiva.thejudgement.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.yiva.thejudgement.Config;
import com.yiva.thejudgement.TheJudgement;
import com.yiva.thejudgement.client.particle.BeamParticleController;
import com.yiva.thejudgement.entity.BeamEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Draws the emissive beam overlay and contact flare after translucent world and weather rendering.
 */
@Mod.EventBusSubscriber(modid = TheJudgement.MOD_ID, value = Dist.CLIENT)
public final class BeamLateEffectRenderer {

    private static final double MAX_RENDER_DISTANCE_SQR = 192.0 * 192.0;
    private static final int GLOW_SEGMENTS = 32;

    private BeamLateEffectRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Vec3 cameraPos = event.getCamera().getPosition();
        beginAdditiveRendering();
        try {
            for (Entity entity : minecraft.level.entitiesForRendering()) {
                if (!(entity instanceof BeamEntity beam) || !shouldRender(beam)) {
                    continue;
                }

                Vec3 impact = BeamParticleController.getImpactPosition(beam);
                if (cameraPos.distanceToSqr(impact) > MAX_RENDER_DISTANCE_SQR) {
                    continue;
                }

                renderLateBeam(event.getPoseStack(), beam, impact, cameraPos, event.getPartialTick());
                renderImpactFlare(event, beam, impact, cameraPos);
            }
        } finally {
            endAdditiveRendering();
        }
    }

    private static boolean shouldRender(BeamEntity beam) {
        return beam.getPhase() == BeamEntity.Phase.FIRING
                || (beam.getPhase() == BeamEntity.Phase.DISSIPATING && beam.getCurrentRadius() > 0.02);
    }

    private static void renderLateBeam(PoseStack poseStack, BeamEntity beam, Vec3 impact,
                                       Vec3 cameraPos, float partialTick) {
        Vec3 source = beam.position().add(0, Config.getBeamHeight(), 0);
        double beamHeight = Math.max(0.05, source.y - impact.y);
        double cameraX = cameraPos.x - impact.x;
        double cameraZ = cameraPos.z - impact.z;
        double horizontalLength = Math.sqrt(cameraX * cameraX + cameraZ * cameraZ);
        if (horizontalLength < 1.0E-5) {
            cameraX = 0;
            cameraZ = 1;
            horizontalLength = 1;
        }

        float tangentX = (float) (cameraZ / horizontalLength);
        float tangentZ = (float) (-cameraX / horizontalLength);
        double radius = getVisualRadius(beam, partialTick);

        poseStack.pushPose();
        poseStack.translate(impact.x - cameraPos.x, impact.y - cameraPos.y, impact.z - cameraPos.z);
        Matrix4f matrix = poseStack.last().pose();
        drawVerticalQuad(matrix, beamHeight, tangentX, tangentZ, radius * 1.15,
                0.34f, 0.08f, 1.0f, 0.22f);
        // Only the narrow emissive core ignores depth so cloud and fluid colors cannot cover it.
        // The wider glow remains depth-tested to reduce visible bleed through solid terrain.
        RenderSystem.disableDepthTest();
        try {
            drawVerticalQuad(matrix, beamHeight, tangentX, tangentZ, radius * 0.42,
                    1.0f, 0.96f, 1.0f, 0.72f);
        } finally {
            RenderSystem.enableDepthTest();
        }
        poseStack.popPose();
    }

    private static void renderImpactFlare(RenderLevelStageEvent event, BeamEntity beam,
                                          Vec3 impact, Vec3 cameraPos) {
        float partialTick = event.getPartialTick();
        double intensity = getEffectIntensity(beam, partialTick);
        double radius = Math.max(1.4, getVisualRadius(beam, partialTick) * 2.1) * intensity;
        double time = beam.getTotalLifeTicks() + partialTick;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(impact.x - cameraPos.x, impact.y - cameraPos.y, impact.z - cameraPos.z);
        drawRadialGlow(poseStack.last().pose(), radius * 1.35, true,
                0.36f, 0.08f, 1.0f, 0.55f);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(impact.x - cameraPos.x, impact.y - cameraPos.y + 0.08, impact.z - cameraPos.z);
        poseStack.mulPose(event.getCamera().rotation());
        drawRadialGlow(poseStack.last().pose(), radius, false,
                0.72f, 0.38f, 1.0f, 0.72f);

        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) (time * 2.8)));
        drawCrossRays(poseStack.last().pose(), radius * 1.9, radius * 0.11,
                0.9f, 0.66f, 1.0f, 0.72f);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) (-time * 1.7 + 45.0)));
        drawCrossRays(poseStack.last().pose(), radius * 1.25, radius * 0.075,
                1.0f, 0.96f, 1.0f, 0.86f);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static double getVisualRadius(BeamEntity beam, float partialTick) {
        double radius = Math.max(0.05, beam.getCurrentRadius());
        double time = beam.getTotalLifeTicks() + partialTick;
        double jitter = Math.sin(time * 2.37 + beam.getId() * 0.71) * 0.025
                + Math.sin(time * 4.91 + beam.getId() * 1.13) * 0.015;
        if (beam.getPhase() == BeamEntity.Phase.FIRING) {
            double progress = Math.min(1.0, (beam.getPhaseTimer() + partialTick) / 2.0);
            double eased = progress * progress * (3.0 - 2.0 * progress);
            return radius * (2.2 + (1.0 - 2.2) * eased) * (1.0 + jitter);
        }
        return radius * (1.0 + jitter);
    }

    private static double getEffectIntensity(BeamEntity beam, float partialTick) {
        double pulse = 1.0 + Math.sin((beam.getTotalLifeTicks() + partialTick) * 0.82) * 0.07;
        if (beam.getPhase() == BeamEntity.Phase.DISSIPATING) {
            double radiusRatio = beam.getCurrentRadius() / Math.max(0.01, Config.getBeamRadius());
            return Math.max(0.18, Math.min(1.0, radiusRatio)) * pulse;
        }
        return pulse;
    }

    private static void drawVerticalQuad(Matrix4f matrix, double height, float tangentX, float tangentZ,
                                         double halfWidth, float red, float green, float blue, float alpha) {
        BufferBuilder builder = begin(VertexFormat.Mode.QUADS);
        float x = (float) (tangentX * halfWidth);
        float z = (float) (tangentZ * halfWidth);
        builder.vertex(matrix, -x, 0, -z).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix, x, 0, z).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix, x, (float) height, z).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix, -x, (float) height, -z).color(red, green, blue, alpha).endVertex();
        Tesselator.getInstance().end();
    }

    private static void drawRadialGlow(Matrix4f matrix, double radius, boolean horizontal,
                                       float red, float green, float blue, float centerAlpha) {
        BufferBuilder builder = begin(VertexFormat.Mode.TRIANGLE_FAN);
        builder.vertex(matrix, 0, horizontal ? 0.035f : 0, 0)
                .color(red, green, blue, centerAlpha).endVertex();
        for (int i = 0; i <= GLOW_SEGMENTS; i++) {
            double angle = Math.PI * 2.0 * i / GLOW_SEGMENTS;
            float first = (float) (Math.cos(angle) * radius);
            float second = (float) (Math.sin(angle) * radius);
            if (horizontal) {
                builder.vertex(matrix, first, 0.035f, second).color(red, green, blue, 0).endVertex();
            } else {
                builder.vertex(matrix, first, second, 0).color(red, green, blue, 0).endVertex();
            }
        }
        Tesselator.getInstance().end();
    }

    private static void drawCrossRays(Matrix4f matrix, double length, double halfWidth,
                                      float red, float green, float blue, float centerAlpha) {
        BufferBuilder builder = begin(VertexFormat.Mode.QUADS);
        addHorizontalRay(builder, matrix, length, halfWidth, red, green, blue, centerAlpha);
        addVerticalRay(builder, matrix, length, halfWidth, red, green, blue, centerAlpha);
        Tesselator.getInstance().end();
    }

    private static void addHorizontalRay(BufferBuilder builder, Matrix4f matrix, double length, double halfWidth,
                                         float red, float green, float blue, float alpha) {
        builder.vertex(matrix, (float) -length, (float) -halfWidth, 0).color(red, green, blue, 0).endVertex();
        builder.vertex(matrix, 0, (float) -halfWidth, 0).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix, 0, (float) halfWidth, 0).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix, (float) -length, (float) halfWidth, 0).color(red, green, blue, 0).endVertex();
        builder.vertex(matrix, 0, (float) -halfWidth, 0).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix, (float) length, (float) -halfWidth, 0).color(red, green, blue, 0).endVertex();
        builder.vertex(matrix, (float) length, (float) halfWidth, 0).color(red, green, blue, 0).endVertex();
        builder.vertex(matrix, 0, (float) halfWidth, 0).color(red, green, blue, alpha).endVertex();
    }

    private static void addVerticalRay(BufferBuilder builder, Matrix4f matrix, double length, double halfWidth,
                                       float red, float green, float blue, float alpha) {
        builder.vertex(matrix, (float) -halfWidth, (float) -length, 0).color(red, green, blue, 0).endVertex();
        builder.vertex(matrix, (float) halfWidth, (float) -length, 0).color(red, green, blue, 0).endVertex();
        builder.vertex(matrix, (float) halfWidth, 0, 0).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix, (float) -halfWidth, 0, 0).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix, (float) -halfWidth, 0, 0).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix, (float) halfWidth, 0, 0).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix, (float) halfWidth, (float) length, 0).color(red, green, blue, 0).endVertex();
        builder.vertex(matrix, (float) -halfWidth, (float) length, 0).color(red, green, blue, 0).endVertex();
    }

    private static BufferBuilder begin(VertexFormat.Mode mode) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(mode, DefaultVertexFormat.POSITION_COLOR);
        return builder;
    }

    private static void beginAdditiveRendering() {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        );
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
    }

    private static void endAdditiveRendering() {
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
