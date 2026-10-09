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
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.control.MoveControl;
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
	static final double WILD_HEALTH = 8.0;
	static final double TAME_HEALTH = 40.0;

	// Hopping, worked out the way the frog's long jump is (LongJumpToRandomPos and LongJumpUtil), but done with a reason. When the
	// hop is off cooldown and it has a reason (it is running from something, fighting something, or its owner is up above it),
	// it looks at the places within HOP_SEARCH_RADIUS blocks sideways, HOP_MAX_RISE up and HOP_MAX_DROP down that it could stand on and could not
	// walk to (no path of HOP_WALK_CHECK_LENGTH reaches them), best for the reason first. The launch is the frog's: one of the
	// angles in HOP_ANGLES (in random order, the first that works), the speed that lands it at the middle of the spot
	// less HOP_AIM_SHORT, 95% of that, and the arc checked by sampling points along it with a box. It goes at once, with no
	// wind-up, and in the air it has no friction (the frog's too), so it flies on the speed it took off with.
	static final int[] HOP_ANGLES = {65, 70, 75};
	/** The fastest it can take off, in blocks a tick (the frog's is about 1.5). */
	static final double HOP_MAX_SPEED = 2.0;
	/** The frog aims this far (in blocks) short of the middle of the spot, and flies at this fraction of the speed that would land it. */
	static final double HOP_AIM_SHORT = 0.5;
	static final double HOP_SPEED_SCALE = 0.95;
	/** The box (width and height, in blocks) the arc is checked with: a little bigger than its body, to play it safe. */
	static final double HOP_SAMPLE_BOX = 0.6;
	/** Where it looks for somewhere to land: this many blocks each way sideways, and this far above or below its feet. */
	static final int HOP_SEARCH_RADIUS = 4;
	static final int HOP_MAX_RISE = 3;
	static final int HOP_MAX_DROP = 3;
	/** A spot is one it could not walk to if no path of this many blocks reaches it (the frog's test uses 8; 4 is being tried). */
	static final int HOP_WALK_CHECK_LENGTH = 4;
	/** A hop has to bring it at least this much closer to where it is going (or the threat this much further away), by effort. */
	static final double HOP_MIN_DISTANCE = 8.0;
	/** The shortest hop, sideways (from the middle of the spot), so that aiming short of it does not aim behind it. */
	static final double HOP_MIN_HORIZONTAL = 0.75;
	/** Each block of climbing counts as this many extra blocks of distance when judging a hop (see effort). */
	static final double CLIMB_COST = 2.0;
	/** The wait after a hop, in ticks: 40, which is 2 seconds (testing; the frog waits 100 to 140). */
	static final int HOP_COOLDOWN_MIN = 40;
	static final int HOP_COOLDOWN_RANGE = 1;
	/** While the hop is off cooldown and it has a reason, it looks for a hop this often, in ticks; and again this soon after finding none. */
	static final int HOP_SCAN_TICKS = 10;
	static final int HOP_RETRY_TICKS = 20;
	/** How many arcs it works out, and how many of those it checks for a way to walk there (the costly part), before giving up. */
	static final int HOP_MAX_ARC_TRIES = 40;
	static final int HOP_MAX_WALK_CHECKS = 6;
	/** A hop in the air this long past what its arc says is called off (it was stopped by something, say). */
	static final int HOP_LATE_TICKS = 20;
	/** How long the hop clip plays if nothing says otherwise. */
	static final int HOP_DEFAULT_AIR_TICKS = 15;
	/** It keeps this much of its sideways speed when it lands (the frog keeps 10%), so it runs on a little. */
	static final double HOP_LANDING_MOMENTUM = 0.1;

	// The idle animation is cosmetic and runs on the client only, on the same timer as the rabbit's: a new one every
	// 180 to 219 ticks (9 to 11 seconds) while standing still.
	static final int IDLE_MIN_TICKS = 180;
	static final int IDLE_EXTRA_TICKS = 40;

	// Fighting: it always swings its two hands in turn, an empty paw counting as a hand (the bunnay's own hit is 2). What it
	// holds adds damage to the hits of the hand that holds it: a bamboo, a breeze rod or a blaze rod +2, or a stick
	// +1. A breeze rod also blasts the foe the way a wind charge would; a blaze rod also does what a blaze's small
	// fireball does to whatever it hits (holding one does not protect the bunnay from fire in any way). All of them swing
	// at the same pace.
	static final double ROD_BONUS_DAMAGE = 2.0;
	static final double BAMBOO_BONUS_DAMAGE = 2.0;
	static final double STICK_BONUS_DAMAGE = 1.0;
	/**
	 * How long a hit with a blaze rod sets a foe on fire, in seconds: the same 5 a small fireball does (SmallFireball also
	 * does 5 damage, which is left out here, and only places fire when it hits a block, not an entity, so no fire is placed).
	 */
	static final float BLAZE_ROD_FIRE_SECONDS = 5.0F;
	private static final Identifier WEAPON_DAMAGE_ID = AllayVariantsMod.id("weapon_damage");
	/** The wind burst's shove on a foe: this fast away from the bunnay, and this fast straight up (0.9 is about 4 blocks of height). */
	static final double WIND_BURST_HORIZONTAL = 0.5;
	static final double WIND_BURST_VERTICAL = 0.9;
	/**
	 * How long it waits between swings, in ticks: 12, quicker than a warden's 18 because the bunnay is always swinging two hands
	 * in turn. The wait is shared by both hands, and it is the same whatever they hold.
	 */
	static final int ATTACK_INTERVAL_TICKS = 12;
	/** How fast the ready stance eases in and out, per tick (it takes 4 ticks to raise and about 7 to lower). */
	static final float READY_RISE = 0.25F;
	static final float READY_FALL = 0.15F;

	// Fleeing at low health. A tamed bunnay at FLEE_BELOW_HEALTH or lower stops fighting and runs from the enemies instead: it
	// takes no target at all (so its owner's fights, and whatever hurts it, do not pull it in) and runs from the mobs that are
	// after it (and only those: other hostile mobs are not its concern) the way a rabbit runs: to a random place away from them,
	// and then to another (see BunnayBehaviors.FleeFromThreat), now and then hopping instead. Once nothing is near it and it has
	// had a quiet moment, it eats a carrot as usual, and when it is back above that health it fights again.
	static final float FLEE_BELOW_HEALTH = 20.0F;
	/** The speed it runs at when fleeing, the same as a wolf's or cat's panic. */
	static final float FLEE_SPEED = 1.5F;
	/** It looks this far (in blocks) for mobs that are after it, and keeps running until it is this far from every one of them. */
	static final double FLEE_SEARCH_RADIUS = 34.0;

	// How fast it walks when doing each thing (times its movement speed), and the distances for following.
	static final float CHASE_SPEED = 1.3F;
	static final float FOLLOW_SPEED = 1.0F;
	static final float TEMPT_SPEED = 1.0F;
	static final float FARM_SPEED = 1.2F;
	static final float GIVE_SPEED = 1.2F;
	static final float PICK_UP_SPEED = 1.2F;
	/** Follows its owner like a wolf: starts from this many blocks away and stops this close to them. */
	static final int FOLLOW_START_DISTANCE = 10;
	static final int FOLLOW_STOP_DISTANCE = 2;
	/** It breaks a carrot from this far away (in blocks). */
	static final double HARVEST_REACH = 1.9;

	// Climbing steps. Walking up a run of one-block steps a mob jumps onto each one, and by default it must wait 10 ticks
	// between jumps while the hop itself is over in 9, and in the air it has almost no sideways thrust, so it creeps onto the
	// step. Two things speed that up. The wait is cut to STEP_JUMP_DELAY while it follows a path. And a jump up onto the next
	// step of the path gets a push towards the middle of that step, worked out from how far away it is so it lands there and
	// does not overshoot (STEP_HOP_REACH is the blocks covered per point of sideways speed over the 9 ticks the hop takes,
	// from the same arithmetic as HOP_REACH).
	static final int STEP_JUMP_DELAY = 3;
	static final double STEP_HOP_REACH = 4.2;
	static final double STEP_HOP_MAX_SPEED = 0.3;
	/** The next step of the path counts as a step up when it is at least this much higher, and at most this much. */
	static final double STEP_MIN_RISE = 0.5;
	static final double STEP_MAX_RISE = 1.6;
	/** Only a step this close (sideways, in blocks) is given the push; further is not a step but a longer way to go. */
	static final double STEP_MAX_DISTANCE = 1.6;
	/** LivingEntity's wait between jumps is private, so it is reached by reflection (null if that fails, and nothing changes). */
	private static final java.lang.reflect.Field NO_JUMP_DELAY_FIELD = noJumpDelayField();

	// Farming at a note block, the way an allay attends to one. A note block played within FARM_HEARING_RADIUS makes a tamed
	// bunnay "tune in" to it: for FARM_FORGET_TICKS (30 seconds) after the last note it works the carrots around it with its
	// normal logic (looking for ripe ones, harvesting, replanting), is pulled back if it gets more than FARM_LEASH from the note
	// block, and takes what it has harvested to the note block and tosses it there (so a hopper can catch it) whenever its food
	// slot is full or there is nothing left to harvest. Another note from the same block starts the 30 seconds again; a different
	// note block is ignored until it has forgotten the first. It does not follow its owner while it is tuned in. Wool between the
	// note block and the bunnay muffles the note, as it does for an allay. Sitting makes it forget at once.
	static final int FARM_HEARING_RADIUS = 16;
	static final int FARM_FORGET_TICKS = 600;
	/** It is pulled back when it is further than this from the note block (in blocks), until it is within FARM_CLOSE_ENOUGH. */
	static final double FARM_LEASH = 16.0;
	static final double FARM_CLOSE_ENOUGH = 4.0;
	/** It keeps this many carrots for itself when it delivers, so it can still heal. */
	static final int FARM_KEEP_CARROTS = 1;
	/** The wait after a delivery before the next, and how long it tries to reach the note block, in ticks. */
	static final int FARM_DELIVER_COOLDOWN = 100;
	static final int FARM_DELIVER_GIVE_UP_TICKS = 400;

	// Picking up carrots lying about. As something to do when it has nothing better, a tamed bunnay with room in its food slot
	// goes to carrots on the ground nearby and picks them up, whoever dropped them (but not ones a bunnay tossed: its own gifts and
	// deliveries). It stays within reach of its owner, or of the note block it is tuned in to.
	static final int PICK_UP_RADIUS = 8;
	static final int PICK_UP_HEIGHT = 3;

	// Harvesting carrots. Whenever it can carry more (its food slot is empty or holds ordinary carrots with room, not golden
	// carrots), a tamed bunnay out of a fight goes to fully grown carrots it can see, breaks them, and puts what they drop
	// straight into its food slot. It only does this where mobs are allowed to grief, and never strays far from its owner.
	/** How far it looks for carrots, sideways and up or down, in blocks. */
	static final int HARVEST_SEARCH_RADIUS = 10;
	static final int HARVEST_SEARCH_HEIGHT = 2;
	/** It only works carrots this close to its owner, so harvesting does not lead it off. */
	static final double HARVEST_OWNER_RANGE = 16.0;
	/** It breaks the carrots this long after it gets there (it swings at them first), in ticks. */
	static final int HARVEST_WORK_TICKS = 8;

	// Eating the carrots in its off hand to heal.
	/** How long one carrot takes to eat, in ticks (the same as a player's). */
	static final int EAT_TICKS = 32;
	/** It starts eating when it is missing at least this much health (1 heart), so a golden carrot is not wasted on a scratch... */
	static final float EAT_MIN_MISSING_HEALTH = 2.0F;
	/** ...but once it has started it keeps going, carrot after carrot, until it is missing less than this (about healed). */
	static final float EAT_KEEP_GOING_MISSING_HEALTH = 0.5F;
	/** Pause after finishing (or being interrupted) before it starts on the next carrot. */
	static final int EAT_COOLDOWN_TICKS = 40;
	/** It will not eat until this many ticks have passed since it was last hurt by a mob, so it is out of the fight (5 seconds). */
	static final int EAT_SAFE_TICKS = 100;
	/** How fast the eating pose eases in and out, per tick. */
	static final float EAT_RISE = 0.2F;
	static final float EAT_FALL = 0.15F;

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
	/** Server side: ticks into the carrot being eaten. */
	private int eatTicks;
	/**
	 * The carrots it eats. They are not in a hand (both hands hold weapons); it takes one out to eat, which is shown by
	 * swapping the carrot into its left hand for the meal, like a player switching to food and back.
	 */
	final SimpleContainer food = new SimpleContainer(1);

	// Giving carrots to a hungry owner, the way an allay hands items to the player it likes: when its owner is hungry and in
	// sight it walks over and tosses some of the carrots from its food slot to them (vanilla's GoAndGiveItemsToTarget, as for the allay). It keeps GIFT_KEEP_CARROTS for itself,
	// so it can still heal, and waits a long while before it does it again.
	/** The owner is hungry at or below this food level: the point where they can no longer sprint (three drumsticks). */
	static final int GIFT_HUNGRY_AT = 6;
	static final int GIFT_MAX_CARROTS = 5;
	static final int GIFT_KEEP_CARROTS = 1;
	/** The wait after a gift, in ticks (10 minutes). */
	static final int GIFT_COOLDOWN_TICKS = 12000;
	/** How close its owner has to be, in blocks, for it to notice they are hungry, and how close it gets to give. */
	static final double GIFT_NOTICE_RANGE = 12.0;
	/** It gives up reaching its owner after this many ticks. */
	static final int GIFT_GIVE_UP_TICKS = 200;

	/** The hand it attacked with last. With a weapon in each hand it swings them in turn. */
	private InteractionHand attackHand = InteractionHand.MAIN_HAND;

	// Dancing to a jukebox, the way the allay does: it listens for the jukebox game events, remembers which jukebox
	// is playing, and stops dancing when the music stops or the jukebox is gone or too far away.
	private final DynamicGameEventListener<JukeboxListener> dynamicJukeboxListener;
	private @Nullable BlockPos jukeboxPos;
	/** Listens for the note blocks that tune it in (see FARM_FORGET_TICKS); the one it is tuned in to is a Brain memory. */
	private final DynamicGameEventListener<NoteBlockListener> dynamicNoteBlockListener;
	/** DEBUG-TEMP: the tick the last hop ended, to log what happens just after it while fleeing. */
	int lastHopEndTick = -1000;
	/** DEBUG-TEMP: writes what the hops, the flee and the fights are doing to the game log (lines starting [bunnay). /bunnay log toggles it. */
	public static volatile boolean debugLogging = true;
	/** DEBUG-TEMP: marks the landing spots a hop scan looks at with coloured sparks (see glowSpot). /bunnay glow toggles it. */
	public static volatile boolean debugGlow = true;

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
		entityData.define(DATA_HOP_AIR_TICKS, HOP_DEFAULT_AIR_TICKS);
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
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		input.read("Food", ItemStack.OPTIONAL_CODEC).ifPresent(stack -> this.food.setItem(0, stack));
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
	boolean canEat(boolean alreadyEating) {
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
			&& !this.isFleeing();
	}

	/** Whether another ordinary carrot would fit in the food slot (it is empty, or holds carrots with room; golden ones do not mix). */
	/** How many carrots it would give right now: up to GIFT_MAX_CARROTS of the ordinary ones in its food slot, keeping GIFT_KEEP_CARROTS. */
	int carrotsToGive() {
		ItemStack held = this.food.getItem(0);
		return held.is(Items.CARROT) ? Math.max(0, Math.min(GIFT_MAX_CARROTS, held.getCount() - GIFT_KEEP_CARROTS)) : 0;
	}

	/** Its owner, if they are hungry, close and in sight, so that it notices. */
	@Nullable ServerPlayer hungryOwner() {
		if (!(this.getOwner() instanceof ServerPlayer owner) || !owner.isAlive() || owner.isSpectator() || owner.level() != this.level()) {
			return null;
		}
		boolean hungry = owner.getFoodData().getFoodLevel() <= GIFT_HUNGRY_AT;
		return hungry && this.distanceToSqr(owner) <= GIFT_NOTICE_RANGE * GIFT_NOTICE_RANGE && this.hasLineOfSight(owner) ? owner : null;
	}

	boolean canTakeCarrots() {
		ItemStack held = this.food.getItem(0);
		return held.isEmpty() || (held.is(Items.CARROT) && held.getCount() < held.getMaxStackSize());
	}

	/** Low on health, and tame: it runs from fights instead of taking part in them (see FLEE_BELOW_HEALTH). */
	boolean isScared() {
		return this.isTame() && this.getHealth() <= FLEE_BELOW_HEALTH;
	}

	// While it is scared it takes no target, whoever asks: its owner's fight, its owner being hit, or a mob that hurt it.
	@Override
	public void setTarget(@Nullable LivingEntity target) {
		if (target != null && (this.isScared() || this.isFleeing())) {
			return;
		}
		super.setTarget(target);
		LivingEntity valid = super.getTarget();
		if (valid == null) {
			this.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
		} else {
			this.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, valid);
		}
	}

	// The target lives in the Brain's attack target memory, as for the other Brain mobs.
	@Override
	public @Nullable LivingEntity getTarget() {
		return this.getTargetFromBrain();
	}

	// ---- the Brain (see BunnayAi) ----

	@Override
	protected Brain<BunnayEntity> makeBrain(Brain.Packed packed) {
		return BunnayAi.makeBrain(this, packed);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Brain<BunnayEntity> getBrain() {
		return (Brain<BunnayEntity>) super.getBrain();
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		// DEBUG: the hop made by /bunnay hop flies at the same point of the tick the Brain's hops do.
		this.debugHopRun.tick();
		this.getBrain().tick(level, this);
		BunnayAi.updateActivity(this);
		this.setAggressive(this.hasLiveTarget());
		super.customServerAiStep(level);
	}

	/** Ordered to sit, and able to: the sit goal's own conditions (it stays sitting once it has started). */
	boolean wantsToSitNow() {
		if (!this.isOrderedToSit() || !this.isTame()) {
			return false;
		}
		if (this.isInSittingPose()) {
			return true;
		}
		if (this.isInWater() || !this.onGround()) {
			return false;
		}
		LivingEntity owner = this.getOwner();
		return owner == null || owner.level() != this.level() || !(this.distanceToSqr(owner) < 144.0 && owner.getLastHurtByMob() != null);
	}

	boolean hasLiveTarget() {
		LivingEntity target = this.getTarget();
		return target != null && target.isAlive();
	}

	/** Whether it is running from something (the fleeing activity). */
	public boolean isFleeing() {
		return this.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.AVOID);
	}

	/** Called as it starts to flee: whatever it was doing stops. */
	void startFleeing() {
		this.stopEating();
		this.setTarget(null);
		this.getNavigation().stop();
		this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
	}

	/** Whether it should be following its owner now, the way a wolf does (it starts 10 blocks away and stops 2 from them). */
	boolean canFollowOwner() {
		return this.isTame() && this.getOwner() != null && !this.unableToMoveToOwner() && !this.isFarming();
	}

	/** Free to run: not sitting, riding or a baby. */
	boolean canRunNow() {
		return !this.isOrderedToSit() && !this.isPassenger() && !this.isBaby();
	}

	/** Starts the wait before the next hop. */
	void startHopCooldown() {
		this.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, HOP_COOLDOWN_MIN + this.getRandom().nextInt(HOP_COOLDOWN_RANGE));
	}

	/** Calm and free to work: tame, not busy with anything else, and not in a fight. */
	boolean freeToHarvest() {
		return this.isTame() && !this.isBaby() && !this.isOrderedToSit() && !this.isDancing() && !this.isEating() && !this.isHopping()
			&& !this.isPassenger() && !this.hasLiveTarget();
	}

	/** Able to start on a new carrot: free to work, with room for carrots, and mobs may grief. */
	boolean canStartHarvest() {
		return this.level() instanceof ServerLevel level && this.freeToHarvest() && this.canTakeCarrots() && level.getGameRules().get(GameRules.MOB_GRIEFING);
	}

	boolean canPickUpGroundCarrots() {
		return this.level() instanceof ServerLevel level && this.freeForFarmWork() && this.canTakeCarrots() && level.getGameRules().get(GameRules.MOB_GRIEFING);
	}

	// Loose carrots are picked up the way an allay picks up what it wants: the vanilla item sensor and go-to-item behaviour take it to
	// one, and the mob's own pick-up (Mob.aiStep) puts it in its food slot when it is close.
	@Override
	public boolean canPickUpLoot() {
		return this.canPickUpGroundCarrots();
	}

	@Override
	public boolean wantsToPickUp(ServerLevel level, ItemStack stack) {
		return isCarrot(stack) && this.food.canAddItem(stack);
	}

	@Override
	protected void pickUpItem(ServerLevel level, ItemEntity entity) {
		// Not the carrots a bunnay tossed (its own gifts and deliveries).
		if (entity.getOwner() instanceof BunnayEntity) {
			return;
		}
		ItemStack stack = entity.getItem();
		ItemStack leftover = this.food.addItem(stack.copy());
		int taken = stack.getCount() - leftover.getCount();
		if (taken > 0) {
			this.onItemPickup(entity);
			this.take(entity, taken);
			stack.shrink(taken);
			if (stack.isEmpty()) {
				entity.discard();
			}
			this.food.setChanged();
		}
	}

	boolean isRipeCarrot(BlockPos pos) {
		BlockState state = this.level().getBlockState(pos);
		return state.getBlock() instanceof CarrotBlock carrots && carrots.isMaxAge(state);
	}

	/** A carrot on the ground it would pick up: not one a bunnay tossed (its own gifts and deliveries), within reach of the note block or its owner. */
	boolean isWantedGroundItem(ItemEntity candidate) {
		ItemStack stack = candidate.getItem();
		if (candidate.isRemoved() || candidate.hasPickUpDelay() || !isCarrot(stack) || !this.food.canAddItem(stack) || candidate.getOwner() instanceof BunnayEntity) {
			return false;
		}
		BlockPos farm = this.getFarmPos();
		LivingEntity owner = this.getOwner();
		if (farm != null) {
			return farm.distToCenterSqr(candidate.position()) <= FARM_LEASH * FARM_LEASH;
		}
		return owner == null || owner.distanceToSqr(candidate) <= HARVEST_OWNER_RANGE * HARVEST_OWNER_RANGE;
	}

	/** The nearest wanted carrot on the ground (of the five nearest) that it has a path to. */
	@Nullable ItemEntity findGroundCarrot() {
		List<ItemEntity> found = this.level().getEntitiesOfClass(ItemEntity.class,
			this.getBoundingBox().inflate(PICK_UP_RADIUS, PICK_UP_HEIGHT, PICK_UP_RADIUS), this::isWantedGroundItem);
		found.sort(Comparator.comparingDouble(this::distanceToSqr));
		for (ItemEntity candidate : found.stream().limit(5).toList()) {
			if (this.getNavigation().createPath(candidate, 1) != null) {
				return candidate;
			}
		}
		return null;
	}

	/** The nearest ripe carrot (close enough to the note block or its owner) that it has a path to, or null. */
	@Nullable BlockPos findRipeCarrot() {
		LivingEntity owner = this.getOwner();
		BlockPos farm = this.getFarmPos();
		BlockPos origin = this.blockPosition();
		List<BlockPos> ripe = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(
			origin.offset(-HARVEST_SEARCH_RADIUS, -HARVEST_SEARCH_HEIGHT, -HARVEST_SEARCH_RADIUS),
			origin.offset(HARVEST_SEARCH_RADIUS, HARVEST_SEARCH_HEIGHT, HARVEST_SEARCH_RADIUS))) {
			boolean near = farm != null
				? farm.distToCenterSqr(Vec3.atCenterOf(pos)) <= FARM_LEASH * FARM_LEASH
				: owner == null || owner.distanceToSqr(Vec3.atCenterOf(pos)) <= HARVEST_OWNER_RANGE * HARVEST_OWNER_RANGE;
			if (this.isRipeCarrot(pos) && near) {
				ripe.add(pos.immutable());
			}
		}
		ripe.sort(Comparator.comparingDouble(pos -> this.distanceToSqr(Vec3.atCenterOf(pos))));
		for (BlockPos pos : ripe.stream().limit(5).toList()) {
			if (this.getNavigation().createPath(pos, 1) != null) {
				return pos;
			}
		}
		return null;
	}

	/** Mobs it runs from: whatever is after it (has it as its target), and nothing else. */
	List<Mob> findThreats() {
		return this.level().getEntitiesOfClass(
			Mob.class,
			this.getBoundingBox().inflate(FLEE_SEARCH_RADIUS),
			mob -> mob != this && mob.isAlive() && mob.getTarget() == this && mob.distanceToSqr(this) <= FLEE_SEARCH_RADIUS * FLEE_SEARCH_RADIUS
		);
	}

	/**
	 * How hard it is to get from one place to another: the distance, plus CLIMB_COST blocks for each block that has to be
	 * climbed (dropping is free). Hops are judged by it everywhere: how much closer a landing is to where it is going, or (when
	 * running from something) how much further the threat is from the landing.
	 */
	static double effort(Vec3 from, Vec3 to) {
		return from.distanceTo(to) + CLIMB_COST * Math.max(0.0, to.y - from.y);
	}

	void beginEating(ItemStack stack) {
		this.eatTicks = 0;
		this.entityData.set(DATA_EATING_FOOD, stack.copyWithCount(1));
		this.entityData.set(DATA_EATING, true);
	}

	/** One tick of a meal in progress; it ends (stopEating) when it cannot go on or the carrots are used up or it is healed. */
	void continueEating() {
		ItemStack stack = this.food.getItem(0);
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
	}

	void stopEating() {
		this.entityData.set(DATA_EATING, false);
		this.entityData.set(DATA_EATING_FOOD, ItemStack.EMPTY);
		this.eatTicks = 0;
		this.getBrain().setMemoryWithExpiry(BunnayAi.EAT_COOLDOWN, true, EAT_COOLDOWN_TICKS);
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

	// The melee behaviour swings the main hand; this swaps in the hand it should be, so the swing shows on that arm and
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

	// A hop flies on the speed it was launched with and nothing else. Without this the walking input left over from running (the
	// move control sets it as the hop starts) is added on the launch tick, on the ground where it counts for the most, and again
	// in the air with no friction to take it off, so a hop made while running lands well past where it was aimed.
	@Override
	public void travel(Vec3 input) {
		super.travel(this.isHopping() ? Vec3.ZERO : input);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		this.faceHopDirection();
		if (!this.level().isClientSide()) {
			if (this.getNavigation().isInProgress()) {
				this.shortenJumpDelay(STEP_JUMP_DELAY);
			}
			if (debugLogging && this.tickCount % 20 == 0 && (this.getTarget() != null || this.isFleeing() || this.hopReason() != null)) {
				this.debugStatus();
			}
			if (debugLogging && this.isFleeing() && this.tickCount - this.lastHopEndTick <= 40) {
				this.debugAfterHop();
			}
			if ((this.isScared() || this.isFleeing()) && this.getTarget() != null) {
				this.setTarget(null);
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
		return this.getFarmPos() != null;
	}

	/** The note block it is tuned in to: the vanilla "liked note block" memory, which forgets itself after FARM_FORGET_TICKS. */
	@Nullable BlockPos getFarmPos() {
		return this.getBrain().getMemory(MemoryModuleType.LIKED_NOTEBLOCK_POSITION).map(GlobalPos::pos).orElse(null);
	}

	boolean isNoCropsFound() {
		return this.getBrain().hasMemoryValue(BunnayAi.NO_CROPS_FOUND);
	}

	/** Forgets the note block if it has gone or the bunnay has been told to sit. */
	private void tickFarm() {
		BlockPos farm = this.getFarmPos();
		if (farm == null) {
			return;
		}
		if (this.isOrderedToSit() || (this.tickCount % 20 == 0 && !this.level().getBlockState(farm).is(Blocks.NOTE_BLOCK))) {
			this.forgetFarm();
		}
	}

	private void forgetFarm() {
		this.getBrain().eraseMemory(MemoryModuleType.LIKED_NOTEBLOCK_POSITION);
		this.getBrain().eraseMemory(BunnayAi.NO_CROPS_FOUND);
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
		BlockPos farm = this.getFarmPos();
		if (farm == null) {
			this.getBrain().setMemoryWithExpiry(MemoryModuleType.LIKED_NOTEBLOCK_POSITION, GlobalPos.of(level.dimension(), pos), FARM_FORGET_TICKS);
			// It has been given something to do: a chirp and a few notes over its head.
			this.playSound(SoundEvents.RABBIT_AMBIENT, 1.0F, 1.4F);
			level.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + this.getBbHeight() + 0.3, this.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
		} else if (farm.equals(pos)) {
			this.getBrain().setMemoryWithExpiry(MemoryModuleType.LIKED_NOTEBLOCK_POSITION, GlobalPos.of(level.dimension(), pos), FARM_FORGET_TICKS);
		}
	}

	/** How many carrots it would deliver now: all the ordinary ones in its food slot but the FARM_KEEP_CARROTS it keeps. */
	int carrotsToDeliver() {
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

	/** The way it is facing for the hop it is in, in degrees, or NaN when it is not in one (set by the hop run). */
	private float hopYaw = Float.NaN;

	void setHopYaw(float yaw) {
		this.hopYaw = yaw;
	}

	/**
	 * Keeps it facing where it is hopping, from take-off until it lands: the look and body turning that run later in the tick (for
	 * a target, say) cannot turn it away.
	 */
	private void faceHopDirection() {
		if (!this.level().isClientSide() && !Float.isNaN(this.hopYaw)) {
			this.setYRot(this.hopYaw);
			this.yBodyRot = this.hopYaw;
			this.setYHeadRot(this.hopYaw);
		}
	}

	// DEBUG: a hop run that only TopoDebugCommand uses. Remove together with that command.
	final HopRun debugHopRun = new HopRun(this);

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
	record Hop(Vec3 velocity, Vec3 landing, int airTicks) {
	}

	private record Spot(double cost, Vec3 landing) {
	}

	/** Free to hop: on the ground and not busy with anything that has it standing, swimming, riding or dancing. */
	boolean canHopNow() {
		return this.onGround() && !this.isInWater() && !this.isPassenger() && !this.isBaby() && !this.isHopping() && !this.isEating()
			&& !this.isDancing() && !this.isInSittingPose();
	}

	// ---- DEBUG-TEMP logging ----

	void debugLog(String message, Object... args) {
		if (debugLogging) {
			AllayVariantsMod.LOGGER.info("[bunnay {} t{}] {}", this.getId(), this.tickCount, String.format(message, args));
		}
	}

	private static String at(Vec3 v) {
		return String.format("(%.2f, %.2f, %.2f)", v.x, v.y, v.z);
	}

	private double lastThreatDistance = -1.0;
	private final java.util.Map<Integer, Mob> lastThreats = new java.util.HashMap<>();

	/** DEBUG-TEMP: called with whatever is after it each time it looks; says why anything that was has stopped. */
	void debugThreats(List<Mob> threats) {
		if (!debugLogging) {
			return;
		}
		java.util.Set<Integer> now = new java.util.HashSet<>();
		threats.forEach(mob -> now.add(mob.getId()));
		this.lastThreats.entrySet().removeIf(entry -> {
			if (now.contains(entry.getKey())) {
				return false;
			}
			Mob mob = entry.getValue();
			LivingEntity target = mob.getTarget();
			this.debugLog("threat DROPPED: %s alive=%s dist=%.1f | its target now: %s | it can see me: %s | I can see it: %s | its follow range %.0f",
				mob.getType().getDescriptionId(), mob.isAlive(), this.distanceTo(mob),
				target == null ? "nothing" : target.getType().getDescriptionId() + (target == this ? " (me)" : "") + " at " + String.format("%.1f", mob.distanceTo(target)),
				mob.hasLineOfSight(this), this.hasLineOfSight(mob), mob.getAttributeValue(Attributes.FOLLOW_RANGE));
			return true;
		});
		threats.forEach(mob -> this.lastThreats.putIfAbsent(mob.getId(), mob));
	}

	/**
	 * DEBUG-TEMP: every tick for 2 seconds after a hop ends while it flees: what is holding it, and when it starts moving again.
	 */
	private void debugAfterHop() {
		Brain<BunnayEntity> brain = this.getBrain();
		Vec3 motion = this.getDeltaMovement();
		this.debugLog("after hop +%d: ground=%s hopping=%s | walkTarget=%s hopTarget=%s midJump=%s hopCooldown=%d | nav inProgress=%s | speed %.3f b/tick",
			this.tickCount - this.lastHopEndTick, this.onGround(), this.isHopping(), brain.hasMemoryValue(MemoryModuleType.WALK_TARGET),
			brain.hasMemoryValue(BunnayAi.HOP_TARGET), brain.hasMemoryValue(MemoryModuleType.LONG_JUMP_MID_JUMP),
			brain.getMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS).orElse(0), this.getNavigation().isInProgress(), Math.hypot(motion.x, motion.z));
	}

	/** DEBUG-TEMP: once a second while it has something to do: its state, and how it is doing against what is after it. */
	private void debugStatus() {
		LivingEntity target = this.getTarget();
		Path path = this.getNavigation().getPath();
		Vec3 motion = this.getDeltaMovement();
		this.debugLog("status: at %s activity=%s hp=%.1f/%.1f fleeing=%s | speed=%.2f horizontal=%.2f b/tick | hop: cooldown=%d canHopNow=%s reason=%s (ground=%s water=%s eating=%s dancing=%s sitting=%s hopping=%s) | nav: inProgress=%s path=%s",
			at(this.position()), this.getBrain().getActiveNonCoreActivity().map(Object::toString).orElse("?"),
			this.getHealth(), this.getMaxHealth(), this.isFleeing(),
			this.getSpeed(), Math.hypot(motion.x, motion.z),
			this.getBrain().getMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS).orElse(0), this.canHopNow(),
			this.hopReason() == null ? "none" : this.hopReason().name(),
			this.onGround(), this.isInWater(), this.isEating(), this.isDancing(), this.isInSittingPose(), this.isHopping(),
			this.getNavigation().isInProgress(),
			path == null ? "none" : "nodes=" + path.getNodeCount() + " next=" + path.getNextNodeIndex() + " reach=" + path.canReach() + " done=" + path.isDone());
		if (this.isFleeing()) {
			List<Mob> threats = this.findThreats();
			double nearest = threats.stream().mapToDouble(this::distanceTo).min().orElse(-1.0);
			this.debugLog("flee: %d threats, nearest %.1f blocks (%s since last look), %s", threats.size(), nearest,
				this.lastThreatDistance < 0 || nearest < 0 ? "-" : String.format("%+.1f", nearest - this.lastThreatDistance),
				threats.stream().limit(3).map(mob -> mob.getType().getDescriptionId() + "@" + String.format("%.1f", this.distanceTo(mob))).toList());
			this.lastThreatDistance = nearest;
		} else {
			this.lastThreatDistance = -1.0;
		}
		if (target != null) {
			this.debugLog("target: %s dist=%.2f dy=%.2f inMeleeRange=%s lineOfSight=%s", target.getType().getDescriptionId(), this.distanceTo(target),
				target.getY() - this.getY(), this.isWithinMeleeAttackRange(target), this.getSensing().hasLineOfSight(target));
		}
	}

	/** What a hop is for: how good a landing spot is (lower is better), and the most it may cost to be worth making. */
	record HopReason(String name, ToDoubleFunction<Vec3> cost, double maxCost) {
	}

	/** To get away from a point: the landing the hardest for it to reach, and at least HOP_MIN_DISTANCE harder than where it is now. */
	private HopReason awayFrom(Vec3 point, String name) {
		double now = effort(point, this.position());
		return new HopReason(name, landing -> {
			double away = effort(point, landing);
			return away < now + HOP_MIN_DISTANCE ? Double.MAX_VALUE : -away;
		}, Double.MAX_VALUE / 2.0);
	}

	/** To get nearer a point: the landing the least effort from it, and at least HOP_MIN_DISTANCE less than from here. It does not land on it. */
	private HopReason toward(Vec3 goal, String name) {
		double now = effort(this.position(), goal);
		return new HopReason(name, landing -> landing.distanceTo(goal) < 1.2 ? Double.MAX_VALUE : effort(landing, goal), now - HOP_MIN_DISTANCE);
	}

	/**
	 * Why it would hop now, if it has a reason: it is going somewhere (it has a walk target, which is what following, tempting,
	 * chasing, fetching and wandering all set). Nothing else is a reason: it does not hop about for the sake of it. (A flee that
	 * wants to hop says so itself, with the HOP_TARGET memory.)
	 */
	@Nullable HopReason hopReason() {
		WalkTarget walk = this.getBrain().getMemory(MemoryModuleType.WALK_TARGET).orElse(null);
		if (walk != null) {
			return this.toward(walk.getTarget().currentPosition(), "toward where it is going");
		}
		return null;
	}

	/** The best hop for the reason it has, or null. */
	@Nullable Hop planReasonedHop() {
		HopReason reason = this.hopReason();
		if (reason == null) {
			return null;
		}
		List<Hop> hops = this.findHops(reason, 1);
		return hops.isEmpty() ? null : hops.get(0);
	}

	/** The best hops there are for getting away from a point, up to this many, best first. */
	List<Hop> hopsAwayFrom(Vec3 point, int wanted) {
		return this.findHops(this.awayFrom(point, "running away"), wanted);
	}

	/** A hop that makes it harder for a point to reach it, or null (for the debug command). */
	@Nullable Hop planAwayHop(Vec3 point) {
		List<Hop> hops = this.hopsAwayFrom(point, 1);
		return hops.isEmpty() ? null : hops.get(0);
	}

	/**
	 * The hops for a reason: every place it could stand within reach (see HOP_SEARCH_RADIUS) that costs no more than the reason
	 * allows, best first, keeping those with an arc that works and that cannot be walked to, until there are as many as wanted.
	 */
	private List<Hop> findHops(HopReason reason, int wanted) {
		ToDoubleFunction<Vec3> cost = reason.cost();
		double maxCost = reason.maxCost();
		Vec3 here = this.position();
		BlockPos origin = this.blockPosition();
		List<Spot> spots = new ArrayList<>();
		for (int dx = -HOP_SEARCH_RADIUS; dx <= HOP_SEARCH_RADIUS; dx++) {
			for (int dz = -HOP_SEARCH_RADIUS; dz <= HOP_SEARCH_RADIUS; dz++) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				for (int dy = -HOP_MAX_DROP; dy <= HOP_MAX_RISE; dy++) {
					Vec3 landing = this.standingSpot(origin.offset(dx, dy, dz));
					if (landing == null || Math.hypot(landing.x - here.x, landing.z - here.z) < HOP_MIN_HORIZONTAL) {
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
		List<Hop> found = new ArrayList<>();
		int walkChecks = 0;
		int arcsWorked = 0;
		int arcsTried = 0;
		int walkable = 0;
		List<String> triedLines = new ArrayList<>();
		for (int i = 0; i < Math.min(spots.size(), HOP_MAX_ARC_TRIES) && walkChecks < HOP_MAX_WALK_CHECKS && found.size() < wanted; i++) {
			Spot spot = spots.get(i);
			Vec3 landing = spot.landing();
			arcsTried++;
			Hop hop = this.jumpTo(landing);
			if (hop == null) {
				if (debugLogging) {
					triedLines.add(this.describeSpot("#" + (i + 1), landing, spot.cost(), "arc: none"));
				}
				if (i < 8) {
					this.glowSpot(landing, 0xFF2020, Math.max(2, 7 - i));
				}
				continue;
			}
			arcsWorked++;
			walkChecks++;
			boolean canWalk = this.canWalkTo(landing);
			if (debugLogging) {
				triedLines.add(this.describeSpot("#" + (i + 1), landing, spot.cost(), "arc: ok, walkable: " + canWalk));
			}
			if (!canWalk) {
				found.add(hop);
				this.glowSpot(landing, 0x20FF20, 9);
			} else {
				if (i < 8) {
					this.glowSpot(landing, 0xFFD000, Math.max(2, 7 - i));
				}
				walkable++;
			}
		}
		if (debugLogging) {
			String result = "no hop";
			if (!found.isEmpty()) {
				Hop best = found.get(0);
				Vec3 v = best.velocity();
				Vec3 landing = best.landing();
				result = String.format("%d hop(s), best to %s (+%.0f up, %.1f across), angle %.0f speed %.2f, ~%d ticks", found.size(), at(landing), landing.y - here.y,
					Math.hypot(landing.x - here.x, landing.z - here.z), Math.toDegrees(Math.atan2(v.y, Math.hypot(v.x, v.z))), v.length(), best.airTicks());
			}
			this.debugLog("hop scan (%s): %d spots in reach, %d arcs tried, %d worked, %d walkable (%d of %d walk checks used) -> %s", reason.name(), spots.size(), arcsTried,
				arcsWorked, walkable, walkChecks, HOP_MAX_WALK_CHECKS, result);
			if (spots.size() > arcsTried) {
				triedLines.add("    (" + (spots.size() - arcsTried) + " lower-ranked spots were not looked at)");
			}
			triedLines.forEach(line -> AllayVariantsMod.LOGGER.info(line));
			this.debugOwnerSpots(reason, spots, maxCost);
		}
		return found;
	}

	/**
	 * DEBUG-TEMP: marks a spot with a column of coloured dust, taller for a better rank. Green is a hop it can make, yellow a spot an
	 * arc reached but that could be walked to, red a spot no arc reaches.
	 */
	private void glowSpot(Vec3 spot, int color, int height) {
		if (debugGlow && this.level() instanceof ServerLevel server) {
			for (int i = 0; i < height; i++) {
				server.sendParticles(new net.minecraft.core.particles.DustParticleOptions(color, 1.1F), spot.x, spot.y + 0.1 + 0.3 * i, spot.z, 1, 0.04, 0.0, 0.04, 0.0);
			}
		}
	}

	/** DEBUG-TEMP: one line about a landing spot: how high, how far from the bunnay and from its owner, and what it costs. */
	private String describeSpot(String label, Vec3 landing, double cost, String note) {
		Vec3 here = this.position();
		LivingEntity owner = this.getOwner();
		return String.format("    %-4s %s %+.0f up, %.1f from the bunnay, %s from you | cost %.1f | %s", label, at(landing), landing.y - here.y,
			Math.hypot(landing.x - here.x, landing.z - here.z),
			owner == null ? "?" : String.format("%.1f", Math.hypot(landing.x - owner.getX(), landing.z - owner.getZ())) + " (" + String.format("%+.0f", landing.y - owner.getY()) + " up)",
			cost, note);
	}

	/**
	 * DEBUG-TEMP: the ground on each side of its owner (north, east, south, west), and for each whether the bunnay could land there:
	 * in its reach, where it ranks among the spots it tries, whether an arc works and whether it is walkable.
	 */
	private void debugOwnerSpots(HopReason reason, List<Spot> spots, double maxCost) {
		LivingEntity owner = this.getOwner();
		if (owner == null || owner.level() != this.level()) {
			return;
		}
		Vec3 here = this.position();
		BlockPos bunnayCell = this.blockPosition();
		BlockPos ownerCell = owner.blockPosition();
		if (debugLogging) {
			this.debugLog("  you are at %s: %.1f across and %+.1f up from the bunnay", at(owner.position()), Math.hypot(owner.getX() - here.x, owner.getZ() - here.z), owner.getY() - here.y);
		}
		String[] names = {"north", "east", "south", "west"};
		int[][] offsets = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
		for (int side = 0; side < 4; side++) {
			Vec3 spot = null;
			for (int dy : new int[] {0, 1, -1, -2}) {
				spot = this.standingSpot(ownerCell.offset(offsets[side][0], dy, offsets[side][1]));
				if (spot != null) {
					break;
				}
			}
			if (spot == null) {
				this.debugLog("    ground %s of you: nowhere to stand", names[side]);
				continue;
			}
			BlockPos cell = BlockPos.containing(spot);
			boolean inReach = Math.abs(cell.getX() - bunnayCell.getX()) <= HOP_SEARCH_RADIUS && Math.abs(cell.getZ() - bunnayCell.getZ()) <= HOP_SEARCH_RADIUS
				&& cell.getY() - bunnayCell.getY() <= HOP_MAX_RISE && bunnayCell.getY() - cell.getY() <= HOP_MAX_DROP;
			double cost = reason.cost().applyAsDouble(spot);
			int rank = -1;
			for (int i = 0; i < spots.size(); i++) {
				if (spots.get(i).landing().equals(spot)) {
					rank = i + 1;
					break;
				}
			}
			String note = String.format("ground %s of you | in the bunnay's reach: %s | passes the cost limit: %s | rank %s of %d", names[side], inReach, cost <= maxCost,
				rank < 0 ? "-" : String.valueOf(rank), spots.size());
			if (inReach && debugLogging) {
				note += " | arc: " + (this.jumpTo(spot) != null ? "ok" : "none") + " | walkable: " + this.canWalkTo(spot);
			}
			if (debugLogging) {
				AllayVariantsMod.LOGGER.info(this.describeSpot("", spot, cost, note));
			}
		}
	}

	/** The frog's test of a spot: there is no way to walk there, by a path of HOP_WALK_CHECK_LENGTH blocks. */
	private boolean canWalkTo(Vec3 landing) {
		Path path = this.getNavigation().createPath(BlockPos.containing(landing), 0, HOP_WALK_CHECK_LENGTH);
		return path != null && path.canReach();
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

	/** The hop that lands it on a spot, if it has one (see jumpTo). */
	@Nullable Hop hopTo(Vec3 landing) {
		return this.jumpTo(landing);
	}

	/** The hop that lands it on a spot, worked out as the frog does: the first of the angles (in random order) that has a launch speed and a clear arc, or null. */
	@Nullable Hop jumpTo(Vec3 landing) {
		Vec3 target = Vec3.atCenterOf(BlockPos.containing(landing));
		int[] angles = HOP_ANGLES.clone();
		for (int i = angles.length - 1; i > 0; i--) {
			int j = this.random.nextInt(i + 1);
			int swap = angles[i];
			angles[i] = angles[j];
			angles[j] = swap;
		}
		for (int angle : angles) {
			Vec3 velocity = this.launchVelocity(target, angle);
			if (velocity != null) {
				return new Hop(velocity, landing, this.flightTicks(velocity, landing.y - this.getY()));
			}
		}
		return null;
	}

	/**
	 * The frog's launch maths (LongJumpUtil.calculateJumpVectorForAngle): the speed that, launched at this angle, passes through
	 * the point HOP_AIM_SHORT short of the target along the way there, under gravity with no drag; null if no speed up to the
	 * most it can do does it or the arc is not clear. The speed it takes off at is 95% of that.
	 */
	private @Nullable Vec3 launchVelocity(Vec3 targetPos, int angleDegrees) {
		Vec3 mobPos = this.position();
		Vec3 plane = new Vec3(targetPos.x - mobPos.x, 0.0, targetPos.z - mobPos.z).normalize().scale(HOP_AIM_SHORT);
		Vec3 direction = targetPos.subtract(plane).subtract(mobPos);
		double angle = angleDegrees * Math.PI / 180.0;
		double xzAngle = Math.atan2(direction.z, direction.x);
		double r2 = direction.x * direction.x + direction.z * direction.z;
		double r = Math.sqrt(r2);
		double y = direction.y;
		double g = this.getGravity();
		double sinAngle = Math.sin(angle);
		double cosAngle = Math.cos(angle);
		double v0sqr = r2 * g / (r * Math.sin(2.0 * angle) - 2.0 * y * cosAngle * cosAngle);
		if (!(v0sqr >= 0.0) || Double.isInfinite(v0sqr)) {
			return null;
		}
		double v0 = Math.sqrt(v0sqr);
		if (v0 > HOP_MAX_SPEED) {
			return null;
		}
		double v0r = v0 * cosAngle;
		double v0y = v0 * sinAngle;
		int samples = Math.min(Mth.ceil(r / v0r) * 2, 200);
		double ri = 0.0;
		Vec3 previous = null;
		for (int i = 0; i < samples - 1; i++) {
			ri += r / samples;
			double yi = sinAngle / cosAngle * ri - ri * ri * g / (2.0 * v0sqr * cosAngle * cosAngle);
			Vec3 sample = new Vec3(mobPos.x + ri * Math.cos(xzAngle), mobPos.y + yi, mobPos.z + ri * Math.sin(xzAngle));
			if (previous != null && !this.clearBetween(previous, sample)) {
				return null;
			}
			previous = sample;
		}
		return new Vec3(v0r * Math.cos(xzAngle), v0y, v0r * Math.sin(xzAngle)).scale(HOP_SPEED_SCALE);
	}

	/** The frog's LongJumpUtil.isClearTransition, with the sample box instead of its own: nothing in the way as the box moves from one point to the next. */
	private boolean clearBetween(Vec3 from, Vec3 to) {
		Vec3 direction = to.subtract(from);
		int checks = Mth.ceil(direction.length() / HOP_SAMPLE_BOX);
		Vec3 step = direction.normalize();
		Vec3 point = from;
		double half = HOP_SAMPLE_BOX / 2.0;
		for (int i = 0; i < checks; i++) {
			point = i == checks - 1 ? to : point.add(step.scale(HOP_SAMPLE_BOX * 0.9));
			if (!this.level().noCollision(this, new AABB(point.x - half, point.y, point.z - half, point.x + half, point.y + HOP_SAMPLE_BOX, point.z + half))) {
				return false;
			}
		}
		return true;
	}

	/** How many ticks a launch spends in the air before its feet get down to this height above where they are now (for the hop clip to fit). */
	private int flightTicks(Vec3 velocity, double rise) {
		double height = 0.0;
		double lift = velocity.y;
		double gravity = this.getGravity();
		for (int tick = 1; tick <= 60; tick++) {
			height += lift;
			lift -= gravity;
			if (lift < 0.0 && height <= rise) {
				return Math.max(tick, 6);
			}
		}
		return 60;
	}

	@Override
	public void tick() {
		super.tick();
		this.faceHopDirection();
		if (this.level().isClientSide()) {
			this.hopAnimationState.animateWhen(this.isHopping(), this.tickCount);
			this.tickIdleAnimation();
			this.readyProgressO = this.readyProgress;
			boolean ready = this.isAggressive() && this.isHoldingWeapon();
			this.readyProgress = Mth.clamp(this.readyProgress + (ready ? READY_RISE : -READY_FALL), 0.0F, 1.0F);
			this.eatProgressO = this.eatProgress;
			this.eatProgress = Mth.clamp(this.eatProgress + (this.isEating() ? EAT_RISE : -EAT_FALL), 0.0F, 1.0F);
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
			.add(Attributes.ATTACK_DAMAGE, 2.0)
			// Swimming speed. A land mob moves at a crawl in water (about 1.6 blocks a second for this one); the water movement
			// efficiency attribute (what Depth Strider sets, 0 to 1) closes that gap. 0.4 is about 2.8 times as fast, 4.5 blocks a
			// second, roughly a walking pace; 0.2 is 2.1 times and 0.6 is 3.4 times.
			.add(Attributes.WATER_MOVEMENT_EFFICIENCY, 0.4);
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
		this.debugLog("hurt by %s (%s) for %.1f at %s%s", source.getEntity() == null ? "nothing" : source.getEntity().getType().getDescriptionId(),
			source.type().msgId(), damage, at(this.position()), this.isHopping() ? " WHILE HOPPING" : "");
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
	 * Carries out one hop: the launch at once (facing the way it will go, with friction off), the flight, and the landing
	 * (silent, which puts friction back and takes some of the sideways speed off it) after which it is free to move on that
	 * very tick. The hop behaviours own one of these and run it each tick. The animation is held while the flag is set.
	 */
	static final class HopRun {
		private final BunnayEntity bunnay;
		private Hop hop;
		private int ticks;
		boolean running;
		// DEBUG-TEMP: where it started and how it flew, to explain a miss.
		private Vec3 launchFrom = Vec3.ZERO;
		private final List<String> trace = new ArrayList<>();

		HopRun(BunnayEntity bunnay) {
			this.bunnay = bunnay;
		}

		void start(Hop hop) {
			this.hop = hop;
			this.ticks = 0;
			this.trace.clear();
			this.running = true;
			this.bunnay.getNavigation().stop();
			this.bunnay.entityData.set(DATA_HOP_AIR_TICKS, hop.airTicks());
			this.bunnay.entityData.set(DATA_HOPPING, true);
			this.bunnay.debugLog("hop START from %s toward %s, launch velocity %s, ~%d ticks", at(this.bunnay.position()), at(hop.landing()), at(hop.velocity()), hop.airTicks());
		}

		void tick() {
			if (!this.running) {
				return;
			}
			this.ticks++;
			if (debugLogging && this.ticks > 1) {
				int t = this.ticks - 1;
				Vec3 v = this.hop.velocity();
				Vec3 now = this.bunnay.position();
				Vec3 plan = this.launchFrom.add(v.x * t, v.y * t - this.bunnay.getGravity() * t * (t - 1) / 2.0, v.z * t);
				this.trace.add(String.format("    after %d: at %s expected %s (off %+.2f %+.2f %+.2f) ground=%s", t, at(now), at(plan), now.x - plan.x, now.y - plan.y,
					now.z - plan.z, this.bunnay.onGround()));
			}
			if (this.ticks == 1) {
				// Off at once, facing where it is going.
				Vec3 velocity = this.hop.velocity();
				float yaw = (float) Math.toDegrees(Math.atan2(velocity.z, velocity.x)) - 90.0F;
				this.bunnay.setYRot(yaw);
				this.bunnay.yBodyRot = yaw;
				this.bunnay.setYHeadRot(yaw);
				this.bunnay.setHopYaw(yaw);
				this.bunnay.setDiscardFriction(true);
				this.bunnay.setDeltaMovement(velocity);
				this.launchFrom = this.bunnay.position();
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
			this.bunnay.setHopYaw(Float.NaN);
			this.bunnay.setDiscardFriction(false);
			this.bunnay.entityData.set(DATA_HOPPING, false);
			this.bunnay.lastHopEndTick = this.bunnay.tickCount;
			double off = this.bunnay.position().distanceTo(this.hop.landing());
			this.bunnay.debugLog("hop END at %s after %d ticks (aimed at %s, %.2f away, %.2f across %+.2f up)", at(this.bunnay.position()), this.ticks, at(this.hop.landing()), off,
				Math.hypot(this.bunnay.getX() - this.hop.landing().x, this.bunnay.getZ() - this.hop.landing().z), this.bunnay.getY() - this.hop.landing().y);
			if (debugLogging && off > 1.0) {
				this.bunnay.debugLog("hop MISSED by %.2f, tick by tick against the arc it launched on:", off);
				this.trace.forEach(line -> AllayVariantsMod.LOGGER.info(line));
			}
			this.bunnay.startHopCooldown();
		}
	}

	/** Calm and free to go about farm business: tame, not sitting, dancing, eating, hopping, riding or fighting. */
	boolean freeForFarmWork() {
		LivingEntity target = this.getTarget();
		return this.isTame() && !this.isBaby() && !this.isOrderedToSit() && !this.isDancing() && !this.isEating() && !this.isHopping()
			&& !this.isPassenger() && !this.isFleeing() && (target == null || !target.isAlive());
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

}
