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

	/**
	 * Keeps track of the mobs a scared bunnay runs from, and says which is the nearest (the AVOID_TARGET the flee goes by). A mob
	 * joins when it is after the bunnay (has it as its target) within FLEE_SEARCH_RADIUS while the bunnay is scared, and stays until
	 * it is dead or gone, the bunnay is further than FLEE_SEARCH_RADIUS from it, or it has not been after the bunnay for
	 * LOST_INTEREST_TICKS. So the bunnay keeps running, even once it is back above the health that made it run, until nothing has
	 * been after it for 5 seconds.
	 */
	static final class Threats extends Sensor<BunnayEntity> {
		private static final int LOST_INTEREST_TICKS = 100;

		/** Each pursuer, and the last time it was seen after the bunnay. */
		private final java.util.Map<Mob, Long> pursuers = new java.util.HashMap<>();

		Threats() {
			super(5);
		}

		@Override
		public Set<MemoryModuleType<?>> requires() {
			return Set.of(MemoryModuleType.AVOID_TARGET);
		}

		@Override
		protected void doTick(ServerLevel level, BunnayEntity bunnay) {
			Brain<BunnayEntity> brain = bunnay.getBrain();
			double farSqr = BunnayEntity.FLEE_SEARCH_RADIUS * BunnayEntity.FLEE_SEARCH_RADIUS;
			long now = level.getGameTime();
			this.pursuers.keySet().removeIf(mob -> !mob.isAlive() || mob.isRemoved() || mob.level() != level || mob.distanceToSqr(bunnay) > farSqr);
			// It can only run when it is free to, and only starts when it is scared; once running it carries on.
			if (!bunnay.canRunNow() || !(bunnay.isScared() || bunnay.isFleeing())) {
				this.pursuers.clear();
			} else {
				for (Mob mob : bunnay.findThreats()) {
					this.pursuers.put(mob, now);
				}
				this.pursuers.values().removeIf(last -> now - last > LOST_INTEREST_TICKS);
			}
			List<Mob> threats = List.copyOf(this.pursuers.keySet());
			Mob nearest = threats.stream().min(java.util.Comparator.comparingDouble(bunnay::distanceToSqr)).orElse(null);
			if (nearest != null) {
				brain.setMemory(MemoryModuleType.AVOID_TARGET, nearest);
			} else {
				brain.eraseMemory(MemoryModuleType.AVOID_TARGET);
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
