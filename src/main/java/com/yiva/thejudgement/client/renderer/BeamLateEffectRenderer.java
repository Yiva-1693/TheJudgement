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
import net.minecraft.resources.ResourceLocation;
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
    private static final ResourceLocation WHITE_BEAM_TEXTURE =
            new ResourceLocation(TheJudgement.MOD_ID, "textures/entity/white_beam.png");
    private static final ResourceLocation SURFACE_TEXTURE =
            new ResourceLocation(TheJudgement.MOD_ID, "textures/entity/surface.png");
    private static final ResourceLocation BEAM_TEXTURE =
            new ResourceLocation(TheJudgement.MOD_ID, "textures/entity/abyss_laser_beam_single.png");

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

                renderLateBeam(beam, impact, cameraPos, event.getPartialTick());
                renderImpactFlare(event, beam, impact, cameraPos, 1.0, 0.0);
                int surfaceIndex = 0;
                for (Vec3 surface : BeamParticleController.getSurfacePositions(beam)) {
                    if (Math.abs(surface.y - impact.y) < 0.5) {
                        continue;
                    }
                    renderImpactFlare(event, beam, surface, cameraPos, 0.58, surfaceIndex * 7.3);
                    surfaceIndex++;
                }
            }
        } finally {
            endAdditiveRendering();
        }
    }

    private static boolean shouldRender(BeamEntity beam) {
        return beam.getPhase() == BeamEntity.Phase.FIRING
                || (beam.getPhase() == BeamEntity.Phase.DISSIPATING && beam.getCurrentRadius() > 0.02);
    }

    private static void renderLateBeam(BeamEntity beam, Vec3 impact, Vec3 cameraPos, float partialTick) {
        Vec3 source = new Vec3(beam.getX(), beam.getRiftY(), beam.getZ());
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

        // RenderLevelStageEvent 的矩阵已经包含视角变换。立即模式顶点再使用它会重复应用相机旋转，
        // 因此这里从单位矩阵开始，只做一次世界坐标到相机相对坐标的平移。
        PoseStack poseStack = new PoseStack();
        poseStack.translate(impact.x - cameraPos.x, impact.y - cameraPos.y, impact.z - cameraPos.z);
        Matrix4f matrix = poseStack.last().pose();

        // 完整激光在天气之后一次绘制，避免实体渲染与补光渲染形成两根光柱。
        // 保持深度测试启用，使实体方块能够正确遮挡光柱。
        drawVerticalQuad(matrix, beamHeight, tangentX, tangentZ, radius * 1.18,
                0.34f, 0.08f, 1.0f, 0.24f);
        drawTexturedVerticalBand(matrix, beamHeight, tangentX, tangentZ,
                radius * 0.72, radius, SURFACE_TEXTURE, 0.75f);
        drawTexturedVerticalBand(matrix, beamHeight, tangentX, tangentZ,
                radius * 0.36, radius * 0.72, BEAM_TEXTURE, 0.95f);
        drawTexturedVerticalBand(matrix, beamHeight, tangentX, tangentZ,
                0, radius * 0.36, WHITE_BEAM_TEXTURE, 1.0f);
    }

    private static void renderImpactFlare(RenderLevelStageEvent event, BeamEntity beam,
                                          Vec3 impact, Vec3 cameraPos,
                                          double sizeScale, double timeOffset) {
        float partialTick = event.getPartialTick();
        double intensity = getEffectIntensity(beam, partialTick);
        double radius = Math.max(1.4, getVisualRadius(beam, partialTick) * 2.1)
                * intensity * sizeScale;
        double time = beam.getTotalLifeTicks() + partialTick + timeOffset;

        PoseStack poseStack = new PoseStack();
        poseStack.translate(impact.x - cameraPos.x, impact.y - cameraPos.y, impact.z - cameraPos.z);
        drawRadialGlow(poseStack.last().pose(), radius * 1.35, true,
                0.36f, 0.08f, 1.0f, 0.55f);

        poseStack = new PoseStack();
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

    private static void drawTexturedVerticalBand(Matrix4f matrix, double height, float tangentX, float tangentZ,
                                                 double innerRadius, double outerRadius,
                                                 ResourceLocation texture, float alpha) {
        RenderSystem.setShaderTexture(0, texture);
        BufferBuilder builder = beginTextured();
        if (innerRadius <= 0) {
            addTexturedVerticalQuad(builder, matrix, height, tangentX, tangentZ,
                    -outerRadius, outerRadius, alpha);
        } else {
            addTexturedVerticalQuad(builder, matrix, height, tangentX, tangentZ,
                    -outerRadius, -innerRadius, alpha);
            addTexturedVerticalQuad(builder, matrix, height, tangentX, tangentZ,
                    innerRadius, outerRadius, alpha);
        }
        Tesselator.getInstance().end();
    }

    private static void addTexturedVerticalQuad(BufferBuilder builder, Matrix4f matrix, double height,
                                                float tangentX, float tangentZ,
                                                double fromRadius, double toRadius, float alpha) {
        float x1 = (float) (tangentX * fromRadius);
        float z1 = (float) (tangentZ * fromRadius);
        float x2 = (float) (tangentX * toRadius);
        float z2 = (float) (tangentZ * toRadius);
        builder.vertex(matrix, x1, 0, z1).uv(0, 1).color(1, 1, 1, alpha).endVertex();
        builder.vertex(matrix, x2, 0, z2).uv(1, 1).color(1, 1, 1, alpha).endVertex();
        builder.vertex(matrix, x2, (float) height, z2).uv(1, 0).color(1, 1, 1, alpha).endVertex();
        builder.vertex(matrix, x1, (float) height, z1).uv(0, 0).color(1, 1, 1, alpha).endVertex();
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

    private static BufferBuilder beginTextured() {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
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
