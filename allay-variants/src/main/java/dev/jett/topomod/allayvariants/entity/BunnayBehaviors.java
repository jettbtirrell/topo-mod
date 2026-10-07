package dev.jett.topomod.allayvariants.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;

// The bunnay's behaviours. The "Job" ones are the movement behaviours, which take turns by job priority (lowest number first).
final class BunnayBehaviors {
	/** Long enough that a behaviour never times out on its own; each one ends when its own conditions say so. */
	private static final int NO_TIMEOUT = 1_000_000;

	private BunnayBehaviors() {
	}

	private static Map<MemoryModuleType<?>, MemoryStatus> memories(Object... pairs) {
		Map<MemoryModuleType<?>, MemoryStatus> map = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2) {
			map.put((MemoryModuleType<?>) pairs[i], (MemoryStatus) pairs[i + 1]);
		}
		return map;
	}

	/** Mid-hop or panicking: nothing else gets to move it. */
	private static boolean suspended(BunnayEntity bunnay) {
		Brain<BunnayEntity> brain = bunnay.getBrain();
		return brain.hasMemoryValue(MemoryModuleType.LONG_JUMP_MID_JUMP) || brain.hasMemoryValue(MemoryModuleType.IS_PANICKING);
	}

	// ---- jobs ----

	/**
	 * A movement job. It may start when no job holds the legs or the one that does has a lower priority (a bigger number); it
	 * stops when its activity ends, when a better job takes over, or when it is mid-hop or panicking. Walking is always set in
	 * step(), not begin(), because the vanilla move sink stops its path later in the very tick a job starts.
	 */
	abstract static class Job extends Behavior<BunnayEntity> {
		private final int jobPriority;
		private final Activity activity;
		/** Ticks stepped so far: 0 until the first step, for the checks that only make sense once it has had a go. */
		int age;

		Job(int jobPriority, Activity activity, Map<MemoryModuleType<?>, MemoryStatus> required) {
			super(withJob(required), NO_TIMEOUT);
			this.jobPriority = jobPriority;
			this.activity = activity;
		}

		private static Map<MemoryModuleType<?>, MemoryStatus> withJob(Map<MemoryModuleType<?>, MemoryStatus> required) {
			Map<MemoryModuleType<?>, MemoryStatus> map = new HashMap<>(required);
			map.put(BunnayAi.JOB_PRIORITY, MemoryStatus.REGISTERED);
			return map;
		}

		abstract boolean canStart(ServerLevel level, BunnayEntity bunnay);

		void begin(ServerLevel level, BunnayEntity bunnay) {
		}

		abstract boolean keepGoing(ServerLevel level, BunnayEntity bunnay);

		abstract void step(ServerLevel level, BunnayEntity bunnay);

		void end(ServerLevel level, BunnayEntity bunnay) {
		}

		@Override
		protected final boolean checkExtraStartConditions(ServerLevel level, BunnayEntity bunnay) {
			Optional<Integer> held = bunnay.getBrain().getMemory(BunnayAi.JOB_PRIORITY);
			if (held.isPresent() && held.get() <= this.jobPriority) {
				return false;
			}
			return !suspended(bunnay) && this.canStart(level, bunnay);
		}

		@Override
		protected final void start(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			Brain<BunnayEntity> brain = bunnay.getBrain();
			brain.setMemory(BunnayAi.JOB_PRIORITY, this.jobPriority);
			brain.eraseMemory(MemoryModuleType.WALK_TARGET);
			this.age = 0;
			this.begin(level, bunnay);
		}

		@Override
		protected final boolean canStillUse(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			Brain<BunnayEntity> brain = bunnay.getBrain();
			return brain.isActive(this.activity) && brain.isMemoryValue(BunnayAi.JOB_PRIORITY, this.jobPriority)
				&& !suspended(bunnay) && this.keepGoing(level, bunnay);
		}

		@Override
		protected final void tick(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			this.step(level, bunnay);
			this.age++;
		}

		@Override
		protected final void stop(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			this.end(level, bunnay);
			Brain<BunnayEntity> brain = bunnay.getBrain();
			if (brain.isMemoryValue(BunnayAi.JOB_PRIORITY, this.jobPriority)) {
				brain.eraseMemory(BunnayAi.JOB_PRIORITY);
			}
		}
	}

	// ---- core ----

	/** Like a wolf, it teleports to a far-off owner while panicking. */
	static final class PanicTeleport extends Behavior<BunnayEntity> {
		PanicTeleport() {
			super(memories(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_PRESENT), NO_TIMEOUT);
		}

		@Override
		protected boolean canStillUse(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			return bunnay.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
		}

		@Override
		protected void tick(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			if (!bunnay.unableToMoveToOwner() && bunnay.shouldTryTeleportToOwner()) {
				bunnay.tryToTeleportToOwner();
			}
		}
	}

	/**
	 * Every tick: drops a target that is no longer valid, and takes a new one the way a wolf does, in this
	 * order of importance: whatever hurt its owner, whatever its owner attacked, whatever hurt it. A new reason only replaces
	 * the current target if it is at least as important as the one that gave it. The work is done in the start check, which
	 * never lets the behaviour start, so it costs a tick and nothing else.
	 */
	static final class TargetTracking extends Behavior<BunnayEntity> {
		private static final int NONE = 99;
		private static final int HURT_BY_UNSEEN_TICKS = 300;
		private static final TargetingConditions HURT_BY = TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting();

		private int ownerHurtByStamp;
		private int ownerHurtStamp;
		private int hurtByStamp;
		private int priority = NONE;
		private int unseenTicks;

		TargetTracking() {
			super(memories(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED), NO_TIMEOUT);
		}

		@Override
		protected boolean checkExtraStartConditions(ServerLevel level, BunnayEntity bunnay) {
			Brain<BunnayEntity> brain = bunnay.getBrain();
			LivingEntity current = brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
			if (current != null && !this.stillValid(bunnay, current)) {
				bunnay.setTarget(null);
				current = null;
			}
			if (current == null) {
				this.priority = NONE;
				this.unseenTicks = 0;
			}
			if (bunnay.isScared() && current != null) {
				bunnay.setTarget(null);
			}
			LivingEntity owner = bunnay.isTame() && !bunnay.isOrderedToSit() ? bunnay.getOwner() : null;
			if (owner != null) {
				LivingEntity hurtOwner = brain.getMemory(BunnayAi.OWNER_HURT_BY).orElse(null);
				int stamp = owner.getLastHurtByMobTimestamp();
				if (hurtOwner != null && stamp != this.ownerHurtByStamp && this.priority >= 1
					&& TargetingConditions.DEFAULT.test(level, bunnay, hurtOwner) && bunnay.wantsToAttack(hurtOwner, owner)) {
					this.ownerHurtByStamp = stamp;
					this.take(bunnay, hurtOwner, 1);
					return false;
				}
				LivingEntity ownerTarget = brain.getMemory(BunnayAi.OWNER_HURT_TARGET).orElse(null);
				stamp = owner.getLastHurtMobTimestamp();
				if (ownerTarget != null && stamp != this.ownerHurtStamp && this.priority >= 2
					&& TargetingConditions.DEFAULT.test(level, bunnay, ownerTarget) && bunnay.wantsToAttack(ownerTarget, owner)) {
					this.ownerHurtStamp = stamp;
					this.take(bunnay, ownerTarget, 2);
					return false;
				}
			}
			int stamp = bunnay.getLastHurtByMobTimestamp();
			LivingEntity attacker = bunnay.getLastHurtByMob();
			if (this.priority >= 3 && stamp != this.hurtByStamp && attacker != null
				&& !(attacker.is(net.minecraft.world.entity.EntityTypes.PLAYER) && level.getGameRules().get(GameRules.UNIVERSAL_ANGER))
				&& HURT_BY.test(level, bunnay, attacker)) {
				this.hurtByStamp = stamp;
				this.take(bunnay, attacker, 3);
				this.alertOthers(level, bunnay, attacker);
			}
			return false;
		}

		private void take(BunnayEntity bunnay, LivingEntity target, int importance) {
			bunnay.setTarget(target);
			if (bunnay.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
				this.priority = importance;
				this.unseenTicks = 0;
			}
		}

		/** Whether it should keep its target: the wolf's own check, and (for what hurt it) losing sight for 15 seconds ends it. */
		private boolean stillValid(BunnayEntity bunnay, LivingEntity target) {
			if (!bunnay.canAttack(target)) {
				return false;
			}
			Team team = bunnay.getTeam();
			if (team != null && target.getTeam() == team) {
				return false;
			}
			double range = bunnay.getAttributeValue(Attributes.FOLLOW_RANGE);
			if (bunnay.distanceToSqr(target) > range * range) {
				return false;
			}
			if (this.priority == 3) {
				if (bunnay.getSensing().hasLineOfSight(target)) {
					this.unseenTicks = 0;
				} else if (++this.unseenTicks > HURT_BY_UNSEEN_TICKS) {
					return false;
				}
			}
			return true;
		}

		/** Its own kind that belong to the same owner join in, as with a wolf. */
		private void alertOthers(ServerLevel level, BunnayEntity bunnay, LivingEntity attacker) {
			double within = bunnay.getAttributeValue(Attributes.FOLLOW_RANGE);
			AABB area = AABB.unitCubeFromLowerCorner(bunnay.position()).inflate(within, 10.0, within);
			for (BunnayEntity other : level.getEntitiesOfClass(BunnayEntity.class, area, entity -> !entity.isSpectator())) {
				if (other != bunnay && other.getTarget() == null && bunnay.getOwner() == other.getOwner() && !other.isAlliedTo(attacker)) {
					other.setTarget(attacker);
				}
			}
		}
	}

	/**
	 * The two halves of a hop, as in the frog's long jump: HopPrepare looks every so often (once the cooldown is over) for a hop
	 * worth making and starts it, setting the mid-jump memory, and HopMidJump flies it. There is no wind-up: the launch is on the
	 * very tick the hop is chosen. Fleeing uses the same pair (the hop away from its threats when it has no route to run along).
	 */
	static final class HopPrepare extends Behavior<BunnayEntity> {
		private final BunnayEntity.HopRun run;
		private int scanDelay;
		private BunnayEntity.Hop plan;

		HopPrepare(BunnayEntity.HopRun run) {
			super(memories(
				MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT,
				MemoryModuleType.LONG_JUMP_MID_JUMP, MemoryStatus.VALUE_ABSENT,
				MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT,
				BunnayAi.FLEE_THREAT_CENTER, MemoryStatus.REGISTERED), 2);
			this.run = run;
		}

		@Override
		protected boolean checkExtraStartConditions(ServerLevel level, BunnayEntity bunnay) {
			if (!bunnay.canHopNow()) {
				return false;
			}
			Optional<Vec3> threats = bunnay.getBrain().getMemory(BunnayAi.FLEE_THREAT_CENTER);
			if (threats.isPresent()) {
				// Fleeing: hop along the way it is running when it can; with no way to run, hop away if there is somewhere to.
				if (bunnay.tickCount % BunnayEntity.HOP_SCAN_INTERVAL != 0) {
					return false;
				}
				this.plan = bunnay.planTravelHop();
				if (this.plan == null && !bunnay.getNavigation().isInProgress()) {
					this.plan = bunnay.planAwayHop(threats.get());
				}
			} else {
				// A scan every 10 ticks.
				if (--this.scanDelay > 0) {
					return false;
				}
				this.scanDelay = BunnayEntity.HOP_SCAN_INTERVAL * 2;
				this.plan = bunnay.planTravelHop();
			}
			return this.plan != null;
		}

		@Override
		protected void start(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			Brain<BunnayEntity> brain = bunnay.getBrain();
			brain.setMemory(MemoryModuleType.LONG_JUMP_MID_JUMP, true);
			brain.eraseMemory(MemoryModuleType.WALK_TARGET);
			this.run.start(this.plan);
			this.plan = null;
		}
	}

	static final class HopMidJump extends Behavior<BunnayEntity> {
		private final BunnayEntity.HopRun run;

		HopMidJump(BunnayEntity.HopRun run) {
			super(memories(MemoryModuleType.LONG_JUMP_MID_JUMP, MemoryStatus.VALUE_PRESENT), NO_TIMEOUT);
			this.run = run;
		}

		@Override
		protected boolean canStillUse(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			return this.run.running;
		}

		@Override
		protected void tick(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			this.run.tick();
		}

		@Override
		protected void stop(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			this.run.stop();
			bunnay.getBrain().eraseMemory(MemoryModuleType.LONG_JUMP_MID_JUMP);
		}
	}

	/** Eats the carrot in its food slot to heal, when out of the fight (see BunnayEntity.canEat). */
	static final class EatCarrot extends Behavior<BunnayEntity> {
		private boolean fresh;

		EatCarrot() {
			super(memories(BunnayAi.EAT_COOLDOWN, MemoryStatus.VALUE_ABSENT), NO_TIMEOUT);
		}

		@Override
		protected boolean checkExtraStartConditions(ServerLevel level, BunnayEntity bunnay) {
			return !bunnay.isEating() && BunnayEntity.isCarrot(bunnay.food.getItem(0)) && bunnay.canEat(false);
		}

		@Override
		protected void start(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			bunnay.beginEating(bunnay.food.getItem(0));
			this.fresh = true;
		}

		@Override
		protected boolean canStillUse(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			return bunnay.isEating();
		}

		@Override
		protected void tick(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			// The first tick only starts the meal, as it did before.
			if (this.fresh) {
				this.fresh = false;
				return;
			}
			bunnay.continueEating();
		}

		@Override
		protected void stop(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			if (bunnay.isEating()) {
				bunnay.stopEating();
			}
		}
	}

	// ---- the jobs ----

	static final class Sit extends Job {
		Sit() {
			super(2, BunnayAi.SIT, Map.of());
		}

		@Override
		boolean canStart(ServerLevel level, BunnayEntity bunnay) {
			return bunnay.wantsToSitNow();
		}

		@Override
		void begin(ServerLevel level, BunnayEntity bunnay) {
			bunnay.getNavigation().stop();
			bunnay.setInSittingPose(true);
		}

		@Override
		boolean keepGoing(ServerLevel level, BunnayEntity bunnay) {
			return bunnay.isOrderedToSit();
		}

		@Override
		void step(ServerLevel level, BunnayEntity bunnay) {
		}

		@Override
		void end(ServerLevel level, BunnayEntity bunnay) {
			bunnay.setInSittingPose(false);
		}
	}

	/** Stands still while it dances; a fight takes priority (the activity changes). */
	static final class Dance extends Job {
		Dance() {
			super(3, BunnayAi.DANCE, Map.of());
		}

		@Override
		boolean canStart(ServerLevel level, BunnayEntity bunnay) {
			return bunnay.isDancing() && !bunnay.hasLiveTarget();
		}

		@Override
		void begin(ServerLevel level, BunnayEntity bunnay) {
			bunnay.getNavigation().stop();
		}

		@Override
		boolean keepGoing(ServerLevel level, BunnayEntity bunnay) {
			return bunnay.isDancing() && !bunnay.hasLiveTarget();
		}

		@Override
		void step(ServerLevel level, BunnayEntity bunnay) {
		}
	}

	/**
	 * Runs from the enemies when it is low on health (see FLEE_BELOW_HEALTH). The sensor keeps track of what it runs from; this
	 * picks a place well away from them that it can walk to and runs there, picking again as it goes. The hopping is the core
	 * hop pair's.
	 */
	static final class Flee extends Job {
		private int repathDelay;
		private boolean lastTryFailed;

		Flee() {
			super(1, Activity.AVOID, memories(BunnayAi.FLEE_THREAT_CENTER, MemoryStatus.VALUE_PRESENT));
		}

		@Override
		boolean canStart(ServerLevel level, BunnayEntity bunnay) {
			return bunnay.fleeReady();
		}

		@Override
		void begin(ServerLevel level, BunnayEntity bunnay) {
			bunnay.fleeing = true;
			bunnay.setFleeSwimming(true);
			bunnay.stopEating();
			bunnay.setTarget(null);
			bunnay.getNavigation().stop();
			this.repathDelay = 0;
			this.lastTryFailed = false;
		}

		@Override
		boolean keepGoing(ServerLevel level, BunnayEntity bunnay) {
			return bunnay.fleeReady() && bunnay.getBrain().hasMemoryValue(BunnayAi.FLEE_THREAT_CENTER);
		}

		@Override
		void step(ServerLevel level, BunnayEntity bunnay) {
			Vec3 center = bunnay.getBrain().getMemory(BunnayAi.FLEE_THREAT_CENTER).orElse(null);
			if (center == null) {
				return;
			}
			// It picks again when it gets there, or every so often; after a try that found no way it waits out the delay.
			if (--this.repathDelay <= 0 || bunnay.getNavigation().isDone() && !this.lastTryFailed) {
				this.repathDelay = BunnayEntity.FLEE_REPATH_TICKS;
				Path path = bunnay.findFleePath(center);
				this.lastTryFailed = path == null;
				if (path != null) {
					bunnay.getNavigation().moveTo(path, BunnayEntity.FLEE_SPEED);
				}
			}
		}

		@Override
		void end(ServerLevel level, BunnayEntity bunnay) {
			bunnay.fleeing = false;
			bunnay.setFleeSwimming(false);
			bunnay.getNavigation().stop();
		}
	}

	/** The usual melee chase and attack, except that it may swing a little before the cooldown is up (see ATTACK_HEAD_START_TICKS). */
	static final class Melee extends Job {
		private static final double SPEED = 1.3;
		private double pathedX;
		private double pathedY;
		private double pathedZ;
		private int ticksUntilRepath;
		private int ticksUntilAttack;
		private long lastCanUseCheck;

		Melee() {
			super(3, Activity.FIGHT, memories(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
		}

		@Override
		boolean canStart(ServerLevel level, BunnayEntity bunnay) {
			long time = level.getGameTime();
			if (time - this.lastCanUseCheck < 20L) {
				return false;
			}
			this.lastCanUseCheck = time;
			LivingEntity target = bunnay.getTarget();
			if (target == null || !target.isAlive()) {
				return false;
			}
			return bunnay.getNavigation().createPath(target, 0) != null || bunnay.isWithinMeleeAttackRange(target);
		}

		@Override
		void begin(ServerLevel level, BunnayEntity bunnay) {
			bunnay.setAggressive(true);
			this.ticksUntilRepath = 0;
			this.ticksUntilAttack = 0;
			this.pathedX = 0.0;
			this.pathedY = 0.0;
			this.pathedZ = 0.0;
		}

		@Override
		boolean keepGoing(ServerLevel level, BunnayEntity bunnay) {
			LivingEntity target = bunnay.getTarget();
			return target != null && target.isAlive() && !(target instanceof Player player && (player.isSpectator() || player.isCreative()));
		}

		@Override
		void step(ServerLevel level, BunnayEntity bunnay) {
			LivingEntity target = bunnay.getTarget();
			if (target == null) {
				return;
			}
			bunnay.getLookControl().setLookAt(target, 30.0F, 30.0F);
			this.ticksUntilRepath = Math.max(this.ticksUntilRepath - 1, 0);
			if (this.ticksUntilRepath <= 0
				&& (this.pathedX == 0.0 && this.pathedY == 0.0 && this.pathedZ == 0.0
					|| target.distanceToSqr(this.pathedX, this.pathedY, this.pathedZ) >= 1.0
					|| bunnay.getRandom().nextFloat() < 0.05F)) {
				this.pathedX = target.getX();
				this.pathedY = target.getY();
				this.pathedZ = target.getZ();
				this.ticksUntilRepath = 4 + bunnay.getRandom().nextInt(7);
				double distance = bunnay.distanceToSqr(target);
				if (distance > 1024.0) {
					this.ticksUntilRepath += 10;
				} else if (distance > 256.0) {
					this.ticksUntilRepath += 5;
				}
				if (!bunnay.getNavigation().moveTo(target, 0, SPEED)) {
					this.ticksUntilRepath += 15;
				}
			}
			this.ticksUntilAttack = Math.max(this.ticksUntilAttack - 1, 0);
			if (this.ticksUntilAttack <= BunnayEntity.ATTACK_HEAD_START_TICKS && bunnay.isWithinMeleeAttackRange(target)
				&& bunnay.getSensing().hasLineOfSight(target)) {
				this.ticksUntilAttack = 20;
				bunnay.swingForAttack(InteractionHand.MAIN_HAND);
				bunnay.doHurtTarget(level, target);
			}
		}

		@Override
		void end(ServerLevel level, BunnayEntity bunnay) {
			LivingEntity target = bunnay.getTarget();
			if (target instanceof Player player && (player.isSpectator() || player.isCreative())) {
				bunnay.setTarget(null);
			}
			bunnay.setAggressive(false);
			bunnay.getNavigation().stop();
		}
	}

	/** Follows a player holding a carrot (it calms down for 5 seconds afterwards). */
	static final class Tempt extends Job {
		private static final double STOP_DISTANCE = 2.5;
		private TargetingConditions conditions;
		private Player player;
		private int calmDown;

		Tempt() {
			super(4, Activity.IDLE, Map.of());
		}

		private boolean find(ServerLevel level, BunnayEntity bunnay) {
			if (this.conditions == null) {
				this.conditions = TargetingConditions.forNonCombat().ignoreLineOfSight()
					.selector((target, lvl) -> bunnay.isFood(target.getMainHandItem()) || bunnay.isFood(target.getOffhandItem()));
			}
			this.player = level.getNearestPlayer(this.conditions.range(bunnay.getAttributeValue(Attributes.TEMPT_RANGE)), bunnay);
			return this.player != null;
		}

		@Override
		boolean canStart(ServerLevel level, BunnayEntity bunnay) {
			if (this.calmDown > 0) {
				this.calmDown--;
				return false;
			}
			return this.find(level, bunnay);
		}

		@Override
		boolean keepGoing(ServerLevel level, BunnayEntity bunnay) {
			return this.find(level, bunnay);
		}

		@Override
		void step(ServerLevel level, BunnayEntity bunnay) {
			if (this.player == null) {
				return;
			}
			bunnay.getLookControl().setLookAt(this.player, bunnay.getMaxHeadYRot() + 20, bunnay.getMaxHeadXRot());
			if (bunnay.distanceToSqr(this.player) < STOP_DISTANCE * STOP_DISTANCE) {
				bunnay.getNavigation().stop();
			} else {
				bunnay.getNavigation().moveTo(this.player, 1.0);
			}
		}

		@Override
		void end(ServerLevel level, BunnayEntity bunnay) {
			this.player = null;
			bunnay.getNavigation().stop();
			this.calmDown = 100;
		}
	}

	/** Follows its owner like a wolf: starts from 10 blocks away, stops 2 blocks from them, and teleports when far behind. */
	static final class FollowOwner extends Job {
		private static final double SPEED = 1.0;
		private static final double START_DISTANCE = 10.0;
		private static final double STOP_DISTANCE = 2.0;
		private LivingEntity owner;
		private int timeToRecalcPath;
		private float oldWaterCost;

		FollowOwner() {
			super(5, Activity.IDLE, Map.of());
		}

		@Override
		boolean canStart(ServerLevel level, BunnayEntity bunnay) {
			LivingEntity found = bunnay.getOwner();
			if (bunnay.isFarming() || found == null || bunnay.unableToMoveToOwner() || bunnay.distanceToSqr(found) < START_DISTANCE * START_DISTANCE) {
				return false;
			}
			this.owner = found;
			return true;
		}

		@Override
		void begin(ServerLevel level, BunnayEntity bunnay) {
			this.timeToRecalcPath = 0;
			this.oldWaterCost = bunnay.getPathfindingMalus(PathType.WATER);
			bunnay.setPathfindingMalus(PathType.WATER, 0.0F);
			bunnay.following = true;
		}

		@Override
		boolean keepGoing(ServerLevel level, BunnayEntity bunnay) {
			return this.owner != null && !bunnay.isFarming() && (this.age == 0 || !bunnay.getNavigation().isDone())
				&& !bunnay.unableToMoveToOwner() && bunnay.distanceToSqr(this.owner) > STOP_DISTANCE * STOP_DISTANCE;
		}

		@Override
		void step(ServerLevel level, BunnayEntity bunnay) {
			boolean far = bunnay.shouldTryTeleportToOwner();
			if (!far) {
				bunnay.getLookControl().setLookAt(this.owner, 10.0F, bunnay.getMaxHeadXRot());
			}
			if (--this.timeToRecalcPath <= 0) {
				this.timeToRecalcPath = 10;
				if (far) {
					bunnay.tryToTeleportToOwner();
				} else {
					bunnay.getNavigation().moveTo(this.owner, SPEED);
				}
			}
		}

		@Override
		void end(ServerLevel level, BunnayEntity bunnay) {
			this.owner = null;
			bunnay.following = false;
			bunnay.getNavigation().stop();
			bunnay.setPathfindingMalus(PathType.WATER, this.oldWaterCost);
		}
	}

	/** Pulls it back to the note block it is tuned in to when it has got too far from it. */
	static final class StayNearFarm extends Job {
		StayNearFarm() {
			super(4, Activity.IDLE, Map.of());
		}

		@Override
		boolean canStart(ServerLevel level, BunnayEntity bunnay) {
			BlockPos farm = bunnay.getFarmPos();
			return farm != null && bunnay.freeForFarmWork() && bunnay.distanceToSqr(Vec3.atCenterOf(farm)) > BunnayEntity.FARM_LEASH * BunnayEntity.FARM_LEASH;
		}

		@Override
		boolean keepGoing(ServerLevel level, BunnayEntity bunnay) {
			BlockPos farm = bunnay.getFarmPos();
			return farm != null && bunnay.freeForFarmWork() && this.age < BunnayEntity.FARM_DELIVER_GIVE_UP_TICKS
				&& bunnay.distanceToSqr(Vec3.atCenterOf(farm)) > BunnayEntity.FARM_CLOSE_ENOUGH * BunnayEntity.FARM_CLOSE_ENOUGH;
		}

		@Override
		void step(ServerLevel level, BunnayEntity bunnay) {
			BlockPos farm = bunnay.getFarmPos();
			if (farm != null && (this.age + 1) % 10 == 1) {
				bunnay.getNavigation().moveTo(farm.getX() + 0.5, farm.getY() + 1, farm.getZ() + 0.5, 1.2);
			}
		}

		@Override
		void end(ServerLevel level, BunnayEntity bunnay) {
			bunnay.getNavigation().stop();
		}
	}

	/** Takes the harvest to the note block it is tuned in to and tosses it there. */
	static final class DeliverToNoteBlock extends Job {
		private boolean delivered;

		DeliverToNoteBlock() {
			super(4, Activity.IDLE, memories(BunnayAi.DELIVER_COOLDOWN, MemoryStatus.VALUE_ABSENT));
		}

		private boolean shouldDeliver(BunnayEntity bunnay) {
			return bunnay.getFarmPos() != null && bunnay.freeForFarmWork() && bunnay.carrotsToDeliver() > 0
				&& (!bunnay.canTakeCarrots() || bunnay.isNoCropsFound());
		}

		@Override
		boolean canStart(ServerLevel level, BunnayEntity bunnay) {
			return this.shouldDeliver(bunnay);
		}

		@Override
		void begin(ServerLevel level, BunnayEntity bunnay) {
			this.delivered = false;
		}

		@Override
		boolean keepGoing(ServerLevel level, BunnayEntity bunnay) {
			return !this.delivered && this.age < BunnayEntity.FARM_DELIVER_GIVE_UP_TICKS && this.shouldDeliver(bunnay);
		}

		@Override
		void step(ServerLevel level, BunnayEntity bunnay) {
			BlockPos farm = bunnay.getFarmPos();
			if (farm == null) {
				return;
			}
			int ticks = this.age + 1;
			Vec3 target = Vec3.atCenterOf(farm);
			bunnay.getLookControl().setLookAt(target.x, target.y, target.z);
			if (bunnay.distanceToSqr(target) <= BunnayEntity.FARM_DELIVER_DISTANCE * BunnayEntity.FARM_DELIVER_DISTANCE) {
				ItemStack load = bunnay.food.removeItem(0, bunnay.carrotsToDeliver());
				BehaviorUtils.throwItem(bunnay, load, target);
				bunnay.playSound(SoundEvents.RABBIT_AMBIENT, 1.0F, 1.3F);
				bunnay.getBrain().setMemoryWithExpiry(BunnayAi.DELIVER_COOLDOWN, true, BunnayEntity.FARM_DELIVER_COOLDOWN);
				bunnay.getNavigation().stop();
				this.delivered = true;
			} else if (ticks % 10 == 1) {
				bunnay.getNavigation().moveTo(target.x, farm.getY() + 1, target.z, 1.2);
			}
		}

		@Override
		void end(ServerLevel level, BunnayEntity bunnay) {
			bunnay.getNavigation().stop();
		}
	}

	/** Gives carrots to a hungry owner: it walks to them and tosses them the carrots (see the GIFT_ constants). */
	static final class GiveCarrots extends Job {
		private boolean gave;

		GiveCarrots() {
			super(4, Activity.IDLE, Map.of());
		}

		private boolean shouldGive(BunnayEntity bunnay) {
			return bunnay.freeToHarvest() && bunnay.carrotsToGive() > 0 && bunnay.hungryOwner() != null;
		}

		@Override
		boolean canStart(ServerLevel level, BunnayEntity bunnay) {
			return bunnay.giftCooldown <= 0 && this.shouldGive(bunnay);
		}

		@Override
		void begin(ServerLevel level, BunnayEntity bunnay) {
			this.gave = false;
		}

		@Override
		boolean keepGoing(ServerLevel level, BunnayEntity bunnay) {
			return !this.gave && this.age < BunnayEntity.GIFT_GIVE_UP_TICKS && this.shouldGive(bunnay);
		}

		@Override
		void step(ServerLevel level, BunnayEntity bunnay) {
			ServerPlayer owner = bunnay.hungryOwner();
			if (owner == null) {
				return;
			}
			int ticks = this.age + 1;
			bunnay.getLookControl().setLookAt(owner, 30.0F, 30.0F);
			if (bunnay.distanceToSqr(owner) <= BunnayEntity.GIFT_REACH * BunnayEntity.GIFT_REACH) {
				ItemStack gift = bunnay.food.removeItem(0, bunnay.carrotsToGive());
				BehaviorUtils.throwItem(bunnay, gift, owner.position());
				bunnay.playSound(SoundEvents.RABBIT_AMBIENT, 1.0F, 1.3F);
				bunnay.giftCooldown = BunnayEntity.GIFT_COOLDOWN_TICKS;
				bunnay.getNavigation().stop();
				this.gave = true;
			} else if (ticks % 10 == 1) {
				bunnay.getNavigation().moveTo(owner, 1.2);
			}
		}

		@Override
		void end(ServerLevel level, BunnayEntity bunnay) {
			if (!this.gave && this.age >= BunnayEntity.GIFT_GIVE_UP_TICKS) {
				bunnay.giftCooldown = BunnayEntity.GIFT_RETRY_TICKS;
			}
			bunnay.getNavigation().stop();
		}
	}

	/** Goes to the carrot on the ground that the ground-carrots sensor found and picks it up into its food slot. */
	static final class PickUpCarrots extends Job {
		private ItemEntity item;

		PickUpCarrots() {
			super(6, Activity.IDLE, memories(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryStatus.VALUE_PRESENT));
		}

		@Override
		boolean canStart(ServerLevel level, BunnayEntity bunnay) {
			ItemEntity found = bunnay.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM).orElse(null);
			if (found == null || !bunnay.canPickUpGroundCarrots() || !bunnay.isWantedGroundItem(found)) {
				return false;
			}
			this.item = found;
			return true;
		}

		@Override
		void begin(ServerLevel level, BunnayEntity bunnay) {
			bunnay.getBrain().eraseMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
		}

		@Override
		boolean keepGoing(ServerLevel level, BunnayEntity bunnay) {
			return this.item != null && this.age < BunnayEntity.PICK_UP_GIVE_UP_TICKS && bunnay.canPickUpGroundCarrots() && bunnay.isWantedGroundItem(this.item);
		}

		@Override
		void step(ServerLevel level, BunnayEntity bunnay) {
			if (this.item == null) {
				return;
			}
			bunnay.getLookControl().setLookAt(this.item);
			if (bunnay.distanceToSqr(this.item) > 1.3 * 1.3) {
				if (bunnay.getNavigation().isDone()) {
					bunnay.getNavigation().moveTo(this.item, 1.2);
				}
				return;
			}
			ItemStack leftover = bunnay.food.addItem(this.item.getItem().copy());
			if (leftover.isEmpty()) {
				this.item.discard();
			} else {
				this.item.setItem(leftover);
			}
			bunnay.food.setChanged();
			level.playSound(null, bunnay.getX(), bunnay.getY(), bunnay.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.3F, 1.4F);
			this.item = null;
		}

		@Override
		void end(ServerLevel level, BunnayEntity bunnay) {
			this.item = null;
			bunnay.getNavigation().stop();
		}
	}

	/**
	 * Harvests one fully grown carrot at a time: walk to it, swing and break it, collect the drops,
	 * wait a moment, and replant, then straight on to the next ripe one. Every part has a time limit, and a carrot that was
	 * broken is always replanted when the job ends, however it ends.
	 */
	static final class HarvestCarrots extends Job {
		private enum Phase {
			GO_TO_CROP,
			PICK_UP,
			WAIT
		}

		private BlockPos crop;
		private Phase phase = Phase.GO_TO_CROP;
		private boolean broken;
		private final List<ItemEntity> drops = new ArrayList<>();
		private int workTicks;
		private int phaseTicks;
		private int giveUpTicks;

		HarvestCarrots() {
			super(5, Activity.IDLE, memories(BunnayAi.RIPE_CARROT, MemoryStatus.VALUE_PRESENT));
		}

		@Override
		boolean canStart(ServerLevel level, BunnayEntity bunnay) {
			BlockPos found = bunnay.getBrain().getMemory(BunnayAi.RIPE_CARROT).orElse(null);
			if (found == null || !bunnay.canStartHarvest() || !bunnay.isRipeCarrot(found)) {
				return false;
			}
			this.crop = found;
			return true;
		}

		@Override
		void begin(ServerLevel level, BunnayEntity bunnay) {
			bunnay.getBrain().eraseMemory(BunnayAi.RIPE_CARROT);
			this.reset();
		}

		private void reset() {
			this.phase = Phase.GO_TO_CROP;
			this.broken = false;
			this.drops.clear();
			this.workTicks = 0;
			this.phaseTicks = 0;
			this.giveUpTicks = 0;
		}

		@Override
		boolean keepGoing(ServerLevel level, BunnayEntity bunnay) {
			if (this.crop == null || this.giveUpTicks >= BunnayEntity.HARVEST_GIVE_UP_TICKS || !bunnay.freeToHarvest()) {
				return false;
			}
			return this.phase != Phase.GO_TO_CROP || (bunnay.canTakeCarrots() && bunnay.isRipeCarrot(this.crop));
		}

		@Override
		void end(ServerLevel level, BunnayEntity bunnay) {
			if (this.broken && this.crop != null) {
				this.plant(level, bunnay);
			}
			this.crop = null;
			this.broken = false;
			this.drops.clear();
			bunnay.getNavigation().stop();
		}

		@Override
		void step(ServerLevel level, BunnayEntity bunnay) {
			if (this.crop == null) {
				return;
			}
			this.giveUpTicks++;
			switch (this.phase) {
				case GO_TO_CROP -> this.goToCrop(level, bunnay);
				case PICK_UP -> this.pickUp(bunnay);
				case WAIT -> this.waitToPlant(level, bunnay);
			}
		}

		private void goToCrop(ServerLevel level, BunnayEntity bunnay) {
			Vec3 center = Vec3.atCenterOf(this.crop);
			bunnay.getLookControl().setLookAt(center.x, center.y - 0.3, center.z);
			if (bunnay.distanceToSqr(center) > 1.9 * 1.9) {
				this.workTicks = 0;
				if (bunnay.getNavigation().isDone()) {
					bunnay.getNavigation().moveTo(center.x, this.crop.getY(), center.z, 1.2);
				}
				return;
			}
			bunnay.getNavigation().stop();
			this.workTicks++;
			if (this.workTicks == 1) {
				bunnay.swing(InteractionHand.MAIN_HAND, bunnay.getMainHandItem().getInteractAnimation());
			}
			if (this.workTicks >= BunnayEntity.HARVEST_WORK_TICKS && bunnay.isRipeCarrot(this.crop)) {
				level.destroyBlock(this.crop, true, bunnay);
				this.broken = true;
				this.drops.clear();
				this.drops.addAll(level.getEntitiesOfClass(ItemEntity.class, new AABB(this.crop).inflate(1.5), item -> item.getAge() < 20));
				this.phase = Phase.PICK_UP;
				this.phaseTicks = 0;
			}
		}

		private void pickUp(BunnayEntity bunnay) {
			this.phaseTicks++;
			this.drops.removeIf(item -> item.isRemoved() || item.getItem().isEmpty());
			if (this.drops.isEmpty() || !bunnay.canTakeCarrots() || this.phaseTicks > BunnayEntity.HARVEST_PICK_UP_TICKS) {
				this.phase = Phase.WAIT;
				this.phaseTicks = 0;
				bunnay.getNavigation().stop();
				return;
			}
			ItemEntity next = this.drops.stream().min(Comparator.comparingDouble(bunnay::distanceToSqr)).get();
			bunnay.getLookControl().setLookAt(next);
			if (bunnay.distanceToSqr(next) > 1.3 * 1.3) {
				if (bunnay.getNavigation().isDone()) {
					bunnay.getNavigation().moveTo(next, 1.2);
				}
				return;
			}
			ItemStack leftover = bunnay.food.addItem(next.getItem().copy());
			if (leftover.isEmpty()) {
				next.discard();
			} else {
				next.setItem(leftover);
			}
			bunnay.food.setChanged();
			bunnay.level().playSound(null, bunnay.getX(), bunnay.getY(), bunnay.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.3F, 1.4F);
		}

		private void waitToPlant(ServerLevel level, BunnayEntity bunnay) {
			Vec3 center = Vec3.atCenterOf(this.crop);
			bunnay.getLookControl().setLookAt(center.x, center.y - 0.3, center.z);
			if (bunnay.distanceToSqr(center) > 2.2 * 2.2) {
				this.phaseTicks = 0;
				if (bunnay.getNavigation().isDone()) {
					bunnay.getNavigation().moveTo(center.x, this.crop.getY(), center.z, 1.2);
				}
				return;
			}
			bunnay.getNavigation().stop();
			this.phaseTicks++;
			if (this.phaseTicks >= BunnayEntity.HARVEST_REPLANT_DELAY_TICKS) {
				this.plant(level, bunnay);
				this.startNextCarrot(bunnay);
			}
		}

		/** Straight on to the next ripe carrot without letting go of the job; with none left it ends. */
		private void startNextCarrot(BunnayEntity bunnay) {
			boolean canStart = bunnay.canStartHarvest();
			BlockPos next = canStart ? bunnay.findRipeCarrot() : null;
			if (canStart) {
				if (next == null) {
					bunnay.getBrain().setMemory(BunnayAi.NO_CROPS_FOUND, true);
				} else {
					bunnay.getBrain().eraseMemory(BunnayAi.NO_CROPS_FOUND);
				}
			}
			if (next == null) {
				this.crop = null;
				return;
			}
			this.crop = next;
			this.reset();
		}

		private void plant(ServerLevel level, BunnayEntity bunnay) {
			BlockPos spot = this.crop;
			this.broken = false;
			if (spot == null || !level.isLoaded(spot) || !level.getBlockState(spot).isAir() || !level.getBlockState(spot.below()).is(Blocks.FARMLAND)) {
				return;
			}
			level.setBlockAndUpdate(spot, Blocks.CARROTS.defaultBlockState());
			level.playSound(null, spot, SoundEvents.CROP_PLANTED, SoundSource.NEUTRAL, 0.8F, 1.0F);
			bunnay.swing(InteractionHand.MAIN_HAND, bunnay.getMainHandItem().getInteractAnimation());
		}
	}
}
