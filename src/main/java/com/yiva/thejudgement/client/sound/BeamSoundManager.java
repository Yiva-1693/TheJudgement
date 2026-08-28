package com.yiva.thejudgement.client.sound;

import com.yiva.thejudgement.TheJudgement;
import com.yiva.thejudgement.entity.BeamEntity;
import com.yiva.thejudgement.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Creates one phase-aware sound controller for every beam tracked by this client.
 */
@Mod.EventBusSubscriber(modid = TheJudgement.MOD_ID, value = Dist.CLIENT)
public final class BeamSoundManager {

    private static final Map<Integer, BeamSoundState> ACTIVE_BEAMS = new HashMap<>();

    private BeamSoundManager() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide && event.getEntity() instanceof BeamEntity beam) {
            ACTIVE_BEAMS.put(beam.getId(), new BeamSoundState(beam));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        SoundManager soundManager = minecraft.getSoundManager();
        if (minecraft.level == null) {
            stopAll(soundManager);
            return;
        }

        Iterator<BeamSoundState> iterator = ACTIVE_BEAMS.values().iterator();
        while (iterator.hasNext()) {
            BeamSoundState state = iterator.next();
            if (state.beam.isRemoved() || state.beam.level() != minecraft.level) {
                state.stop(soundManager);
                iterator.remove();
                continue;
            }

            state.tick(soundManager);
        }
    }

    private static void stopAll(SoundManager soundManager) {
        ACTIVE_BEAMS.values().forEach(state -> state.stop(soundManager));
        ACTIVE_BEAMS.clear();
    }

    private static final class BeamSoundState {
        private final BeamEntity beam;
        private BeamSoundInstance currentSound;
        private BeamSoundInstance.SoundPhase currentPhase;
        private boolean chargeStarted;
        private boolean shotStarted;

        private BeamSoundState(BeamEntity beam) {
            this.beam = beam;
        }

        private void tick(SoundManager soundManager) {
            BeamSoundInstance.SoundPhase nextPhase = BeamSoundInstance.SoundPhase.from(beam.getPhase());
            if (nextPhase == currentPhase) {
                return;
            }

            stopCurrent(soundManager);
            currentPhase = nextPhase;

            if (nextPhase == BeamSoundInstance.SoundPhase.CHARGE && !chargeStarted) {
                currentSound = createSound(ModSounds.BEAM_CHARGE.get(), SoundSource.PLAYERS, nextPhase);
                chargeStarted = true;
                soundManager.play(currentSound);
            } else if (nextPhase == BeamSoundInstance.SoundPhase.SHOT && !shotStarted) {
                currentSound = createSound(ModSounds.BEAM_SHOT.get(), SoundSource.HOSTILE, nextPhase);
                shotStarted = true;
                soundManager.play(currentSound);
            }
        }

        private BeamSoundInstance createSound(SoundEvent event, SoundSource source,
                                              BeamSoundInstance.SoundPhase phase) {
            return new BeamSoundInstance(beam, event, source, phase);
        }

        private void stop(SoundManager soundManager) {
            stopCurrent(soundManager);
            currentPhase = null;
        }

        private void stopCurrent(SoundManager soundManager) {
            if (currentSound != null) {
                soundManager.stop(currentSound);
                currentSound = null;
            }
        }
    }
}
