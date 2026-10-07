package dev.jett.topomod.allayvariants.entity;

import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

// The bunnay's own sensors. The combat one looks every tick (a hit has to count at once); the rest keep the old scan paces.
final class BunnaySensors {
	private BunnaySensors() {
	}

	/** What hurt it, what hurt its owner, and what its owner hurt: written every tick so reactions are instant. */
	static final class Combat extends Sensor<BunnayEntity> {
		Combat() {
			super(1);
		}

		@Override
		public Set<MemoryModuleType<?>> requires() {
			return Set.of(MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY, BunnayAi.OWNER_HURT_BY, BunnayAi.OWNER_HURT_TARGET);
		}

		@Override
		protected void doTick(ServerLevel level, BunnayEntity bunnay) {
			Brain<BunnayEntity> brain = bunnay.getBrain();
			// The same as the vanilla hurt-by sensor.
			DamageSource source = bunnay.getLastDamageSource();
			if (source != null) {
				brain.setMemory(MemoryModuleType.HURT_BY, source);
				if (source.getEntity() instanceof LivingEntity attacker) {
					brain.setMemory(MemoryModuleType.HURT_BY_ENTITY, attacker);
				}
			} else {
				brain.eraseMemory(MemoryModuleType.HURT_BY);
			}
			brain.getMemory(MemoryModuleType.HURT_BY_ENTITY).ifPresent(entity -> {
				if (!entity.isAlive() || entity.level() != level) {
					brain.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
				}
			});

			// Its owner's fights, like a wolf's: only while tame and standing.
			LivingEntity owner = bunnay.isTame() && !bunnay.isOrderedToSit() ? bunnay.getOwner() : null;
			if (owner == null) {
				brain.eraseMemory(BunnayAi.OWNER_HURT_BY);
				brain.eraseMemory(BunnayAi.OWNER_HURT_TARGET);
				return;
			}
			DamageSource ownerHit = owner.getLastDamageSource(100);
			if (ownerHit != null && !ownerHit.is(DamageTypeTags.NO_WOLF_RETALIATION)) {
				brain.setMemory(BunnayAi.OWNER_HURT_BY, owner.getLastHurtByMob());
			} else {
				brain.eraseMemory(BunnayAi.OWNER_HURT_BY);
			}
			brain.setMemory(BunnayAi.OWNER_HURT_TARGET, owner.getLastHurtMob());
		}
	}

	/** Finds the mobs a scared bunnay runs from, and the middle of them (nearer ones count for more). */
	static final class Threats extends Sensor<BunnayEntity> {
		Threats() {
			super(5);
		}

		@Override
		public Set<MemoryModuleType<?>> requires() {
			return Set.of(BunnayAi.FLEE_THREAT_CENTER, BunnayAi.FLEE_AWAY);
		}

		@Override
		protected void doTick(ServerLevel level, BunnayEntity bunnay) {
			Brain<BunnayEntity> brain = bunnay.getBrain();
			if (!bunnay.fleeReady()) {
				brain.eraseMemory(BunnayAi.FLEE_THREAT_CENTER);
				brain.eraseMemory(BunnayAi.FLEE_AWAY);
				return;
			}
			List<Mob> threats = bunnay.findThreats();
			Vec3 sum = Vec3.ZERO;
			Vec3 away = Vec3.ZERO;
			double total = 0.0;
			for (Mob mob : threats) {
				double weight = 1.0 / (bunnay.distanceTo(mob) + 2.0);
				sum = sum.add(mob.position().scale(weight));
				// Each threat pushes it straight away from itself, the nearer ones harder, so threats on both sides cancel out.
				Vec3 offset = bunnay.position().subtract(mob.position()).multiply(1.0, 0.0, 1.0);
				if (offset.lengthSqr() > 1.0E-4) {
					away = away.add(offset.normalize().scale(weight));
				}
				total += weight;
			}
			if (total > 0.0) {
				brain.setMemory(BunnayAi.FLEE_THREAT_CENTER, sum.scale(1.0 / total));
				brain.setMemory(BunnayAi.FLEE_AWAY, away);
			} else {
				brain.eraseMemory(BunnayAi.FLEE_THREAT_CENTER);
				brain.eraseMemory(BunnayAi.FLEE_AWAY);
			}
		}
	}

	/** The nearest carrot on the ground it can pick up and has a path to (searched about every 3 seconds). */
	static final class GroundCarrots extends Sensor<BunnayEntity> {
		GroundCarrots() {
			super(60);
		}

		@Override
		public Set<MemoryModuleType<?>> requires() {
			return Set.of(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
		}

		@Override
		protected void doTick(ServerLevel level, BunnayEntity bunnay) {
			Brain<BunnayEntity> brain = bunnay.getBrain();
			ItemEntity found = bunnay.canPickUpGroundCarrots() ? bunnay.findGroundCarrot() : null;
			if (found != null) {
				brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, found);
			} else {
				brain.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
			}
		}
	}

	/** The nearest fully grown carrot it can harvest and has a path to, and whether there is none . */
	static final class RipeCarrots extends Sensor<BunnayEntity> {
		RipeCarrots() {
			super(60);
		}

		@Override
		public Set<MemoryModuleType<?>> requires() {
			return Set.of(BunnayAi.RIPE_CARROT, BunnayAi.NO_CROPS_FOUND);
		}

		@Override
		protected void doTick(ServerLevel level, BunnayEntity bunnay) {
			Brain<BunnayEntity> brain = bunnay.getBrain();
			if (!bunnay.canStartHarvest()) {
				brain.eraseMemory(BunnayAi.RIPE_CARROT);
				return;
			}
			BlockPos crop = bunnay.findRipeCarrot();
			if (crop != null) {
				brain.setMemory(BunnayAi.RIPE_CARROT, crop);
				brain.eraseMemory(BunnayAi.NO_CROPS_FOUND);
			} else {
				brain.eraseMemory(BunnayAi.RIPE_CARROT);
				brain.setMemory(BunnayAi.NO_CROPS_FOUND, true);
			}
		}
	}
}
