package dev.jett.topomod.companion.entity;

import java.util.Comparator;
import java.util.List;
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
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import dev.jett.topomod.companion.CompanionMod;
import dev.jett.topomod.companion.menu.BunnayMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

// Bunnay: a bunny-allay cross that can be tamed with carrots. It follows its owner and fights like a wolf; holding
// weapons, sitting and an item screen will be built on this the way they were for the topo.
public class BunnayEntity extends TamableAnimal {
	private static final double WILD_HEALTH = 16.0;
	private static final double TAME_HEALTH = 80.0;

	// The big hop: a crouch, a launch, a couple of blocks of height, and a squashy landing. The keyframe clip is
	// BunnayAnimation.BIG_HOP; these numbers have to stay in step with it (see the comment there).
	/** Length of the whole hop in ticks (the clip is 1.3 seconds). */
	private static final int BIG_HOP_TICKS = 26;
	/** Tick of the hop that the legs push off (the crouch lasts this long). */
	private static final int BIG_HOP_TAKEOFF_TICK = 4;
	/** Upward speed at takeoff; 0.55 gives about 2 blocks of height and 15 ticks in the air. */
	private static final double BIG_HOP_LAUNCH_SPEED = 0.55;
	/** Forward speed at takeoff, which carries it about 2.5 blocks: about 5.45 blocks per point of speed (see LEAP_BLOCKS_PER_SPEED). */
	private static final double BIG_HOP_FORWARD_SPEED = 0.45;
	/** A walking bunnay starts a big hop with a 1-in-this chance each time its goals are checked (about every 6 seconds). */
	private static final int BIG_HOP_ODDS = 40;
	/** Minimum ticks between big hops. */
	private static final int BIG_HOP_COOLDOWN = 100;

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
	private static final Identifier WEAPON_DAMAGE_ID = CompanionMod.id("weapon_damage");
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

	// The leap: from a distance, it can spring at its target with the same crouch and big hop animation, to close the gap.
	// It is only a way of getting there; the landing does no damage of its own.
	/** The target has to be at least this far away (in blocks, along the ground) for a leap to be worth it, and no further than the max. */
	private static final double LEAP_MIN_DISTANCE = 5.0;
	private static final double LEAP_MAX_DISTANCE = 11.0;
	/** Minimum ticks between leaps (8 seconds). */
	private static final int LEAP_COOLDOWN = 160;
	/** When it can leap, it decides to with a 1-in-this chance each time its goals are checked (so within a second or so). */
	private static final int LEAP_ODDS = 4;
	/** How far short of the target it aims to land, in blocks, so it does not run into it. */
	private static final double LEAP_STOP_SHORT = 1.0;
	/**
	 * Blocks covered by the time it lands, per point of forward launch speed. The speed is cut to 0.546 of itself on the
	 * launch tick (the friction of the block it is leaving), then to 0.91 of itself every tick in the air, over the 15
	 * ticks the hop spends there: 1 + 0.546 * (1 + 0.91 + 0.91^2 + ... for 14 ticks) is about 5.45.
	 */
	private static final double LEAP_BLOCKS_PER_SPEED = 5.45;
	/** Fast enough to cover the longest leap: (11 - 1) / 5.45 is about 1.83. */
	private static final double LEAP_MAX_SPEED = 1.85;

	// Retreating to heal. A tamed bunnay in a fight that is hurt down to RETREAT_BELOW_FRACTION of its health, and has a
	// carrot to eat, breaks off: it springs well away from the enemies (the big hop, aimed at open ground away from them),
	// drops the fight, and stands still eating until it is about healed, without being drawn back in. Then it goes back to
	// the nearest enemy that is after its owner.
	private static final float RETREAT_BELOW_FRACTION = 0.35F;
	/** It only breaks off a fight it is really in: it has a target, or was hurt by a mob this recently (in ticks). */
	private static final int RETREAT_FIGHT_TICKS = 100;
	/** How far the escape jump aims to land, in blocks (the forward speed is this over LEAP_BLOCKS_PER_SPEED). */
	private static final double RETREAT_JUMP_DISTANCE = 9.0;
	/** An enemy that is after it, or this close (in blocks), while it recovers makes it spring away again... */
	private static final double RETREAT_THREAT_DISTANCE = 5.0;
	/** ...up to this many jumps in one retreat. */
	private static final int RETREAT_MAX_JUMPS = 3;
	/** The most ticks a retreat can last (30 seconds), so it can never get stuck in one. */
	private static final int RETREAT_MAX_TICKS = 600;
	/** How far it looks for enemies to flee from, and to turn back on at the end, in blocks. */
	private static final double RETREAT_SEARCH_RADIUS = 14.0;
	/** The wait after a retreat before it can start another, in ticks. */
	private static final int RETREAT_COOLDOWN = 60;
	/** How far from it enemies are made to forget it, in blocks (further than they could be chasing it from). */
	private static final double RETREAT_FORGET_RADIUS = 32.0;
	/** While it retreats, enemies are made to forget it again this often, in ticks. */
	private static final int RETREAT_FORGET_INTERVAL = 5;

	private static final java.lang.reflect.Field TARGET_SELECTOR_FIELD = selectorField("targetSelector");

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

	private static final EntityDataAccessor<Boolean> DATA_BIG_HOPPING = SynchedEntityData.defineId(BunnayEntity.class, EntityDataSerializers.BOOLEAN);

	/** Drives the big hop animation on the client. */
	public final AnimationState bigHopAnimationState = new AnimationState();
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

	// Retreating to heal (server side only).
	private boolean retreating;
	private int retreatTicks;
	private int retreatJumps;
	private int retreatCooldown;
	/** The hand it attacked with last. With a weapon in each hand it swings them in turn. */
	private InteractionHand attackHand = InteractionHand.MAIN_HAND;

	// Dancing to a jukebox, the way the allay does: it listens for the jukebox game events, remembers which jukebox
	// is playing, and stops dancing when the music stops or the jukebox is gone or too far away.
	private final DynamicGameEventListener<JukeboxListener> dynamicJukeboxListener;
	private @Nullable BlockPos jukeboxPos;
	private int bigHopCooldown;
	private int leapCooldown;
	/** Ticks left of the big hop animation; the leap ends its goal on landing but lets the clip play out. */
	private int bigHopAnimationTicks;

	public BunnayEntity(EntityType<? extends BunnayEntity> type, Level level) {
		super(type, level);
		this.dynamicJukeboxListener = new DynamicGameEventListener<>(new JukeboxListener(
			this, new EntityPositionSource(this, this.getEyeHeight()), GameEvent.JUKEBOX_PLAY.value().notificationRadius()));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_BIG_HOPPING, false);
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
			&& (alreadyEating || this.retreating || this.tickCount - this.getLastHurtByMobTimestamp() > EAT_SAFE_TICKS)
			&& this.getMaxHealth() - this.getHealth() >= (alreadyEating ? EAT_KEEP_GOING_MISSING_HEALTH : EAT_MIN_MISSING_HEALTH)
			&& this.onGround()
			&& !this.isInWater()
			&& !this.isBigHopping()
			&& !this.isDancing()
			&& !this.isPassenger();
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

	private static java.lang.reflect.Field selectorField(String name) {
		try {
			java.lang.reflect.Field field = Mob.class.getDeclaredField(name);
			field.setAccessible(true);
			return field;
		} catch (ReflectiveOperationException e) {
			return null;
		}
	}

	/**
	 * Stops the target goals a mob is running (the one that picked its target). Clearing the mob's target is not enough: a
	 * running target goal keeps its own copy of the target, and the next tick it sees that the mob has none and puts that
	 * copy back. Stopping the goal clears that copy too (TargetGoal.stop), and it does not start again unless the mob
	 * is hurt again or finds something else to go for.
	 */
	private static void stopRunningTargetGoals(Mob mob) {
		if (TARGET_SELECTOR_FIELD == null) {
			return;
		}
		try {
			for (WrappedGoal wrapped : ((GoalSelector) TARGET_SELECTOR_FIELD.get(mob)).getAvailableGoals()) {
				if (wrapped.isRunning() && wrapped.getGoal() instanceof TargetGoal) {
					wrapped.stop();
				}
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			CompanionMod.LOGGER.warn("Could not stop target goals on {}", mob, e);
		}
	}

	/** Hurt enough to want to get out, with a carrot to eat once it has, and in a fight right now. */
	private boolean wantsToRetreat() {
		LivingEntity target = this.getTarget();
		boolean inFight = (target != null && target.isAlive()) || this.tickCount - this.getLastHurtByMobTimestamp() <= RETREAT_FIGHT_TICKS;
		return this.isTame() && !this.isBaby() && !this.isOrderedToSit() && !this.isDancing()
			&& this.getHealth() <= this.getMaxHealth() * RETREAT_BELOW_FRACTION
			&& isCarrot(this.food.getItem(0))
			&& inFight;
	}

	/** Starts a retreat when it is time to, and ends it when it has healed, run out of carrots, or taken too long. */
	private void tickRetreat() {
		if (this.retreatCooldown > 0) {
			this.retreatCooldown--;
		}
		if (!this.retreating) {
			if (this.retreatCooldown <= 0 && this.wantsToRetreat()) {
				this.startRetreat();
			}
			return;
		}

		this.retreatTicks++;
		// A mob can pick it up again (a group alerted by one that was hit, an anger that runs on its own timer), so keep
		// making them forget it for as long as it recovers.
		if (this.retreatTicks % RETREAT_FORGET_INTERVAL == 0) {
			this.makeEnemiesForget();
		}
		boolean healed = this.getMaxHealth() - this.getHealth() < EAT_KEEP_GOING_MISSING_HEALTH;
		boolean outOfFood = !isCarrot(this.food.getItem(0)) && !this.isEating();
		if (healed || outOfFood || this.retreatTicks > RETREAT_MAX_TICKS || this.isOrderedToSit() || !this.isAlive()) {
			this.endRetreat(healed);
		}
	}

	private void startRetreat() {
		this.retreating = true;
		this.retreatTicks = 0;
		this.retreatJumps = 0;
		this.eatCooldown = 0;
		this.dropAggro();
	}

	/** Back to normal. If it healed, it turns on an enemy that is after its owner (or near them) to rejoin the fight. */
	private void endRetreat(boolean healed) {
		this.retreating = false;
		this.retreatCooldown = RETREAT_COOLDOWN;
		if (healed && this.level() instanceof ServerLevel level) {
			LivingEntity owner = this.getOwner();
			Vec3 center = owner != null ? owner.position() : this.position();
			List<Mob> foes = level.getEntitiesOfClass(
				Mob.class,
				new AABB(center, center).inflate(RETREAT_SEARCH_RADIUS),
				mob -> mob != this && mob instanceof Enemy && mob.isAlive()
					&& (owner == null || (this.wantsToAttack(mob, owner) && (mob.getTarget() == owner || mob.distanceToSqr(owner) < 64.0)))
			);
			foes.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(center)));
			if (!foes.isEmpty()) {
				this.setTarget(foes.get(0));
			}
		}
	}

	/** The enemies that are fighting it or are close to it, for working out which way is away. */
	private List<Mob> nearbyThreats() {
		return this.level().getEntitiesOfClass(
			Mob.class,
			this.getBoundingBox().inflate(RETREAT_SEARCH_RADIUS),
			mob -> mob != this && mob instanceof Enemy && mob.isAlive() && (mob.getTarget() == this || mob.distanceToSqr(this) < 64.0)
		);
	}

	/** It forgets who it was fighting, and everything that was after it forgets it. */
	private void dropAggro() {
		this.setTarget(null);
		this.setLastHurtByMob(null);
		this.makeEnemiesForget();
	}

	/**
	 * Every mob nearby that is targeting it, or remembers it as what hurt it (which is what makes a mob hit back), lets go.
	 * They only go after it again if it hits them again.
	 */
	private void makeEnemiesForget() {
		for (Mob mob : this.level().getEntitiesOfClass(
			Mob.class,
			this.getBoundingBox().inflate(RETREAT_FORGET_RADIUS),
			m -> m != this && (m.getTarget() == this || m.getLastHurtByMob() == this)
		)) {
			if (mob.getTarget() == this) {
				stopRunningTargetGoals(mob);
				mob.setTarget(null);
				mob.getNavigation().stop();
			}
			if (mob.getLastHurtByMob() == this) {
				mob.setLastHurtByMob(null);
			}
		}
	}

	// While it retreats it takes no new target: neither its owner's fight nor a mob that hurt it pulls it back in.
	@Override
	public void setTarget(@Nullable LivingEntity target) {
		if (this.retreating && target != null) {
			return;
		}
		super.setTarget(target);
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
			this.tickRetreat();
			this.tickEating();
			if (this.giftCooldown > 0) {
				this.giftCooldown--;
			}
		}
		if (!this.level().isClientSide() && this.isDancing() && this.shouldStopDancing() && this.tickCount % 20 == 0) {
			this.jukeboxPos = null;
			this.setDancing(false);
		}

	}

	public boolean isBigHopping() {
		return this.entityData.get(DATA_BIG_HOPPING);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.bigHopAnimationState.animateWhen(this.isBigHopping(), this.tickCount);
			this.tickIdleAnimation();
			this.readyProgressO = this.readyProgress;
			boolean ready = this.isAggressive() && this.isHoldingWeapon();
			this.readyProgress = Mth.clamp(this.readyProgress + (ready ? READY_RISE : -READY_FALL), 0.0F, 1.0F);
			this.eatProgressO = this.eatProgress;
			this.eatProgress = Mth.clamp(this.eatProgress + (this.isEating() ? EAT_RISE : -EAT_FALL), 0.0F, 1.0F);
		} else {
			if (this.bigHopCooldown > 0) {
				this.bigHopCooldown--;
			}
			if (this.leapCooldown > 0) {
				this.leapCooldown--;
			}
			if (this.bigHopAnimationTicks > 0 && --this.bigHopAnimationTicks == 0) {
				this.entityData.set(DATA_BIG_HOPPING, false);
			}
		}
	}

	/** Starts the idle clip when its timer runs out, if standing still; stops it if the bunnay moves off. */
	private void tickIdleAnimation() {
		boolean still = this.onGround() && !this.isInWater() && !this.isBigHopping() && !this.isDancing() && this.hurtTime == 0
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
		this.goalSelector.addGoal(0, new RetreatGoal(this));
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(1, new TamableAnimalPanicGoal(1.5, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
		// Sitting is priority 2, as it is for a wolf: below floating and panicking, above everything else. What used to be
		// at 2 and below is one number lower in the list for it (the melee goal is 3, and so on).
		this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		// The leap is priority 1 so it can interrupt the chase (the melee goal is 3), and it cannot be interrupted itself.
		this.goalSelector.addGoal(1, new LeapGoal(this));
		this.goalSelector.addGoal(3, new DanceGoal(this));
		this.goalSelector.addGoal(3, new BigHopGoal(this));
		this.goalSelector.addGoal(3, new BunnayMeleeGoal(this));
		this.goalSelector.addGoal(4, new TemptGoal(this, 1.0, this::isFood, false));
		this.goalSelector.addGoal(4, new GiveCarrotsGoal(this));
		this.goalSelector.addGoal(5, new HarvestCarrotsGoal(this));
		// Follows its owner like a wolf: starts from 10 blocks away and stops 2 blocks from them.
		this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

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

	/** Health a carrot or golden carrot restores: a carrot 6 (3 hearts), a golden carrot 30 (15 hearts). */
	private static float healAmount(ItemStack food) {
		return food.is(Items.GOLDEN_CARROT) ? 30.0F : 6.0F;
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
	 * Now and then, while walking somewhere, stops to do a big hop. The model's keyframe clip plays while the entity
	 * is flagged as hopping; this goal times the launch to match it and can't be interrupted until the hop is over.
	 */
	private static final class BigHopGoal extends Goal {
		private final BunnayEntity bunnay;
		private int ticks;

		BigHopGoal(BunnayEntity bunnay) {
			this.bunnay = bunnay;
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (!this.bunnay.onGround() || this.bunnay.isInWater() || this.bunnay.isPassenger() || this.bunnay.isBaby() || this.bunnay.isDancing() || this.bunnay.isInSittingPose()) {
				return false;
			}
			LivingEntity target = this.bunnay.getTarget();
			return this.bunnay.bigHopCooldown <= 0
				&& (target == null || !target.isAlive())
				&& this.bunnay.getNavigation().isInProgress()
				&& this.bunnay.getRandom().nextInt(BIG_HOP_ODDS) == 0;
		}

		@Override
		public boolean canContinueToUse() {
			return this.ticks < BIG_HOP_TICKS;
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
			this.ticks = 0;
			this.bunnay.getNavigation().stop();
			this.bunnay.entityData.set(DATA_BIG_HOPPING, true);
		}

		@Override
		public void tick() {
			this.ticks++;
			if (this.ticks == BIG_HOP_TAKEOFF_TICK) {
				// Forward is the way the bunnay is facing (Minecraft's forward is (-sin yaw, cos yaw)).
				double yaw = Math.toRadians(this.bunnay.getYRot());
				this.bunnay.setDeltaMovement(new Vec3(-Math.sin(yaw) * BIG_HOP_FORWARD_SPEED, BIG_HOP_LAUNCH_SPEED, Math.cos(yaw) * BIG_HOP_FORWARD_SPEED));
				this.bunnay.needsSync = true;
				this.bunnay.playSound(SoundEvents.RABBIT_JUMP, 1.0F, 1.0F);
			}
		}

		@Override
		public void stop() {
			this.bunnay.entityData.set(DATA_BIG_HOPPING, false);
			this.bunnay.bigHopCooldown = BIG_HOP_COOLDOWN;
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

	/**
	 * Springs at the target from a distance, with the big hop's crouch, flip and landing. The crouch lasts until the
	 * takeoff tick, facing the target; then it launches with the forward speed it takes to land about a block short.
	 * The goal ends as soon as it lands, so the chase resumes right away.
	 */
	private static final class LeapGoal extends Goal {
		private final BunnayEntity bunnay;
		private int ticks;

		LeapGoal(BunnayEntity bunnay) {
			this.bunnay = bunnay;
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = this.bunnay.getTarget();
			if (target == null || !target.isAlive() || this.bunnay.leapCooldown > 0 || this.bunnay.isBigHopping()) {
				return false;
			}
			if (!this.bunnay.onGround() || this.bunnay.isInWater() || this.bunnay.isPassenger() || this.bunnay.isBaby() || this.bunnay.isDancing() || this.bunnay.isInSittingPose()) {
				return false;
			}
			double dx = target.getX() - this.bunnay.getX();
			double dz = target.getZ() - this.bunnay.getZ();
			double distance = Math.sqrt(dx * dx + dz * dz);
			return distance >= LEAP_MIN_DISTANCE && distance <= LEAP_MAX_DISTANCE
				&& Math.abs(target.getY() - this.bunnay.getY()) <= 3.0
				&& this.bunnay.hasLineOfSight(target)
				&& this.bunnay.getRandom().nextInt(LEAP_ODDS) == 0;
		}

		@Override
		public boolean canContinueToUse() {
			// Done on landing, so the chase starts again at once; the animation's squash and recovery play out by themselves.
			boolean landed = this.ticks > BIG_HOP_TAKEOFF_TICK + 2 && this.bunnay.onGround();
			return this.ticks < BIG_HOP_TICKS && !landed;
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
			this.ticks = 0;
			this.bunnay.getNavigation().stop();
			this.bunnay.entityData.set(DATA_BIG_HOPPING, true);
			this.bunnay.bigHopAnimationTicks = BIG_HOP_TICKS;
		}

		@Override
		public void tick() {
			this.ticks++;
			LivingEntity target = this.bunnay.getTarget();
			if (target != null && this.ticks <= BIG_HOP_TAKEOFF_TICK) {
				// Crouching down, turn to face it.
				this.bunnay.getLookControl().setLookAt(target, 60.0F, 60.0F);
			}
			if (this.ticks != BIG_HOP_TAKEOFF_TICK) {
				return;
			}

			double forwardX;
			double forwardZ;
			double speed;
			if (target != null && target.isAlive()) {
				double dx = target.getX() - this.bunnay.getX();
				double dz = target.getZ() - this.bunnay.getZ();
				double distance = Math.max(Math.sqrt(dx * dx + dz * dz), 1.0E-4);
				forwardX = dx / distance;
				forwardZ = dz / distance;
				speed = Math.min(Math.max(distance - LEAP_STOP_SHORT, 0.0) / LEAP_BLOCKS_PER_SPEED, LEAP_MAX_SPEED);
				// Face the way it is going, so it does not leap sideways.
				float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
				this.bunnay.setYRot(yaw);
				this.bunnay.yBodyRot = yaw;
				this.bunnay.setYHeadRot(yaw);
			} else {
				// The target died or went away mid-crouch: just hop the way it is facing.
				double yaw = Math.toRadians(this.bunnay.getYRot());
				forwardX = -Math.sin(yaw);
				forwardZ = Math.cos(yaw);
				speed = BIG_HOP_FORWARD_SPEED;
			}
			this.bunnay.setDeltaMovement(new Vec3(forwardX * speed, BIG_HOP_LAUNCH_SPEED, forwardZ * speed));
			this.bunnay.needsSync = true;
			this.bunnay.playSound(SoundEvents.RABBIT_JUMP, 1.0F, 1.0F);
		}

		@Override
		public void stop() {
			// The animation flag is cleared by the entity's timer, not here, so the clip is not cut short.
			this.bunnay.leapCooldown = LEAP_COOLDOWN;
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
	 * Breaks off a fight to heal. First it springs away from the enemies (the big hop, aimed at open ground away from
	 * them); then it holds still, which is when tickEating gets to eat its carrots. If an enemy that is after it gets
	 * close while it recovers, it springs away again, a few times at most. If it is in water it can do none of that, so
	 * it swims for the nearest shore first (away from the enemies if it can), and the rest follows once it is out.
	 */
	private static final class RetreatGoal extends Goal {
		private final BunnayEntity bunnay;
		private boolean jumping;
		private int jumpTicks;
		private Vec3 jumpDirection = Vec3.ZERO;

		RetreatGoal(BunnayEntity bunnay) {
			this.bunnay = bunnay;
			// No JUMP flag: that is what FloatGoal uses to keep it afloat, and it must keep running while it swims out.
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return this.bunnay.retreating;
		}

		@Override
		public boolean canContinueToUse() {
			return this.bunnay.retreating;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			this.jumping = false;
			this.bunnay.getNavigation().stop();
		}

		@Override
		public void tick() {
			if (this.jumping) {
				this.tickJump();
				return;
			}
			if (this.bunnay.isInWater()) {
				this.swimToShore();
				return;
			}
			// Holding still, so that it can eat.
			this.bunnay.getNavigation().stop();
			if (this.bunnay.retreatJumps >= RETREAT_MAX_JUMPS || !this.canJump()) {
				return;
			}
			if (this.bunnay.retreatJumps == 0 || (this.bunnay.tickCount % 5 == 0 && this.threatenedNow())) {
				this.startJump();
			}
		}

		/** In water it can neither hop nor eat: head for the nearest dry ground, preferring the way away from the enemies. */
		private void swimToShore() {
			if (this.bunnay.getNavigation().isDone() || this.bunnay.tickCount % 20 == 0) {
				Vec3 shore = this.findShore();
				if (shore != null && !this.bunnay.getNavigation().moveTo(shore.x, shore.y, shore.z, 1.3)) {
					// No path through the water: just head straight there.
					this.bunnay.getMoveControl().setWantedPosition(shore.x, shore.y, shore.z, 1.3);
				}
			}
		}

		/** The nearest safe dry spot in a ring around it, with the ones away from the enemies counting as a few blocks nearer. */
		private Vec3 findShore() {
			Vec3 here = this.bunnay.position();
			Vec3 threatCenter = Vec3.ZERO;
			List<Mob> threats = this.bunnay.nearbyThreats();
			for (Mob mob : threats) {
				threatCenter = threatCenter.add(mob.position());
			}
			Vec3 away = Vec3.ZERO;
			if (!threats.isEmpty()) {
				away = here.subtract(threatCenter.scale(1.0 / threats.size())).multiply(1.0, 0.0, 1.0);
				away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
			}
			Vec3 best = null;
			double bestScore = Double.MAX_VALUE;
			for (int i = 0; i < 16; i++) {
				double radians = Math.PI * 2.0 * i / 16.0;
				Vec3 dir = new Vec3(Math.cos(radians), 0.0, Math.sin(radians));
				for (double distance : new double[]{2.0, 3.0, 4.5, 6.0, 8.0, 10.0, 12.0}) {
					Vec3 landing = this.landingAt(here.add(dir.scale(distance)));
					if (landing == null) {
						continue;
					}
					double score = distance - 3.0 * dir.dot(away);
					if (score < bestScore) {
						bestScore = score;
						best = landing;
					}
					break;
				}
			}
			if (best == null) {
				LivingEntity owner = this.bunnay.getOwner();
				return owner != null ? owner.position() : null;
			}
			return best;
		}

		private boolean canJump() {
			return this.bunnay.onGround() && !this.bunnay.isInWater() && !this.bunnay.isBigHopping() && !this.bunnay.isPassenger()
				&& !this.bunnay.isInSittingPose() && !this.bunnay.isDancing();
		}

		/** An enemy is after it, or close enough that it cannot eat in peace. */
		private boolean threatenedNow() {
			for (Mob mob : this.bunnay.nearbyThreats()) {
				if (mob.getTarget() == this.bunnay || mob.distanceToSqr(this.bunnay) < RETREAT_THREAT_DISTANCE * RETREAT_THREAT_DISTANCE) {
					return true;
				}
			}
			return false;
		}

		private void startJump() {
			this.jumping = true;
			this.jumpTicks = 0;
			this.bunnay.retreatJumps++;
			if (this.bunnay.isEating()) {
				this.bunnay.stopEating();
			}
			this.jumpDirection = this.chooseDirection();
			this.bunnay.getNavigation().stop();
			this.bunnay.entityData.set(DATA_BIG_HOPPING, true);
			this.bunnay.bigHopAnimationTicks = BIG_HOP_TICKS;
		}

		private void tickJump() {
			this.jumpTicks++;
			// Crouching, turn to face the way it will go.
			if (this.jumpTicks <= BIG_HOP_TAKEOFF_TICK) {
				float yaw = (float) Math.toDegrees(Math.atan2(this.jumpDirection.z, this.jumpDirection.x)) - 90.0F;
				this.bunnay.setYRot(yaw);
				this.bunnay.yBodyRot = yaw;
				this.bunnay.setYHeadRot(yaw);
			}
			if (this.jumpTicks == BIG_HOP_TAKEOFF_TICK) {
				double speed = Math.min(RETREAT_JUMP_DISTANCE / LEAP_BLOCKS_PER_SPEED, LEAP_MAX_SPEED);
				this.bunnay.setDeltaMovement(new Vec3(this.jumpDirection.x * speed, BIG_HOP_LAUNCH_SPEED, this.jumpDirection.z * speed));
				this.bunnay.needsSync = true;
				this.bunnay.playSound(SoundEvents.RABBIT_JUMP, 1.0F, 1.0F);
				// A puff of smoke where it leaves, and everything that was after it loses it.
				if (this.bunnay.level() instanceof ServerLevel level) {
					level.sendParticles(ParticleTypes.POOF, this.bunnay.getX(), this.bunnay.getY() + 0.3, this.bunnay.getZ(), 12, 0.3, 0.1, 0.3, 0.05);
				}
				this.bunnay.dropAggro();
			}
			boolean landed = this.jumpTicks > BIG_HOP_TAKEOFF_TICK + 2 && this.bunnay.onGround();
			if (landed || this.jumpTicks >= BIG_HOP_TICKS) {
				this.jumping = false;
			}
		}

		/**
		 * Which way to jump: away from the enemies, but swung to either side if the way straight away has no safe place to land,
		 * and among the safe ways the one that ends nearest its owner.
		 */
		private Vec3 chooseDirection() {
			Vec3 here = this.bunnay.position();
			Vec3 threatCenter = Vec3.ZERO;
			List<Mob> threats = this.bunnay.nearbyThreats();
			for (Mob mob : threats) {
				threatCenter = threatCenter.add(mob.position());
			}
			Vec3 away;
			if (threats.isEmpty()) {
				double yaw = Math.toRadians(this.bunnay.getYRot());
				away = new Vec3(Math.sin(yaw), 0.0, -Math.cos(yaw));
			} else {
				away = here.subtract(threatCenter.scale(1.0 / threats.size())).multiply(1.0, 0.0, 1.0);
				away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
			}

			LivingEntity owner = this.bunnay.getOwner();
			Vec3 best = null;
			double bestScore = Double.MAX_VALUE;
			for (double degrees : new double[]{0, 30, -30, 60, -60, 90, -90, 130, -130}) {
				double radians = Math.toRadians(degrees);
				Vec3 dir = new Vec3(away.x * Math.cos(radians) - away.z * Math.sin(radians), 0.0, away.x * Math.sin(radians) + away.z * Math.cos(radians));
				Vec3 landing = here.add(dir.scale(RETREAT_JUMP_DISTANCE));
				if (!this.pathClear(here, dir) || !this.safeLanding(landing)) {
					continue;
				}
				double score = owner != null ? landing.distanceToSqr(owner.position()) : Math.abs(degrees);
				if (score < bestScore) {
					bestScore = score;
					best = dir;
				}
			}
			return best != null ? best : away;
		}

		/** Nothing solid in the way at body height along the first stretch of the jump. */
		private boolean pathClear(Vec3 from, Vec3 dir) {
			for (double along = 1.5; along <= RETREAT_JUMP_DISTANCE; along += 1.5) {
				BlockPos pos = BlockPos.containing(from.x + dir.x * along, from.y + 1.0, from.z + dir.z * along);
				if (!this.bunnay.level().getBlockState(pos).getCollisionShape(this.bunnay.level(), pos).isEmpty()) {
					return false;
				}
			}
			return true;
		}

		/** Solid ground to stand on within a few blocks up or down of here, with room above it, and no water or lava. */
		private boolean safeLanding(Vec3 point) {
			return this.landingAt(point) != null;
		}

		/** Where it would stand if it came down at this point (see safeLanding), or null if there is nowhere safe. */
		private Vec3 landingAt(Vec3 point) {
			Level level = this.bunnay.level();
			for (int dy = 2; dy >= -4; dy--) {
				BlockPos feet = BlockPos.containing(point.x, this.bunnay.getY() + dy, point.z);
				BlockPos floor = feet.below();
				BlockState floorState = level.getBlockState(floor);
				if (!floorState.getCollisionShape(level, floor).isEmpty() && floorState.getFluidState().isEmpty()
					&& level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
					&& level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
					&& level.getFluidState(feet).isEmpty() && !floorState.is(Blocks.MAGMA_BLOCK)) {
					return new Vec3(point.x, feet.getY(), point.z);
				}
			}
			return null;
		}
	}

	/**
	 * Harvests one fully grown carrot at a time, the way a person would: it walks to the carrot, swings at it and breaks it, so
	 * the carrots really drop; then it walks to the dropped carrots and picks them up into its food slot; waits a moment; and
	 * plants a new carrot in the spot. It is built so that nothing can leave it stuck: every part has a time limit, drops that
	 * are picked up by someone else (or despawn) are simply skipped, and if the job is interrupted after the carrot was broken
	 * (a fight, a retreat), the new carrot is planted right then so there is never a hole in the field. When it is done with one
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
				&& !this.bunnay.retreating && !this.bunnay.isEating() && !this.bunnay.isBigHopping() && !this.bunnay.isPassenger()
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
			return this.crop != null;
		}

		/** The nearest ripe carrot (close enough to its owner) that it has a path to, or null. */
		private BlockPos findCrop() {
			LivingEntity owner = this.bunnay.getOwner();
			BlockPos origin = this.bunnay.blockPosition();
			List<BlockPos> ripe = new java.util.ArrayList<>();
			for (BlockPos pos : BlockPos.betweenClosed(
				origin.offset(-HARVEST_SEARCH_RADIUS, -HARVEST_SEARCH_HEIGHT, -HARVEST_SEARCH_RADIUS),
				origin.offset(HARVEST_SEARCH_RADIUS, HARVEST_SEARCH_HEIGHT, HARVEST_SEARCH_RADIUS)
			)) {
				if (this.isRipeCarrot(pos) && (owner == null || owner.distanceToSqr(Vec3.atCenterOf(pos)) <= HARVEST_OWNER_RANGE * HARVEST_OWNER_RANGE)) {
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
			BlockPos next = this.canStart() ? this.findCrop() : null;
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
	 * it when it has nothing else going on: no fight, not sitting, dancing, eating, retreating or hopping.
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
				&& !this.bunnay.retreating && !this.bunnay.isEating() && !this.bunnay.isBigHopping() && !this.bunnay.isPassenger()
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
