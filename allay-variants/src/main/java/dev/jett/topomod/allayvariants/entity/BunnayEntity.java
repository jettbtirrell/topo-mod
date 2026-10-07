package dev.jett.topomod.allayvariants.entity;

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.function.ToDoubleFunction;
import java.util.function.BiConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import dev.jett.topomod.allayvariants.AllayVariantsMod;
import dev.jett.topomod.allayvariants.menu.BunnayMenu;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

// Bunnay: a bunny-allay cross that can be tamed with carrots. It follows its owner and fights like a wolf; holding
// weapons, sitting and an item screen will be built on this the way they were for the topo.
public class BunnayEntity extends TamableAnimal {
	private static final double WILD_HEALTH = 16.0;
	private static final double TAME_HEALTH = 80.0;

	// Hopping, like a frog's long jump. Whenever the hop is off cooldown, the bunnay looks for a place within reach to land
	// that gets it where it is going at least HOP_MIN_DISTANCE closer than walking would, and if there is one it hops there
	// at once: no wind-up and no recovery, like the frog. Where it is going is its target, the end of the path it is walking,
	// or its owner when it is following them and there is no way to walk there. In the air it has no friction (the frog's
	// too), so it keeps the sideways speed it took off with, and the animation (BunnayAnimation.HOP) is one pose held for as
	// long as it is in the air.
	/**
	 * The most ticks a hop spends in the air. How long a hop actually takes depends on how fast the bunnay is moving (see
	 * HOP_AIR_SPEED_FACTOR): the faster it runs, the sooner it is across.
	 */
	private static final int HOP_AIR_TICKS = 15;
	/** The fewest ticks a hop spends in the air, however fast it is going. */
	private static final int HOP_MIN_AIR_TICKS = 6;
	/**
	 * How its sideways speed in the air compares to its running speed (1.0 is the same, so a hop never slows it down: it is
	 * as fast through the air as it was on the ground). Its running speed is its movement speed times the speed a goal asks
	 * for (1.5 while fleeing), and a mob covers HOP_GROUND_SPEED_PER_SPEED blocks a tick for each point of it.
	 */
	private static final double HOP_AIR_SPEED_FACTOR = 1.0;
	private static final double HOP_GROUND_SPEED_PER_SPEED = 2.158;
	/** How far a hop can take it: 6 blocks along the ground (the frog's long jump is 4) and 2 up or down, the same as the frog's. */
	private static final double HOP_MAX_DISTANCE = 6.0;
	private static final int HOP_MAX_RISE = 2;
	private static final int HOP_MAX_DROP = 2;
	/** A hop has to be at least this long (in blocks), and has to bring it at least this much closer to where it is going. */
	private static final double HOP_MIN_DISTANCE = 2.0;
	/** The wait after a hop, in ticks: 100 to 140, which is 5 to 7 seconds, the frog's. */
	private static final int HOP_COOLDOWN_MIN = 100;
	private static final int HOP_COOLDOWN_RANGE = 41;
	/** While the hop is off cooldown it looks for somewhere to hop this often, in ticks. */
	private static final int HOP_SCAN_INTERVAL = 5;
	/** A hop that has not landed this many ticks after it should have is called off (it was stopped by something, say). */
	private static final int HOP_LATE_TICKS = 10;
	/** It works out the arc of this many of the best landing spots before giving up. */
	private static final int HOP_MAX_TRIES = 12;
	/**
	 * What a mob does each tick while its friction is discarded (see Mob.setDiscardFriction, which the frog uses too), which the
	 * arcs are worked out from: its sideways speed does not change, and its vertical speed just loses HOP_GRAVITY a tick (no
	 * drag either), so the path is a plain parabola. It keeps this much of its sideways speed when it lands, so it runs on.
	 */
	private static final double HOP_GRAVITY = 0.08;
	private static final double HOP_LANDING_MOMENTUM = 0.5;
	/** After n ticks, the blocks covered per point of horizontal launch speed. */
	private static final double[] HOP_REACH = new double[HOP_AIR_TICKS + 1];
	/** After n ticks, the blocks risen per point of upward launch speed (that is, without gravity). */
	private static final double[] HOP_RISE = new double[HOP_AIR_TICKS + 1];
	/** After n ticks, the blocks fallen under gravity alone (a negative number). */
	private static final double[] HOP_SAG = new double[HOP_AIR_TICKS + 1];

	static {
		double sag = 0.0;
		double fall = 0.0;
		for (int tick = 1; tick <= HOP_AIR_TICKS; tick++) {
			HOP_REACH[tick] = tick;
			HOP_RISE[tick] = tick;
			sag += fall;
			HOP_SAG[tick] = sag;
			fall -= HOP_GRAVITY;
		}
	}

	// The idle animation is cosmetic and runs on the client only, on the same timer as the rabbit's: a new one every
	// 180 to 219 ticks (9 to 11 seconds) while standing still.
	private static final int IDLE_MIN_TICKS = 180;
	private static final int IDLE_EXTRA_TICKS = 40;

	// Fighting: it always swings its two hands in turn, an empty paw counting as a hand (the bunnay's own hit is 3). What it
	// holds adds damage to the hits of the hand that holds it: a bamboo, a breeze rod or a blaze rod +2, or a stick
	// +1. A breeze rod also blasts the foe the way a wind charge would; a blaze rod also does what a blaze's small
	// fireball does to whatever it hits (holding one does not protect the bunnay from fire in any way). All of them swing
	// at the same pace.
	private static final double ROD_BONUS_DAMAGE = 2.0;
	private static final double BAMBOO_BONUS_DAMAGE = 2.0;
	private static final double STICK_BONUS_DAMAGE = 1.0;
	/**
	 * How long a hit with a blaze rod sets a foe on fire, in seconds: the same 5 a small fireball does (SmallFireball also
	 * does 5 damage, which is left out here, and only places fire when it hits a block, not an entity, so no fire is placed).
	 */
	private static final float BLAZE_ROD_FIRE_SECONDS = 5.0F;
	private static final Identifier WEAPON_DAMAGE_ID = AllayVariantsMod.id("weapon_damage");
	/** The wind burst's shove on a foe: this fast away from the bunnay, and this fast straight up (0.9 is about 4 blocks of height). */
	private static final double WIND_BURST_HORIZONTAL = 0.5;
	private static final double WIND_BURST_VERTICAL = 0.9;
	/**
	 * How fast it swings: it may swing this many ticks before the usual 20 tick wait between swings is up, so 4 means a swing
	 * every 16 ticks instead of 20, 25% faster. The wait is shared by both hands (it swings them in turn), and it is the
	 * same whatever they hold. It is higher than a plain mob's because the bunnay is always swinging two hands.
	 */
	private static final int ATTACK_HEAD_START_TICKS = 4;
	/** How fast the ready stance eases in and out, per tick (it takes 4 ticks to raise and about 7 to lower). */
	private static final float READY_RISE = 0.25F;
	private static final float READY_FALL = 0.15F;

	// Fleeing at low health. A tamed bunnay at FLEE_BELOW_HEALTH or lower stops fighting and runs from the enemies instead: it
	// takes no target at all (so its owner's fights, and whatever hurts it, do not pull it in), keeps away from the mobs that
	// are after it (and only those: other hostile mobs are not its concern), and hops along its escape route (on the usual hop cooldown). It runs
	// at the speed a pet runs from fire. Once nothing is near it and it has had a quiet moment, it eats a carrot as usual,
	// and when it is back above that health it fights again.
	private static final float FLEE_BELOW_HEALTH = 20.0F;
	/** The speed it runs at, the same as a wolf's or cat's panic goal (what a pet does when it is on fire). A rabbit's 2.2 is for an animal that only moves while it hops, so it is far too fast for one that runs. */
	private static final double FLEE_SPEED = 1.5;
	/** It looks this far (in blocks) for mobs that are after it. */
	private static final double FLEE_SEARCH_RADIUS = 34.0;
	/** How far it picks a place to run to: horizontally and vertically, in blocks. */
	private static final int FLEE_AWAY_RANGE = 14;
	private static final int FLEE_AWAY_VERTICAL = 7;
	/** Ticks between looks at what is around it, and between choosing a new place to run to. */
	private static final int FLEE_SCAN_TICKS = 5;
	private static final int FLEE_REPATH_TICKS = 10;

	// Climbing steps. Walking up a run of one-block steps a mob jumps onto each one, and by default it must wait 10 ticks
	// between jumps while the hop itself is over in 9, and in the air it has almost no sideways thrust, so it creeps onto the
	// step. Two things speed that up. The wait is cut to STEP_JUMP_DELAY while it follows a path. And a jump up onto the next
	// step of the path gets a push towards the middle of that step, worked out from how far away it is so it lands there and
	// does not overshoot (STEP_HOP_REACH is the blocks covered per point of sideways speed over the 9 ticks the hop takes,
	// from the same arithmetic as HOP_REACH).
	private static final int STEP_JUMP_DELAY = 3;
	private static final double STEP_HOP_REACH = 4.2;
	private static final double STEP_HOP_MAX_SPEED = 0.3;
	/** The next step of the path counts as a step up when it is at least this much higher, and at most this much. */
	private static final double STEP_MIN_RISE = 0.5;
	private static final double STEP_MAX_RISE = 1.6;
	/** Only a step this close (sideways, in blocks) is given the push; further is not a step but a longer way to go. */
	private static final double STEP_MAX_DISTANCE = 1.6;
	/** LivingEntity's wait between jumps is private, so it is reached by reflection (null if that fails, and nothing changes). */
	private static final java.lang.reflect.Field NO_JUMP_DELAY_FIELD = noJumpDelayField();

	// Farming at a note block, the way an allay attends to one. A note block played within FARM_HEARING_RADIUS makes a tamed
	// bunnay "tune in" to it: for FARM_FORGET_TICKS (30 seconds) after the last note it works the carrots around it with its
	// normal logic (looking for ripe ones, harvesting, replanting), is pulled back if it gets more than FARM_LEASH from the note
	// block, and takes what it has harvested to the note block and tosses it there (so a hopper can catch it) whenever its food
	// slot is full or there is nothing left to harvest. Another note from the same block starts the 30 seconds again; a different
	// note block is ignored until it has forgotten the first. It does not follow its owner while it is tuned in. Wool between the
	// note block and the bunnay muffles the note, as it does for an allay. Sitting makes it forget at once.
	private static final int FARM_HEARING_RADIUS = 16;
	private static final int FARM_FORGET_TICKS = 600;
	/** It is pulled back when it is further than this from the note block (in blocks), until it is within FARM_CLOSE_ENOUGH. */
	private static final double FARM_LEASH = 16.0;
	private static final double FARM_CLOSE_ENOUGH = 4.0;
	/** It tosses the carrots when it is this close to the note block. */
	private static final double FARM_DELIVER_DISTANCE = 3.0;
	/** It keeps this many carrots for itself when it delivers, so it can still heal. */
	private static final int FARM_KEEP_CARROTS = 1;
	/** The wait after a delivery before the next, and how long it tries to reach the note block, in ticks. */
	private static final int FARM_DELIVER_COOLDOWN = 100;
	private static final int FARM_DELIVER_GIVE_UP_TICKS = 400;

	// Picking up carrots lying about. As something to do when it has nothing better, a tamed bunnay with room in its food slot
	// goes to carrots on the ground nearby and picks them up, whoever dropped them (but not ones a bunnay tossed: its own gifts and
	// deliveries). It stays within reach of its owner, or of the note block it is tuned in to.
	private static final int PICK_UP_RADIUS = 8;
	private static final int PICK_UP_HEIGHT = 3;
	private static final int PICK_UP_GIVE_UP_TICKS = 200;

	// Harvesting carrots. Whenever it can carry more (its food slot is empty or holds ordinary carrots with room, not golden
	// carrots), a tamed bunnay out of a fight goes to fully grown carrots it can see, breaks them, and puts what they drop
	// straight into its food slot. It only does this where mobs are allowed to grief, and never strays far from its owner.
	/** How far it looks for carrots, sideways and up or down, in blocks. */
	private static final int HARVEST_SEARCH_RADIUS = 10;
	private static final int HARVEST_SEARCH_HEIGHT = 2;
	/** It only works carrots this close to its owner, so harvesting does not lead it off. */
	private static final double HARVEST_OWNER_RANGE = 16.0;
	/** It breaks the carrots this long after it gets there (it swings at them first), in ticks. */
	private static final int HARVEST_WORK_TICKS = 8;
	/** The whole job (walk over, break, pick up, wait, plant) is abandoned after this long, in ticks (20 seconds). */
	private static final int HARVEST_GIVE_UP_TICKS = 400;
	/** It stops trying to reach the dropped carrots after this long (5 seconds), and goes on to planting. */
	private static final int HARVEST_PICK_UP_TICKS = 100;
	/** After picking up (or giving up on) the drops it waits this long (1 second) before planting. */
	private static final int HARVEST_REPLANT_DELAY_TICKS = 20;

	// Eating the carrots in its off hand to heal.
	/** How long one carrot takes to eat, in ticks (the same as a player's). */
	private static final int EAT_TICKS = 32;
	/** It starts eating when it is missing at least this much health (1 heart), so a golden carrot is not wasted on a scratch... */
	private static final float EAT_MIN_MISSING_HEALTH = 2.0F;
	/** ...but once it has started it keeps going, carrot after carrot, until it is missing less than this (about healed). */
	private static final float EAT_KEEP_GOING_MISSING_HEALTH = 0.5F;
	/** Pause after finishing (or being interrupted) before it starts on the next carrot. */
	private static final int EAT_COOLDOWN_TICKS = 40;
	/** It will not eat until this many ticks have passed since it was last hurt by a mob, so it is out of the fight (5 seconds). */
	private static final int EAT_SAFE_TICKS = 100;
	/** How fast the eating pose eases in and out, per tick. */
	private static final float EAT_RISE = 0.2F;
	private static final float EAT_FALL = 0.15F;

	/** Synced so the client can show the eating pose... */
	private static final EntityDataAccessor<Boolean> DATA_EATING = SynchedEntityData.defineId(BunnayEntity.class, EntityDataSerializers.BOOLEAN);
	/** ...and the food it has in its hand while it eats (empty the rest of the time). */
	private static final EntityDataAccessor<ItemStack> DATA_EATING_FOOD = SynchedEntityData.defineId(BunnayEntity.class, EntityDataSerializers.ITEM_STACK);

	private static final EntityDataAccessor<Boolean> DATA_DANCING = SynchedEntityData.defineId(BunnayEntity.class, EntityDataSerializers.BOOLEAN);

	private static final EntityDataAccessor<Boolean> DATA_HOPPING = SynchedEntityData.defineId(BunnayEntity.class, EntityDataSerializers.BOOLEAN);
	/** How many ticks the current hop spends in the air, which the client plays the hop clip to fit. */
	private static final EntityDataAccessor<Integer> DATA_HOP_AIR_TICKS = SynchedEntityData.defineId(BunnayEntity.class, EntityDataSerializers.INT);

	/** Drives the hop animation on the client. */
	public final AnimationState hopAnimationState = new AnimationState();
	/** Plays the idle animation on the client. */
	public final AnimationState idleAnimationState = new AnimationState();
	private int idleAnimationTimeout;
	/** 0 to 1, client side: how far into the ready stance (weapon raised, closing on a target) it is. */
	private float readyProgress;
	private float readyProgressO;
	/** 0 to 1, client side: how far into the eating pose it is. */
	private float eatProgress;
	private float eatProgressO;
	/** Server side: ticks into the carrot being eaten, and the wait before the next one. */
	private int eatTicks;
	private int eatCooldown;
	/**
	 * The carrots it eats. They are not in a hand (both hands hold weapons); it takes one out to eat, which is shown by
	 * swapping the carrot into its left hand for the meal, like a player switching to food and back.
	 */
	private final SimpleContainer food = new SimpleContainer(1);

	// Giving carrots to a hungry owner, the way an allay hands items to the player it likes: when its owner is hungry and in
	// sight it walks over and tosses some of the carrots from its food slot to them. It keeps GIFT_KEEP_CARROTS for itself,
	// so it can still heal, and waits a long while before it does it again.
	/** The owner is hungry at or below this food level: the point where they can no longer sprint (three drumsticks). */
	private static final int GIFT_HUNGRY_AT = 6;
	private static final int GIFT_MAX_CARROTS = 5;
	private static final int GIFT_KEEP_CARROTS = 1;
	/** The wait after a gift, in ticks (10 minutes). */
	private static final int GIFT_COOLDOWN_TICKS = 12000;
	/** How close its owner has to be, in blocks, for it to notice they are hungry, and how close it gets to give. */
	private static final double GIFT_NOTICE_RANGE = 12.0;
	private static final double GIFT_REACH = 2.5;
	/** It gives up reaching its owner after this many ticks, and tries again after GIFT_RETRY_TICKS. */
	private static final int GIFT_GIVE_UP_TICKS = 200;
	private static final int GIFT_RETRY_TICKS = 200;
	private int giftCooldown;

	/** The hand it attacked with last. With a weapon in each hand it swings them in turn. */
	private InteractionHand attackHand = InteractionHand.MAIN_HAND;

	// Dancing to a jukebox, the way the allay does: it listens for the jukebox game events, remembers which jukebox
	// is playing, and stops dancing when the music stops or the jukebox is gone or too far away.
	private final DynamicGameEventListener<JukeboxListener> dynamicJukeboxListener;
	private @Nullable BlockPos jukeboxPos;
	/** The note block it is tuned in to (see FARM_FORGET_TICKS), and the ticks left before it forgets it. */
	private final DynamicGameEventListener<NoteBlockListener> dynamicNoteBlockListener;
	private @Nullable BlockPos farmPos;
	private int farmTicks;
	/** Set by the harvest goal each time it looks: there was no ripe carrot for it to go to. Used to decide when to deliver. */
	private boolean noCropsFound;
	private int deliverCooldown;
	private int hopCooldown;
	/** Whether its follow-the-owner goal is running, which is when it hops towards its owner even if it cannot walk there. */
	private boolean following;
	/** Whether its flee goal is running (see FLEE_BELOW_HEALTH). */
	private boolean fleeing;

	public BunnayEntity(EntityType<? extends BunnayEntity> type, Level level) {
		super(type, level);
		this.dynamicJukeboxListener = new DynamicGameEventListener<>(new JukeboxListener(
			this, new EntityPositionSource(this, this.getEyeHeight()), GameEvent.JUKEBOX_PLAY.value().notificationRadius()));
		this.dynamicNoteBlockListener = new DynamicGameEventListener<>(new NoteBlockListener(
			this, new EntityPositionSource(this, this.getEyeHeight()), FARM_HEARING_RADIUS));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_HOPPING, false);
		entityData.define(DATA_HOP_AIR_TICKS, HOP_AIR_TICKS);
		entityData.define(DATA_EATING, false);
		entityData.define(DATA_EATING_FOOD, ItemStack.EMPTY);
		entityData.define(DATA_DANCING, false);
	}

	public boolean isEating() {
		return this.entityData.get(DATA_EATING);
	}

	/** The carrot in its hand while it is eating, for the renderer; empty when it is not. */
	public ItemStack getEatingFood() {
		return this.entityData.get(DATA_EATING_FOOD);
	}

	/** The container behind the screen's food slot. */
	public SimpleContainer getFoodContainer() {
		return this.food;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.store("Food", ItemStack.OPTIONAL_CODEC, this.food.getItem(0));
		output.putInt("GiftCooldown", this.giftCooldown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		input.read("Food", ItemStack.OPTIONAL_CODEC).ifPresent(stack -> this.food.setItem(0, stack));
		this.giftCooldown = input.getIntOr("GiftCooldown", 0);
	}

	// The carrots in its food slot are dropped when it dies, like what it is holding.
	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		ItemStack stack = this.food.getItem(0);
		if (!stack.isEmpty()) {
			this.spawnAtLocation(level, stack.copy());
			this.food.setItem(0, ItemStack.EMPTY);
		}
	}

	/** How far into the eating pose it is, for the renderer, smoothed between ticks. */
	public float getEatProgress(float partialTick) {
		return Mth.lerp(partialTick, this.eatProgressO, this.eatProgress);
	}

	/** Anything it puts in either hand is dropped for sure when it dies, however it got there (the screen, a click). */
	@Override
	public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
		super.setItemSlot(slot, stack);
		if (!this.level().isClientSide() && !stack.isEmpty() && (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND)) {
			this.setGuaranteedDrop(slot);
		}
	}

	/**
	 * Out of the fight, hurt enough to want a carrot, and standing around: not fighting, dancing, hopping or in water.
	 * Once it is already eating, how hurt it has to be is lower (see EAT_KEEP_GOING_MISSING_HEALTH), so it finishes the job,
	 * and being hurt no longer matters (only a new target, which means a fight has found it, stops it).
	 */
	private boolean canEat(boolean alreadyEating) {
		LivingEntity target = this.getTarget();
		// Starting needs it to be out of the fight, but once it is eating, damage does not stop it: a bunnay hit by something
		// that keeps hurting it (a wither effect, poison, fire) would otherwise never get to finish a carrot.
		return (target == null || !target.isAlive())
			&& (alreadyEating || this.hurtTime == 0)
			&& (alreadyEating || this.tickCount - this.getLastHurtByMobTimestamp() > EAT_SAFE_TICKS)
			&& this.getMaxHealth() - this.getHealth() >= (alreadyEating ? EAT_KEEP_GOING_MISSING_HEALTH : EAT_MIN_MISSING_HEALTH)
			&& this.onGround()
			&& !this.isInWater()
			&& !this.isHopping()
			&& !this.isDancing()
			&& !this.isPassenger()
			&& !this.fleeing;
	}

	/** Whether another ordinary carrot would fit in the food slot (it is empty, or holds carrots with room; golden ones do not mix). */
	/** How many carrots it would give right now: up to GIFT_MAX_CARROTS of the ordinary ones in its food slot, keeping GIFT_KEEP_CARROTS. */
	private int carrotsToGive() {
		ItemStack held = this.food.getItem(0);
		return held.is(Items.CARROT) ? Math.max(0, Math.min(GIFT_MAX_CARROTS, held.getCount() - GIFT_KEEP_CARROTS)) : 0;
	}

	/** Its owner, if they are hungry, close and in sight, so that it notices. */
	private @Nullable ServerPlayer hungryOwner() {
		if (!(this.getOwner() instanceof ServerPlayer owner) || !owner.isAlive() || owner.isSpectator() || owner.level() != this.level()) {
			return null;
		}
		boolean hungry = owner.getFoodData().getFoodLevel() <= GIFT_HUNGRY_AT;
		return hungry && this.distanceToSqr(owner) <= GIFT_NOTICE_RANGE * GIFT_NOTICE_RANGE && this.hasLineOfSight(owner) ? owner : null;
	}

	private boolean canTakeCarrots() {
		ItemStack held = this.food.getItem(0);
		return held.isEmpty() || (held.is(Items.CARROT) && held.getCount() < held.getMaxStackSize());
	}

	/** Low on health, and tame: it runs from fights instead of taking part in them (see FLEE_BELOW_HEALTH). */
	private boolean isScared() {
		return this.isTame() && this.getHealth() <= FLEE_BELOW_HEALTH;
	}

	// While it is scared it takes no target, whoever asks: its owner's fight, its owner being hit, or a mob that hurt it.
	@Override
	public void setTarget(@Nullable LivingEntity target) {
		if (target != null && this.isScared()) {
			return;
		}
		super.setTarget(target);
	}

	/** Mobs it runs from: whatever is after it (has it as its target), and nothing else. */
	private List<Mob> findThreats() {
		return this.level().getEntitiesOfClass(
			Mob.class,
			this.getBoundingBox().inflate(FLEE_SEARCH_RADIUS),
			mob -> mob != this && mob.isAlive() && mob.getTarget() == this && mob.distanceToSqr(this) <= FLEE_SEARCH_RADIUS * FLEE_SEARCH_RADIUS
		);
	}

	/** A hop that gets it further from a point (its threats), used when there is no route to run along. */
	private @Nullable Hop planAwayHop(Vec3 threatCenter) {
		double now = this.position().distanceTo(threatCenter);
		return this.planHop(landing -> {
			double away = landing.distanceTo(threatCenter);
			return away < now + HOP_MIN_DISTANCE ? Double.MAX_VALUE : -away;
		}, Double.MAX_VALUE / 2.0);
	}

	/** Starts, plays and finishes eating the carrot in its off hand. The server decides; the client just shows the pose. */
	private void tickEating() {
		ItemStack stack = this.food.getItem(0);
		if (this.isEating()) {
			if (!this.canEat(true) || !isCarrot(stack)) {
				this.stopEating();
				return;
			}
			this.eatTicks++;
			// It stands still to eat (a sitting bunnay already is).
			if (!this.isInSittingPose()) {
				this.getNavigation().stop();
			}
			if (this.eatTicks % 4 == 0 && this.eatTicks < EAT_TICKS) {
				this.eatEffects(stack, 3);
			}
			if (this.eatTicks >= EAT_TICKS) {
				this.heal(healAmount(stack));
				ItemStack eaten = stack.copyWithCount(1);
				stack.shrink(1);
				if (stack.isEmpty()) {
					this.food.setItem(0, ItemStack.EMPTY);
				}
				this.food.setChanged();
				this.eatEffects(eaten, 8);
				// Straight on to the next one, with the carrot still in its hand, until it is healed or out of carrots.
				ItemStack next = this.food.getItem(0);
				if (isCarrot(next) && this.canEat(true)) {
					this.eatTicks = 0;
					this.entityData.set(DATA_EATING_FOOD, next.copyWithCount(1));
				} else {
					this.stopEating();
				}
			}
		} else if (this.eatCooldown > 0) {
			this.eatCooldown--;
		} else if (isCarrot(stack) && this.canEat(false)) {
			this.eatTicks = 0;
			this.entityData.set(DATA_EATING_FOOD, stack.copyWithCount(1));
			this.entityData.set(DATA_EATING, true);
		}
	}

	private void stopEating() {
		this.entityData.set(DATA_EATING, false);
		this.entityData.set(DATA_EATING_FOOD, ItemStack.EMPTY);
		this.eatTicks = 0;
		this.eatCooldown = EAT_COOLDOWN_TICKS;
	}

	/** The crunch and a few crumbs of the food, in front of its mouth. */
	private void eatEffects(ItemStack food, int crumbs) {
		this.playSound(SoundEvents.GENERIC_EAT.value(), 0.5F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
		if (this.level() instanceof ServerLevel server) {
			Vec3 look = this.getViewVector(1.0F);
			server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, food.getItem()),
				this.getX() + look.x * 0.3, this.getY() + this.getEyeHeight() * 0.7, this.getZ() + look.z * 0.3, crumbs, 0.05, 0.05, 0.05, 0.05);
		}
	}

	/** Whether it has a weapon in either hand; this is what makes it raise the weapon when it closes on a target. */
	public boolean isHoldingWeapon() {
		return isHoldable(this.getMainHandItem()) || isHoldable(this.getOffhandItem());
	}

	/**
	 * Which hand to attack with next: it always swings its two hands in turn, whether or not they hold anything (an empty
	 * paw swings too, for the bunnay's own damage).
	 */
	private InteractionHand chooseAttackHand() {
		return this.attackHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
	}

	// The melee goal swings the main hand; this swaps in the hand it should be, so the swing shows on that arm and
	// doHurtTarget (which follows straight after) uses that hand's weapon.
	@Override
	public void swingForAttack(InteractionHand hand) {
		this.attackHand = this.chooseAttackHand();
		super.swingForAttack(this.attackHand);
	}

	/** Extra damage per hit from what it is holding (neither item has an attack stat of its own). */
	private static double weaponBonusDamage(ItemStack stack) {
		if (stack.is(Items.BAMBOO)) {
			return BAMBOO_BONUS_DAMAGE;
		}
		if (stack.is(Items.BREEZE_ROD) || stack.is(Items.BLAZE_ROD)) {
			return ROD_BONUS_DAMAGE;
		}
		if (stack.is(Items.STICK)) {
			return STICK_BONUS_DAMAGE;
		}
		return 0.0;
	}



	/**
	 * Treats a foe as if a blaze's small fireball had hit it, without the fireball's damage: it is set on fire for 5
	 * seconds (a fire-immune mob ignores that, as with a real fireball), with a puff of flames and smoke and the fire
	 * charge's whoosh. A small fireball has no blast, so there is none.
	 */
	private void fireballHit(ServerLevel level, Entity target) {
		target.igniteForSeconds(BLAZE_ROD_FIRE_SECONDS);
		level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 14, 0.25, 0.3, 0.25, 0.03);
		level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 6, 0.2, 0.3, 0.2, 0.02);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 1.0F, 1.0F);
	}

	/**
	 * Blasts a foe as if a wind charge had burst on it: a gust at its feet and the burst's sound, and a shove away from
	 * the bunnay and up into the air, which it then falls from. This is the burst's launch without its explosion, so it
	 * does not hit anything else or trigger blocks.
	 */
	private void windBurst(ServerLevel level, LivingEntity victim) {
		double dx = victim.getX() - this.getX();
		double dz = victim.getZ() - this.getZ();
		double length = Math.max(Math.sqrt(dx * dx + dz * dz), 1.0E-4);
		victim.setDeltaMovement(dx / length * WIND_BURST_HORIZONTAL, WIND_BURST_VERTICAL, dz / length * WIND_BURST_HORIZONTAL);
		victim.needsSync = true;
		level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, victim.getX(), victim.getY() + 0.1, victim.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
	}

	/** How far into the ready stance it is, for the renderer, smoothed between ticks. */
	public float getReadyProgress(float partialTick) {
		return Mth.lerp(partialTick, this.readyProgressO, this.readyProgress);
	}

	// Mobs spawn left-handed now and then; the model and the held item layer only handle the right hand.
	@Override
	public HumanoidArm getMainArm() {
		return HumanoidArm.RIGHT;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		// What it holds has no attack stat of its own, so add its bonus just for this swing.
		ItemStack weapon = this.getItemInHand(this.attackHand);
		double bonus = weaponBonusDamage(weapon);
		AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
		if (bonus > 0.0) {
			attack.addTransientModifier(new AttributeModifier(WEAPON_DAMAGE_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
		}
		boolean hit;
		try {
			hit = super.doHurtTarget(level, target);
		} finally {
			if (bonus > 0.0) {
				attack.removeModifier(WEAPON_DAMAGE_ID);
			}
		}
		if (hit && target instanceof LivingEntity victim && weapon.is(Items.BREEZE_ROD)) {
			this.windBurst(level, victim);
		}
		if (hit && weapon.is(Items.BLAZE_ROD)) {
			this.fireballHit(level, target);
		}
		return hit;
	}

	public boolean isDancing() {
		return this.entityData.get(DATA_DANCING);
	}

	private void setDancing(boolean dancing) {
		if (!this.level().isClientSide()) {
			this.entityData.set(DATA_DANCING, dancing);
		}
	}

	@Override
	public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> listenerConsumer) {
		super.updateDynamicGameEventListener(listenerConsumer);
		if (this.level() instanceof ServerLevel serverLevel) {
			listenerConsumer.accept(this.dynamicJukeboxListener, serverLevel);
			listenerConsumer.accept(this.dynamicNoteBlockListener, serverLevel);
		}
	}

	/** Called by the jukebox listener: starts dancing to a jukebox that began playing, or stops for the one it was dancing to. */
	private void setJukeboxPlaying(BlockPos pos, boolean playing) {
		if (playing) {
			if (!this.isDancing()) {
				this.jukeboxPos = pos;
				this.setDancing(true);
			}
		} else if (pos.equals(this.jukeboxPos) || this.jukeboxPos == null) {
			this.jukeboxPos = null;
			this.setDancing(false);
		}
	}

	private boolean shouldStopDancing() {
		return this.jukeboxPos == null
			|| !this.jukeboxPos.closerToCenterThan(this.position(), (double) GameEvent.JUKEBOX_PLAY.value().notificationRadius())
			|| !this.level().getBlockState(this.jukeboxPos).is(Blocks.JUKEBOX);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!this.level().isClientSide()) {
			if (this.getNavigation().isInProgress()) {
				this.shortenJumpDelay(STEP_JUMP_DELAY);
			}
			this.debugHopRun.tick();
			if (this.isScared() && this.getTarget() != null) {
				this.setTarget(null);
			}
			this.tickEating();
			if (this.giftCooldown > 0) {
				this.giftCooldown--;
			}
			if (this.deliverCooldown > 0) {
				this.deliverCooldown--;
			}
			this.tickFarm();
		}
		if (!this.level().isClientSide() && this.isDancing() && this.shouldStopDancing() && this.tickCount % 20 == 0) {
			this.jukeboxPos = null;
			this.setDancing(false);
		}

	}

	/** Whether it is tuned in to a note block, so it works the farm and does not follow its owner. */
	public boolean isFarming() {
		return this.farmPos != null;
	}

	/** Counts down the time to forget the note block, and forgets it if it has gone or the bunnay has been told to sit. */
	private void tickFarm() {
		if (this.farmPos == null) {
			return;
		}
		if (--this.farmTicks <= 0 || this.isOrderedToSit()
			|| (this.tickCount % 20 == 0 && !this.level().getBlockState(this.farmPos).is(Blocks.NOTE_BLOCK))) {
			this.forgetFarm();
		}
	}

	private void forgetFarm() {
		this.farmPos = null;
		this.farmTicks = 0;
		this.noCropsFound = false;
	}

	// Being told to sit makes it forget the note block at once, so it is not kept waiting for the 30 seconds to be up.
	@Override
	public void setOrderedToSit(boolean orderedToSit) {
		if (orderedToSit) {
			this.forgetFarm();
		}
		super.setOrderedToSit(orderedToSit);
	}

	/**
	 * A note block played at this spot, within hearing. It is ignored if the bunnay is not a tame adult, is sitting, or there is
	 * wool between them. If it is not tuned in to anything it tunes in to this one; if this is the note block it is tuned in to,
	 * the 30 seconds start again; any other is ignored.
	 */
	private void hearNoteBlock(ServerLevel level, BlockPos pos, Vec3 at) {
		if (!this.isTame() || this.isBaby() || this.isOrderedToSit()) {
			return;
		}
		boolean muffled = level.isBlockInLine(new ClipBlockStateContext(at, this.getEyePosition(), state -> state.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS))).getType() != HitResult.Type.MISS;
		if (muffled) {
			return;
		}
		if (this.farmPos == null) {
			this.farmPos = pos;
			this.farmTicks = FARM_FORGET_TICKS;
			// It has been given something to do: a chirp and a few notes over its head.
			this.playSound(SoundEvents.RABBIT_AMBIENT, 1.0F, 1.4F);
			level.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + this.getBbHeight() + 0.3, this.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
		} else if (this.farmPos.equals(pos)) {
			this.farmTicks = FARM_FORGET_TICKS;
		}
	}

	/** How many carrots it would deliver now: all the ordinary ones in its food slot but the FARM_KEEP_CARROTS it keeps. */
	private int carrotsToDeliver() {
		ItemStack held = this.food.getItem(0);
		return held.is(Items.CARROT) ? Math.max(0, held.getCount() - FARM_KEEP_CARROTS) : 0;
	}

	private static java.lang.reflect.Field noJumpDelayField() {
		try {
			java.lang.reflect.Field field = LivingEntity.class.getDeclaredField("noJumpDelay");
			field.setAccessible(true);
			return field;
		} catch (ReflectiveOperationException e) {
			return null;
		}
	}

	/** Cuts the wait before the next jump down to at most this many ticks. */
	private void shortenJumpDelay(int ticks) {
		if (NO_JUMP_DELAY_FIELD == null) {
			return;
		}
		try {
			if (NO_JUMP_DELAY_FIELD.getInt(this) > ticks) {
				NO_JUMP_DELAY_FIELD.setInt(this, ticks);
			}
		} catch (IllegalAccessException ignored) {
			// Cannot happen once setAccessible has worked.
		}
	}

	/**
	 * A jump up onto the next step of its path gets a push towards the middle of that step. The speed is the distance over
	 * how far a unit of speed carries it in the hop, so it comes down on the step's middle and no further, whatever the
	 * distance (and at most STEP_HOP_MAX_SPEED).
	 */
	@Override
	public void jumpFromGround() {
		super.jumpFromGround();
		if (this.level().isClientSide() || !this.getNavigation().isInProgress()) {
			return;
		}
		MoveControl move = this.getMoveControl();
		double rise = move.getWantedY() - this.getY();
		double dx = move.getWantedX() - this.getX();
		double dz = move.getWantedZ() - this.getZ();
		double distance = Math.hypot(dx, dz);
		if (rise < STEP_MIN_RISE || rise > STEP_MAX_RISE || distance < 1.0E-3 || distance > STEP_MAX_DISTANCE) {
			return;
		}
		double speed = Math.min(distance / STEP_HOP_REACH, STEP_HOP_MAX_SPEED);
		Vec3 motion = this.getDeltaMovement();
		this.setDeltaMovement(dx / distance * speed, motion.y, dz / distance * speed);
		this.needsSync = true;
	}

	// DEBUG: a hop run that only TopoDebugCommand uses. Remove together with that command.
	private final HopRun debugHopRun = new HopRun(this);

	/** DEBUG: hops away from the given point. False if it cannot hop right now. */
	public boolean debugHop(Vec3 awayFrom) {
		if (!this.canHopNow() || this.debugHopRun.running) {
			return false;
		}
		Hop plan = this.planAwayHop(awayFrom);
		if (plan == null) {
			return false;
		}
		this.debugHopRun.start(plan);
		return true;
	}

	/** How many ticks the current hop spends in the air (see DATA_HOP_AIR_TICKS). */
	public int getHopAirTicks() {
		return this.entityData.get(DATA_HOP_AIR_TICKS);
	}

	public boolean isHopping() {
		return this.entityData.get(DATA_HOPPING);
	}

	// ---- planning a hop ----

	/** A hop worked out: the launch velocity, where it lands, and the ticks it spends in the air. */
	private record Hop(Vec3 velocity, Vec3 landing, int airTicks) {
	}

	private record Spot(double cost, Vec3 landing) {
	}

	/** Free to hop: on the ground and not busy with anything that has it standing, swimming, riding or dancing. */
	private boolean canHopNow() {
		return this.onGround() && !this.isInWater() && !this.isPassenger() && !this.isBaby() && !this.isHopping() && !this.isEating()
			&& !this.isDancing() && !this.isInSittingPose();
	}

	/**
	 * A hop towards where it is going, if there is one worth making. Where it is going is, in order: its target, when it is
	 * fighting and cannot just walk there; the path it is walking, when that leads to where it is going; its owner, when it is
	 * following them and cannot walk there; or the end of the path it is walking.
	 */
	private @Nullable Hop planTravelHop() {
		LivingEntity target = this.getTarget();
		Path path = this.getNavigation().getPath();
		boolean walking = path != null && !path.isDone() && path.getNodeCount() > 0;
		if (walking && path.canReach()) {
			return this.planAlong(path);
		}
		LivingEntity owner = this.getOwner();
		Vec3 goal = target != null && target.isAlive() ? target.position() : this.following && owner != null ? owner.position() : null;
		if (goal != null) {
			return this.planToward(goal);
		}
		return walking ? this.planAlong(path) : null;
	}

	/** A hop that lands nearer a point, when there is no path to follow to it. It does not land on top of it. */
	private @Nullable Hop planToward(Vec3 goal) {
		double now = this.position().distanceTo(goal);
		return this.planHop(landing -> {
			double distance = landing.distanceTo(goal);
			return distance < 1.2 ? Double.MAX_VALUE : distance;
		}, now - HOP_MIN_DISTANCE);
	}

	/**
	 * A hop that skips ahead along the path it is walking: landing next to one of the later nodes of the path, so that it
	 * cuts off at least HOP_MIN_DISTANCE of walking. It never leaves the route, so it cannot hop into a dead end, and where the
	 * route goes the long way round a gap or up a ledge, a hop over the gap or up the ledge is a big saving.
	 */
	private @Nullable Hop planAlong(Path path) {
		int count = path.getNodeCount();
		int next = path.getNextNodeIndex();
		if (next >= count) {
			return null;
		}
		Vec3[] centers = new Vec3[count];
		double[] remaining = new double[count];
		for (int i = 0; i < count; i++) {
			centers[i] = Vec3.atBottomCenterOf(path.getNodePos(i));
		}
		for (int i = count - 2; i >= 0; i--) {
			remaining[i] = remaining[i + 1] + centers[i].distanceTo(centers[i + 1]);
		}
		double now = this.position().distanceTo(centers[next]) + remaining[next];
		return this.planHop(landing -> {
			double best = Double.MAX_VALUE;
			for (int i = next; i < count; i++) {
				Vec3 node = centers[i];
				if (Math.abs(landing.x - node.x) <= 1.0 && Math.abs(landing.z - node.z) <= 1.0 && Math.abs(landing.y - node.y) <= 1.5) {
					best = Math.min(best, landing.distanceTo(node) + remaining[i]);
				}
			}
			return best;
		}, now - HOP_MIN_DISTANCE);
	}

	/**
	 * The best hop there is: every place it could stand within reach (see HOP_MAX_DISTANCE) that costs no more than maxCost,
	 * cheapest first, until one has an arc that is clear.
	 */
	private @Nullable Hop planHop(ToDoubleFunction<Vec3> cost, double maxCost) {
		Vec3 here = this.position();
		BlockPos origin = this.blockPosition();
		int reach = (int) Math.ceil(HOP_MAX_DISTANCE);
		List<Spot> spots = new ArrayList<>();
		for (int dx = -reach; dx <= reach; dx++) {
			for (int dz = -reach; dz <= reach; dz++) {
				for (int dy = -HOP_MAX_DROP; dy <= HOP_MAX_RISE; dy++) {
					Vec3 landing = this.standingSpot(origin.offset(dx, dy, dz));
					if (landing == null || Math.hypot(landing.x - here.x, landing.z - here.z) > HOP_MAX_DISTANCE
						|| landing.distanceTo(here) < HOP_MIN_DISTANCE) {
						continue;
					}
					double spotCost = cost.applyAsDouble(landing);
					if (spotCost <= maxCost) {
						spots.add(new Spot(spotCost, landing));
					}
				}
			}
		}
		spots.sort(Comparator.comparingDouble(Spot::cost));
		for (int i = 0; i < Math.min(spots.size(), HOP_MAX_TRIES); i++) {
			Hop hop = this.solveHop(spots.get(i).landing());
			if (hop != null) {
				return hop;
			}
		}
		return null;
	}

	/** Where it would stand with its feet in this block: solid floor that is safe to land on, and room for it. Null if it cannot. */
	private @Nullable Vec3 standingSpot(BlockPos feet) {
		Level level = this.level();
		BlockPos floor = feet.below();
		BlockState floorState = level.getBlockState(floor);
		BlockState feetState = level.getBlockState(feet);
		if (floorState.getCollisionShape(level, floor).isEmpty() || !floorState.getFluidState().isEmpty()
			|| floorState.is(Blocks.MAGMA_BLOCK) || floorState.is(Blocks.CACTUS) || floorState.is(Blocks.POWDER_SNOW)
			|| !feetState.getCollisionShape(level, feet).isEmpty() || !feetState.getFluidState().isEmpty()
			|| feetState.is(BlockTags.FIRE) || feetState.is(Blocks.SWEET_BERRY_BUSH)
			|| !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
			return null;
		}
		return Vec3.atBottomCenterOf(feet);
	}

	/** The quickest hop that lands it on a spot (see groundSpeed and HOP_AIR_SPEED_FACTOR), or null if there is none with a clear arc. */
	private @Nullable Hop solveHop(Vec3 landing) {
		Vec3 here = this.position();
		double distance = Math.hypot(landing.x - here.x, landing.z - here.z);
		// As quick as its running speed says, and no quicker than the clip can bear; if the arc does not work that quick (a
		// landing well below it needs time in the air), the next longest that does.
		double airSpeed = Math.max(this.groundSpeed() * HOP_AIR_SPEED_FACTOR, 1.0E-3);
		int quickest = Mth.clamp((int) Math.ceil(distance / airSpeed), HOP_MIN_AIR_TICKS, HOP_AIR_TICKS);
		for (int air = quickest; air <= HOP_AIR_TICKS; air++) {
			Hop hop = this.solveHop(landing, air);
			if (hop != null) {
				return hop;
			}
		}
		return null;
	}

	/** How fast it is moving along the ground, in blocks a tick: the speed it is set to move at (or, standing, its usual speed). */
	private double groundSpeed() {
		double speed = this.getSpeed();
		if (speed < 0.02) {
			speed = this.getAttributeValue(Attributes.MOVEMENT_SPEED);
		}
		return speed * HOP_GROUND_SPEED_PER_SPEED;
	}

	/**
	 * Works out the launch that lands it on a spot after exactly this many ticks in the air, and checks the whole arc is clear
	 * for its body. Both come from the table of what a tick of movement does: the sideways speed follows from the distance, and
	 * the upward speed from the height of the spot against where gravity would have it by then.
	 */
	private @Nullable Hop solveHop(Vec3 landing, int air) {
		Vec3 here = this.position();
		double dx = landing.x - here.x;
		double dz = landing.z - here.z;
		double distance = Math.hypot(dx, dz);
		double speed = distance / HOP_REACH[air];
		double up = (landing.y - here.y - HOP_SAG[air]) / HOP_RISE[air];
		if (up < 0.1 || up > 1.0) {
			return null;
		}
		double dirX = distance < 1.0E-4 ? 0.0 : dx / distance;
		double dirZ = distance < 1.0E-4 ? 0.0 : dz / distance;
		AABB body = this.getBoundingBox();
		// Every half tick along the arc, with a little room so that brushing the floor at the end does not count.
		for (int half = 1; half <= air * 2; half++) {
			int low = half / 2;
			int high = Math.min(low + 1, air);
			double frac = (half % 2) * 0.5;
			double across = Mth.lerp(frac, HOP_REACH[low], HOP_REACH[high]) * speed;
			double height = Mth.lerp(frac, HOP_RISE[low] * up + HOP_SAG[low], HOP_RISE[high] * up + HOP_SAG[high]);
			if (!this.level().noCollision(this, body.move(dirX * across, height, dirZ * across).deflate(0.02))) {
				return null;
			}
		}
		return new Hop(new Vec3(dirX * speed, up, dirZ * speed), landing, air);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.hopAnimationState.animateWhen(this.isHopping(), this.tickCount);
			this.tickIdleAnimation();
			this.readyProgressO = this.readyProgress;
			boolean ready = this.isAggressive() && this.isHoldingWeapon();
			this.readyProgress = Mth.clamp(this.readyProgress + (ready ? READY_RISE : -READY_FALL), 0.0F, 1.0F);
			this.eatProgressO = this.eatProgress;
			this.eatProgress = Mth.clamp(this.eatProgress + (this.isEating() ? EAT_RISE : -EAT_FALL), 0.0F, 1.0F);
		} else {
			if (this.hopCooldown > 0) {
				this.hopCooldown--;
			}
		}
	}

	/** Starts the idle clip when its timer runs out, if standing still; stops it if the bunnay moves off. */
	private void tickIdleAnimation() {
		boolean still = this.onGround() && !this.isInWater() && !this.isHopping() && !this.isDancing() && this.hurtTime == 0
			&& this.walkAnimation.speed() < 0.02F;
		if (!still) {
			this.idleAnimationState.stop();
		} else if (this.idleAnimationTimeout <= 0) {
			this.idleAnimationTimeout = IDLE_MIN_TICKS + this.random.nextInt(IDLE_EXTRA_TICKS);
			this.idleAnimationState.start(this.tickCount);
		}
		if (this.idleAnimationTimeout > 0) {
			this.idleAnimationTimeout--;
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
			.add(Attributes.MAX_HEALTH, WILD_HEALTH)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			// Swimming speed. A land mob moves at a crawl in water (about 1.6 blocks a second for this one); the water movement
			// efficiency attribute (what Depth Strider sets, 0 to 1) closes that gap. 0.4 is about 2.8 times as fast, 4.5 blocks a
			// second, roughly a walking pace; 0.2 is 2.1 times and 0.6 is 3.4 times.
			.add(Attributes.WATER_MOVEMENT_EFFICIENCY, 0.4);
	}

	@Override
	protected void registerGoals() {
		// Fleeing is priority 1 so that it beats the chase, following and everything else (and it hops itself, since the hop goal
		// could not run beside it).
		this.goalSelector.addGoal(1, new FleeGoal(this));
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(1, new TamableAnimalPanicGoal(1.5, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
		// Sitting is priority 2, as it is for a wolf: below floating and panicking, above everything else. What used to be
		// at 2 and below is one number lower in the list for it (the melee goal is 3, and so on).
		this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		// The hop is priority 2 so that it can interrupt the chase, the walk to its owner and so on (all lower), and it cannot be
		// interrupted itself.
		this.goalSelector.addGoal(2, new HopGoal(this));
		this.goalSelector.addGoal(3, new DanceGoal(this));
		this.goalSelector.addGoal(3, new BunnayMeleeGoal(this));
		this.goalSelector.addGoal(4, new TemptGoal(this, 1.0, this::isFood, false));
		this.goalSelector.addGoal(4, new GiveCarrotsGoal(this));
		this.goalSelector.addGoal(4, new StayNearFarmGoal(this));
		this.goalSelector.addGoal(4, new DeliverToNoteBlockGoal(this));
		this.goalSelector.addGoal(5, new HarvestCarrotsGoal(this));
		// Follows its owner like a wolf: starts from 10 blocks away and stops 2 blocks from them.
		this.goalSelector.addGoal(5, new BunnayFollowOwnerGoal(this, 1.0, 10.0F, 2.0F));
		// Picking up loose carrots is a low priority: below harvesting and following, above wandering about.
		this.goalSelector.addGoal(6, new PickUpCarrotsGoal(this));
		this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

		// Fights like a wolf: defends its owner, backs up the owner's attacks, and retaliates when hurt.
		this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
		this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
	}

	// What it will go after on its owner's behalf is the wolf's own rule (Wolf.wantsToAttack): never a creeper, a ghast or an
	// armor stand; another bunnay only if it is wild or belongs to someone else; a player only if player fighting is allowed
	// between the two; and never a tamed horse or any other tamed pet.
	@Override
	public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
		if (target instanceof Creeper || target instanceof Ghast || target instanceof ArmorStand) {
			return false;
		}
		if (target instanceof BunnayEntity other) {
			return !other.isTame() || other.getOwner() != owner;
		}
		if (target instanceof Player player && owner instanceof Player ownerPlayer && !ownerPlayer.canHarmPlayer(player)) {
			return false;
		}
		if (target instanceof AbstractHorse horse && horse.isTamed()) {
			return false;
		}
		return !(target instanceof TamableAnimal tamable && tamable.isTame());
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return isCarrot(stack);
	}

	/** A carrot or golden carrot: what it is tamed, healed and fed with, and what its food slot holds. */
	public static boolean isCarrot(ItemStack stack) {
		return stack.is(Items.CARROT) || stack.is(Items.GOLDEN_CARROT);
	}

	/** Health a carrot or golden carrot restores: a carrot 6 (3 hearts), a golden carrot 12 (6 hearts): twice the food's nutrition, the same as a wolf. */
	private static float healAmount(ItemStack food) {
		return food.is(Items.GOLDEN_CARROT) ? 12.0F : 6.0F;
	}

	// It can't breed with its own kind (see getBreedOffspring), so feeding it never puts it in love mode.
	@Override
	public boolean canFallInLove() {
		return false;
	}

	/** Anything the bunnay can hold in a hand as a weapon: a bamboo, a breeze rod, a blaze rod or a stick. */
	public static boolean isHoldable(ItemStack stack) {
		return stack.is(Items.BAMBOO) || stack.is(Items.BREEZE_ROD) || stack.is(Items.BLAZE_ROD) || stack.is(Items.STICK);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (this.isTame()) {
			if (!this.isOwnedBy(player)) {
				return super.mobInteract(player, hand);
			}

			// Sneak + right-click opens the equipment screen.
			if (player.isSecondaryUseActive()) {
				if (!this.level().isClientSide()) {
					player.openMenu(new EquipmentMenuProvider());
				}
				return InteractionResult.SUCCESS;
			}

			// Handing it a weapon equips it straight away, in the main hand or, if that is taken, the off hand.
			if (isHoldable(stack) && (this.getMainHandItem().isEmpty() || this.getOffhandItem().isEmpty())) {
				if (!this.level().isClientSide()) {
					EquipmentSlot slot = this.getMainHandItem().isEmpty() ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
					this.setItemSlot(slot, stack.split(1));
					this.setGuaranteedDrop(slot);
					this.setPersistenceRequired();
				}
				return InteractionResult.SUCCESS;
			}

			// Carrots heal a hurt bunnay, and a golden carrot heals a great deal more. At full health the carrot goes into
			// its food slot instead, one at a time, for it to eat later when it is hurt.
			if (this.isFood(stack)) {
				if (this.getHealth() < this.getMaxHealth()) {
					if (!this.level().isClientSide()) {
						this.heal(healAmount(stack));
						stack.consume(1, player);
					}
					return InteractionResult.SUCCESS;
				}
				ItemStack held = this.food.getItem(0);
				if (held.isEmpty() || (ItemStack.isSameItemSameComponents(held, stack) && held.getCount() < held.getMaxStackSize())) {
					if (!this.level().isClientSide()) {
						ItemStack one = stack.copyWithCount(1);
						stack.consume(1, player);
						if (held.isEmpty()) {
							this.food.setItem(0, one);
						} else {
							held.grow(1);
							this.food.setChanged();
						}
						this.setPersistenceRequired();
					}
					return InteractionResult.SUCCESS;
				}
				return InteractionResult.PASS;
			}

			// Empty-handed (or any other item) click toggles sitting.
			if (!this.level().isClientSide()) {
				this.setOrderedToSit(!this.isOrderedToSit());
				this.setJumping(false);
				this.getNavigation().stop();
				this.setTarget(null);
			}
			return InteractionResult.SUCCESS;
		}

		if (this.isFood(stack)) {
			// A carrot has a one in three chance of taming it (the same odds as the topo), and a golden carrot always does.
			if (!this.level().isClientSide()) {
				boolean golden = stack.is(Items.GOLDEN_CARROT);
				stack.consume(1, player);
				if (golden || this.random.nextInt(3) == 0) {
					this.tame(player);
					this.level().broadcastEntityEvent(this, (byte) 7);
				} else {
					this.level().broadcastEntityEvent(this, (byte) 6);
				}
				this.setPersistenceRequired();
			}
			return InteractionResult.SUCCESS;
		}

		return super.mobInteract(player, hand);
	}

	// Like a wolf, any hit makes a sitting bunnay stand up, so it can defend itself. Unlike a wolf, its owner cannot hurt it
	// (the hit still stands it up, but does no damage).
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (this.isInvulnerableTo(level, source)) {
			return false;
		}
		if (this.isOrderedToSit()) {
			this.setOrderedToSit(false);
		}
		if (source.getEntity() instanceof LivingEntity attacker && this.isOwnedBy(attacker)) {
			return false;
		}
		return super.hurtServer(level, source, damage);
	}

	/** Opens the equipment screen; the client is told which bunnay it belongs to via the entity id. */
	private final class EquipmentMenuProvider implements ExtendedMenuProvider<Integer> {
		@Override
		public Component getDisplayName() {
			return BunnayEntity.this.getDisplayName();
		}

		@Override
		public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
			return new BunnayMenu(containerId, inventory, BunnayEntity.this);
		}

		@Override
		public Integer getScreenOpeningData(ServerPlayer player) {
			return BunnayEntity.this.getId();
		}
	}

	@Override
	protected void applyTamingSideEffects() {
		if (this.isTame()) {
			this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(TAME_HEALTH);
			this.setHealth((float) TAME_HEALTH);
		} else {
			this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(WILD_HEALTH);
		}
	}

	/** Bunnays never take fall damage, however far they drop. */
	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	// Bunnays come from an allay dancing to the bunny music disc (see BunnayBreeding), not from breeding with each other.
	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.RABBIT_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.RABBIT_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.RABBIT_DEATH;
	}

	/**
	 * Runs from the enemies when it is low on health (see FLEE_BELOW_HEALTH). It looks around every few ticks for what to run
	 * from, picks a place well away from them that it can walk to, and runs
	 * there at FLEE_SPEED, picking again as it goes. Whenever its hop is off cooldown it hops along that route instead (the
	 * cooldown is short while it flees); with no route, a hop that gets it further away. It ends when nothing is near it.
	 */
	private static final class FleeGoal extends Goal {
		private final BunnayEntity bunnay;
		private final HopRun hop;
		private List<Mob> threats = List.of();
		private Vec3 threatCenter = Vec3.ZERO;
		private int scanDelay;
		private int repathDelay;

		FleeGoal(BunnayEntity bunnay) {
			this.bunnay = bunnay;
			this.hop = new HopRun(bunnay);
			// No JUMP flag, so that it keeps floating if it ends up in water.
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		private boolean ready() {
			return this.bunnay.isScared() && !this.bunnay.isOrderedToSit() && !this.bunnay.isPassenger() && !this.bunnay.isBaby();
		}

		/** What it is running from, and the middle of it: nearer mobs count for more. */
		private void look() {
			this.threats = this.bunnay.findThreats();
			Vec3 sum = Vec3.ZERO;
			double total = 0.0;
			for (Mob mob : this.threats) {
				double weight = 1.0 / (this.bunnay.distanceTo(mob) + 2.0);
				sum = sum.add(mob.position().scale(weight));
				total += weight;
			}
			this.threatCenter = total > 0.0 ? sum.scale(1.0 / total) : Vec3.ZERO;
		}

		@Override
		public boolean canUse() {
			if (!this.ready() || --this.scanDelay > 0) {
				return false;
			}
			this.scanDelay = FLEE_SCAN_TICKS;
			this.look();
			return !this.threats.isEmpty();
		}

		@Override
		public boolean canContinueToUse() {
			return this.hop.running || (this.ready() && !this.threats.isEmpty());
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			this.bunnay.fleeing = true;
			this.bunnay.stopEating();
			this.bunnay.setTarget(null);
			this.bunnay.getNavigation().stop();
			this.repathDelay = 0;
		}

		@Override
		public void tick() {
			if (this.hop.running) {
				this.hop.tick();
				// In the air there is nothing else to do; the tick it lands it carries straight on running.
				if (this.hop.running) {
					return;
				}
			}
			if (this.bunnay.tickCount % FLEE_SCAN_TICKS == 0) {
				this.look();
			}
			if (this.threats.isEmpty()) {
				return;
			}
			// Hop along the way it is running when it can; with no way to run, hop away if there is somewhere to.
			if (!this.hop.running && this.bunnay.hopCooldown <= 0 && this.bunnay.canHopNow() && this.bunnay.tickCount % HOP_SCAN_INTERVAL == 0) {
				Hop plan = this.bunnay.planTravelHop();
				if (plan == null && !this.bunnay.getNavigation().isInProgress()) {
					plan = this.bunnay.planAwayHop(this.threatCenter);
				}
				if (plan != null) {
					this.hop.start(plan);
					return;
				}
			}
			if (--this.repathDelay <= 0 || this.bunnay.getNavigation().isDone()) {
				this.repathDelay = FLEE_REPATH_TICKS;
				this.runAway();
			}
		}

		/** Picks a place away from the threats that it can walk to, and sets off for it. */
		private void runAway() {
			double now = this.bunnay.position().distanceTo(this.threatCenter);
			for (int attempt = 0; attempt < 6; attempt++) {
				Vec3 spot = DefaultRandomPos.getPosAway(this.bunnay, FLEE_AWAY_RANGE, FLEE_AWAY_VERTICAL, this.threatCenter);
				if (spot == null || spot.distanceTo(this.threatCenter) <= now) {
					continue;
				}
				Path path = this.bunnay.getNavigation().createPath(spot.x, spot.y, spot.z, 0);
				if (path != null && path.canReach()) {
					this.bunnay.getNavigation().moveTo(path, FLEE_SPEED);
					return;
				}
			}
		}

		@Override
		public void stop() {
			this.hop.stop();
			this.bunnay.fleeing = false;
			this.bunnay.getNavigation().stop();
		}
	}

	/** Follows its owner like any pet, and lets the bunnay know when it is doing it (see HopGoal and planTravelHop). */
	private static final class BunnayFollowOwnerGoal extends FollowOwnerGoal {
		private final BunnayEntity bunnay;

		BunnayFollowOwnerGoal(BunnayEntity bunnay, double speed, float startDistance, float stopDistance) {
			super(bunnay, speed, startDistance, stopDistance);
			this.bunnay = bunnay;
		}

		// It does not follow its owner while it is tuned in to a note block.
		@Override
		public boolean canUse() {
			return !this.bunnay.isFarming() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return !this.bunnay.isFarming() && super.canContinueToUse();
		}

		@Override
		public void start() {
			super.start();
			this.bunnay.following = true;
		}

		@Override
		public void stop() {
			super.stop();
			this.bunnay.following = false;
		}
	}

	/**
	 * Carries out one hop: the launch at once (facing the way it will go, with friction off), the flight, and the landing
	 * (silent, which puts friction back and takes some of the sideways speed off it) after which it is free to move on that
	 * very tick. The goals that hop own one of these and run it each tick. The animation is held while the flag is set.
	 */
	private static final class HopRun {
		private final BunnayEntity bunnay;
		private Hop hop;
		private int ticks;
		private boolean running;

		HopRun(BunnayEntity bunnay) {
			this.bunnay = bunnay;
		}

		void start(Hop hop) {
			this.hop = hop;
			this.ticks = 0;
			this.running = true;
			this.bunnay.getNavigation().stop();
			this.bunnay.entityData.set(DATA_HOP_AIR_TICKS, hop.airTicks());
			this.bunnay.entityData.set(DATA_HOPPING, true);
		}

		void tick() {
			if (!this.running) {
				return;
			}
			this.ticks++;
			if (this.ticks == 1) {
				// Off at once, facing where it is going. Worked out again from where it is now, in case it has been nudged since
				// it chose; if it cannot any more, it does not go.
				Hop launch = this.bunnay.solveHop(this.hop.landing(), this.hop.airTicks());
				if (launch == null) {
					this.stop();
					return;
				}
				float yaw = (float) Math.toDegrees(Math.atan2(launch.velocity().z, launch.velocity().x)) - 90.0F;
				this.bunnay.setYRot(yaw);
				this.bunnay.yBodyRot = yaw;
				this.bunnay.setYHeadRot(yaw);
				this.bunnay.setDiscardFriction(true);
				this.bunnay.setDeltaMovement(launch.velocity());
				this.bunnay.needsSync = true;
				this.bunnay.playSound(SoundEvents.RABBIT_JUMP, 1.0F, 1.0F);
			} else if (this.bunnay.onGround() && this.ticks > 2) {
				// Down: friction is back, it keeps some of its speed, and it is free to move on this very tick.
				this.bunnay.setDeltaMovement(this.bunnay.getDeltaMovement().multiply(HOP_LANDING_MOMENTUM, 1.0, HOP_LANDING_MOMENTUM));
				this.stop();
			} else if (this.ticks > this.hop.airTicks() + HOP_LATE_TICKS) {
				this.stop();
			}
		}

		/** Ends the run (landed, or called off), puts friction back, and starts the wait for the next hop. */
		void stop() {
			if (!this.running) {
				return;
			}
			this.running = false;
			this.bunnay.setDiscardFriction(false);
			this.bunnay.entityData.set(DATA_HOPPING, false);
			this.bunnay.hopCooldown = HOP_COOLDOWN_MIN + this.bunnay.getRandom().nextInt(HOP_COOLDOWN_RANGE);
		}
	}

	/**
	 * Hops towards where it is going, whenever the hop is off cooldown and there is a hop worth making (see planTravelHop).
	 * It looks every HOP_SCAN_INTERVAL ticks, and it cannot be interrupted once it has started.
	 */
	private static final class HopGoal extends Goal {
		private final BunnayEntity bunnay;
		private final HopRun run;
		private Hop plan;
		private int scanDelay;

		HopGoal(BunnayEntity bunnay) {
			this.bunnay = bunnay;
			this.run = new HopRun(bunnay);
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (this.bunnay.hopCooldown > 0 || !this.bunnay.canHopNow() || --this.scanDelay > 0) {
				return false;
			}
			this.scanDelay = HOP_SCAN_INTERVAL;
			this.plan = this.bunnay.planTravelHop();
			return this.plan != null;
		}

		@Override
		public boolean canContinueToUse() {
			return this.run.running;
		}

		@Override
		public boolean isInterruptable() {
			return false;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			this.run.start(this.plan);
		}

		@Override
		public void tick() {
			this.run.tick();
		}

		@Override
		public void stop() {
			this.run.stop();
		}
	}

	/** Calm and free to go about farm business: tame, not sitting, dancing, eating, hopping, riding or fighting. */
	private boolean freeForFarmWork() {
		LivingEntity target = this.getTarget();
		return this.isTame() && !this.isBaby() && !this.isOrderedToSit() && !this.isDancing() && !this.isEating() && !this.isHopping()
			&& !this.isPassenger() && !this.fleeing && (target == null || !target.isAlive());
	}

	/**
	 * Pulls it back to the note block it is tuned in to when it has got further than FARM_LEASH from it, like the allay's
	 * "stay close to the target": it heads for the note block until it is within FARM_CLOSE_ENOUGH.
	 */
	private static final class StayNearFarmGoal extends Goal {
		private final BunnayEntity bunnay;
		private int ticks;

		StayNearFarmGoal(BunnayEntity bunnay) {
			this.bunnay = bunnay;
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			BlockPos farm = this.bunnay.farmPos;
			return farm != null && this.bunnay.freeForFarmWork() && this.bunnay.distanceToSqr(Vec3.atCenterOf(farm)) > FARM_LEASH * FARM_LEASH;
		}

		@Override
		public boolean canContinueToUse() {
			BlockPos farm = this.bunnay.farmPos;
			return farm != null && this.bunnay.freeForFarmWork() && this.ticks < FARM_DELIVER_GIVE_UP_TICKS
				&& this.bunnay.distanceToSqr(Vec3.atCenterOf(farm)) > FARM_CLOSE_ENOUGH * FARM_CLOSE_ENOUGH;
		}

		@Override
		public void start() {
			this.ticks = 0;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			BlockPos farm = this.bunnay.farmPos;
			if (farm == null) {
				return;
			}
			this.ticks++;
			if (this.ticks % 10 == 1) {
				this.bunnay.getNavigation().moveTo(farm.getX() + 0.5, farm.getY() + 1, farm.getZ() + 0.5, 1.2);
			}
		}

		@Override
		public void stop() {
			this.bunnay.getNavigation().stop();
		}
	}

	/**
	 * Takes the harvest to the note block it is tuned in to and tosses it there, the way an allay hands over what it has
	 * collected. It does that when its food slot is full or the harvest goal found nothing left to harvest, and it has more
	 * carrots than the FARM_KEEP_CARROTS it keeps for itself.
	 */
	private static final class DeliverToNoteBlockGoal extends Goal {
		private final BunnayEntity bunnay;
		private int ticks;
		private boolean delivered;

		DeliverToNoteBlockGoal(BunnayEntity bunnay) {
			this.bunnay = bunnay;
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		private boolean shouldDeliver() {
			return this.bunnay.farmPos != null && this.bunnay.freeForFarmWork() && this.bunnay.carrotsToDeliver() > 0
				&& (!this.bunnay.canTakeCarrots() || this.bunnay.noCropsFound);
		}

		@Override
		public boolean canUse() {
			return this.bunnay.deliverCooldown <= 0 && this.shouldDeliver();
		}

		@Override
		public boolean canContinueToUse() {
			return !this.delivered && this.ticks < FARM_DELIVER_GIVE_UP_TICKS && this.shouldDeliver();
		}

		@Override
		public void start() {
			this.ticks = 0;
			this.delivered = false;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			BlockPos farm = this.bunnay.farmPos;
			if (farm == null) {
				return;
			}
			this.ticks++;
			Vec3 target = Vec3.atCenterOf(farm);
			this.bunnay.getLookControl().setLookAt(target.x, target.y, target.z);
			if (this.bunnay.distanceToSqr(target) <= FARM_DELIVER_DISTANCE * FARM_DELIVER_DISTANCE) {
				ItemStack load = this.bunnay.food.removeItem(0, this.bunnay.carrotsToDeliver());
				BehaviorUtils.throwItem(this.bunnay, load, target);
				this.bunnay.playSound(SoundEvents.RABBIT_AMBIENT, 1.0F, 1.3F);
				this.bunnay.deliverCooldown = FARM_DELIVER_COOLDOWN;
				this.bunnay.getNavigation().stop();
				this.delivered = true;
			} else if (this.ticks % 10 == 1) {
				this.bunnay.getNavigation().moveTo(target.x, farm.getY() + 1, target.z, 1.2);
			}
		}

		@Override
		public void stop() {
			this.bunnay.getNavigation().stop();
		}
	}

	/** Hears a note block played: the allay's listener for it (Allay.VibrationUser), pointed at this bunnay (see hearNoteBlock). */
	private static final class NoteBlockListener implements GameEventListener {
		private final BunnayEntity bunnay;
		private final PositionSource listenerSource;
		private final int listenerRadius;

		NoteBlockListener(BunnayEntity bunnay, PositionSource listenerSource, int listenerRadius) {
			this.bunnay = bunnay;
			this.listenerSource = listenerSource;
			this.listenerRadius = listenerRadius;
		}

		@Override
		public PositionSource getListenerSource() {
			return this.listenerSource;
		}

		@Override
		public int getListenerRadius() {
			return this.listenerRadius;
		}

		@Override
		public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 pos) {
			if (event.is(GameEvent.NOTE_BLOCK_PLAY)) {
				this.bunnay.hearNoteBlock(level, BlockPos.containing(pos), pos);
				return true;
			}
			return false;
		}
	}

	/**
	 * Goes to carrots lying on the ground and picks them up into its food slot, when it has room and nothing better to do (see
	 * PICK_UP_RADIUS). It looks every second or so, takes the nearest one it has a path to, and gives up on one it cannot reach.
	 */
	private static final class PickUpCarrotsGoal extends Goal {
		private final BunnayEntity bunnay;
		private ItemEntity item;
		private int ticks;
		private int scanDelay;

		PickUpCarrotsGoal(BunnayEntity bunnay) {
			this.bunnay = bunnay;
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		private boolean ready() {
			return this.bunnay.level() instanceof ServerLevel level && this.bunnay.freeForFarmWork() && this.bunnay.canTakeCarrots()
				&& level.getGameRules().get(GameRules.MOB_GRIEFING);
		}

		private boolean wanted(ItemEntity candidate) {
			ItemStack stack = candidate.getItem();
			if (candidate.isRemoved() || candidate.hasPickUpDelay() || !isCarrot(stack) || !this.bunnay.food.canAddItem(stack)
				|| candidate.getOwner() instanceof BunnayEntity) {
				return false;
			}
			// Within reach of the note block it is tuned in to, or of its owner when it is not.
			BlockPos farm = this.bunnay.farmPos;
			LivingEntity owner = this.bunnay.getOwner();
			if (farm != null) {
				return farm.distToCenterSqr(candidate.position()) <= FARM_LEASH * FARM_LEASH;
			}
			return owner == null || owner.distanceToSqr(candidate) <= HARVEST_OWNER_RANGE * HARVEST_OWNER_RANGE;
		}

		@Override
		public boolean canUse() {
			if (this.scanDelay > 0) {
				this.scanDelay--;
				return false;
			}
			this.scanDelay = 20 + this.bunnay.getRandom().nextInt(20);
			if (!this.ready()) {
				return false;
			}
			List<ItemEntity> found = this.bunnay.level().getEntitiesOfClass(ItemEntity.class,
				this.bunnay.getBoundingBox().inflate(PICK_UP_RADIUS, PICK_UP_HEIGHT, PICK_UP_RADIUS), this::wanted);
			found.sort(Comparator.comparingDouble(candidate -> this.bunnay.distanceToSqr(candidate)));
			for (ItemEntity candidate : found.stream().limit(5).toList()) {
				if (this.bunnay.getNavigation().createPath(candidate, 1) != null) {
					this.item = candidate;
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean canContinueToUse() {
			return this.item != null && this.ticks < PICK_UP_GIVE_UP_TICKS && this.ready() && this.wanted(this.item);
		}

		@Override
		public void start() {
			this.ticks = 0;
			this.bunnay.getNavigation().moveTo(this.item, 1.2);
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			if (this.item == null) {
				return;
			}
			this.ticks++;
			this.bunnay.getLookControl().setLookAt(this.item);
			if (this.bunnay.distanceToSqr(this.item) > 1.3 * 1.3) {
				if (this.bunnay.getNavigation().isDone()) {
					this.bunnay.getNavigation().moveTo(this.item, 1.2);
				}
				return;
			}
			// Close enough: into the food slot (what does not fit stays on the ground).
			ItemStack leftover = this.bunnay.food.addItem(this.item.getItem().copy());
			if (leftover.isEmpty()) {
				this.item.discard();
			} else {
				this.item.setItem(leftover);
			}
			this.bunnay.food.setChanged();
			this.bunnay.level().playSound(null, this.bunnay.getX(), this.bunnay.getY(), this.bunnay.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.3F, 1.4F);
			this.item = null;
		}

		@Override
		public void stop() {
			this.item = null;
			this.bunnay.getNavigation().stop();
		}
	}

	/** Hears the jukebox: the allay's listener (Allay.JukeboxListener), pointed at this bunnay. */
	private static final class JukeboxListener implements GameEventListener {
		private final BunnayEntity bunnay;
		private final PositionSource listenerSource;
		private final int listenerRadius;

		JukeboxListener(BunnayEntity bunnay, PositionSource listenerSource, int listenerRadius) {
			this.bunnay = bunnay;
			this.listenerSource = listenerSource;
			this.listenerRadius = listenerRadius;
		}

		@Override
		public PositionSource getListenerSource() {
			return this.listenerSource;
		}

		@Override
		public int getListenerRadius() {
			return this.listenerRadius;
		}

		@Override
		public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 pos) {
			if (event.is(GameEvent.JUKEBOX_PLAY)) {
				this.bunnay.setJukeboxPlaying(BlockPos.containing(pos), true);
				return true;
			}
			if (event.is(GameEvent.JUKEBOX_STOP_PLAY)) {
				this.bunnay.setJukeboxPlaying(BlockPos.containing(pos), false);
				return true;
			}
			return false;
		}
	}

	/** Stands still while it dances, so the sway in the model is all that moves. A fight takes priority. */
	private static final class DanceGoal extends Goal {
		private final BunnayEntity bunnay;

		DanceGoal(BunnayEntity bunnay) {
			this.bunnay = bunnay;
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = this.bunnay.getTarget();
			return this.bunnay.isDancing() && (target == null || !target.isAlive());
		}

		@Override
		public boolean canContinueToUse() {
			return this.canUse();
		}

		@Override
		public void start() {
			this.bunnay.getNavigation().stop();
		}
	}

	/** The usual melee goal, except that it may swing a little before the cooldown is up (see ATTACK_HEAD_START_TICKS). */
	private static final class BunnayMeleeGoal extends MeleeAttackGoal {
		BunnayMeleeGoal(BunnayEntity bunnay) {
			super(bunnay, 1.3, true);
		}

		@Override
		protected boolean isTimeToAttack() {
			return this.getTicksUntilNextAttack() <= ATTACK_HEAD_START_TICKS;
		}
	}

	/**
	 * Harvests one fully grown carrot at a time, the way a person would: it walks to the carrot, swings at it and breaks it, so
	 * the carrots really drop; then it walks to the dropped carrots and picks them up into its food slot; waits a moment; and
	 * plants a new carrot in the spot. It is built so that nothing can leave it stuck: every part has a time limit, drops that
	 * are picked up by someone else (or despawn) are simply skipped, and if the job is interrupted after the carrot was broken
	 * (a fight), the new carrot is planted right then so there is never a hole in the field. When it is done with one
	 * carrot it goes straight on to the next ripe one, so a row is worked through without pause.
	 */
	private static final class HarvestCarrotsGoal extends Goal {
		private enum Phase {
			/** Heading for the ripe carrot, then swinging at it. */
			GO_TO_CROP,
			/** The carrot is broken; collecting what it dropped. */
			PICK_UP,
			/** Waiting a moment next to the spot before planting. */
			WAIT
		}

		private final BunnayEntity bunnay;
		private BlockPos crop;
		private Phase phase = Phase.GO_TO_CROP;
		/** Whether the carrot at crop has been broken, so a new one is owed. */
		private boolean broken;
		private final List<ItemEntity> drops = new java.util.ArrayList<>();
		private int workTicks;
		private int phaseTicks;
		private int giveUpTicks;
		/** Ticks until it next scans for carrots, so the search is not done every tick. */
		private int scanDelay;

		HarvestCarrotsGoal(BunnayEntity bunnay) {
			this.bunnay = bunnay;
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		/** Calm and free to work: tame, not busy with anything else, and not in a fight. */
		private boolean freeToWork() {
			LivingEntity target = this.bunnay.getTarget();
			return this.bunnay.isTame() && !this.bunnay.isBaby() && !this.bunnay.isOrderedToSit() && !this.bunnay.isDancing()
				&& !this.bunnay.isEating() && !this.bunnay.isHopping() && !this.bunnay.isPassenger()
				&& (target == null || !target.isAlive());
		}

		/** Able to start on a new carrot: free to work, with room for carrots, and mobs may grief. */
		private boolean canStart() {
			return this.bunnay.level() instanceof ServerLevel level && this.freeToWork() && this.bunnay.canTakeCarrots()
				&& level.getGameRules().get(GameRules.MOB_GRIEFING);
		}

		private boolean isRipeCarrot(BlockPos pos) {
			BlockState state = this.bunnay.level().getBlockState(pos);
			return state.getBlock() instanceof CarrotBlock carrots && carrots.isMaxAge(state);
		}

		@Override
		public boolean canUse() {
			if (this.scanDelay > 0) {
				this.scanDelay--;
				return false;
			}
			this.scanDelay = 20 + this.bunnay.getRandom().nextInt(20);
			if (!this.canStart()) {
				return false;
			}
			this.crop = this.findCrop();
			this.bunnay.noCropsFound = this.crop == null;
			return this.crop != null;
		}

		/** The nearest ripe carrot (close enough to its owner) that it has a path to, or null. */
		private BlockPos findCrop() {
			LivingEntity owner = this.bunnay.getOwner();
			BlockPos farm = this.bunnay.farmPos;
			BlockPos origin = this.bunnay.blockPosition();
			List<BlockPos> ripe = new java.util.ArrayList<>();
			for (BlockPos pos : BlockPos.betweenClosed(
				origin.offset(-HARVEST_SEARCH_RADIUS, -HARVEST_SEARCH_HEIGHT, -HARVEST_SEARCH_RADIUS),
				origin.offset(HARVEST_SEARCH_RADIUS, HARVEST_SEARCH_HEIGHT, HARVEST_SEARCH_RADIUS)
			)) {
				// Close enough to the note block it is tuned in to, or to its owner when it is not.
				boolean near = farm != null
					? farm.distToCenterSqr(Vec3.atCenterOf(pos)) <= FARM_LEASH * FARM_LEASH
					: owner == null || owner.distanceToSqr(Vec3.atCenterOf(pos)) <= HARVEST_OWNER_RANGE * HARVEST_OWNER_RANGE;
				if (this.isRipeCarrot(pos) && near) {
					ripe.add(pos.immutable());
				}
			}
			ripe.sort(Comparator.comparingDouble(pos -> this.bunnay.distanceToSqr(Vec3.atCenterOf(pos))));
			for (BlockPos pos : ripe.stream().limit(5).toList()) {
				if (this.bunnay.getNavigation().createPath(pos, 1) != null) {
					return pos;
				}
			}
			return null;
		}

		@Override
		public boolean canContinueToUse() {
			if (this.crop == null || this.giveUpTicks >= HARVEST_GIVE_UP_TICKS || !this.freeToWork()) {
				return false;
			}
			// Until it is broken, the carrot has to still be there and ripe (someone else may have taken it).
			return this.phase != Phase.GO_TO_CROP || (this.bunnay.canTakeCarrots() && this.isRipeCarrot(this.crop));
		}

		@Override
		public void start() {
			this.phase = Phase.GO_TO_CROP;
			this.broken = false;
			this.drops.clear();
			this.workTicks = 0;
			this.phaseTicks = 0;
			this.giveUpTicks = 0;
			this.bunnay.getNavigation().moveTo(this.crop.getX() + 0.5, this.crop.getY(), this.crop.getZ() + 0.5, 1.2);
		}

		@Override
		public void stop() {
			// However the job ended (finished, interrupted, timed out), a carrot that was broken is replaced now, so it
			// never leaves a hole in the field.
			if (this.broken && this.crop != null && this.bunnay.level() instanceof ServerLevel level) {
				this.plant(level);
			}
			this.crop = null;
			this.broken = false;
			this.drops.clear();
			this.bunnay.getNavigation().stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			// The goal can be ticked once more after the job is done (it is checked for ending only every other tick).
			if (this.crop == null || !(this.bunnay.level() instanceof ServerLevel level)) {
				return;
			}
			this.giveUpTicks++;
			switch (this.phase) {
				case GO_TO_CROP -> this.tickGoToCrop(level);
				case PICK_UP -> this.tickPickUp();
				case WAIT -> this.tickWait(level);
			}
		}

		private void tickGoToCrop(ServerLevel level) {
			Vec3 center = Vec3.atCenterOf(this.crop);
			this.bunnay.getLookControl().setLookAt(center.x, center.y - 0.3, center.z);
			if (this.bunnay.distanceToSqr(center) > 1.9 * 1.9) {
				// Still on its way (or the path ran out short): keep heading for it.
				this.workTicks = 0;
				if (this.bunnay.getNavigation().isDone()) {
					this.bunnay.getNavigation().moveTo(center.x, this.crop.getY(), center.z, 1.2);
				}
				return;
			}
			this.bunnay.getNavigation().stop();
			this.workTicks++;
			if (this.workTicks == 1) {
				this.bunnay.swing(InteractionHand.MAIN_HAND, this.bunnay.getMainHandItem().getInteractAnimation());
			}
			if (this.workTicks >= HARVEST_WORK_TICKS) {
				this.breakCrop(level);
			}
		}

		/** Breaks the carrot so that it drops its carrots, and notes the dropped items to go and collect. */
		private void breakCrop(ServerLevel level) {
			if (!this.isRipeCarrot(this.crop)) {
				return;
			}
			level.destroyBlock(this.crop, true, this.bunnay);
			this.broken = true;
			// The items it just dropped: carrots that appeared a moment ago next to where the carrot was.
			this.drops.clear();
			this.drops.addAll(level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(this.crop).inflate(1.5), item -> item.getAge() < 20));
			this.phase = Phase.PICK_UP;
			this.phaseTicks = 0;
		}

		private void tickPickUp() {
			this.phaseTicks++;
			// Drops that someone else picked up, or that are gone, are simply skipped.
			this.drops.removeIf(item -> item.isRemoved() || item.getItem().isEmpty());
			if (this.drops.isEmpty() || !this.bunnay.canTakeCarrots() || this.phaseTicks > HARVEST_PICK_UP_TICKS) {
				this.startWaiting();
				return;
			}

			ItemEntity next = this.drops.stream().min(Comparator.comparingDouble(item -> this.bunnay.distanceToSqr(item))).get();
			this.bunnay.getLookControl().setLookAt(next);
			if (this.bunnay.distanceToSqr(next) > 1.3 * 1.3) {
				if (this.bunnay.getNavigation().isDone()) {
					this.bunnay.getNavigation().moveTo(next, 1.2);
				}
				return;
			}

			// Close enough: pick it up into the food slot (what does not fit stays on the ground).
			ItemStack stack = next.getItem();
			ItemStack leftover = this.bunnay.food.addItem(stack.copy());
			if (leftover.isEmpty()) {
				next.discard();
			} else {
				next.setItem(leftover);
			}
			this.bunnay.food.setChanged();
			this.bunnay.level().playSound(null, this.bunnay.getX(), this.bunnay.getY(), this.bunnay.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.3F, 1.4F);
		}

		private void startWaiting() {
			this.phase = Phase.WAIT;
			this.phaseTicks = 0;
			this.bunnay.getNavigation().stop();
		}

		/** A second beside the spot (walking back to it if it wandered off after the drops), then plants. */
		private void tickWait(ServerLevel level) {
			Vec3 center = Vec3.atCenterOf(this.crop);
			this.bunnay.getLookControl().setLookAt(center.x, center.y - 0.3, center.z);
			if (this.bunnay.distanceToSqr(center) > 2.2 * 2.2) {
				this.phaseTicks = 0;
				if (this.bunnay.getNavigation().isDone()) {
					this.bunnay.getNavigation().moveTo(center.x, this.crop.getY(), center.z, 1.2);
				}
				return;
			}
			this.bunnay.getNavigation().stop();
			this.phaseTicks++;
			if (this.phaseTicks >= HARVEST_REPLANT_DELAY_TICKS) {
				this.plant(level);
				this.startNextCarrot();
			}
		}

		/**
		 * Straight on to the next ripe carrot, if there is one, without letting go of the goal (so nothing else gets a turn in
		 * between, and there is no wait for the next search). With none left it ends, and a search follows shortly.
		 */
		private void startNextCarrot() {
			boolean canStart = this.canStart();
			BlockPos next = canStart ? this.findCrop() : null;
			this.bunnay.noCropsFound = canStart && next == null;
			if (next == null) {
				this.crop = null;
				this.scanDelay = 20;
				return;
			}
			this.crop = next;
			this.phase = Phase.GO_TO_CROP;
			this.broken = false;
			this.drops.clear();
			this.workTicks = 0;
			this.phaseTicks = 0;
			this.giveUpTicks = 0;
			this.bunnay.getNavigation().moveTo(next.getX() + 0.5, next.getY(), next.getZ() + 0.5, 1.2);
		}

		/** Plants a new carrot in the spot, if it is still empty with farmland under it (and loaded). Free of cost. */
		private void plant(ServerLevel level) {
			BlockPos spot = this.crop;
			this.broken = false;
			if (spot == null || !level.isLoaded(spot) || !level.getBlockState(spot).isAir() || !level.getBlockState(spot.below()).is(Blocks.FARMLAND)) {
				return;
			}
			level.setBlockAndUpdate(spot, Blocks.CARROTS.defaultBlockState());
			level.playSound(null, spot, SoundEvents.CROP_PLANTED, SoundSource.NEUTRAL, 0.8F, 1.0F);
			this.bunnay.swing(InteractionHand.MAIN_HAND, this.bunnay.getMainHandItem().getInteractAnimation());
		}
	}

	/**
	 * Gives carrots to a hungry owner: it walks to them and tosses them the carrots (see the GIFT_ constants). It only does
	 * it when it has nothing else going on: no fight, not sitting, dancing, eating or hopping.
	 */
	private static final class GiveCarrotsGoal extends Goal {
		private final BunnayEntity bunnay;
		private int ticks;
		private boolean gave;

		GiveCarrotsGoal(BunnayEntity bunnay) {
			this.bunnay = bunnay;
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		private boolean shouldGive() {
			LivingEntity target = this.bunnay.getTarget();
			return this.bunnay.isTame() && !this.bunnay.isBaby() && !this.bunnay.isOrderedToSit() && !this.bunnay.isDancing()
				&& !this.bunnay.isEating() && !this.bunnay.isHopping() && !this.bunnay.isPassenger()
				&& (target == null || !target.isAlive())
				&& this.bunnay.carrotsToGive() > 0 && this.bunnay.hungryOwner() != null;
		}

		@Override
		public boolean canUse() {
			return this.bunnay.giftCooldown <= 0 && this.shouldGive();
		}

		@Override
		public boolean canContinueToUse() {
			return !this.gave && this.ticks < GIFT_GIVE_UP_TICKS && this.shouldGive();
		}

		@Override
		public void start() {
			this.ticks = 0;
			this.gave = false;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			ServerPlayer owner = this.bunnay.hungryOwner();
			if (owner == null) {
				return;
			}
			this.ticks++;
			this.bunnay.getLookControl().setLookAt(owner, 30.0F, 30.0F);
			if (this.bunnay.distanceToSqr(owner) <= GIFT_REACH * GIFT_REACH) {
				ItemStack gift = this.bunnay.food.removeItem(0, this.bunnay.carrotsToGive());
				BehaviorUtils.throwItem(this.bunnay, gift, owner.position());
				this.bunnay.playSound(SoundEvents.RABBIT_AMBIENT, 1.0F, 1.3F);
				this.bunnay.giftCooldown = GIFT_COOLDOWN_TICKS;
				this.bunnay.getNavigation().stop();
				this.gave = true;
			} else if (this.ticks % 10 == 1) {
				this.bunnay.getNavigation().moveTo(owner, 1.2);
			}
		}

		@Override
		public void stop() {
			if (!this.gave && this.ticks >= GIFT_GIVE_UP_TICKS) {
				this.bunnay.giftCooldown = GIFT_RETRY_TICKS;
			}
			this.bunnay.getNavigation().stop();
		}
	}
}
