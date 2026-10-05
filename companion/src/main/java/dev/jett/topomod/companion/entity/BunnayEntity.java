package dev.jett.topomod.companion.entity;

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
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import dev.jett.topomod.companion.CompanionMod;
import dev.jett.topomod.companion.menu.BunnayMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
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
	private static final double WILD_HEALTH = 10.0;
	private static final double TAME_HEALTH = 60.0;

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

	// Fighting with something in its hands: each hit does a little extra damage. A bamboo is plain extra damage; a breeze
	// rod does less, but every hit also blasts the foe the way a wind charge would; a blaze rod adds no damage at all, but
	// every hit does what a blaze's small fireball does to whatever it hits. Holding a blaze rod does not protect the
	// bunnay from fire in any way.
	private static final double BAMBOO_BONUS_DAMAGE = 2.0;
	private static final double BREEZE_ROD_BONUS_DAMAGE = 1.0;
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
	 * With a weapon in each hand it attacks faster: it may swing this many ticks before its usual 20 tick wait between
	 * swings is up, so 4 means a swing every 16 ticks instead of 20, 25% faster. Raise it for a bigger bonus.
	 */
	private static final int DUAL_WIELD_ATTACK_HEAD_START_TICKS = 4;
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
	 * Once it is already eating, how hurt it has to be is lower (see EAT_KEEP_GOING_MISSING_HEALTH), so it finishes the job.
	 */
	private boolean canEat(boolean alreadyEating) {
		LivingEntity target = this.getTarget();
		return (target == null || !target.isAlive())
			&& this.hurtTime == 0
			&& this.tickCount - this.getLastHurtByMobTimestamp() > EAT_SAFE_TICKS
			&& this.getMaxHealth() - this.getHealth() >= (alreadyEating ? EAT_KEEP_GOING_MISSING_HEALTH : EAT_MIN_MISSING_HEALTH)
			&& this.onGround()
			&& !this.isInWater()
			&& !this.isBigHopping()
			&& !this.isDancing()
			&& !this.isPassenger();
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

	/** Whether it has a weapon in its main hand; this is what makes it raise the weapon when it closes on a target. */
	public boolean isDualWielding() {
		return isHoldable(this.getMainHandItem()) && isHoldable(this.getOffhandItem());
	}

	public boolean isHoldingWeapon() {
		return isHoldable(this.getMainHandItem()) || isHoldable(this.getOffhandItem());
	}

	/**
	 * Which hand to attack with next: with a weapon in each it swings them in turn, and with only one it uses that one
	 * (the main hand if it holds nothing at all).
	 */
	private InteractionHand chooseAttackHand() {
		boolean main = isHoldable(this.getMainHandItem());
		boolean off = isHoldable(this.getOffhandItem());
		if (this.isDualWielding()) {
			return this.attackHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		}
		return off ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
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
		if (stack.is(Items.BREEZE_ROD)) {
			return BREEZE_ROD_BONUS_DAMAGE;
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
			this.tickEating();
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
			.add(Attributes.ATTACK_DAMAGE, 3.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
		this.goalSelector.addGoal(1, new TamableAnimalPanicGoal(1.5, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
		this.goalSelector.addGoal(2, new DanceGoal(this));
		// The leap is priority 1 so it can interrupt the chase (the melee goal is 2), and it cannot be interrupted itself.
		this.goalSelector.addGoal(1, new LeapGoal(this));
		this.goalSelector.addGoal(2, new BigHopGoal(this));
		this.goalSelector.addGoal(2, new BunnayMeleeGoal(this));
		this.goalSelector.addGoal(3, new TemptGoal(this, 1.0, this::isFood, false));
		this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.1, 8.0F, 2.5F));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

		// Fights like a wolf: defends its owner, backs up the owner's attacks, and retaliates when hurt.
		this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
		this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
	}

	@Override
	public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
		// Don't pick a fight with a creeper, and never attack the owner or another companion of the same owner.
		if (target instanceof Creeper) {
			return false;
		}
		if (target instanceof TamableAnimal tamable && tamable.isTame() && tamable.getOwner() == owner) {
			return false;
		}
		return target != owner;
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return isCarrot(stack);
	}

	/** A carrot or golden carrot: what it is tamed, healed and fed with, and what its food slot holds. */
	public static boolean isCarrot(ItemStack stack) {
		return stack.is(Items.CARROT) || stack.is(Items.GOLDEN_CARROT);
	}

	/** Health a carrot or golden carrot restores: a carrot 4 (2 hearts), a golden carrot 30 (15 hearts). */
	private static float healAmount(ItemStack food) {
		return food.is(Items.GOLDEN_CARROT) ? 30.0F : 4.0F;
	}

	// It can't breed with its own kind (see getBreedOffspring), so feeding it never puts it in love mode.
	@Override
	public boolean canFallInLove() {
		return false;
	}

	/** Anything the bunnay can hold in its main hand as a weapon: a bamboo, a breeze rod or a blaze rod. */
	public static boolean isHoldable(ItemStack stack) {
		return stack.is(Items.BAMBOO) || stack.is(Items.BREEZE_ROD) || stack.is(Items.BLAZE_ROD);
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

	// A hit makes a sitting bunnay stand up, so it can defend itself.
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && this.isOrderedToSit()) {
			this.setOrderedToSit(false);
		}
		return hurt;
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

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return !this.isTame();
	}

	// Bunnays come from a bunny and an allay (see BunnayBreeding), not from breeding with each other.
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

	/** The usual melee goal, except that with a weapon in each hand it may swing a little before the cooldown is up. */
	private static final class BunnayMeleeGoal extends MeleeAttackGoal {
		private final BunnayEntity bunnay;

		BunnayMeleeGoal(BunnayEntity bunnay) {
			super(bunnay, 1.3, true);
			this.bunnay = bunnay;
		}

		@Override
		protected boolean isTimeToAttack() {
			int headStart = this.bunnay.isDualWielding() ? DUAL_WIELD_ATTACK_HEAD_START_TICKS : 0;
			return this.getTicksUntilNextAttack() <= headStart;
		}
	}
}
