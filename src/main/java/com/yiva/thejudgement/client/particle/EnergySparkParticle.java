package com.yiva.thejudgement.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EnergySparkParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;

    protected EnergySparkParticle(ClientLevel level, double x, double y, double z,
                                  double xSpeed, double ySpeed, double zSpeed,
                                  SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.spriteSet = spriteSet;
        // 粒子大小：初始 0.2~0.4，后面会缩小
        this.quadSize = 0.2f + random.nextFloat() * 0.2f;
        // 生命周期：60~120 tick（3~6秒），缓慢飘散
        this.lifetime = 60 + random.nextInt(60);
        this.gravity = 0;               // 无重力
        this.hasPhysics = false;        // 不受碰撞影响
        this.friction = 0.96f;          // 轻微空气阻力

        // 颜色：淡紫色/白色，可根据素材调整
        this.rCol = 0.9f + random.nextFloat() * 0.1f;
        this.gCol = 0.8f + random.nextFloat() * 0.2f;
        this.bCol = 1.0f;
        this.alpha = 1.0f;

        // 初始速度：很慢的水平飘散 + 轻微向上
        double angle = random.nextDouble() * 2 * Math.PI;
        double speed = 0.01 + random.nextDouble() * 0.03; // 很慢
        this.xd = Math.cos(angle) * speed;
        this.zd = Math.sin(angle) * speed;
        this.yd = 0.02 + random.nextDouble() * 0.05;

        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(spriteSet);
        // 透明度逐渐降低，最后 20% 时加速消失
        float lifeRatio = (float) this.age / this.lifetime;
        this.alpha = 1.0f - lifeRatio * 0.8f; // 最终降到 0.2
        // 后半段开始缩小
        if (lifeRatio > 0.5f) {
            this.quadSize *= 0.98f;
        }
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