package com.zaext.simple.item.oilmod.mixin;

import com.zaext.simple.item.oilmod.OilMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "clearFire", at = @At("HEAD"), cancellable = true)
    private void preventOilFireExtinguish(CallbackInfo ci) {
        if ((Object) this instanceof LivingEntity living && living.hasEffect(OilMod.OIL_SOAKED)) {
            ci.cancel();
        }
    }
}
