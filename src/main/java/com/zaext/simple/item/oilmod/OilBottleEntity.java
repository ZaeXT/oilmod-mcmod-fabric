package com.zaext.simple.item.oilmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class OilBottleEntity extends ThrowableItemProjectile {

    private static final Logger log = LoggerFactory.getLogger(OilBottleEntity.class);

    public OilBottleEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public OilBottleEntity(Level level, LivingEntity owner, ItemStack stack) {
        super(OilMod.OIL_BOTTLE_ENTITY_TYPE, owner, level, stack);
    }

    // dispense behavior
    public OilBottleEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(OilMod.OIL_BOTTLE_ENTITY_TYPE, x, y, z, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return OilMod.OIL_BOTTLE;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.07D;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    public void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide()) {
            BlockPos hitPos = BlockPos.containing(hitResult.getLocation());

            FluidState fluidState = this.level().getFluidState(hitPos);

            if (fluidState.is(FluidTags.WATER) || this.isInWater()) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.5F, 2.6F + (this.random.nextFloat() - this.random.nextFloat()) * 0.8F);

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 15, 0.2D, 0.2D, 0.2D, 0.05D);
                }

                this.discard();
                return;
            }

            for (BlockPos pos : BlockPos.betweenClosed(hitPos.offset(-1, -1, -1), hitPos.offset(1, 1, 1))) {
                if (this.level().getBlockState(pos).isAir()) {
                    this.level().setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                }
            }

            if (this.level() instanceof ServerLevel serverLevel) {
                AABB searchArea = new AABB(hitPos).inflate(3.0D);

                List<Entity> entitiesNearby = this.level().getEntitiesOfClass(Entity.class, searchArea);

                Set<LivingEntity> uniqueTargets = new HashSet<>();

                for (Entity entity : entitiesNearby) {
                    if (entity instanceof LivingEntity living) {
                        uniqueTargets.add(living);
                    } else if (entity instanceof EnderDragonPart part) {
                        if (part.parentMob instanceof LivingEntity dragon) {
                            uniqueTargets.add(dragon);
                        }
                    }
                }
                for (LivingEntity entity : uniqueTargets) {
                    int curAmp = -1;
                    var existingEffect = entity.getEffect(OilMod.OIL_SOAKED);
                    if (existingEffect != null) {
                        curAmp = existingEffect.getAmplifier();
                    }
                    entity.addEffect(new MobEffectInstance(OilMod.OIL_SOAKED, 400, Math.min(curAmp + 1, 2)));
                    entity.igniteForSeconds(20.0F);
                    entity.hurtServer(serverLevel, entity instanceof EnderDragon ? this.damageSources().indirectMagic(this, this.getOwner()) : this.damageSources().magic(), entity.getHealth() * RandomUtils.secure().randomFloat(0.05f, 0.25f));
                }
            }



            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }

}
