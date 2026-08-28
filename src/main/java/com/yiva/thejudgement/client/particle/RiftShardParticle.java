//package com.yiva.thejudgement.client.particle;
//
//import net.minecraft.client.multiplayer.ClientLevel;
//import net.minecraft.client.particle.*;
//import net.minecraft.core.particles.SimpleParticleType;
//import net.minecraftforge.api.distmarker.Dist;
//import net.minecraftforge.api.distmarker.OnlyIn;
//
//@OnlyIn(Dist.CLIENT)
//public class RiftShardParticle extends TextureSheetParticle {
//
//    private final SpriteSet spriteSet;
//    private float rotationSpeed;
//
//    protected RiftShardParticle(ClientLevel level, double x, double y, double z,
//                                double xSpeed, double ySpeed, double zSpeed,
//                                SpriteSet spriteSet) {
//        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
//        this.spriteSet = spriteSet;
//        this.quadSize = 0.4f + random.nextFloat() * 0.6f;
//        this.lifetime = 25 + random.nextInt(30); // 1.25~2.75 秒
//        this.gravity = 0.08f;                     // 受重力影响，坠落
//        this.hasPhysics = true;
//
//        // 颜色：紫色/蓝色/白色渐变
//        this.rCol = 0.6f + random.nextFloat() * 0.4f;
//        this.gCol = 0.4f + random.nextFloat() * 0.5f;
//        this.bCol = 0.8f + random.nextFloat() * 0.2f;
//        this.alpha = 1.0f;
//
//        // 速度：向外爆裂（方向随机水平 + 轻微向上）
//        double angle = random.nextDouble() * 2 * Math.PI;
//        double speed = 0.5 + random.nextDouble() * 1.5;
//        this.xd = Math.cos(angle) * speed;
//        this.zd = Math.sin(angle) * speed;
//        this.yd = 0.3 + random.nextDouble() * 0.6;
//
//        this.rotationSpeed = (random.nextFloat() - 0.5f) * 0.5f;
//
//        this.setSpriteFromAge(spriteSet);
//    }
//
//    @Override
//    public void tick() {
//        super.tick();
//        this.setSpriteFromAge(spriteSet);
//        // 旋转
//        this.oRoll = this.roll;
//        this.roll += rotationSpeed;
//        // 透明度随年龄衰减（后半段开始消失）
//        if (this.age > this.lifetime * 0.6f) {
//            this.alpha = 1.0f * (1 - (float) (this.age - this.lifetime * 0.6f) / (this.lifetime * 0.4f));
//        }
//        // 缩小
//        if (this.age > this.lifetime * 0.5f) {
//            this.quadSize *= 0.98f;
//        }
//    }
//
//    @Override
//    public ParticleRenderType getRenderType() {
//        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
//    }
//
//    @OnlyIn(Dist.CLIENT)
//    public static class Provider implements ParticleProvider<SimpleParticleType> {
//        private final SpriteSet spriteSet;
//
//        public Provider(SpriteSet spriteSet) {
//            this.spriteSet = spriteSet;
//        }
//
//        @Override
//        public Particle createParticle(SimpleParticleType type, ClientLevel level,
//                                       double x, double y, double z,
//                                       double xSpeed, double ySpeed, double zSpeed) {
//            return new RiftShardParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet);
//        }
//    }
//}