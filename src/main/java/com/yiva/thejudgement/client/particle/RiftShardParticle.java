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
public class RiftShardParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;
    private final float rotationSpeed;
    private final float baseSize;

    protected RiftShardParticle(ClientLevel level, double x, double y, double z,
                                double xSpeed, double ySpeed, double zSpeed,
                                SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.spriteSet = spriteSet;
        this.baseSize = 0.3f + random.nextFloat() * 0.45f;
        this.quadSize = baseSize;
        this.lifetime = 24 + random.nextInt(28);
        this.gravity = 0.045f;
        this.hasPhysics = true;
        this.friction = 0.94f;
        this.rCol = 0.7f + random.nextFloat() * 0.25f;
        this.gCol = 0.35f + random.nextFloat() * 0.3f;
        this.bCol = 1.0f;
        this.alpha = 1.0f;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.roll = random.nextFloat() * ((float) Math.PI * 2.0f);
        this.oRoll = this.roll;
        this.rotationSpeed = (random.nextFloat() - 0.5f) * 0.4f;
        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(spriteSet);
        this.oRoll = this.roll;
        this.roll += rotationSpeed;

        float life = (float) this.age / this.lifetime;
        if (life > 0.55f) {
            this.alpha = Math.max(0, (1.0f - life) / 0.45f);
            this.quadSize = baseSize * (1.0f - (life - 0.55f) * 0.8f);
        }
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
            return new RiftShardParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet);
        }
    }
}
