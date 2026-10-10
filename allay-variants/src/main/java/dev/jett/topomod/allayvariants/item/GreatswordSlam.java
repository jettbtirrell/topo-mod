package dev.jett.topomod.allayvariants.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The shockwave of a fully charged greatsword: like a sword's sweep, it hurts the other creatures around the one that was
 * struck. Kept apart from GreatswordItem so it can be changed or removed on its own.
 */
public final class GreatswordSlam {
	private static final double RADIUS = 3.0;
	/** Share of the main hit's damage that the creatures around it take. */
	private static final float DAMAGE_SHARE = 0.5F;
	private static final double KNOCKBACK = 0.6;
	private static final double KNOCKBACK_LIFT = 0.25;

	private GreatswordSlam() {
	}

	/** Slams around {@code center}; {@code struck} already took the main hit, so it is skipped. */
	public static void slam(ServerLevel level, Player player, Entity struck, Vec3 center, float damage) {
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, struck.getBoundingBox().inflate(RADIUS, 0.25, RADIUS), target -> isSlamTarget(player, struck, target))) {
			if (target.distanceToSqr(center) > RADIUS * RADIUS) {
				continue;
			}
			Vec3 away = target.position().subtract(center).multiply(1.0, 0.0, 1.0).normalize().scale(KNOCKBACK);
			target.push(away.x, KNOCKBACK_LIFT, away.z);
			target.hurtServer(level, level.damageSources().playerAttack(player), damage * DAMAGE_SHARE);
		}
		level.playSound(null, center.x, center.y, center.z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.0F, 1.0F);
		level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.1, center.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y + 0.1, center.z, 8, RADIUS / 2.0, 0.0, RADIUS / 2.0, 0.0);
	}

	private static boolean isSlamTarget(Player player, Entity struck, LivingEntity target) {
		return target != player && target != struck && target.isAlive() && target.isAttackable() && !player.isAlliedTo(target)
			&& !(target instanceof TamableAnimal pet && pet.isOwnedBy(player));
	}
}
