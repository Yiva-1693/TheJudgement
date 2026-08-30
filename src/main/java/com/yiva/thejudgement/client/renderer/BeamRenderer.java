package com.yiva.thejudgement.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yiva.thejudgement.Config;
import com.yiva.thejudgement.entity.BeamEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class BeamRenderer extends EntityRenderer<BeamEntity> {

    private static final ResourceLocation WHITE_BEAM_TEXTURE =
            new ResourceLocation("thejudgement", "textures/entity/white_beam.png");

    private static final ResourceLocation SURFACE_TEXTURE =
            new ResourceLocation("thejudgement", "textures/entity/surface.png");

    private static final ResourceLocation BEAM_TEXTURE =
            new ResourceLocation("thejudgement", "textures/entity/abyss_laser_beam_single.png");

    private static final ResourceLocation[] RIFT_GROW = {
            new ResourceLocation("thejudgement", "textures/entity/rift/dimensional_rift_grow_0.png"),
            new ResourceLocation("thejudgement", "textures/entity/rift/dimensional_rift_grow_1.png"),
            new ResourceLocation("thejudgement", "textures/entity/rift/dimensional_rift_grow_2.png"),
            new ResourceLocation("thejudgement", "textures/entity/rift/dimensional_rift_grow_3.png")
    };
    private static final ResourceLocation[] RIFT_IDLE = {
            new ResourceLocation("thejudgement", "textures/entity/rift/dimensional_rift_idle1.png"),
            new ResourceLocation("thejudgement", "textures/entity/rift/dimensional_rift_idle2.png"),
            new ResourceLocation("thejudgement", "textures/entity/rift/dimensional_rift_idle3.png"),
            new ResourceLocation("thejudgement", "textures/entity/rift/dimensional_rift_idle4.png")
    };
    private static final ResourceLocation[] PORTAL_GROW = {
            new ResourceLocation("thejudgement", "textures/entity/portal/abyss_portal_grow_0.png"),
            new ResourceLocation("thejudgement", "textures/entity/portal/abyss_portal_grow_1.png"),
            new ResourceLocation("thejudgement", "textures/entity/portal/abyss_portal_grow_2.png"),
            new ResourceLocation("thejudgement", "textures/entity/portal/abyss_portal_grow_3.png"),
            new ResourceLocation("thejudgement", "textures/entity/portal/abyss_portal_grow_4.png"),
            new ResourceLocation("thejudgement", "textures/entity/portal/abyss_portal_grow_5.png"),
            new ResourceLocation("thejudgement", "textures/entity/portal/abyss_portal_grow_6.png"),
            new ResourceLocation("thejudgement", "textures/entity/portal/abyss_portal_grow_7.png")
    };
    private static final ResourceLocation[] PORTAL_IDLE = {
            new ResourceLocation("thejudgement", "textures/entity/portal/abyss_portal_idle_0.png"),
            new ResourceLocation("thejudgement", "textures/entity/portal/abyss_portal_idle_1.png"),
            new ResourceLocation("thejudgement", "textures/entity/portal/abyss_portal_idle_2.png"),
            new ResourceLocation("thejudgement", "textures/entity/portal/abyss_portal_idle_3.png")
    };
    private static final ResourceLocation AIM_MARKER_TEXTURE =
            new ResourceLocation("thejudgement", "textures/entity/aim_marker.png");

    private static final float RIFT_RADIUS = 15.0f;
    private static final float PORTAL_RADIUS = 8.0f;
    private static final float AIM_MARKER_RADIUS = 3f;
    private static final double RIFT_HEIGHT_OFFSET = 0.02;
    private static final double PORTAL_HEIGHT_OFFSET = 0.04;

    private static final int RIFT_GROW_INTERVAL = 12;
    private static final int RIFT_IDLE_INTERVAL = 3;
    private static final int PORTAL_IDLE_INTERVAL = 3;

    public BeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(BeamEntity entity) {
        return BEAM_TEXTURE;
    }

    @Override
    public void render(BeamEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        Vec3 center = entity.position();
        double radius = entity.getCurrentRadius();
        double height = Config.getBeamHeight();

        BeamEntity.Phase currentPhase = entity.getPhase();

        int fullBright = LightTexture.FULL_BRIGHT;

        switch (currentPhase) {
            case CHARGING, CHARGED_WAIT:
                renderRiftGrow(entity, center, partialTicks, poseStack, buffer, fullBright);
                renderAimMarker(entity, center, partialTicks, poseStack, buffer, fullBright);
                break;
            case FIRING:
                renderRiftIdle(entity, center, partialTicks, poseStack, buffer, fullBright);
                renderPortalIdle(entity, center, partialTicks, poseStack, buffer, fullBright);
                if (radius > 0.01) {
                    renderBeam(entity, center, radius, height, partialTicks, poseStack, buffer, fullBright);
                }
                break;
            case DISSIPATING:
                renderRiftReverseGrow(entity, center, partialTicks, poseStack, buffer, fullBright);
                renderPortalReverseGrow(entity, center, partialTicks, poseStack, buffer, fullBright);
                if (radius > 0.01) {
                    renderBeam(entity, center, radius, height, partialTicks, poseStack, buffer, fullBright);
                }
                break;
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void renderBillboardBand(MultiBufferSource buffer, PoseStack poseStack,
                                     float bottomY, float topY, float normalX, float normalZ,
                                     float tangentX, float tangentZ, double innerRadius, double outerRadius,
                                     ResourceLocation texture, int packedLight, float alpha,
                                     boolean translucent) {
        RenderType renderType = RenderType.beaconBeam(texture, translucent);
        VertexConsumer consumer = buffer.getBuffer(renderType);
        Matrix4f pose = poseStack.last().pose();

        if (innerRadius <= 0) {
            addBillboardQuad(consumer, pose, bottomY, topY, normalX, normalZ, tangentX, tangentZ,
                    -outerRadius, outerRadius, packedLight, alpha);
        } else {
            addBillboardQuad(consumer, pose, bottomY, topY, normalX, normalZ, tangentX, tangentZ,
                    -outerRadius, -innerRadius, packedLight, alpha);
            addBillboardQuad(consumer, pose, bottomY, topY, normalX, normalZ, tangentX, tangentZ,
                    innerRadius, outerRadius, packedLight, alpha);
        }
    }

    private void addBillboardQuad(VertexConsumer consumer, Matrix4f pose,
                                  float bottomY, float topY, float normalX, float normalZ,
                                  float tangentX, float tangentZ, double fromRadius, double toRadius,
                                  int packedLight, float alpha) {
        float x1 = (float) (tangentX * fromRadius);
        float z1 = (float) (tangentZ * fromRadius);
        float x2 = (float) (tangentX * toRadius);
        float z2 = (float) (tangentZ * toRadius);

        consumer.vertex(pose, x1, bottomY, z1)
                .color(1f, 1f, 1f, alpha)
                .uv(0, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalX, 0, normalZ)
                .endVertex();
        consumer.vertex(pose, x2, bottomY, z2)
                .color(1f, 1f, 1f, alpha)
                .uv(1, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalX, 0, normalZ)
                .endVertex();
        consumer.vertex(pose, x2, topY, z2)
                .color(1f, 1f, 1f, alpha)
                .uv(1, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalX, 0, normalZ)
                .endVertex();
        consumer.vertex(pose, x1, topY, z1)
                .color(1f, 1f, 1f, alpha)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalX, 0, normalZ)
                .endVertex();
    }

    private void renderBeam(BeamEntity entity, Vec3 center, double radius, double height,
                            float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(center.x - entity.getX(), center.y - entity.getY(), center.z - entity.getZ());

        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double cameraX = cameraPos.x - center.x;
        double cameraZ = cameraPos.z - center.z;
        double horizontalLength = Math.sqrt(cameraX * cameraX + cameraZ * cameraZ);
        if (horizontalLength < 1.0E-5) {
            cameraX = 0;
            cameraZ = 1;
            horizontalLength = 1;
        }

        float normalX = (float) (cameraX / horizontalLength);
        float normalZ = (float) (cameraZ / horizontalLength);
        float tangentX = normalZ;
        float tangentZ = -normalX;

        double visualRadius = getVisualRadius(entity, radius, partialTicks);
        float bottomY = -(float) height;
        float topY = (float) height;

        // 三层互不重叠，避免透明面重复混合产生圆柱明暗。
        renderBillboardBand(buffer, poseStack, bottomY, topY, normalX, normalZ, tangentX, tangentZ,
                visualRadius * 0.72, visualRadius, SURFACE_TEXTURE, packedLight, 0.75f, true);

        renderBillboardBand(buffer, poseStack, bottomY, topY, normalX, normalZ, tangentX, tangentZ,
                visualRadius * 0.36, visualRadius * 0.72, BEAM_TEXTURE, packedLight, 0.95f, true);

        renderBillboardBand(buffer, poseStack, bottomY, topY, normalX, normalZ, tangentX, tangentZ,
                0, visualRadius * 0.36, WHITE_BEAM_TEXTURE, packedLight, 1.0f, false);

        poseStack.popPose();
    }

    private double getVisualRadius(BeamEntity entity, double radius, float partialTicks) {
        double time = entity.getTotalLifeTicks() + partialTicks;
        double jitter = Math.sin(time * 2.37 + entity.getId() * 0.71) * 0.025
                + Math.sin(time * 4.91 + entity.getId() * 1.13) * 0.015
                + Math.sin(time * 7.43 + entity.getId() * 0.37) * 0.01;

        if (entity.getPhase() == BeamEntity.Phase.FIRING) {
            double progress = Math.min(1.0, (entity.getPhaseTimer() + partialTicks) / 2.0);
            double easedProgress = progress * progress * (3.0 - 2.0 * progress);
            double burstScale = 2.2 + (1.0 - 2.2) * easedProgress;
            return radius * (progress < 1.0 ? burstScale : 1.0 + jitter);
        }

        return radius * (1.0 + jitter);
    }

    private void renderRiftGrow(BeamEntity entity, Vec3 center, float partialTicks,
                                PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int totalTicks = entity.getTotalLifeTicks();
        int idx = Math.min(totalTicks / RIFT_GROW_INTERVAL, 3);
        ResourceLocation tex = RIFT_GROW[idx];
        renderHorizontalQuad(poseStack, buffer, entity, center,
                Config.getBeamHeight() + RIFT_HEIGHT_OFFSET, RIFT_RADIUS, tex, packedLight, 1.0f, false);
    }

    private void renderRiftIdle(BeamEntity entity, Vec3 center, float partialTicks,
                                PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int totalTicks = entity.getTotalLifeTicks();
        int idx = (totalTicks / RIFT_IDLE_INTERVAL) % 4;
        ResourceLocation tex = RIFT_IDLE[idx];
        renderHorizontalQuad(poseStack, buffer, entity, center,
                Config.getBeamHeight() + RIFT_HEIGHT_OFFSET, RIFT_RADIUS, tex, packedLight, 1.0f, false);
    }

    private void renderRiftReverseGrow(BeamEntity entity, Vec3 center, float partialTicks,
                                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int phaseTicks = entity.getPhaseTimer();
        int totalFrames = RIFT_GROW.length;
        int duration = (int) (Config.getDissipateTime() * 20);
        int progress = Math.min(phaseTicks, duration);
        int idx = totalFrames - 1 - (progress * totalFrames / duration);
        idx = Math.max(0, Math.min(totalFrames - 1, idx));
        ResourceLocation tex = RIFT_GROW[idx];
        renderHorizontalQuad(poseStack, buffer, entity, center,
                Config.getBeamHeight() + RIFT_HEIGHT_OFFSET, RIFT_RADIUS, tex, packedLight, 1.0f, false);
    }

    private void renderPortalIdle(BeamEntity entity, Vec3 center, float partialTicks,
                                  PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int phaseTicks = entity.getPhaseTimer();
        int idx = (phaseTicks / PORTAL_IDLE_INTERVAL) % 4;
        ResourceLocation tex = PORTAL_IDLE[idx];
        renderHorizontalQuad(poseStack, buffer, entity, center,
                Config.getBeamHeight() + PORTAL_HEIGHT_OFFSET, PORTAL_RADIUS, tex, packedLight, 1.0f, true);
    }

    private void renderPortalReverseGrow(BeamEntity entity, Vec3 center, float partialTicks,
                                         PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int phaseTicks = entity.getPhaseTimer();
        int totalFrames = PORTAL_GROW.length;
        int duration = (int) (Config.getDissipateTime() * 20);
        int progress = Math.min(phaseTicks, duration);
        int idx = totalFrames - 1 - (progress * totalFrames / duration);
        idx = Math.max(0, Math.min(totalFrames - 1, idx));
        ResourceLocation tex = PORTAL_GROW[idx];
        renderHorizontalQuad(poseStack, buffer, entity, center,
                Config.getBeamHeight() + PORTAL_HEIGHT_OFFSET, PORTAL_RADIUS, tex, packedLight, 1.0f, true);
    }

    private void renderAimMarker(BeamEntity entity, Vec3 center, float partialTicks,
                                 PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        renderHorizontalQuad(poseStack, buffer, entity, center, 0, AIM_MARKER_RADIUS,
                AIM_MARKER_TEXTURE, packedLight, 0.5f, false);
    }

    private void renderHorizontalQuad(PoseStack poseStack, MultiBufferSource buffer,
                                      BeamEntity entity, Vec3 center, double yOffset, float size,
                                      ResourceLocation texture, int packedLight, float alpha, boolean spin) {
        if (center == null || texture == null) return;

        poseStack.pushPose();
        poseStack.translate(center.x - entity.getX(), center.y - entity.getY() + yOffset, center.z - entity.getZ());

        // 只有 spin 为 true 时才旋转
        if (spin) {
            float rot = (entity.getTotalLifeTicks() * 1.5f) % 360;
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rot));
        }

        RenderType renderType = RenderType.entityTranslucent(texture);
        VertexConsumer consumer = buffer.getBuffer(renderType);
        Matrix4f pose = poseStack.last().pose();
        float half = size;

        consumer.vertex(pose, -half, 0, -half)
                .color(1f, 1f, 1f, alpha)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(0, 1, 0)
                .endVertex();
        consumer.vertex(pose, half, 0, -half)
                .color(1f, 1f, 1f, alpha)
                .uv(1, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(0, 1, 0)
                .endVertex();
        consumer.vertex(pose, half, 0, half)
                .color(1f, 1f, 1f, alpha)
                .uv(1, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(0, 1, 0)
                .endVertex();
        consumer.vertex(pose, -half, 0, half)
                .color(1f, 1f, 1f, alpha)
                .uv(0, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(0, 1, 0)
                .endVertex();

        poseStack.popPose();
    }
}
