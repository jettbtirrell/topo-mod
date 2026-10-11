package dev.jett.topomod.allayvariants.effect;

import dev.jett.topomod.allayvariants.registry.ModAttachments;
import dev.jett.topomod.allayvariants.registry.ModEffects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Sets things on soul fire, and says which things are. */
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
		// The effect does the burning, but it draws nothing itself (its swirl of particles is hidden, its icon shows).
		mob.addEffect(new MobEffectInstance(ModEffects.SOUL_FIRE, ticks, 0, false, false, true));
		MobEffectInstance burning = mob.getEffect(ModEffects.SOUL_FIRE);
		showFlamesFor(mob, burning == null ? ticks : burning.getDuration());
	}

	/** Server side: tells the clients to draw the flames on this mob for this long (a mob's effects are not sent to them, so this is). */
	public static void showFlamesFor(Entity mob, int ticks) {
		mob.setAttached(ModAttachments.SOUL_FIRE_UNTIL, mob.level().getGameTime() + ticks);
	}

	/** Server side: the flames are out. */
	public static void putOut(Entity mob) {
		mob.removeAttached(ModAttachments.SOUL_FIRE_UNTIL);
	}

	/** On either side: whether flames should be drawn on this mob (soul fire's, not the plain fire the game knows about). */
	public static boolean isBurning(Entity mob) {
		Long until = mob.getAttached(ModAttachments.SOUL_FIRE_UNTIL);
		return until != null && until > mob.level().getGameTime();
	}
}
