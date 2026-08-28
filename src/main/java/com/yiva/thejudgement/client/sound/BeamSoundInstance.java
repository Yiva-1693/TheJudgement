package com.yiva.thejudgement.client.sound;

import com.yiva.thejudgement.entity.BeamEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * A positional sound whose source follows a beam entity for its entire lifetime.
 */
final class BeamSoundInstance extends AbstractTickableSoundInstance {

    private final BeamEntity beam;
    private final SoundPhase soundPhase;

    BeamSoundInstance(BeamEntity beam, SoundEvent soundEvent, SoundSource source, SoundPhase soundPhase) {
        super(soundEvent, source, RandomSource.create());
        this.beam = beam;
        this.soundPhase = soundPhase;
        this.volume = 1.0f;
        this.pitch = 1.0f;
        this.looping = false;
        this.delay = 0;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        updatePosition();
    }

    @Override
    public void tick() {
        if (beam.isRemoved() || !soundPhase.matches(beam.getPhase())) {
            stop();
            return;
        }

        updatePosition();
    }

    private void updatePosition() {
        this.x = beam.getX();
        this.y = beam.getY();
        this.z = beam.getZ();
    }

    enum SoundPhase {
        CHARGE {
            @Override
            boolean matches(BeamEntity.Phase phase) {
                return phase == BeamEntity.Phase.CHARGING || phase == BeamEntity.Phase.CHARGED_WAIT;
            }
        },
        SHOT {
            @Override
            boolean matches(BeamEntity.Phase phase) {
                return phase == BeamEntity.Phase.FIRING || phase == BeamEntity.Phase.DISSIPATING;
            }
        };

        abstract boolean matches(BeamEntity.Phase phase);

        static SoundPhase from(BeamEntity.Phase phase) {
            return switch (phase) {
                case CHARGING, CHARGED_WAIT -> CHARGE;
                case FIRING, DISSIPATING -> SHOT;
            };
        }
    }
}
