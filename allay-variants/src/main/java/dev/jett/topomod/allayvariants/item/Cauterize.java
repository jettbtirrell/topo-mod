package dev.jett.topomod.allayvariants.item;

import dev.jett.topomod.allayvariants.enchantment.BackdraftEffect;
import dev.jett.topomod.allayvariants.registry.ModEnchantmentEffects;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * The club's payoff: a full-cooldown hit on a burning creature uses up the burn for extra damage, more the longer it had left
 * to burn. With the Backdraft enchantment the flare of it also sets the creatures standing around it alight. Kept apart from ClubItem so it can be
 * changed or removed on its own.
 */
public final class Cauterize {
	/** Extra damage for each second of burning that was left. */
	private static final float DAMAGE_PER_SECOND = 2.0F;
	/** The most burning that counts (a flint and steel's 8 seconds is not far off it); lava's 15 gains nothing more. */
	private static final float MAX_SECONDS = 10.0F;

	/**
	 * The target whose burn this tick's hit is going to use up, and the tick. The bonus is worked out before the hit and
	 * the burn is used up after it, so the full-cooldown check done for the one is remembered for the other (the
	 * cooldown has been spent by then). Everything runs on the server thread.
	 */
	private static Entity pendingTarget;
	private static long pendingTick;

	private Cauterize() {
	}

	/** The damage added to a hit on {@code target}, before the burn is used up. Only a hit on a full cooldown gets any. */
	public static float bonusDamage(Entity target, float damage, DamageSource source) {
		pendingTarget = null;
		if (target.fireImmune() || !target.isOnFire() || !(source.getDirectEntity() instanceof Player attacker) || !AttackMeter.isFull(AttackMeter.ofHit(attacker, damage))) {
			return 0.0F;
		}
		pendingTarget = target;
		pendingTick = target.level().getGameTime();
		return Math.min(target.getRemainingFireTicks() / 20.0F, MAX_SECONDS) * DAMAGE_PER_SECOND;
	}

	/** Called after the hit: puts the burn out and lets it flare onto the creatures around. */
	public static void consume(ServerLevel level, Player attacker, LivingEntity target, ItemStack club) {
		boolean pending = pendingTarget == target && pendingTick == level.getGameTime();
		pendingTarget = null;
		if (!pending || target.fireImmune() || !target.isOnFire()) {
			return;
		}
		target.clearFire();
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : club.getEnchantments().entrySet()) {
			BackdraftEffect backdraft = entry.getKey().value().effects().get(ModEnchantmentEffects.BACKDRAFT);
			if (backdraft != null) {
				spread(level, attacker, target, backdraft.radius().calculate(entry.getIntValue()), backdraft.seconds().calculate(entry.getIntValue()));
			}
		}
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
		level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY(0.5), target.getZ(), 24, 0.4, 0.4, 0.4, 0.1);
	}

	private static void spread(ServerLevel level, Player attacker, LivingEntity target, float radius, float seconds) {
		for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(radius), nearby -> canSpreadTo(attacker, target, nearby))) {
			if (nearby.distanceToSqr(target) <= radius * radius) {
				nearby.igniteForSeconds(seconds);
			}
		}
	}

	private static boolean canSpreadTo(Player attacker, LivingEntity target, LivingEntity nearby) {
		return nearby != attacker && nearby != target && nearby.isAlive() && !attacker.isAlliedTo(nearby)
			&& !(nearby instanceof TamableAnimal pet && pet.isOwnedBy(attacker));
	}
}
