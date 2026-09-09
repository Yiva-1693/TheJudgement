package com.yiva.thejudgement.client.renderer;

import com.yiva.thejudgement.TheJudgement;
import com.yiva.thejudgement.client.handler.ChargingPoseClientState;
import com.yiva.thejudgement.item.JudgementStaffItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.IdentityHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = TheJudgement.MOD_ID, value = Dist.CLIENT)
public final class ChargingPoseRenderer {

    private static final Map<PlayerModel, PoseSnapshot> ACTIVE_POSES = new IdentityHashMap<>();
    private static final Map<PlayerModel, PoseParameters> ACTIVE_PARAMETERS = new IdentityHashMap<>();

    private ChargingPoseRenderer() {
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        Minecraft minecraft = Minecraft.getInstance();
        if (player == minecraft.player
                && minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        if (!(event.getRenderer().getModel() instanceof PlayerModel)) {
            return;
        }
        PlayerModel model = (PlayerModel) event.getRenderer().getModel();
        restorePendingPose(model);
        if (!(player.getMainHandItem().getItem() instanceof JudgementStaffItem)) {
            return;
        }

        float blend = ChargingPoseClientState.getBlend(
                player.getUUID(),
                player.level().getGameTime(),
                event.getPartialTick()
        );
        if (blend <= 0.001F) {
            return;
        }

        double horizontalSpeed = player.getDeltaMovement().horizontalDistance();
        float movement = Mth.clamp((float) (horizontalSpeed / 0.12D), 0.0F, 1.0F);
        float phase = (player.tickCount + event.getPartialTick()) * 0.35F;
        float sway = (float) Math.sin(phase) * 0.10F * movement;
        float bodyLean = 0.10F * movement;

        ACTIVE_POSES.put(model, PoseSnapshot.capture(model));
        ACTIVE_PARAMETERS.put(model, new PoseParameters(blend, movement, sway, bodyLean));
        model.rightArmPose = PoseHolder.INSTANCE;
        model.leftArmPose = PoseHolder.INSTANCE;
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (event.getRenderer().getModel() instanceof PlayerModel) {
            PlayerModel model = (PlayerModel) event.getRenderer().getModel();
            restorePendingPose(model);
        }
    }

    private static void restorePendingPose(PlayerModel model) {
        ACTIVE_PARAMETERS.remove(model);
        PoseSnapshot snapshot = ACTIVE_POSES.remove(model);
        if (snapshot != null) {
            snapshot.restore(model);
        }
    }

    private static void applyArmPose(
            HumanoidModel<?> model,
            LivingEntity entity,
            HumanoidArm arm
    ) {
        if (!(entity instanceof Player player)
                || !(model instanceof PlayerModel)) {
            return;
        }
        PlayerModel playerModel = (PlayerModel) model;

        PoseParameters parameters = ACTIVE_PARAMETERS.get(playerModel);
        if (parameters == null) {
            return;
        }

        ModelPart armPart = arm == HumanoidArm.RIGHT ? playerModel.rightArm : playerModel.leftArm;
        float armXRot = -1.15F - parameters.movement * 0.08F;
        float armYRot = arm == HumanoidArm.RIGHT
                ? -0.35F - parameters.sway
                : 0.35F + parameters.sway;
        float armZRot = arm == HumanoidArm.RIGHT ? -0.12F : 0.12F;

        blendRotation(armPart, armXRot, armYRot, armZRot, parameters.blend);

        if (arm == HumanoidArm.RIGHT) {
            blendRotation(
                    playerModel.body,
                    playerModel.body.xRot + parameters.bodyLean * parameters.blend,
                    playerModel.body.yRot + parameters.sway * 0.35F * parameters.blend,
                    playerModel.body.zRot,
                    1.0F
            );
        }

        playerModel.rightSleeve.copyFrom(playerModel.rightArm);
        playerModel.leftSleeve.copyFrom(playerModel.leftArm);
    }

    private static void blendRotation(
            ModelPart part,
            float targetXRot,
            float targetYRot,
            float targetZRot,
            float blend
    ) {
        float baseXRot = part.xRot;
        float baseYRot = part.yRot;
        float baseZRot = part.zRot;
        part.xRot = Mth.lerp(blend, baseXRot, targetXRot);
        part.yRot = Mth.lerp(blend, baseYRot, targetYRot);
        part.zRot = Mth.lerp(blend, baseZRot, targetZRot);
    }

    private static final class PoseParameters {
        private final float blend;
        private final float movement;
        private final float sway;
        private final float bodyLean;

        private PoseParameters(float blend, float movement, float sway, float bodyLean) {
            this.blend = blend;
            this.movement = movement;
            this.sway = sway;
            this.bodyLean = bodyLean;
        }
    }

    private static final class PoseHolder {
        private static final HumanoidModel.ArmPose INSTANCE = HumanoidModel.ArmPose.create(
                "thejudgement_charging",
                true,
                ChargingPoseRenderer::applyArmPose
        );
    }

    private static final class PoseSnapshot {
        private final PartPose body;
        private final PartPose rightArm;
        private final PartPose leftArm;
        private final PartPose rightSleeve;
        private final PartPose leftSleeve;
        private final PartPose jacket;
        private final HumanoidModel.ArmPose rightArmPose;
        private final HumanoidModel.ArmPose leftArmPose;

        private PoseSnapshot(
                PartPose body,
                PartPose rightArm,
                PartPose leftArm,
                PartPose rightSleeve,
                PartPose leftSleeve,
                PartPose jacket,
                HumanoidModel.ArmPose rightArmPose,
                HumanoidModel.ArmPose leftArmPose
        ) {
            this.body = body;
            this.rightArm = rightArm;
            this.leftArm = leftArm;
            this.rightSleeve = rightSleeve;
            this.leftSleeve = leftSleeve;
            this.jacket = jacket;
            this.rightArmPose = rightArmPose;
            this.leftArmPose = leftArmPose;
        }

        private static PoseSnapshot capture(PlayerModel model) {
            return new PoseSnapshot(
                    model.body.storePose(),
                    model.rightArm.storePose(),
                    model.leftArm.storePose(),
                    model.rightSleeve.storePose(),
                    model.leftSleeve.storePose(),
                    model.jacket.storePose(),
                    model.rightArmPose,
                    model.leftArmPose
            );
        }

        private void restore(PlayerModel model) {
            model.body.loadPose(body);
            model.rightArm.loadPose(rightArm);
            model.leftArm.loadPose(leftArm);
            model.rightSleeve.loadPose(rightSleeve);
            model.leftSleeve.loadPose(leftSleeve);
            model.jacket.loadPose(jacket);
            model.rightArmPose = rightArmPose;
            model.leftArmPose = leftArmPose;
        }
    }
}