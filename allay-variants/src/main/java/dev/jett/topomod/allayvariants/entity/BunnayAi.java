package dev.jett.topomod.allayvariants.entity;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;

import dev.jett.topomod.allayvariants.AllayVariantsMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.AnimalPanic;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.FollowTemptation;
import net.minecraft.world.entity.ai.behavior.GoToWantedItem;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.behavior.RandomLookAround;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.StayCloseToTarget;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

// The bunnay's Brain, built the way the newer mobs' are. Every behaviour that takes it somewhere sets the walk target and the move
// sink walks there; which one wins is the order they are listed in (a later one that sets the target replaces an earlier one, and
// the ones that only act when there is none, like wandering, come last). Which behaviours are even considered is the activity.
public final class BunnayAi {
	// ---- registered types (registries freeze after mod init, so BunnayAi.register() must be called from there) ----

	public static final Activity SIT = registerActivity("sit");
	public static final Activity DANCE = registerActivity("dance");

	static final MemoryModuleType<Boolean> EAT_COOLDOWN = registerMemory("eat_cooldown", null);
	/** Ticks until it will give carrots to a hungry owner again (saved with it, so a long wait survives a restart). */
	static final MemoryModuleType<Integer> GIFT_COOLDOWN = registerMemory("gift_cooldown", Codec.INT);
	static final MemoryModuleType<Integer> DELIVER_COOLDOWN = registerMemory("deliver_cooldown", null);
	/** Set when it looked for ripe carrots and found none (what decides when to deliver). */
	static final MemoryModuleType<Boolean> NO_CROPS_FOUND = registerMemory("no_crops_found", null);
	static final MemoryModuleType<BlockPos> RIPE_CARROT = registerMemory("ripe_carrot", null);
	static final MemoryModuleType<LivingEntity> OWNER_HURT_BY = registerMemory("owner_hurt_by", null);
	static final MemoryModuleType<LivingEntity> OWNER_HURT_TARGET = registerMemory("owner_hurt_target", null);

	static final SensorType<BunnaySensors.Combat> COMBAT_SENSOR = registerSensor("combat", BunnaySensors.Combat::new);
	static final SensorType<BunnaySensors.Threats> THREATS_SENSOR = registerSensor("threats", BunnaySensors.Threats::new);
	static final SensorType<BunnaySensors.GroundCarrots> GROUND_CARROTS_SENSOR = registerSensor("ground_carrots", BunnaySensors.GroundCarrots::new);
	static final SensorType<BunnaySensors.RipeCarrots> RIPE_CARROTS_SENSOR = registerSensor("ripe_carrots", BunnaySensors.RipeCarrots::new);

	private static Activity registerActivity(String name) {
		return Registry.register(BuiltInRegistries.ACTIVITY, AllayVariantsMod.id(name), new Activity(name));
	}

	private static <U> MemoryModuleType<U> registerMemory(String name, Codec<U> codec) {
		return Registry.register(BuiltInRegistries.MEMORY_MODULE_TYPE, AllayVariantsMod.id(name), new MemoryModuleType<>(Optional.ofNullable(codec)));
	}

	private static <U extends Sensor<?>> SensorType<U> registerSensor(String name, Supplier<U> factory) {
		return Registry.register(BuiltInRegistries.SENSOR_TYPE, AllayVariantsMod.id(name), new SensorType<>(factory));
	}

	/** Called from the mod initializer, so that the static registrations above happen before the registries freeze. */
	public static void register() {
	}

	// ---- the brain ----

	private static final ImmutableList<MemoryModuleType<?>> MEMORIES = ImmutableList.of(
		MemoryModuleType.WALK_TARGET, MemoryModuleType.LOOK_TARGET, MemoryModuleType.PATH, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
		MemoryModuleType.ATTACK_TARGET, MemoryModuleType.ATTACK_COOLING_DOWN, MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY,
		MemoryModuleType.AVOID_TARGET, MemoryModuleType.IS_PANICKING, MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, MemoryModuleType.LONG_JUMP_MID_JUMP,
		MemoryModuleType.GAZE_COOLDOWN_TICKS, MemoryModuleType.TEMPTING_PLAYER, MemoryModuleType.TEMPTATION_COOLDOWN_TICKS,
		MemoryModuleType.BREED_TARGET, MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS,
		MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
		MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryModuleType.LIKED_NOTEBLOCK_POSITION,
		EAT_COOLDOWN, GIFT_COOLDOWN, DELIVER_COOLDOWN, NO_CROPS_FOUND, RIPE_CARROT, OWNER_HURT_BY, OWNER_HURT_TARGET);

	private static final ImmutableList<SensorType<? extends Sensor<? super BunnayEntity>>> SENSORS = ImmutableList.of(
		SensorType.NEAREST_LIVING_ENTITIES, SensorType.FOOD_TEMPTATIONS, COMBAT_SENSOR, THREATS_SENSOR, GROUND_CARROTS_SENSOR, RIPE_CARROTS_SENSOR);

	@SuppressWarnings("deprecation")
	private static final Brain.Provider<BunnayEntity> BRAIN_PROVIDER = Brain.provider(MEMORIES, SENSORS, BunnayAi::activities);

	static Brain<BunnayEntity> makeBrain(BunnayEntity body, Brain.Packed packed) {
		return BRAIN_PROVIDER.makeBrain(body, packed);
	}

	// The behaviours of the other activities start at this priority, after all of the core ones: the move sink (core) has looked at
	// the walk target for the tick before anything else sets one, so a hop that has just launched is never also started on a path.
	private static final int ACTIVITY_PRIORITY = 20;

	private static List<ActivityData<BunnayEntity>> activities(BunnayEntity body) {
		BunnayEntity.HopRun hop = new BunnayEntity.HopRun(body);
		return List.of(core(hop), idle(), fight(), sit(), dance(), avoid(), panic());
	}

	// Always on: floating, panic, keeping the target honest, hopping, eating, the cooldowns, and the vanilla look and move sinks.
	private static ActivityData<BunnayEntity> core(BunnayEntity.HopRun hop) {
		return ActivityData.create(Activity.CORE, 0, ImmutableList.<BehaviorControl<? super BunnayEntity>>of(
			new Swim<BunnayEntity>(0.8F),
			new AnimalPanic<BunnayEntity>(1.5F, mob -> DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES),
			new BunnayBehaviors.TargetTracking(),
			new BunnayBehaviors.HopPrepare(hop),
			new BunnayBehaviors.HopMidJump(hop),
			new BunnayBehaviors.EatCarrot(),
			new LookAtTargetSink(45, 90),
			new MoveToTargetSink(),
			new CountDownCooldownTicks(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS),
			new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS),
			new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
			new CountDownCooldownTicks(GIFT_COOLDOWN),
			new CountDownCooldownTicks(DELIVER_COOLDOWN)));
	}

	// Walking behaviours, least important first (a later one that sets the walk target replaces an earlier one), then wandering,
	// which only acts when nothing has set one, then the looking.
	private static ActivityData<BunnayEntity> idle() {
		return ActivityData.create(Activity.IDLE, ACTIVITY_PRIORITY, ImmutableList.<BehaviorControl<? super BunnayEntity>>of(
			// Loose carrots: walk to the one the ground-carrots sensor found, if it is not already going somewhere.
			GoToWantedItem.create(mob -> true, BunnayEntity.PICK_UP_SPEED, false, BunnayEntity.PICK_UP_RADIUS),
			// Follows its owner like a wolf: starts from 10 blocks away and stops 2 blocks from them; teleports when far behind.
			new BunnayBehaviors.TeleportToOwner(false),
			StayCloseToTarget.create(BunnayAi::ownerToFollow, mob -> mob instanceof BunnayEntity bunnay && bunnay.canFollowOwner(),
				BunnayEntity.FOLLOW_STOP_DISTANCE, BunnayEntity.FOLLOW_START_DISTANCE, BunnayEntity.FOLLOW_SPEED),
			// Farming: ripe carrots, and not straying from the note block it is tuned in to.
			new BunnayBehaviors.WalkToRipeCarrot(),
			new BunnayBehaviors.BreakRipeCarrot(),
			StayCloseToTarget.create(BunnayAi::farmToStayNear, mob -> mob instanceof BunnayEntity bunnay && bunnay.freeForFarmWork(),
				(int) BunnayEntity.FARM_CLOSE_ENOUGH, (int) BunnayEntity.FARM_LEASH, BunnayEntity.FARM_SPEED),
			new BunnayBehaviors.DeliverToNoteBlock(),
			new BunnayBehaviors.GiveCarrots(),
			// A player holding a carrot.
			new FollowTemptation(mob -> BunnayEntity.TEMPT_SPEED),
			new RunOne<BunnayEntity>(
				ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
				ImmutableList.of(Pair.of(RandomStroll.stroll(0.8F, false), 2), Pair.of(new DoNothing(60, 120), 3))),
			SetEntityLookTargetSometimes.create(EntityTypes.PLAYER, 8.0F, UniformInt.of(30, 60)),
			new RandomLookAround(UniformInt.of(150, 250), 30.0F, 0.0F, 0.0F)));
	}

	private static Optional<PositionTracker> ownerToFollow(LivingEntity mob) {
		return mob instanceof BunnayEntity bunnay && bunnay.getOwner() != null
			? Optional.of(new EntityTracker(bunnay.getOwner(), true))
			: Optional.empty();
	}

	private static Optional<PositionTracker> farmToStayNear(LivingEntity mob) {
		return mob instanceof BunnayEntity bunnay && bunnay.getFarmPos() != null
			? Optional.of(new BlockPosTracker(bunnay.getFarmPos()))
			: Optional.empty();
	}

	private static ActivityData<BunnayEntity> fight() {
		return ActivityData.create(Activity.FIGHT, ACTIVITY_PRIORITY, ImmutableList.<BehaviorControl<? super BunnayEntity>>of(
			SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(BunnayEntity.CHASE_SPEED),
			MeleeAttack.create(BunnayEntity.ATTACK_INTERVAL_TICKS)),
			ImmutableSet.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT)));
	}

	private static ActivityData<BunnayEntity> sit() {
		return ActivityData.create(SIT, ACTIVITY_PRIORITY, ImmutableList.<BehaviorControl<? super BunnayEntity>>of(new BunnayBehaviors.Sit()));
	}

	private static ActivityData<BunnayEntity> dance() {
		return ActivityData.create(DANCE, ACTIVITY_PRIORITY, ImmutableList.<BehaviorControl<? super BunnayEntity>>of(new BunnayBehaviors.Dance()));
	}

	// Running from what is after it, like a rabbit.
	private static ActivityData<BunnayEntity> avoid() {
		return ActivityData.create(Activity.AVOID,
			ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super BunnayEntity>>>of(Pair.of(ACTIVITY_PRIORITY, new BunnayBehaviors.FleeFromThreat())),
			ImmutableSet.of(Pair.of(MemoryModuleType.AVOID_TARGET, MemoryStatus.VALUE_PRESENT)),
			ImmutableSet.of(MemoryModuleType.WALK_TARGET));
	}

	// Panicking (set on fire, say): the panic behaviour in the core does the running, and nothing else may take the walk target.
	private static ActivityData<BunnayEntity> panic() {
		return ActivityData.create(Activity.PANIC,
			ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super BunnayEntity>>>of(Pair.of(ACTIVITY_PRIORITY, new BunnayBehaviors.TeleportToOwner(true))),
			ImmutableSet.of(Pair.of(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_PRESENT)));
	}

	/** Picks the activity, in this order of importance: panicking, fleeing, sitting, dancing, fighting, and otherwise idling. */
	static void updateActivity(BunnayEntity bunnay) {
		Brain<BunnayEntity> brain = bunnay.getBrain();
		Activity wanted;
		if (brain.hasMemoryValue(MemoryModuleType.IS_PANICKING)) {
			wanted = Activity.PANIC;
		} else if (brain.hasMemoryValue(MemoryModuleType.AVOID_TARGET)) {
			wanted = Activity.AVOID;
		} else if (bunnay.wantsToSitNow()) {
			wanted = SIT;
		} else if (bunnay.isDancing() && !bunnay.hasLiveTarget()) {
			wanted = DANCE;
		} else if (brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
			wanted = Activity.FIGHT;
		} else {
			wanted = Activity.IDLE;
		}
		if (!brain.isActive(wanted)) {
			if (wanted == Activity.AVOID) {
				bunnay.startFleeing();
			}
			brain.setActiveActivityIfPossible(wanted);
		}
	}

	private BunnayAi() {
	}
}
