package com.yiva.thejudgement.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EnergySparkParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;
    private final float baseSize;

    protected EnergySparkParticle(ClientLevel level, double x, double y, double z,
                                  double xSpeed, double ySpeed, double zSpeed,
                                  SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.spriteSet = spriteSet;
        this.baseSize = 0.18f + random.nextFloat() * 0.27f;
        this.quadSize = baseSize;
        this.lifetime = 12 + random.nextInt(14);
        this.gravity = 0;
        this.hasPhysics = false;
        this.friction = 0.93f;

        // 颜色：淡紫色/白色，可根据素材调整
        this.rCol = 0.9f + random.nextFloat() * 0.1f;
        this.gCol = 0.8f + random.nextFloat() * 0.2f;
        this.bCol = 1.0f;
        this.alpha = 1.0f;

        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(spriteSet);
        float lifeRatio = (float) this.age / this.lifetime;
        this.alpha = Math.max(0, 1.0f - lifeRatio);
        this.quadSize = baseSize * (1.0f - lifeRatio * 0.7f);
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
            return new EnergySparkParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet);
        }
    }
}
