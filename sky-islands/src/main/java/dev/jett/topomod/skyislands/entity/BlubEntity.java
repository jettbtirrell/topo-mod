package dev.jett.topomod.skyislands.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jspecify.annotations.Nullable;

// A small, harmless wanderer. A player can tame it with seeds; a tame one follows its owner (and teleports to them when left far behind)
// until it is told to sit. The squish and the idle are all in the client model.
public class BlubEntity extends TamableAnimal {
	/** The wait before the idle clip plays again, in ticks: 180 to 219, the same 9 to 11 seconds as the bunnay's (and the rabbit's). */
	static final int IDLE_MIN_TICKS = 180;
	static final int IDLE_EXTRA_TICKS = 40;
	/** A wild one is as fragile as ever; a tame one is a little sturdier. */
	private static final double WILD_HEALTH = 6.0;
	private static final double TAME_HEALTH = 12.0;
	/** One seed in this many tames it, as a wolf's bone does. */
	private static final int TAME_ODDS = 3;

	/** Plays the idle clip on the client. */
	public final AnimationState idleAnimationState = new AnimationState();
	private int idleAnimationTimeout;

	public BlubEntity(EntityType<? extends BlubEntity> type, Level level) {
		super(type, level);
		// Start each one at a different point of the timer, so a group does not all do it at once.
		this.idleAnimationTimeout = this.random.nextInt(IDLE_MIN_TICKS);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
		// A wild one runs when hurt; a tame one does not (as a wolf does not), only from what the environment does to it, like fire.
		this.goalSelector.addGoal(2, new PanicGoal(this, 1.4) {
			@Override
			protected boolean shouldPanic() {
				return !BlubEntity.this.isTame() && super.shouldPanic();
			}
		});
		this.goalSelector.addGoal(2, new TamableAnimal.TamableAnimalPanicGoal(1.5, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
		// Follows from 5 blocks away, comes to within 2, and teleports to its owner when it is more than 12 blocks behind.
		this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.0, 5.0F, 2.0F));
		this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, WILD_HEALTH).add(Attributes.MOVEMENT_SPEED, 0.2);
	}

	private static boolean isSeed(ItemStack stack) {
		return stack.is(Items.WHEAT_SEEDS) || stack.is(Items.BEETROOT_SEEDS) || stack.is(Items.MELON_SEEDS) || stack.is(Items.PUMPKIN_SEEDS);
	}

	/** Seeds tame a wild one (one try in three) and heal a tame one; the owner's other taps toggle sitting. Nothing breeds. */
	@Override
	public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!this.isTame() && isSeed(stack)) {
			if (!this.level().isClientSide()) {
				stack.consume(1, player);
				if (this.random.nextInt(TAME_ODDS) == 0) {
					this.tame(player);
					this.navigation.stop();
					this.setTarget(null);
					this.setOrderedToSit(false);
					this.level().broadcastEntityEvent(this, (byte) 7);
				} else {
					this.level().broadcastEntityEvent(this, (byte) 6);
				}
			}
			return InteractionResult.SUCCESS;
		}

		if (this.isTame()) {
			if (isSeed(stack) && this.getHealth() < this.getMaxHealth()) {
				if (!this.level().isClientSide()) {
					stack.consume(1, player);
					this.heal(2.0F);
				}
				return InteractionResult.SUCCESS;
			}
			InteractionResult result = super.mobInteract(player, hand);
			if (!result.consumesAction() && this.isOwnedBy(player)) {
				if (!this.level().isClientSide()) {
					this.setOrderedToSit(!this.isOrderedToSit());
					this.jumping = false;
					this.navigation.stop();
					this.setTarget(null);
				}
				return InteractionResult.SUCCESS.withoutItem();
			}
			return result;
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
	public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
		if (this.isInvulnerableTo(level, source)) {
			return false;
		}
		// Being hurt gets it up off the ground.
		this.setOrderedToSit(false);
		return super.hurtServer(level, source, damage);
	}

	// No breeding: the seeds are for taming, so nothing ever counts as breeding food.
	@Override
	public boolean isFood(final ItemStack stack) {
		return false;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
		return null;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.tickIdleAnimation();
		}
	}

	/** Starts the idle clip when its timer runs out, if standing still (and not sitting); stops it if the blub moves off or is hurt. */
	private void tickIdleAnimation() {
		boolean still = this.onGround() && !this.isInWater() && this.hurtTime == 0 && !this.isInSittingPose()
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

	/** Like the other animals: on grass or dirt (light is not checked, the dimension is bright). */
	public static boolean checkSpawnRules(EntityType<BlubEntity> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SLIME_HURT_SMALL;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SLIME_DEATH_SMALL;
	}
}
