package dev.jett.topomod.allayvariants.effect;

import dev.jett.topomod.allayvariants.registry.ModEffects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Sets things on soul fire. */
public final class SoulFire {
	/** How long a spirit fox's bite sets its victim alight (fire aspect's is 4 seconds), and how long standing in soul fire does (a fire block's is 8). */
	public static final int BITE_TICKS = 100;
	public static final int BLOCK_TICKS = 160;

	private SoulFire() {
	}

	/** Puts the mob on soul fire for this long (or leaves it burning longer if it is already). Does nothing on a client or to a mob that cannot burn. */
	public static void ignite(LivingEntity mob, int ticks) {
		if (mob.level().isClientSide() || mob.fireImmune() || mob.hasEffect(MobEffects.FIRE_RESISTANCE) || mob.isInWaterOrRain()) {
			return;
		}
		// The effect draws its own flames, so its swirl of particles is hidden, but its icon shows.
		mob.addEffect(new MobEffectInstance(ModEffects.SOUL_FIRE, ticks, 0, false, false, true));
	}
}
