package com.zaext.simple.item.oilmod.mixin;

import com.zaext.simple.item.oilmod.OilMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    @Final
    private static Logger LOGGER;

    /**
     * @param amount 原始伤害数值
     * @param level 服务端等级
     * @param source 伤害来源
     * @return 修改后的伤害数值
     */
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
    private float increaseFireDamageWhenOilSoaked(float amount, ServerLevel level, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (source.is(DamageTypeTags.IS_FIRE) && self.hasEffect(OilMod.OIL_SOAKED)) {
            return amount * 1.5f; // 增加50%火焰伤害
        }

        return amount;
    }

    // spawn smoke particle
    @Inject(method = "baseTick", at = @At("TAIL"))
    private void spawnOilSmoke(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(OilMod.OIL_SOAKED) && self.isOnFire()) {
            if (self.level().isClientSide()) {
                if (self.tickCount % 2 == 0) {
                    self.level().addParticle(ParticleTypes.LARGE_SMOKE, self.getRandomX(0.5D), self.getRandomY(), self.getRandomZ(0.5D), 0.0D, 0.05D, 0.0D);
                }
            } else if (!self.level().isClientSide() && self.level() instanceof ServerLevel serverLevel) {
                if (self.tickCount % 2 == 0) {
                    serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, self.getRandomX(0.5D), self.getRandomY(), self.getRandomZ(0.5D), 3, 0.1, 0.1, 0.1, 0.02);
                }
            }
        }
    }

}
