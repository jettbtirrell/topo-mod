package dev.jett.topomod.companion.entity;

import java.util.function.BiConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import dev.jett.topomod.companion.CompanionMod;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

// Bunnay: a bunny-allay cross that can be tamed with seeds. It follows its owner and fights like a wolf; holding
// weapons, sitting and an item screen will be built on this the way they were for the topo.
public class BunnayEntity extends TamableAnimal {
	private static final double WILD_HEALTH = 10.0;
	private static final double TAME_HEALTH = 20.0;

	// The big hop: a crouch, a launch, a couple of blocks of height, and a squashy landing. The keyframe clip is
	// BunnayAnimation.BIG_HOP; these numbers have to stay in step with it (see the comment there).
	/** Length of the whole hop in ticks (the clip is 1.3 seconds). */
	private static final int BIG_HOP_TICKS = 26;
	/** Tick of the hop that the legs push off (the crouch lasts this long). */
	private static final int BIG_HOP_TAKEOFF_TICK = 4;
	/** Upward speed at takeoff; 0.55 gives about 2 blocks of height and 15 ticks in the air. */
	private static final double BIG_HOP_LAUNCH_SPEED = 0.55;
	/** Forward speed at takeoff, which carries it about 2.5 blocks (it was 0.25, about 2). */
	private static final double BIG_HOP_FORWARD_SPEED = 0.30;
	/** A walking bunnay starts a big hop with a 1-in-this chance each time its goals are checked (about every 6 seconds). */
	private static final int BIG_HOP_ODDS = 40;
	/** Minimum ticks between big hops. */
	private static final int BIG_HOP_COOLDOWN = 100;

	// The idle animation is cosmetic and runs on the client only, on the same timer as the rabbit's: a new one every
	// 180 to 219 ticks (9 to 11 seconds) while standing still.
	private static final int IDLE_MIN_TICKS = 180;
	private static final int IDLE_EXTRA_TICKS = 40;

	// Fighting with a bamboo: it holds it in its right hand, and each hit does a little extra damage.
	private static final double BAMBOO_BONUS_DAMAGE = 2.0;
	private static final Identifier BAMBOO_DAMAGE_ID = CompanionMod.id("bamboo_damage");
	/** How fast the ready stance eases in and out, per tick (it takes 4 ticks to raise and about 7 to lower). */
	private static final float READY_RISE = 0.25F;
	private static final float READY_FALL = 0.15F;

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

	// Dancing to a jukebox, the way the allay does: it listens for the jukebox game events, remembers which jukebox
	// is playing, and stops dancing when the music stops or the jukebox is gone or too far away.
	private final DynamicGameEventListener<JukeboxListener> dynamicJukeboxListener;
	private @Nullable BlockPos jukeboxPos;
	private int bigHopCooldown;
	private boolean forceBigHop;

	public BunnayEntity(EntityType<? extends BunnayEntity> type, Level level) {
		super(type, level);
		this.dynamicJukeboxListener = new DynamicGameEventListener<>(new JukeboxListener(
			this, new EntityPositionSource(this, this.getEyeHeight()), GameEvent.JUKEBOX_PLAY.value().notificationRadius()));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_BIG_HOPPING, false);
		entityData.define(DATA_DANCING, false);
	}

	public boolean isHoldingBamboo() {
		return this.getMainHandItem().is(Items.BAMBOO);
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
		// The bamboo has no attack stat of its own, so add its bonus just for this swing.
		boolean armed = this.isHoldingBamboo();
		AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
		if (armed) {
			attack.addTransientModifier(new AttributeModifier(BAMBOO_DAMAGE_ID, BAMBOO_BONUS_DAMAGE, AttributeModifier.Operation.ADD_VALUE));
		}
		try {
			return super.doHurtTarget(level, target);
		} finally {
			if (armed) {
				attack.removeModifier(BAMBOO_DAMAGE_ID);
			}
		}
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
		if (!this.level().isClientSide() && this.isDancing() && this.shouldStopDancing() && this.tickCount % 20 == 0) {
			this.jukeboxPos = null;
			this.setDancing(false);
		}
	}

	public boolean isBigHopping() {
		return this.entityData.get(DATA_BIG_HOPPING);
	}

	// DEBUG: used by TopoDebugCommand to preview the big hop. Remove together with that command.
	public void debugBigHop() {
		this.forceBigHop = true;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.bigHopAnimationState.animateWhen(this.isBigHopping(), this.tickCount);
			this.tickIdleAnimation();
			this.readyProgressO = this.readyProgress;
			boolean ready = this.isAggressive() && this.isHoldingBamboo();
			this.readyProgress = Mth.clamp(this.readyProgress + (ready ? READY_RISE : -READY_FALL), 0.0F, 1.0F);
		} else if (this.bigHopCooldown > 0) {
			this.bigHopCooldown--;
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
		this.goalSelector.addGoal(1, new TamableAnimalPanicGoal(1.5, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
		this.goalSelector.addGoal(2, new DanceGoal(this));
		this.goalSelector.addGoal(2, new BigHopGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
		this.goalSelector.addGoal(3, new TemptGoal(this, 1.0, this::isTamingItem, false));
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
		return stack.is(Items.CARROT) || stack.is(Items.GOLDEN_CARROT);
	}

	/** Wild bunnays are tamed, and tempted over, with seeds. Carrots are for feeding a tame one (see isFood). */
	private boolean isTamingItem(ItemStack stack) {
		return stack.is(Items.WHEAT_SEEDS);
	}

	/** Health a carrot or golden carrot restores. */
	private static float healAmount(ItemStack food) {
		return food.is(Items.GOLDEN_CARROT) ? 8.0F : 4.0F;
	}

	// It can't breed with its own kind (see getBreedOffspring), so feeding it never puts it in love mode.
	@Override
	public boolean canFallInLove() {
		return false;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (this.isTame() && this.isOwnedBy(player)) {
			// Handing it a bamboo equips it straight away.
			if (stack.is(Items.BAMBOO) && this.getMainHandItem().isEmpty()) {
				if (!this.level().isClientSide()) {
					this.setItemSlot(EquipmentSlot.MAINHAND, stack.split(1));
					this.setGuaranteedDrop(EquipmentSlot.MAINHAND);
					this.setPersistenceRequired();
				}
				return InteractionResult.SUCCESS;
			}
			// Sneaking with an empty hand takes it back.
			if (player.isSecondaryUseActive() && stack.isEmpty() && !this.getMainHandItem().isEmpty()) {
				if (!this.level().isClientSide()) {
					// Only take it if it fits in the player's inventory, so it can never be lost.
					ItemStack held = this.getMainHandItem().copy();
					if (player.addItem(held)) {
						this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
					}
				}
				return InteractionResult.SUCCESS;
			}
		}

		if (this.isTame()) {
			// Carrots heal a hurt bunnay (a golden carrot heals twice as much).
			if (this.isOwnedBy(player) && this.isFood(stack) && this.getHealth() < this.getMaxHealth()) {
				if (!this.level().isClientSide()) {
					this.heal(healAmount(stack));
					stack.consume(1, player);
				}
				return InteractionResult.SUCCESS;
			}
		} else if (this.isTamingItem(stack)) {
			// Each seed has a one in three chance of taming it (the same odds as the topo).
			if (!this.level().isClientSide()) {
				stack.consume(1, player);
				if (this.random.nextInt(3) == 0) {
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
			if (!this.bunnay.onGround() || this.bunnay.isInWater() || this.bunnay.isPassenger() || this.bunnay.isBaby() || this.bunnay.isDancing()) {
				return false;
			}
			if (this.bunnay.forceBigHop) {
				return true;
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
			this.bunnay.forceBigHop = false;
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
}
