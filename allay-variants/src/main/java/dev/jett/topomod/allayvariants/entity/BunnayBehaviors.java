package dev.jett.topomod.allayvariants.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.GoAndGiveItemsToTarget;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.schedule.Activity;

// The bunnay's own behaviours. Everything that walks it somewhere does it the way the vanilla ones do: it sets the walk target and
// the vanilla move sink walks there (see BunnayAi for which vanilla behaviours are used as they are).
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

	/** A behaviour that does its work in one go, every tick, and is never "running" (like the vanilla one-shots). */
	abstract static class Instant extends OneShot<BunnayEntity> {
		private final Set<MemoryModuleType<?>> required;

		Instant(Set<MemoryModuleType<?>> required) {
			this.required = required;
		}

		@Override
		public Set<MemoryModuleType<?>> getRequiredMemories() {
			return this.required;
		}
	}

	// ---- core ----

	/** Like a wolf, it teleports to a far-off owner. Used while following (idle) and, as a wolf does, while panicking. */
	static final class TeleportToOwner extends Instant {
		private static final int RETRY_TICKS = 10;
		private final boolean evenWhenFarming;
		private int retryDelay;

		TeleportToOwner(boolean evenWhenFarming) {
			super(Set.of());
			this.evenWhenFarming = evenWhenFarming;
		}

		@Override
		public boolean trigger(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			if (!bunnay.isTame() || bunnay.unableToMoveToOwner() || (!this.evenWhenFarming && bunnay.isFarming()) || !bunnay.shouldTryTeleportToOwner()) {
				return false;
			}
			if (--this.retryDelay <= 0) {
				this.retryDelay = RETRY_TICKS;
				bunnay.tryToTeleportToOwner();
			}
			return false;
		}
	}

	/**
	 * Every tick: drops a target that is no longer valid, and takes a new one the way a wolf does, in this
	 * order of importance: whatever hurt its owner, whatever its owner attacked, whatever hurt it. A new reason only replaces
	 * the current target if it is at least as important as the one that gave it. It is a one-shot that never reports
	 * having done anything, so it costs a tick and nothing else.
	 */
	static final class TargetTracking extends Instant {
		private static final int NONE = 99;
		private static final int HURT_BY_UNSEEN_TICKS = 300;
		/** It gives up on a target after this long (10 seconds) with no route to it, out of reach and without having landed a hit. */
		private static final int GIVE_UP_TICKS = 200;
		/** A hit it landed this recently (2 seconds) counts as the fight going on. */
		private static final int RECENT_HIT_TICKS = 40;
		private static final TargetingConditions HURT_BY = TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting();

		private int ownerHurtByStamp;
		private int ownerHurtStamp;
		private int hurtByStamp;
		private int priority = NONE;
		private int unseenTicks;
		private int unreachableTicks;

		TargetTracking() {
			super(Set.of(MemoryModuleType.ATTACK_TARGET));
		}

		@Override
		public boolean trigger(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			Brain<BunnayEntity> brain = bunnay.getBrain();
			LivingEntity current = brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
			if (current != null && !this.stillValid(bunnay, current)) {
				bunnay.setTarget(null);
				current = null;
			}
			if (current != null && this.tiredOfTrying(bunnay, current)) {
				bunnay.debugLog("gave up on %s: no route, out of reach and no hit for %d ticks", current.getType().getDescriptionId(), GIVE_UP_TICKS);
				bunnay.setTarget(null);
				current = null;
			}
			if (current == null) {
				this.priority = NONE;
				this.unseenTicks = 0;
				this.unreachableTicks = 0;
			}
			if ((bunnay.isScared() || bunnay.isFleeing()) && current != null) {
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

		/**
		 * Whether it has had no way to get at the target for long enough to give up, the way the Brain mobs do: counting only the
		 * time with no route to it, out of reach of a swing, and no hit landed lately. Any of those ends the count, and a target
		 * it picks up afresh starts it again.
		 */
		private boolean tiredOfTrying(BunnayEntity bunnay, LivingEntity target) {
			Path path = bunnay.getNavigation().getPath();
			boolean gettingSomewhere = bunnay.isWithinMeleeAttackRange(target) || (path != null && path.canReach())
				|| bunnay.tickCount - bunnay.getLastHurtMobTimestamp() <= RECENT_HIT_TICKS;
			this.unreachableTicks = gettingSomewhere ? 0 : this.unreachableTicks + 1;
			return this.unreachableTicks > GIVE_UP_TICKS;
		}

		private void take(BunnayEntity bunnay, LivingEntity target, int importance) {
			this.unreachableTicks = 0;
			bunnay.setTarget(target);
			bunnay.debugLog("took target %s (importance %d)", target.getType().getDescriptionId(), importance);
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
	 * The two halves of a hop, as in the frog's long jump: HopPrepare looks every so often (once the cooldown is over, and only
	 * with a reason to hop: see BunnayEntity.hopReason) for a spot it could land on and not walk to, and starts the hop, setting
	 * the mid-jump memory, and HopMidJump flies it. A spot some other behaviour has asked for (the HOP_TARGET memory, which the
	 * flee uses) is tried first. There is no wind-up: the launch is on the very tick the hop is chosen.
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
				BunnayAi.HOP_TARGET, MemoryStatus.REGISTERED,
				MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED), 2);
			this.run = run;
		}

		@Override
		protected boolean checkExtraStartConditions(ServerLevel level, BunnayEntity bunnay) {
			Brain<BunnayEntity> brain = bunnay.getBrain();
			if (!bunnay.canHopNow()) {
				if (brain.hasMemoryValue(BunnayAi.HOP_TARGET)) {
					bunnay.debugLog("hop asked for but it cannot hop right now (ground=%s hopping=%s), request still waiting", bunnay.onGround(), bunnay.isHopping());
				}
				this.scanDelay = 0;
				return false;
			}
			BlockPos asked = brain.getMemory(BunnayAi.HOP_TARGET).orElse(null);
			if (asked != null) {
				brain.eraseMemory(BunnayAi.HOP_TARGET);
				this.plan = bunnay.hopTo(Vec3.atBottomCenterOf(asked));
				bunnay.debugLog("hop asked for at %s: %s", asked.toShortString(), this.plan != null ? "launching" : "no arc any more");
				if (this.plan != null) {
					return true;
				}
			}
			// Looking for somewhere to land is only done with a reason to hop, and at a steady pace.
			if (bunnay.hopReason() == null) {
				this.scanDelay = 0;
				return false;
			}
			if (--this.scanDelay > 0) {
				return false;
			}
			this.plan = bunnay.planReasonedHop();
			this.scanDelay = this.plan == null ? BunnayEntity.HOP_RETRY_TICKS : BunnayEntity.HOP_SCAN_TICKS;
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
			// Nothing walks it while it flies: whatever behaviours asked for a walk target this tick are told no, before the move
			// sink (which comes later in the tick) can start a path.
			bunnay.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
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

	// ---- activities of their own ----

	/** Sits while ordered to, and does nothing else (its activity is the only one with behaviours that could move it). */
	static final class Sit extends Behavior<BunnayEntity> {
		Sit() {
			super(Map.of(), NO_TIMEOUT);
		}

		@Override
		protected boolean checkExtraStartConditions(ServerLevel level, BunnayEntity bunnay) {
			return bunnay.wantsToSitNow();
		}

		@Override
		protected void start(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			bunnay.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			bunnay.getNavigation().stop();
			bunnay.setInSittingPose(true);
		}

		@Override
		protected boolean canStillUse(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			return bunnay.getBrain().isActive(BunnayAi.SIT) && bunnay.isOrderedToSit();
		}

		@Override
		protected void stop(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			bunnay.setInSittingPose(false);
		}
	}

	/** Stands still while it dances; a fight takes priority (the activity changes). */
	static final class Dance extends Behavior<BunnayEntity> {
		Dance() {
			super(Map.of(), NO_TIMEOUT);
		}

		@Override
		protected boolean checkExtraStartConditions(ServerLevel level, BunnayEntity bunnay) {
			return bunnay.isDancing() && !bunnay.hasLiveTarget();
		}

		@Override
		protected void start(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			bunnay.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			bunnay.getNavigation().stop();
		}

		@Override
		protected boolean canStillUse(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			return bunnay.getBrain().isActive(BunnayAi.DANCE) && bunnay.isDancing() && !bunnay.hasLiveTarget();
		}
	}

	// ---- fleeing ----

	/**
	 * Runs from what is after it, like a rabbit (this is vanilla's SetWalkTargetAwayFrom with one addition): when it has nowhere it is
	 * already running to, it picks a random place up to 16 blocks away in the half-circle facing away from the threat and runs
	 * there, and when it gets there it picks another. The addition: half the time, if there are places it could hop to and not
	 * walk to, it picks one of those instead (the HOP_TARGET memory, which the hop behaviour carries out).
	 */
	static final class FleeFromThreat extends Instant {
		/** After finding nowhere to go it waits this long before trying again. */
		private static final int RETRY_TICKS = 5;
		private static final int HOP_CANDIDATES = 6;
		/** How many random places it tries for one to run to (vanilla's SetWalkTargetAwayFrom tries 10), and how many of those it looks for a path to in a tick. */
		private static final int SAMPLES = 30;
		private static final int MAX_PATH_CHECKS = 5;
		/** How far it picks a place to run to, sideways and up or down (the rabbit's is 16 and 7; a bunnay can only path about 16 blocks). */
		private static final int PICK_RANGE = 12;
		private static final int PICK_HEIGHT = 7;
		/**
		 * The high ground: when it is this many blocks above what it runs from (in a tree, on a cliff), the ground mobs cannot get
		 * to it, so it does not run to anywhere more than HIGH_GROUND_DROP lower than it stands. If that leaves nowhere, it stays.
		 */
		private static final double HIGH_GROUND_RISE = 2.0;
		private static final double HIGH_GROUND_DROP = 1.0;
		private int retryDelay;

		/** Whether a path gets all the way there without going lower than this. */
		private static boolean staysHigh(Path path, double lowest) {
			if (!path.canReach()) {
				return false;
			}
			int floor = Mth.floor(lowest);
			for (int i = 0; i < path.getNodeCount(); i++) {
				if (path.getNodePos(i).getY() < floor) {
					return false;
				}
			}
			return true;
		}

		FleeFromThreat() {
			super(Set.of(MemoryModuleType.AVOID_TARGET, MemoryModuleType.WALK_TARGET, BunnayAi.HOP_TARGET, MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS,
				MemoryModuleType.LONG_JUMP_MID_JUMP));
		}

		@Override
		public boolean trigger(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			Brain<BunnayEntity> brain = bunnay.getBrain();
			LivingEntity threat = brain.getMemory(MemoryModuleType.AVOID_TARGET).orElse(null);
			if (threat == null || brain.hasMemoryValue(MemoryModuleType.WALK_TARGET) || brain.hasMemoryValue(BunnayAi.HOP_TARGET)
				|| brain.hasMemoryValue(MemoryModuleType.LONG_JUMP_MID_JUMP) || --this.retryDelay > 0) {
				return false;
			}
			Vec3 avoid = threat.position();
			boolean highGround = bunnay.getY() - threat.getY() >= HIGH_GROUND_RISE;
			double lowest = bunnay.getY() - HIGH_GROUND_DROP;
			boolean canHop = bunnay.canHopNow() && !brain.hasMemoryValue(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS);
			if (canHop && bunnay.getRandom().nextBoolean()) {
				List<BunnayEntity.Hop> hops = bunnay.hopsAwayFrom(avoid, HOP_CANDIDATES);
				if (highGround) {
					hops = hops.stream().filter(hop -> hop.landing().y >= lowest).toList();
				}
				if (!hops.isEmpty()) {
					BunnayEntity.Hop pick = hops.get(bunnay.getRandom().nextInt(hops.size()));
					brain.setMemoryWithExpiry(BunnayAi.HOP_TARGET, BlockPos.containing(pick.landing()), 20L);
					bunnay.debugLog("flee pick: asked for a hop to %s (%d candidates)", pick.landing(), hops.size());
					return true;
				}
			}
			// The rabbit's rule (AvoidEntityGoal): a place that is no nearer the threat than it is now, and that it has a path to.
			double nowSqr = avoid.distanceToSqr(bunnay.position());
			int pathChecks = 0;
			for (int attempt = 0; attempt < SAMPLES && pathChecks < MAX_PATH_CHECKS; attempt++) {
				Vec3 spot = LandRandomPos.getPosAway(bunnay, PICK_RANGE, PICK_HEIGHT, avoid);
				if (spot == null || avoid.distanceToSqr(spot) < nowSqr || (highGround && spot.y < lowest)) {
					continue;
				}
				pathChecks++;
				Path path = bunnay.getNavigation().createPath(spot.x, spot.y, spot.z, 0);
				// On the high ground the whole way there has to stay up: a partial path to somewhere it cannot reach, say, goes down.
				if (path != null && (!highGround || staysHigh(path, lowest))) {
					brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot, BunnayEntity.FLEE_SPEED, 0));
					bunnay.debugLog("flee pick: run to %s (%.1f blocks from here, %d path check(s))", spot, spot.distanceTo(bunnay.position()), pathChecks);
					return true;
				}
			}
			this.retryDelay = RETRY_TICKS;
			bunnay.debugLog("flee pick: nowhere to go%s, waiting %d ticks", highGround ? " (holding the high ground)" : "", RETRY_TICKS);
			return false;
		}
	}

	// ---- giving ----

	/** Tosses carrots to a hungry owner, the way an allay hands items to the player it likes (vanilla's GoAndGiveItemsToTarget). */
	static final class GiveCarrots extends GoAndGiveItemsToTarget<BunnayEntity> {
		GiveCarrots() {
			super(
				bunnay -> bunnay instanceof BunnayEntity b && b.hungryOwner() != null
					? Optional.of(new net.minecraft.world.entity.ai.behavior.EntityTracker(b.hungryOwner(), true))
					: Optional.empty(),
				BunnayEntity.GIVE_SPEED, BunnayEntity.GIFT_GIVE_UP_TICKS,
				(level, bunnay, target) -> {
					ItemStack gift = bunnay.food.removeItem(0, bunnay.carrotsToGive());
					BehaviorUtils.throwItem(bunnay, gift, target);
					bunnay.playSound(SoundEvents.RABBIT_AMBIENT, 1.0F, 1.3F);
				},
				BunnayAi.GIFT_COOLDOWN, BunnayEntity.GIFT_COOLDOWN_TICKS,
				bunnay -> bunnay.freeToHarvest() && bunnay.carrotsToGive() > 0 && bunnay.hungryOwner() != null);
		}

		@Override
		protected boolean checkExtraStartConditions(ServerLevel level, BunnayEntity bunnay) {
			return !bunnay.getBrain().hasMemoryValue(BunnayAi.GIFT_COOLDOWN) && super.checkExtraStartConditions(level, bunnay);
		}
	}

	/** Takes the harvest to the note block it is tuned in to and tosses it there, the way an allay hands over what it has collected. */
	static final class DeliverToNoteBlock extends GoAndGiveItemsToTarget<BunnayEntity> {
		DeliverToNoteBlock() {
			super(
				bunnay -> bunnay instanceof BunnayEntity b && b.getFarmPos() != null
					? Optional.of(new BlockPosTracker(b.getFarmPos()))
					: Optional.empty(),
				BunnayEntity.FARM_SPEED, BunnayEntity.FARM_DELIVER_GIVE_UP_TICKS,
				(level, bunnay, target) -> {
					ItemStack load = bunnay.food.removeItem(0, bunnay.carrotsToDeliver());
					BehaviorUtils.throwItem(bunnay, load, target);
					bunnay.playSound(SoundEvents.RABBIT_AMBIENT, 1.0F, 1.3F);
				},
				BunnayAi.DELIVER_COOLDOWN, BunnayEntity.FARM_DELIVER_COOLDOWN,
				bunnay -> bunnay.getFarmPos() != null && bunnay.freeForFarmWork() && bunnay.carrotsToDeliver() > 0
					&& (!bunnay.canTakeCarrots() || bunnay.isNoCropsFound()));
		}

		@Override
		protected boolean checkExtraStartConditions(ServerLevel level, BunnayEntity bunnay) {
			return !bunnay.getBrain().hasMemoryValue(BunnayAi.DELIVER_COOLDOWN) && super.checkExtraStartConditions(level, bunnay);
		}
	}

	// ---- harvesting ----

	/** Heads for the ripe carrot the sensor found (vanilla's way: it sets the walk target and the move sink walks there). */
	static final class WalkToRipeCarrot extends Instant {
		WalkToRipeCarrot() {
			super(Set.of(BunnayAi.RIPE_CARROT, MemoryModuleType.WALK_TARGET, MemoryModuleType.LOOK_TARGET));
		}

		@Override
		public boolean trigger(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			Brain<BunnayEntity> brain = bunnay.getBrain();
			BlockPos crop = brain.getMemory(BunnayAi.RIPE_CARROT).orElse(null);
			if (crop == null || !bunnay.canStartHarvest()) {
				return false;
			}
			if (!bunnay.isRipeCarrot(crop)) {
				brain.eraseMemory(BunnayAi.RIPE_CARROT);
				return false;
			}
			if (bunnay.distanceToSqr(Vec3.atCenterOf(crop)) > BunnayEntity.HARVEST_REACH * BunnayEntity.HARVEST_REACH) {
				BehaviorUtils.setWalkAndLookTargetMemories(bunnay, crop, BunnayEntity.FARM_SPEED, 1);
			}
			return true;
		}
	}

	/**
	 * Breaks the ripe carrot it has come up to (swinging at it first), so the carrots really drop, and plants a new one in its
	 * place at once, so there is never a hole in the field. What it dropped is picked up the way any loose carrot is.
	 */
	static final class BreakRipeCarrot extends Behavior<BunnayEntity> {
		private BlockPos crop;
		private int workTicks;

		BreakRipeCarrot() {
			super(memories(BunnayAi.RIPE_CARROT, MemoryStatus.VALUE_PRESENT), NO_TIMEOUT);
		}

		private boolean canWork(BunnayEntity bunnay) {
			return this.crop != null && bunnay.canStartHarvest() && bunnay.isRipeCarrot(this.crop)
				&& bunnay.distanceToSqr(Vec3.atCenterOf(this.crop)) <= BunnayEntity.HARVEST_REACH * BunnayEntity.HARVEST_REACH;
		}

		@Override
		protected boolean checkExtraStartConditions(ServerLevel level, BunnayEntity bunnay) {
			this.crop = bunnay.getBrain().getMemory(BunnayAi.RIPE_CARROT).orElse(null);
			return this.canWork(bunnay);
		}

		@Override
		protected void start(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			this.workTicks = 0;
			bunnay.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			bunnay.getNavigation().stop();
			bunnay.swing(InteractionHand.MAIN_HAND, bunnay.getMainHandItem().getInteractAnimation());
		}

		@Override
		protected boolean canStillUse(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			return bunnay.getBrain().isActive(Activity.IDLE) && this.canWork(bunnay);
		}

		@Override
		protected void tick(ServerLevel level, BunnayEntity bunnay, long timestamp) {
			Vec3 center = Vec3.atCenterOf(this.crop);
			bunnay.getLookControl().setLookAt(center.x, center.y - 0.3, center.z);
			if (++this.workTicks < BunnayEntity.HARVEST_WORK_TICKS) {
				return;
			}
			level.destroyBlock(this.crop, true, bunnay);
			if (level.getBlockState(this.crop).isAir() && level.getBlockState(this.crop.below()).is(Blocks.FARMLAND)) {
				level.setBlockAndUpdate(this.crop, Blocks.CARROTS.defaultBlockState());
				level.playSound(null, this.crop, SoundEvents.CROP_PLANTED, SoundSource.NEUTRAL, 0.8F, 1.0F);
				bunnay.swing(InteractionHand.MAIN_HAND, bunnay.getMainHandItem().getInteractAnimation());
			}
			// On to the next ripe one, if there is one.
			Brain<BunnayEntity> brain = bunnay.getBrain();
			BlockPos next = bunnay.findRipeCarrot();
			if (next != null) {
				brain.setMemory(BunnayAi.RIPE_CARROT, next);
				brain.eraseMemory(BunnayAi.NO_CROPS_FOUND);
			} else {
				brain.eraseMemory(BunnayAi.RIPE_CARROT);
				brain.setMemory(BunnayAi.NO_CROPS_FOUND, true);
			}
			this.crop = null;
		}
	}
}
