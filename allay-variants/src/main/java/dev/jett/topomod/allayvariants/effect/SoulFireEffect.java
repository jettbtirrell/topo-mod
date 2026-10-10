package dev.jett.topomod.allayvariants.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Soul fire, as a status of its own: it burns like fire (the same damage type, so fire resistance and fire immune mobs shrug it off
 * and water puts it out) but twice as hard, the way a soul fire block hurts more than a fire block, and shows blue flames. It is put
 * on by a spirit fox's bite and by standing in a soul fire block (see BaseFireBlockMixin).
 */
public class SoulFireEffect extends MobEffect {
	/** A soul fire block hurts for 2 where a fire block hurts for 1 (see SoulFireBlock). */
	private static final float DAMAGE = 2.0F;
	private static final int DAMAGE_INTERVAL_TICKS = 20;
	private static final int FLAME_INTERVAL_TICKS = 2;

	public SoulFireEffect(MobEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
		// Put out by water, and nothing to burn on a mob that cannot burn.
		if (mob.isInWaterOrRain() || mob.fireImmune() || mob.hasEffect(MobEffects.FIRE_RESISTANCE)) {
			return false;
		}
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(), 2,
			mob.getBbWidth() * 0.35, mob.getBbHeight() * 0.3, mob.getBbWidth() * 0.35, 0.01);
		if (mob.tickCount % DAMAGE_INTERVAL_TICKS == 0) {
			mob.hurtServer(level, mob.damageSources().onFire(), DAMAGE);
		}
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
		return tickCount % FLAME_INTERVAL_TICKS == 0;
	}
}
