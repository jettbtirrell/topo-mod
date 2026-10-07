package dev.jett.topomod.allayvariants.entity;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;

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
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomLookAround;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;

// The bunnay's Brain, built the way newer mobs are. What a goal system does with priorities is done with activities (which set of behaviours may
// start) plus a "job priority" memory (which of the movement jobs owns the legs, so a higher priority one takes over from a
// lower one the way goal flags did).
public final class BunnayAi {
	// ---- registered types (registries freeze after mod init, so BunnayAi.register() must be called from there) ----

	public static final Activity SIT = registerActivity("sit");
	public static final Activity DANCE = registerActivity("dance");

	/** Priority of the job that is using the legs; a lower number takes over from a higher one. */
	static final MemoryModuleType<Integer> JOB_PRIORITY = registerMemory("job_priority");
	static final MemoryModuleType<Boolean> EAT_COOLDOWN = registerMemory("eat_cooldown");
	static final MemoryModuleType<Boolean> DELIVER_COOLDOWN = registerMemory("deliver_cooldown");
	/** Set when it looked for ripe carrots and found none (what decides when to deliver). */
	static final MemoryModuleType<Boolean> NO_CROPS_FOUND = registerMemory("no_crops_found");
	static final MemoryModuleType<BlockPos> RIPE_CARROT = registerMemory("ripe_carrot");
	/** The weighted middle of the mobs a scared bunnay is running from. */
	static final MemoryModuleType<Vec3> FLEE_THREAT_CENTER = registerMemory("flee_threat_center");
	/** The direction straight away from the mobs a scared bunnay runs from, all of them counted (nearer ones more). */
	static final MemoryModuleType<Vec3> FLEE_AWAY = registerMemory("flee_away");
	static final MemoryModuleType<LivingEntity> OWNER_HURT_BY = registerMemory("owner_hurt_by");
	static final MemoryModuleType<LivingEntity> OWNER_HURT_TARGET = registerMemory("owner_hurt_target");

	static final SensorType<BunnaySensors.Combat> COMBAT_SENSOR = registerSensor("combat", BunnaySensors.Combat::new);
	static final SensorType<BunnaySensors.Threats> THREATS_SENSOR = registerSensor("threats", BunnaySensors.Threats::new);
	static final SensorType<BunnaySensors.GroundCarrots> GROUND_CARROTS_SENSOR = registerSensor("ground_carrots", BunnaySensors.GroundCarrots::new);
	static final SensorType<BunnaySensors.RipeCarrots> RIPE_CARROTS_SENSOR = registerSensor("ripe_carrots", BunnaySensors.RipeCarrots::new);

	private static Activity registerActivity(String name) {
		return Registry.register(BuiltInRegistries.ACTIVITY, AllayVariantsMod.id(name), new Activity(name));
	}

	private static <U> MemoryModuleType<U> registerMemory(String name) {
		return Registry.register(BuiltInRegistries.MEMORY_MODULE_TYPE, AllayVariantsMod.id(name), new MemoryModuleType<>(Optional.empty()));
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
		MemoryModuleType.ATTACK_TARGET, MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY, MemoryModuleType.IS_PANICKING,
		MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, MemoryModuleType.LONG_JUMP_MID_JUMP, MemoryModuleType.GAZE_COOLDOWN_TICKS,
		MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
		MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryModuleType.LIKED_NOTEBLOCK_POSITION,
		JOB_PRIORITY, EAT_COOLDOWN, DELIVER_COOLDOWN, NO_CROPS_FOUND, RIPE_CARROT, FLEE_THREAT_CENTER, FLEE_AWAY, OWNER_HURT_BY, OWNER_HURT_TARGET);

	private static final ImmutableList<SensorType<? extends Sensor<? super BunnayEntity>>> SENSORS = ImmutableList.of(
		SensorType.NEAREST_LIVING_ENTITIES, COMBAT_SENSOR, THREATS_SENSOR, GROUND_CARROTS_SENSOR, RIPE_CARROTS_SENSOR);

	@SuppressWarnings("deprecation")
	private static final Brain.Provider<BunnayEntity> BRAIN_PROVIDER = Brain.provider(MEMORIES, SENSORS, BunnayAi::activities);

	static Brain<BunnayEntity> makeBrain(BunnayEntity body, Brain.Packed packed) {
		return BRAIN_PROVIDER.makeBrain(body, packed);
	}

	// Job behaviours start at this brain priority (after the core ones), in priority order.
	private static final int JOB_BASE = 20;

	private static List<ActivityData<BunnayEntity>> activities(BunnayEntity body) {
		BunnayEntity.HopRun hop = new BunnayEntity.HopRun(body);
		return List.of(core(hop), idle(), fight(), sit(), dance(), avoid());
	}

	// Always on: floating, panic, keeping the target honest, hopping, eating, and the vanilla look/move sinks.
	private static ActivityData<BunnayEntity> core(BunnayEntity.HopRun hop) {
		return ActivityData.create(Activity.CORE, 0, ImmutableList.<BehaviorControl<? super BunnayEntity>>of(
			new Swim<BunnayEntity>(0.8F),
			new AnimalPanic<BunnayEntity>(1.5F, mob -> DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES),
			new BunnayBehaviors.PanicTeleport(),
			new BunnayBehaviors.TargetTracking(),
			new BunnayBehaviors.HopPrepare(hop),
			new BunnayBehaviors.HopMidJump(hop),
			new BunnayBehaviors.EatCarrot(),
			new LookAtTargetSink(45, 90),
			new MoveToTargetSink(),
			new CountDownCooldownTicks(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS),
			new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS)));
	}

	private static ActivityData<BunnayEntity> idle() {
		return ActivityData.create(Activity.IDLE, ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super BunnayEntity>>>of(
			Pair.of(JOB_BASE + 4, new BunnayBehaviors.Tempt()),
			Pair.of(JOB_BASE + 4, new BunnayBehaviors.GiveCarrots()),
			Pair.of(JOB_BASE + 4, new BunnayBehaviors.StayNearFarm()),
			Pair.of(JOB_BASE + 4, new BunnayBehaviors.DeliverToNoteBlock()),
			Pair.of(JOB_BASE + 5, new BunnayBehaviors.HarvestCarrots()),
			Pair.of(JOB_BASE + 5, new BunnayBehaviors.FollowOwner()),
			Pair.of(JOB_BASE + 6, new BunnayBehaviors.PickUpCarrots()),
			// Wandering is the vanilla stroll, only when no job has the legs; the weights give a stroll every few seconds.
			Pair.of(JOB_BASE + 7, new RunOne<BunnayEntity>(
				ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, JOB_PRIORITY, MemoryStatus.VALUE_ABSENT),
				ImmutableList.of(Pair.of(RandomStroll.stroll(0.8F, false), 2), Pair.of(new DoNothing(60, 120), 3)))),
			Pair.of(JOB_BASE + 8, SetEntityLookTargetSometimes.create(EntityTypes.PLAYER, 8.0F, UniformInt.of(30, 60))),
			Pair.of(JOB_BASE + 9, new RandomLookAround(UniformInt.of(150, 250), 30.0F, 0.0F, 0.0F))));
	}

	private static ActivityData<BunnayEntity> fight() {
		return ActivityData.create(Activity.FIGHT,
			ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super BunnayEntity>>>of(Pair.of(JOB_BASE + 3, new BunnayBehaviors.Melee())),
			ImmutableSet.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT)));
	}

	private static ActivityData<BunnayEntity> sit() {
		return ActivityData.create(SIT, ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super BunnayEntity>>>of(
			Pair.of(JOB_BASE + 2, new BunnayBehaviors.Sit())));
	}

	private static ActivityData<BunnayEntity> dance() {
		return ActivityData.create(DANCE, ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super BunnayEntity>>>of(
			Pair.of(JOB_BASE + 3, new BunnayBehaviors.Dance())));
	}

	private static ActivityData<BunnayEntity> avoid() {
		return ActivityData.create(Activity.AVOID,
			ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super BunnayEntity>>>of(Pair.of(JOB_BASE + 1, new BunnayBehaviors.Flee())),
			ImmutableSet.of(Pair.of(FLEE_THREAT_CENTER, MemoryStatus.VALUE_PRESENT)));
	}

	/** Picks the activity, in this order of importance: fleeing, sitting, dancing, fighting, and otherwise idling. */
	static void updateActivity(BunnayEntity bunnay) {
		Brain<BunnayEntity> brain = bunnay.getBrain();
		Activity wanted;
		if (brain.hasMemoryValue(FLEE_THREAT_CENTER)) {
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
			brain.setActiveActivityIfPossible(wanted);
		}
	}

	private BunnayAi() {
	}
}
