package com.yiva.thejudgement.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AbyssMoteParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;
    private final float baseSize;

    protected AbyssMoteParticle(ClientLevel level, double x, double y, double z,
                                double xSpeed, double ySpeed, double zSpeed,
                                SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.spriteSet = spriteSet;
        this.baseSize = 0.18f + random.nextFloat() * 0.24f;
        this.quadSize = baseSize;
        this.lifetime = 40 + random.nextInt(41);
        this.gravity = 0;
        this.hasPhysics = false;
        this.friction = 0.985f;
        this.rCol = 0.55f + random.nextFloat() * 0.2f;
        this.gCol = 0.25f + random.nextFloat() * 0.25f;
        this.bCol = 1.0f;
        this.alpha = 0;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.roll = random.nextFloat() * ((float) Math.PI * 2.0f);
        this.oRoll = this.roll;
        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(spriteSet);
        this.oRoll = this.roll;
        this.roll += 0.015f;

        float life = (float) this.age / this.lifetime;
        float fadeIn = Math.min(1.0f, life / 0.12f);
        float fadeOut = Math.min(1.0f, (1.0f - life) / 0.35f);
        this.alpha = 0.8f * Math.min(fadeIn, fadeOut);
        this.quadSize = baseSize * (0.85f + 0.25f * (float) Math.sin(life * Math.PI));
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new AbyssMoteParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet);
        }
    }
}
