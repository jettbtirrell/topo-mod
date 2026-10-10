package dev.jett.topomod.allayvariants.entity;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import dev.jett.topomod.allayvariants.effect.SoulFire;
import dev.jett.topomod.allayvariants.registry.ModEntities;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.ClimbOnTopOfPowderSnowGoal;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.JumpGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.StrollThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The spirit fox: a fox (its wild behaviour is the vanilla fox's, ported from Fox) that can be tamed, and then has a wolf's ways (sits when
 * told, follows its owner and teleports to it, fights what its owner fights) and sets what it bites on soul fire. It is only ever tamed by
 * being bred: the baby of a pair a player fed berries to is born tame to that player. Where it differs from the vanilla fox, it says so.
 */
public class SpiritFoxEntity extends TamableAnimal {
	private static final EntityDataAccessor<Byte> DATA_FOX_FLAGS_ID = SynchedEntityData.defineId(SpiritFoxEntity.class, EntityDataSerializers.BYTE);
	private static final float BABY_SCALE = 0.6F;
	/** A wild one has the vanilla fox's 10 health; a tame one is sturdier, as a wolf's is (8 and 40). */
	private static final double WILD_HEALTH = 10.0;
	private static final double TAME_HEALTH = 50.0;
	private static final int FLAG_SITTING = 1;
	public static final int FLAG_CROUCHING = 4;
	public static final int FLAG_INTERESTED = 8;
	public static final int FLAG_POUNCING = 16;
	private static final int FLAG_SLEEPING = 32;
	private static final int FLAG_FACEPLANTED = 64;
	private static final int FLAG_DEFENDING = 128;
	private static final Predicate<ItemEntity> ALLOWED_ITEMS = e -> !e.hasPickUpDelay() && e.isAlive();
	private static final Predicate<Entity> STALKABLE_PREY = entity -> entity instanceof Chicken || entity instanceof Rabbit;
	private static final Predicate<Entity> AVOID_PLAYERS = entity -> !entity.isDiscrete() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity);
	private static final int MIN_TICKS_BEFORE_EAT = 600;
	private static final boolean DEFAULT_SLEEPING = false;
	private static final boolean DEFAULT_SITTING = false;
	private static final boolean DEFAULT_CROUCHING = false;
	private Goal landTargetGoal;
	private Goal turtleEggTargetGoal;
	private Goal fishTargetGoal;
	private float interestedAngle;
	private float interestedAngleO;
	private float crouchAmount;
	private float crouchAmountO;
	private static final float MAX_CROUCH_AMOUNT = 5.0F;
	private int ticksSinceEaten;

	public SpiritFoxEntity(final EntityType<? extends SpiritFoxEntity> type, final Level level) {
		super(type, level);
		this.lookControl = new SpiritFoxEntity.FoxLookControl();
		this.moveControl = new SpiritFoxEntity.FoxMoveControl<>(this);
		this.setPathfindingMalus(PathType.DAMAGING_IN_NEIGHBOR, 0.0F);
		this.setPathfindingMalus(PathType.DAMAGING, 0.0F);
		this.setCanPickUpLoot(true);
		this.getNavigation().setRequiredPathLength(32.0F);
	}

	@Override
	protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_FOX_FLAGS_ID, (byte)0);
	}

	@Override
	protected void registerGoals() {
		this.landTargetGoal = new NearestAttackableTargetGoal<>(
			this, Animal.class, 10, false, false, (target, level) -> target instanceof Chicken || target instanceof Rabbit
		);
		this.turtleEggTargetGoal = new NearestAttackableTargetGoal<>(this, Turtle.class, 10, false, false, Turtle.BABY_ON_LAND_SELECTOR);
		this.fishTargetGoal = new NearestAttackableTargetGoal<>(
			this, AbstractFish.class, 20, false, false, (target, level) -> target instanceof AbstractSchoolingFish
		);
		// The fox's goals, with the wolf's (sit, follow its owner, fight with it) between them. The wild-only ones check !isTame().
		this.goalSelector.addGoal(0, new SpiritFoxEntity.FoxFloatGoal());
		this.goalSelector.addGoal(0, new ClimbOnTopOfPowderSnowGoal(this, this.level()));
		this.goalSelector.addGoal(1, new SpiritFoxEntity.FaceplantGoal());
		this.goalSelector.addGoal(2, new SpiritFoxEntity.FoxPanicGoal(2.2));
		this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		this.goalSelector.addGoal(3, new SpiritFoxEntity.FoxBreedGoal(1.0));
		this.goalSelector
			.addGoal(4, new AvoidEntityGoal<>(this, Player.class, 16.0F, 1.6, 1.4, entity -> AVOID_PLAYERS.test(entity) && !this.isTame() && !this.isDefending()));
		this.goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Wolf.class, 8.0F, 1.6, 1.4, entity -> !((Wolf)entity).isTame() && !this.isDefending()));
		this.goalSelector.addGoal(4, new AvoidEntityGoal<>(this, PolarBear.class, 8.0F, 1.6, 1.4, entity -> !this.isDefending()));
		this.goalSelector.addGoal(5, new SpiritFoxEntity.StalkPreyGoal());
		this.goalSelector.addGoal(6, new SpiritFoxEntity.FoxPounceGoal());
		this.goalSelector.addGoal(6, new SpiritFoxEntity.SeekShelterGoal(1.25));
		this.goalSelector.addGoal(7, new SpiritFoxEntity.FoxMeleeAttackGoal(1.2F, true));
		this.goalSelector.addGoal(7, new SpiritFoxEntity.SleepGoal());
		this.goalSelector.addGoal(8, new SpiritFoxEntity.FoxFollowParentGoal(this, 1.25));
		this.goalSelector.addGoal(8, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F));
		this.goalSelector.addGoal(9, new SpiritFoxEntity.FoxStrollThroughVillageGoal(32, 200));
		this.goalSelector.addGoal(10, new SpiritFoxEntity.FoxEatBerriesGoal(1.2F, 12, 1));
		this.goalSelector.addGoal(10, new LeapAtTargetGoal(this, 0.4F));
		this.goalSelector.addGoal(11, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(11, new SpiritFoxEntity.FoxSearchForItemsGoal());
		this.goalSelector.addGoal(12, new SpiritFoxEntity.FoxLookAtPlayerGoal(this, Player.class, 24.0F));
		this.goalSelector.addGoal(13, new SpiritFoxEntity.PerchAndSearchGoal());
		this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
		this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
	}

	@Override
	public void aiStep() {
		if (!this.level().isClientSide() && this.isAlive() && this.isEffectiveAi()) {
			this.ticksSinceEaten++;
			ItemStack itemInMouth = this.getItemBySlot(EquipmentSlot.MAINHAND);
			if (this.canEat(itemInMouth)) {
				if (this.ticksSinceEaten > 600) {
					ItemStack remainingFood = itemInMouth.finishUsingItem(this.level(), this);
					if (!remainingFood.isEmpty()) {
						this.setItemSlot(EquipmentSlot.MAINHAND, remainingFood);
					}

					this.ticksSinceEaten = 0;
				} else if (this.ticksSinceEaten > 560 && this.random.nextFloat() < 0.1F) {
					this.playEatingSound();
					this.level().broadcastEntityEvent(this, (byte)45);
				}
			}

			LivingEntity target = this.getTarget();
			if (target == null || !target.isAlive()) {
				this.setIsCrouching(false);
				this.setIsInterested(false);
			}
		}

		if (this.isSleeping() || this.isImmobile()) {
			this.jumping = false;
			this.xxa = 0.0F;
			this.zza = 0.0F;
		}

		super.aiStep();
		if (this.isDefending() && this.random.nextFloat() < 0.05F) {
			this.playSound(SoundEvents.FOX_AGGRO, 1.0F, 1.0F);
		}
	}

	@Override
	protected boolean isImmobile() {
		return this.isDeadOrDying();
	}

	private boolean canEat(final ItemStack itemInMouth) {
		return this.isConsumableFood(itemInMouth) && this.getTarget() == null && this.onGround() && !this.isSleeping();
	}

	private boolean isConsumableFood(final ItemStack itemStack) {
		return itemStack.has(DataComponents.FOOD) && itemStack.has(DataComponents.CONSUMABLE);
	}

	@Override
	protected void populateDefaultEquipmentSlots(final RandomSource random, final DifficultyInstance difficulty) {
		if (random.nextFloat() < 0.2F) {
			float odds = random.nextFloat();
			ItemStack heldInMouth;
			if (odds < 0.05F) {
				heldInMouth = new ItemStack(Items.EMERALD);
			} else if (odds < 0.2F) {
				heldInMouth = new ItemStack(Items.EGG);
			} else if (odds < 0.4F) {
				heldInMouth = random.nextBoolean() ? new ItemStack(Items.RABBIT_FOOT) : new ItemStack(Items.RABBIT_HIDE);
			} else if (odds < 0.6F) {
				heldInMouth = new ItemStack(Items.WHEAT);
			} else if (odds < 0.8F) {
				heldInMouth = new ItemStack(Items.LEATHER);
			} else {
				heldInMouth = new ItemStack(Items.FEATHER);
			}

			this.setItemSlot(EquipmentSlot.MAINHAND, heldInMouth);
		}
	}

	@Override
	public void handleEntityEvent(final @EntityEvent.Value byte id) {
		if (id == 45) {
			ItemStack mouthItem = this.getItemBySlot(EquipmentSlot.MAINHAND);
			if (!mouthItem.isEmpty()) {
				ItemParticleOption breakParticle = new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(mouthItem));

				for (int i = 0; i < 8; i++) {
					Vec3 direction = new Vec3((this.random.nextFloat() - 0.5) * 0.1, this.random.nextFloat() * 0.1 + 0.1, 0.0)
						.xRot(-this.getXRot() * (float) (Math.PI / 180.0))
						.yRot(-this.getYRot() * (float) (Math.PI / 180.0));
					this.level()
						.addParticle(
							breakParticle,
							this.getX() + this.getLookAngle().x / 2.0,
							this.getY(),
							this.getZ() + this.getLookAngle().z / 2.0,
							direction.x,
							direction.y + 0.05,
							direction.z
						);
				}
			}
		} else {
			super.handleEntityEvent(id);
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.3F)
			.add(Attributes.MAX_HEALTH, 10.0)
			.add(Attributes.ATTACK_DAMAGE, 2.0)
			.add(Attributes.SAFE_FALL_DISTANCE, 5.0)
			.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	public @Nullable SpiritFoxEntity getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
		return ModEntities.SPIRIT_FOX.create(level, EntitySpawnReason.BREEDING);
	}

	/** Wild ones turn up at night, on what a fox spawns on; where (the cherry grove) is set by the biome spawn list in ModEntities. */
	public static boolean checkSpawnRules(
		final EntityType<SpiritFoxEntity> type, final ServerLevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
	) {
		return level.getBlockState(pos.below()).is(BlockTags.FOXES_SPAWNABLE_ON) && level.getLevel().isDarkOutside();
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(
		final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData
	) {
		boolean isBaby = false;
		if (groupData instanceof AgeableMob.AgeableMobGroupData ageableData) {
			if (ageableData.getGroupSize() >= 2) {
				isBaby = true;
			}
		} else {
			groupData = new AgeableMob.AgeableMobGroupData(false);
		}

		if (isBaby) {
			this.setAge(-24000);
		}

		if (level instanceof ServerLevel) {
			this.setTargetGoals();
		}

		this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
		return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
	}

	/** The hunting of a wild red fox (chickens, rabbits, baby turtles, then fish). A tamed one does not hunt: it fights with its owner. */
	private void setTargetGoals() {
		if (this.isTame()) {
			return;
		}

		this.targetSelector.addGoal(4, this.landTargetGoal);
		this.targetSelector.addGoal(4, this.turtleEggTargetGoal);
		this.targetSelector.addGoal(6, this.fishTargetGoal);
	}

	@Override
	protected void playEatingSound() {
		this.playSound(SoundEvents.FOX_EAT, 1.0F, 1.0F);
	}

	@Override
	public EntityDimensions getDefaultDimensions(final Pose pose) {
		return this.isBaby()
			? super.getDefaultDimensions(pose).scale(BABY_SCALE).withEyeHeight(0.34375F).withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, 0.375F, 0.0F))
			: super.getDefaultDimensions(pose);
	}

	@Override
	protected void addAdditionalSaveData(final ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Sleeping", this.isSleeping());
		output.putBoolean("Perching", this.getFlag(FLAG_SITTING));
		output.putBoolean("Crouching", this.isCrouching());
	}

	@Override
	protected void readAdditionalSaveData(final ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setSleeping(input.getBooleanOr("Sleeping", false));
		this.setSitting(input.getBooleanOr("Perching", false));
		this.setIsCrouching(input.getBooleanOr("Crouching", false));
		if (this.level() instanceof ServerLevel) {
			this.setTargetGoals();
		}
	}

	/** Perching (the fox's own sitting) or sitting because its owner said so; either way it stays put and shows the sitting pose. */
	public boolean isSitting() {
		return this.getFlag(FLAG_SITTING) || this.isInSittingPose();
	}

	public void setSitting(final boolean value) {
		this.setFlag(1, value);
	}

	public boolean isFaceplanted() {
		return this.getFlag(64);
	}

	private void setFaceplanted(final boolean faceplanted) {
		this.setFlag(64, faceplanted);
	}

	private boolean isDefending() {
		return this.getFlag(128);
	}

	private void setDefending(final boolean defending) {
		this.setFlag(128, defending);
	}

	@Override
	public boolean isSleeping() {
		return this.getFlag(32);
	}

	private void setSleeping(final boolean sleeping) {
		this.setFlag(32, sleeping);
	}

	private void setFlag(final int flag, final boolean value) {
		if (value) {
			this.entityData.set(DATA_FOX_FLAGS_ID, (byte)(this.entityData.get(DATA_FOX_FLAGS_ID) | flag));
		} else {
			this.entityData.set(DATA_FOX_FLAGS_ID, (byte)(this.entityData.get(DATA_FOX_FLAGS_ID) & ~flag));
		}
	}

	private boolean getFlag(final int flag) {
		return (this.entityData.get(DATA_FOX_FLAGS_ID) & flag) != 0;
	}

	@Override
	protected boolean canDispenserEquipIntoSlot(final EquipmentSlot slot) {
		return slot == EquipmentSlot.MAINHAND && this.canPickUpLoot();
	}

	@Override
	public boolean canHoldItem(final ItemStack itemStack) {
		ItemStack heldItemStack = this.getItemBySlot(EquipmentSlot.MAINHAND);
		return heldItemStack.isEmpty() || this.ticksSinceEaten > 0 && this.isConsumableFood(itemStack) && !this.isConsumableFood(heldItemStack);
	}

	private void spitOutItem(final ItemStack itemStack) {
		if (!itemStack.isEmpty() && !this.level().isClientSide()) {
			ItemEntity thrownItem = new ItemEntity(this.level(), this.getX() + this.getLookAngle().x, this.getY() + 1.0, this.getZ() + this.getLookAngle().z, itemStack);
			thrownItem.setPickUpDelay(40);
			thrownItem.setThrower(this);
			this.playSound(SoundEvents.FOX_SPIT, 1.0F, 1.0F);
			this.level().addFreshEntity(thrownItem);
		}
	}

	private void dropItemStack(final ItemStack itemStack) {
		ItemEntity itemEntity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), itemStack);
		this.level().addFreshEntity(itemEntity);
	}

	@Override
	protected void pickUpItem(final ServerLevel level, final ItemEntity entity) {
		ItemStack itemStack = entity.getItem();
		if (this.canHoldItem(itemStack)) {
			int count = itemStack.getCount();
			if (count > 1) {
				this.dropItemStack(itemStack.split(count - 1));
			}

			this.spitOutItem(this.getItemBySlot(EquipmentSlot.MAINHAND));
			this.onItemPickup(entity);
			this.setItemSlot(EquipmentSlot.MAINHAND, itemStack.split(1));
			this.setGuaranteedDrop(EquipmentSlot.MAINHAND);
			this.take(entity, itemStack.getCount());
			entity.discard();
			this.ticksSinceEaten = 0;
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.isEffectiveAi()) {
			boolean inWater = this.isInWater();
			if (inWater || this.getTarget() != null || this.level().isThundering()) {
				this.wakeUp();
			}

			if (inWater || this.isSleeping()) {
				this.setSitting(false);
			}

			if (this.isFaceplanted() && this.level().getRandom().nextFloat() < 0.2F) {
				BlockPos pos = this.blockPosition();
				BlockState state = this.level().getBlockState(pos);
				this.level().levelEvent(2001, pos, Block.getId(state));
			}
		}

		this.interestedAngleO = this.interestedAngle;
		if (this.isInterested()) {
			this.interestedAngle = this.interestedAngle + (1.0F - this.interestedAngle) * 0.4F;
		} else {
			this.interestedAngle = this.interestedAngle + (0.0F - this.interestedAngle) * 0.4F;
		}

		this.crouchAmountO = this.crouchAmount;
		if (this.isCrouching()) {
			this.crouchAmount += 0.2F;
			if (this.crouchAmount > 5.0F) {
				this.crouchAmount = 5.0F;
			}
		} else {
			this.crouchAmount = 0.0F;
		}
	}

	@Override
	public boolean isFood(final ItemStack itemStack) {
		return itemStack.is(Items.SWEET_BERRIES);
	}

	@Override
	protected void onOffspringSpawnedFromEgg(final Player spawner, final Mob offspring) {
		((SpiritFoxEntity)offspring).tame(spawner);
	}

	public boolean isPouncing() {
		return this.getFlag(16);
	}

	public void setIsPouncing(final boolean pouncing) {
		this.setFlag(16, pouncing);
	}

	public boolean isFullyCrouched() {
		return this.crouchAmount == 5.0F;
	}

	public void setIsCrouching(final boolean isCrouching) {
		this.setFlag(4, isCrouching);
	}

	@Override
	public boolean isCrouching() {
		return this.getFlag(4);
	}

	public void setIsInterested(final boolean value) {
		this.setFlag(8, value);
	}

	public boolean isInterested() {
		return this.getFlag(8);
	}

	public float getHeadRollAngle(final float a) {
		return Mth.lerp(a, this.interestedAngleO, this.interestedAngle) * 0.11F * (float) Math.PI;
	}

	public float getCrouchAmount(final float a) {
		return Mth.lerp(a, this.crouchAmountO, this.crouchAmount);
	}

	@Override
	public void setTarget(final @Nullable LivingEntity target) {
		if (this.isDefending() && target == null) {
			this.setDefending(false);
		}

		super.setTarget(target);
	}

	private void wakeUp() {
		this.setSleeping(false);
	}

	private void clearStates() {
		this.setIsInterested(false);
		this.setIsCrouching(false);
		this.setSitting(false);
		this.setSleeping(false);
		this.setDefending(false);
		this.setFaceplanted(false);
	}

	public boolean canMove() {
		return !this.isSleeping() && !this.isSitting() && !this.isFaceplanted();
	}

	@Override
	public void playAmbientSound() {
		SoundEvent ambient = this.getAmbientSound();
		if (ambient == SoundEvents.FOX_SCREECH) {
			this.playSound(ambient, 2.0F, this.getVoicePitch());
		} else {
			super.playAmbientSound();
		}
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		if (this.isSleeping()) {
			return SoundEvents.FOX_SLEEP;
		}

		if (!this.level().isBrightOutside() && this.random.nextFloat() < 0.1F) {
			List<Player> nearbyEntities = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16.0, 16.0, 16.0), EntitySelector.NO_SPECTATORS);
			if (nearbyEntities.isEmpty()) {
				return SoundEvents.FOX_SCREECH;
			}
		}

		return SoundEvents.FOX_AMBIENT;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(final DamageSource source) {
		return SoundEvents.FOX_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return SoundEvents.FOX_DEATH;
	}

	/** It trusts the one it belongs to, and nobody else (the vanilla fox keeps a list of up to two players it trusts instead). */
	private boolean trusts(final LivingEntity entity) {
		return this.isTame() && this.isOwnedBy(entity);
	}

	@Override
	protected void dropAllDeathLoot(final ServerLevel level, final DamageSource source) {
		ItemStack itemStack = this.getItemBySlot(EquipmentSlot.MAINHAND);
		if (!itemStack.isEmpty()) {
			this.spawnAtLocation(level, itemStack);
			this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		}

		super.dropAllDeathLoot(level, source);
	}

	public static boolean isPathClear(final SpiritFoxEntity fox, final LivingEntity target) {
		double zdiff = target.getZ() - fox.getZ();
		double xdiff = target.getX() - fox.getX();
		double slope = zdiff / xdiff;
		int increments = 6;

		for (int i = 0; i < 6; i++) {
			double z = slope == 0.0 ? 0.0 : zdiff * (i / 6.0F);
			double x = slope == 0.0 ? xdiff * (i / 6.0F) : z / slope;

			for (int j = 1; j < 4; j++) {
				if (!fox.level().getBlockState(BlockPos.containing(fox.getX() + x, fox.getY() + j, fox.getZ() + z)).canBeReplaced()) {
					return false;
				}
			}
		}

		return true;
	}

	/** What the wolf does (Wolf.mobInteract): berries heal a hurt tame one; otherwise the owner's empty hand toggles sitting. Everyone else gets the breeding. */
	@Override
	public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
		ItemStack itemStack = player.getItemInHand(hand);
		if (this.isTame()) {
			if (this.isFood(itemStack) && this.getHealth() < this.getMaxHealth()) {
				this.feed(player, hand, itemStack, 2.0F, 2.0F);
				return InteractionResult.SUCCESS;
			}

			InteractionResult result = super.mobInteract(player, hand);
			if (!result.consumesAction() && this.isOwnedBy(player)) {
				this.setOrderedToSit(!this.isOrderedToSit());
				this.jumping = false;
				this.navigation.stop();
				this.setTarget(null);
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
			this.setHealth((float)TAME_HEALTH);
		} else {
			this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(WILD_HEALTH);
		}
	}

	@Override
	public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
		if (this.isInvulnerableTo(level, source)) {
			return false;
		}

		this.setOrderedToSit(false);
		return super.hurtServer(level, source, damage);
	}

	/** What it bites (and what it pounces on) it sets on soul fire. */
	@Override
	public boolean doHurtTarget(final ServerLevel level, final Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity victim) {
			SoulFire.ignite(victim, SoulFire.BITE_TICKS);
		}

		return hit;
	}

	@Override
	public Vec3 getLeashOffset() {
		return new Vec3(0.0, 0.55F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
	}

	private class FaceplantGoal extends Goal {
		private int countdown;

		public FaceplantGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.LOOK, Goal.Flag.JUMP, Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return SpiritFoxEntity.this.isFaceplanted();
		}

		@Override
		public boolean canContinueToUse() {
			return this.canUse() && this.countdown > 0;
		}

		@Override
		public void start() {
			this.countdown = this.adjustedTickDelay(40);
		}

		@Override
		public void stop() {
			SpiritFoxEntity.this.setFaceplanted(false);
		}

		@Override
		public void tick() {
			this.countdown--;
		}
	}

	public class FoxAlertableEntitiesSelector implements TargetingConditions.Selector {
		@Override
		public boolean test(final LivingEntity target, final ServerLevel level) {
			if (target instanceof SpiritFoxEntity) {
				return false;
			} else if (target instanceof Chicken || target instanceof Rabbit || target instanceof Monster) {
				return true;
			} else if (target instanceof TamableAnimal tamableAnimal) {
				return !tamableAnimal.isTame();
			} else if (target instanceof Player player && (player.isSpectator() || player.isCreative())) {
				return false;
			} else {
				return SpiritFoxEntity.this.trusts(target) ? false : !target.isSleeping() && !target.isDiscrete();
			}
		}
	}

	private abstract class FoxBehaviorGoal extends Goal {
		private final TargetingConditions alertableTargeting = TargetingConditions.forCombat()
			.range(12.0)
			.ignoreLineOfSight()
			.selector(SpiritFoxEntity.this.new FoxAlertableEntitiesSelector());

		protected boolean hasShelter() {
			BlockPos foxPos = BlockPos.containing(SpiritFoxEntity.this.getX(), SpiritFoxEntity.this.getBoundingBox().maxY, SpiritFoxEntity.this.getZ());
			return !SpiritFoxEntity.this.level().canSeeSky(foxPos) && SpiritFoxEntity.this.getWalkTargetValue(foxPos) >= 0.0F;
		}

		protected boolean alertable() {
			return !getServerLevel(SpiritFoxEntity.this.level())
				.getNearbyEntities(LivingEntity.class, this.alertableTargeting, SpiritFoxEntity.this, SpiritFoxEntity.this.getBoundingBox().inflate(12.0, 6.0, 12.0))
				.isEmpty();
		}
	}

	private class FoxBreedGoal extends BreedGoal {
		public FoxBreedGoal(final double speedModifier) {
			super(SpiritFoxEntity.this, speedModifier);
		}

		@Override
		public void start() {
			((SpiritFoxEntity)this.animal).clearStates();
			((SpiritFoxEntity)this.partner).clearStates();
			super.start();
		}

		@Override
		protected void breed() {
			SpiritFoxEntity offspring = (SpiritFoxEntity)this.animal.getBreedOffspring(this.level, this.partner);
			if (offspring != null) {
				// The only way to a tame one: it is born tame to the player who fed its parents (the first parent's feeder, if both were fed by someone).
				ServerPlayer animalLoveCause = this.animal.getLoveCause();
				ServerPlayer loveCause = animalLoveCause != null ? animalLoveCause : this.partner.getLoveCause();
				if (loveCause != null) {
					offspring.tame(loveCause);
					loveCause.awardStat(Stats.ANIMALS_BRED);
					CriteriaTriggers.BRED_ANIMALS.trigger(loveCause, this.animal, this.partner, offspring);
				}

				this.animal.setAge(6000);
				this.partner.setAge(6000);
				this.animal.resetLove();
				this.partner.resetLove();
				offspring.setAge(-24000);
				offspring.snapTo(this.animal.getX(), this.animal.getY(), this.animal.getZ(), 0.0F, 0.0F);
				this.level.addFreshEntityWithPassengers(offspring);
				this.level.broadcastEntityEvent(this.animal, (byte)18);
				if (this.level.getGameRules().get(GameRules.MOB_DROPS)) {
					this.level
						.addFreshEntity(new ExperienceOrb(this.level, this.animal.getX(), this.animal.getY(), this.animal.getZ(), this.animal.getRandom().nextInt(7) + 1));
				}
			}
		}
	}

	public class FoxEatBerriesGoal extends MoveToBlockGoal {
		private static final int WAIT_TICKS = 40;
		protected int ticksWaited;

		public FoxEatBerriesGoal(final double speedModifier, final int searchRange, final int verticalSearchRange) {
			super(SpiritFoxEntity.this, speedModifier, searchRange, verticalSearchRange);
		}

		@Override
		public double acceptedDistance() {
			return 2.0;
		}

		@Override
		public boolean shouldRecalculatePath() {
			return this.tryTicks % 100 == 0;
		}

		@Override
		protected boolean isValidTarget(final LevelReader level, final BlockPos pos) {
			BlockState blockState = level.getBlockState(pos);
			return blockState.is(Blocks.SWEET_BERRY_BUSH) && blockState.getValue(SweetBerryBushBlock.AGE) >= 2 || CaveVines.hasGlowBerries(blockState);
		}

		@Override
		public void tick() {
			if (this.isReachedTarget()) {
				if (this.ticksWaited >= 40) {
					this.onReachedTarget();
				} else {
					this.ticksWaited++;
				}
			} else if (!this.isReachedTarget() && SpiritFoxEntity.this.random.nextFloat() < 0.05F) {
				SpiritFoxEntity.this.playSound(SoundEvents.FOX_SNIFF, 1.0F, 1.0F);
			}

			super.tick();
		}

		protected void onReachedTarget() {
			if (getServerLevel(SpiritFoxEntity.this.level()).getGameRules().get(GameRules.MOB_GRIEFING)) {
				BlockState state = SpiritFoxEntity.this.level().getBlockState(this.blockPos);
				if (state.is(Blocks.SWEET_BERRY_BUSH)) {
					this.pickSweetBerries(state);
				} else if (CaveVines.hasGlowBerries(state)) {
					this.pickGlowBerry(state);
				}
			}
		}

		private void pickGlowBerry(final BlockState state) {
			CaveVines.use(SpiritFoxEntity.this, state, SpiritFoxEntity.this.level(), this.blockPos);
		}

		private void pickSweetBerries(final BlockState state) {
			int age = state.getValue(SweetBerryBushBlock.AGE);
			state.setValue(SweetBerryBushBlock.AGE, 1);
			int count = 1 + SpiritFoxEntity.this.level().getRandom().nextInt(2) + (age == 3 ? 1 : 0);
			ItemStack heldItem = SpiritFoxEntity.this.getItemBySlot(EquipmentSlot.MAINHAND);
			if (heldItem.isEmpty()) {
				SpiritFoxEntity.this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SWEET_BERRIES));
				count--;
			}

			if (count > 0) {
				Block.popResource(SpiritFoxEntity.this.level(), this.blockPos, new ItemStack(Items.SWEET_BERRIES, count));
			}

			SpiritFoxEntity.this.playSound(SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, 1.0F, 1.0F);
			SpiritFoxEntity.this.level().setBlock(this.blockPos, state.setValue(SweetBerryBushBlock.AGE, 1), 2);
			SpiritFoxEntity.this.level().gameEvent(GameEvent.BLOCK_CHANGE, this.blockPos, GameEvent.Context.of(SpiritFoxEntity.this));
		}

		@Override
		public boolean canUse() {
			return !SpiritFoxEntity.this.isTame() && !SpiritFoxEntity.this.isSleeping() && super.canUse();
		}

		@Override
		public void start() {
			this.ticksWaited = 0;
			SpiritFoxEntity.this.setSitting(false);
			super.start();
		}
	}

	private class FoxFloatGoal extends FloatGoal {
		public FoxFloatGoal() {
			super(SpiritFoxEntity.this);
		}

		@Override
		public void start() {
			super.start();
			SpiritFoxEntity.this.clearStates();
		}

		@Override
		public boolean canUse() {
			return SpiritFoxEntity.this.isInFluidDeeperThan(0.25, this.fluid) || SpiritFoxEntity.this.isInLava();
		}
	}

	private static class FoxFollowParentGoal extends FollowParentGoal {
		private final SpiritFoxEntity fox;

		public FoxFollowParentGoal(final SpiritFoxEntity fox, final double speedModifier) {
			super(fox, speedModifier);
			this.fox = fox;
		}

		@Override
		public boolean canUse() {
			return !this.fox.isTame() && !this.fox.isDefending() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return !this.fox.isDefending() && super.canContinueToUse();
		}

		@Override
		public void start() {
			this.fox.clearStates();
			super.start();
		}
	}

	private class FoxLookAtPlayerGoal extends LookAtPlayerGoal {
		public FoxLookAtPlayerGoal(final Mob mob, final Class<? extends LivingEntity> lookAtType, final float lookDistance) {
			super(mob, lookAtType, lookDistance);
		}

		@Override
		public boolean canUse() {
			return super.canUse() && !SpiritFoxEntity.this.isFaceplanted() && !SpiritFoxEntity.this.isInterested();
		}

		@Override
		public boolean canContinueToUse() {
			return super.canContinueToUse() && !SpiritFoxEntity.this.isFaceplanted() && !SpiritFoxEntity.this.isInterested();
		}
	}

	public class FoxLookControl extends LookControl {
		public FoxLookControl() {
			super(SpiritFoxEntity.this);
		}

		@Override
		public void tick() {
			if (!SpiritFoxEntity.this.isSleeping()) {
				super.tick();
			}
		}

		@Override
		protected boolean resetXRotOnTick() {
			return !SpiritFoxEntity.this.isPouncing() && !SpiritFoxEntity.this.isCrouching() && !SpiritFoxEntity.this.isInterested() && !SpiritFoxEntity.this.isFaceplanted();
		}
	}

	private class FoxMeleeAttackGoal extends MeleeAttackGoal {
		public FoxMeleeAttackGoal(final double speedModifier, final boolean trackTarget) {
			super(SpiritFoxEntity.this, speedModifier, trackTarget);
		}

		@Override
		protected void checkAndPerformAttack(final LivingEntity target) {
			if (this.canPerformAttack(target)) {
				this.resetAttackCooldown();
				this.mob.doHurtTarget(getServerLevel(this.mob), target);
				SpiritFoxEntity.this.playSound(SoundEvents.FOX_BITE, 1.0F, 1.0F);
			}
		}

		@Override
		public void start() {
			SpiritFoxEntity.this.setIsInterested(false);
			super.start();
		}

		@Override
		public boolean canUse() {
			return !SpiritFoxEntity.this.isSitting() && !SpiritFoxEntity.this.isSleeping() && !SpiritFoxEntity.this.isCrouching() && !SpiritFoxEntity.this.isFaceplanted() && super.canUse();
		}
	}

	private static class FoxMoveControl<T extends SpiritFoxEntity> extends MoveControl<T> {
		public FoxMoveControl(final T fox) {
			super(fox);
		}

		@Override
		public void tick() {
			if (this.mob.canMove()) {
				super.tick();
			}
		}
	}

	private class FoxPanicGoal extends TamableAnimal.TamableAnimalPanicGoal {
		public FoxPanicGoal(final double speedModifier) {
			super(speedModifier);
		}

		@Override
		public boolean shouldPanic() {
			return !SpiritFoxEntity.this.isDefending() && super.shouldPanic();
		}
	}

	public class FoxPounceGoal extends JumpGoal {
		@Override
		public boolean canUse() {
			if (!SpiritFoxEntity.this.isFullyCrouched()) {
				return false;
			}

			LivingEntity target = SpiritFoxEntity.this.getTarget();
			if (target != null && target.isAlive()) {
				if (target.getMotionDirection() != target.getDirection()) {
					return false;
				}

				boolean hasClearPath = SpiritFoxEntity.isPathClear(SpiritFoxEntity.this, target);
				if (!hasClearPath) {
					SpiritFoxEntity.this.getNavigation().createPath(target, 0);
					SpiritFoxEntity.this.setIsCrouching(false);
					SpiritFoxEntity.this.setIsInterested(false);
				}

				return hasClearPath;
			} else {
				return false;
			}
		}

		@Override
		public boolean canContinueToUse() {
			LivingEntity target = SpiritFoxEntity.this.getTarget();
			if (target != null && target.isAlive()) {
				double yd = SpiritFoxEntity.this.getDeltaMovement().y;
				return (!(yd * yd < 0.05F) || !(Math.abs(SpiritFoxEntity.this.getXRot()) < 15.0F) || !SpiritFoxEntity.this.onGround()) && !SpiritFoxEntity.this.isFaceplanted();
			} else {
				return false;
			}
		}

		@Override
		public boolean isInterruptable() {
			return false;
		}

		@Override
		public void start() {
			SpiritFoxEntity.this.setJumping(true);
			SpiritFoxEntity.this.setIsPouncing(true);
			SpiritFoxEntity.this.setIsInterested(false);
			LivingEntity target = SpiritFoxEntity.this.getTarget();
			if (target != null) {
				SpiritFoxEntity.this.getLookControl().setLookAt(target, 60.0F, 30.0F);
				Vec3 uv = new Vec3(target.getX() - SpiritFoxEntity.this.getX(), target.getY() - SpiritFoxEntity.this.getY(), target.getZ() - SpiritFoxEntity.this.getZ()).normalize();
				SpiritFoxEntity.this.setDeltaMovement(SpiritFoxEntity.this.getDeltaMovement().add(uv.x * 0.8, 0.9, uv.z * 0.8));
			}

			SpiritFoxEntity.this.getNavigation().stop();
		}

		@Override
		public void stop() {
			SpiritFoxEntity.this.setIsCrouching(false);
			SpiritFoxEntity.this.crouchAmount = 0.0F;
			SpiritFoxEntity.this.crouchAmountO = 0.0F;
			SpiritFoxEntity.this.setIsInterested(false);
			SpiritFoxEntity.this.setIsPouncing(false);
		}

		@Override
		public void tick() {
			LivingEntity target = SpiritFoxEntity.this.getTarget();
			if (target != null) {
				SpiritFoxEntity.this.getLookControl().setLookAt(target, 60.0F, 30.0F);
			}

			if (!SpiritFoxEntity.this.isFaceplanted()) {
				Vec3 movement = SpiritFoxEntity.this.getDeltaMovement();
				if (movement.y * movement.y < 0.03F && SpiritFoxEntity.this.getXRot() != 0.0F) {
					SpiritFoxEntity.this.setXRot(Mth.rotLerp(0.2F, SpiritFoxEntity.this.getXRot(), 0.0F));
				} else {
					double direction = movement.horizontalDistance();
					float upwardsBias = SpiritFoxEntity.this.jumping && movement.y > 0.0 ? 6.5F : 1.0F;
					double biasedY = movement.y * upwardsBias;
					double len = Math.sqrt(direction * direction + biasedY * biasedY);
					if (len > 1.0E-5F) {
						double rotation = Math.signum(-biasedY) * Math.acos(direction / len) * 180.0F / (float)Math.PI;
						SpiritFoxEntity.this.setXRot((float)rotation);
					}
				}
			}

			if (target != null && SpiritFoxEntity.this.distanceTo(target) <= 2.0F) {
				SpiritFoxEntity.this.doHurtTarget(getServerLevel(SpiritFoxEntity.this.level()), target);
			} else if (SpiritFoxEntity.this.getXRot() > 0.0F
				&& SpiritFoxEntity.this.onGround()
				&& (float)SpiritFoxEntity.this.getDeltaMovement().y != 0.0F
				&& SpiritFoxEntity.this.level().getBlockState(SpiritFoxEntity.this.blockPosition()).is(Blocks.SNOW)) {
				SpiritFoxEntity.this.setXRot(60.0F);
				SpiritFoxEntity.this.setTarget(null);
				SpiritFoxEntity.this.setFaceplanted(true);
			}
		}
	}

	private class FoxSearchForItemsGoal extends Goal {
		public FoxSearchForItemsGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (SpiritFoxEntity.this.isTame() || !SpiritFoxEntity.this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
				return false;
			}

			if (SpiritFoxEntity.this.getTarget() != null || SpiritFoxEntity.this.getLastHurtByMob() != null) {
				return false;
			}

			if (!SpiritFoxEntity.this.canMove()) {
				return false;
			}

			if (SpiritFoxEntity.this.getRandom().nextInt(reducedTickDelay(10)) != 0) {
				return false;
			}

			List<ItemEntity> items = SpiritFoxEntity.this.level().getEntitiesOfClass(ItemEntity.class, SpiritFoxEntity.this.getBoundingBox().inflate(8.0, 8.0, 8.0), SpiritFoxEntity.ALLOWED_ITEMS);
			return !items.isEmpty() && SpiritFoxEntity.this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty();
		}

		@Override
		public void tick() {
			List<ItemEntity> items = SpiritFoxEntity.this.level().getEntitiesOfClass(ItemEntity.class, SpiritFoxEntity.this.getBoundingBox().inflate(8.0, 8.0, 8.0), SpiritFoxEntity.ALLOWED_ITEMS);
			ItemStack itemStack = SpiritFoxEntity.this.getItemBySlot(EquipmentSlot.MAINHAND);
			if (itemStack.isEmpty() && !items.isEmpty()) {
				SpiritFoxEntity.this.getNavigation().moveTo(items.get(0), 1.2F);
			}
		}

		@Override
		public void start() {
			List<ItemEntity> items = SpiritFoxEntity.this.level().getEntitiesOfClass(ItemEntity.class, SpiritFoxEntity.this.getBoundingBox().inflate(8.0, 8.0, 8.0), SpiritFoxEntity.ALLOWED_ITEMS);
			if (!items.isEmpty()) {
				SpiritFoxEntity.this.getNavigation().moveTo(items.get(0), 1.2F);
			}
		}
	}

	private class FoxStrollThroughVillageGoal extends StrollThroughVillageGoal {
		public FoxStrollThroughVillageGoal(final int searchRadius, final int interval) {
			super(SpiritFoxEntity.this, interval);
		}

		@Override
		public void start() {
			SpiritFoxEntity.this.clearStates();
			super.start();
		}

		@Override
		public boolean canUse() {
			return super.canUse() && this.canFoxMove();
		}

		@Override
		public boolean canContinueToUse() {
			return super.canContinueToUse() && this.canFoxMove();
		}

		private boolean canFoxMove() {
			return !SpiritFoxEntity.this.isTame() && !SpiritFoxEntity.this.isSleeping() && !SpiritFoxEntity.this.isSitting() && !SpiritFoxEntity.this.isDefending()
				&& SpiritFoxEntity.this.getTarget() == null;
		}
	}

	private class PerchAndSearchGoal extends SpiritFoxEntity.FoxBehaviorGoal {
		private double relX;
		private double relZ;
		private int lookTime;
		private int looksRemaining;

		public PerchAndSearchGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return !SpiritFoxEntity.this.isTame()
				&& SpiritFoxEntity.this.getLastHurtByMob() == null
				&& SpiritFoxEntity.this.getRandom().nextFloat() < 0.02F
				&& !SpiritFoxEntity.this.isSleeping()
				&& SpiritFoxEntity.this.getTarget() == null
				&& SpiritFoxEntity.this.getNavigation().isDone()
				&& !this.alertable()
				&& !SpiritFoxEntity.this.isPouncing()
				&& !SpiritFoxEntity.this.isCrouching();
		}

		@Override
		public boolean canContinueToUse() {
			return this.looksRemaining > 0;
		}

		@Override
		public void start() {
			this.resetLook();
			this.looksRemaining = 2 + SpiritFoxEntity.this.getRandom().nextInt(3);
			SpiritFoxEntity.this.setSitting(true);
			SpiritFoxEntity.this.getNavigation().stop();
		}

		@Override
		public void stop() {
			SpiritFoxEntity.this.setSitting(false);
		}

		@Override
		public void tick() {
			this.lookTime--;
			if (this.lookTime <= 0) {
				this.looksRemaining--;
				this.resetLook();
			}

			SpiritFoxEntity.this.getLookControl()
				.setLookAt(SpiritFoxEntity.this.getX() + this.relX, SpiritFoxEntity.this.getEyeY(), SpiritFoxEntity.this.getZ() + this.relZ, SpiritFoxEntity.this.getMaxHeadYRot(), SpiritFoxEntity.this.getMaxHeadXRot());
		}

		private void resetLook() {
			double rnd = (Math.PI * 2) * SpiritFoxEntity.this.getRandom().nextDouble();
			this.relX = Math.cos(rnd);
			this.relZ = Math.sin(rnd);
			this.lookTime = this.adjustedTickDelay(80 + SpiritFoxEntity.this.getRandom().nextInt(20));
		}
	}

	private class SeekShelterGoal extends FleeSunGoal {
		private int interval = reducedTickDelay(100);

		public SeekShelterGoal(final double speedModifier) {
			super(SpiritFoxEntity.this, speedModifier);
		}

		@Override
		public boolean canUse() {
			if (!SpiritFoxEntity.this.isTame() && !SpiritFoxEntity.this.isSleeping() && this.mob.getTarget() == null) {
				if (SpiritFoxEntity.this.level().isThundering() && SpiritFoxEntity.this.level().canSeeSky(this.mob.blockPosition())) {
					return this.setWantedPos();
				} else if (this.interval > 0) {
					this.interval--;
					return false;
				} else {
					this.interval = 100;
					BlockPos pos = this.mob.blockPosition();
					return SpiritFoxEntity.this.level().isBrightOutside() && SpiritFoxEntity.this.level().canSeeSky(pos) && !((ServerLevel)SpiritFoxEntity.this.level()).isVillage(pos) && this.setWantedPos();
				}
			} else {
				return false;
			}
		}

		@Override
		public void start() {
			SpiritFoxEntity.this.clearStates();
			super.start();
		}
	}

	private class SleepGoal extends SpiritFoxEntity.FoxBehaviorGoal {
		private static final int WAIT_TIME_BEFORE_SLEEP = reducedTickDelay(140);
		private int countdown = SpiritFoxEntity.this.random.nextInt(WAIT_TIME_BEFORE_SLEEP);

		public SleepGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return SpiritFoxEntity.this.xxa == 0.0F && SpiritFoxEntity.this.yya == 0.0F && SpiritFoxEntity.this.zza == 0.0F ? this.canSleep() || SpiritFoxEntity.this.isSleeping() : false;
		}

		@Override
		public boolean canContinueToUse() {
			return this.canSleep();
		}

		private boolean canSleep() {
			if (this.countdown > 0) {
				this.countdown--;
				return false;
			} else {
				return !SpiritFoxEntity.this.isTame() && SpiritFoxEntity.this.level().isBrightOutside() && this.hasShelter() && !this.alertable() && !SpiritFoxEntity.this.isInPowderSnow;
			}
		}

		@Override
		public void stop() {
			this.countdown = SpiritFoxEntity.this.random.nextInt(WAIT_TIME_BEFORE_SLEEP);
			SpiritFoxEntity.this.clearStates();
		}

		@Override
		public void start() {
			SpiritFoxEntity.this.setSitting(false);
			SpiritFoxEntity.this.setIsCrouching(false);
			SpiritFoxEntity.this.setIsInterested(false);
			SpiritFoxEntity.this.setJumping(false);
			SpiritFoxEntity.this.setSleeping(true);
			SpiritFoxEntity.this.getNavigation().stop();
			SpiritFoxEntity.this.getMoveControl().setWantedPosition(SpiritFoxEntity.this.getX(), SpiritFoxEntity.this.getY(), SpiritFoxEntity.this.getZ(), 0.0);
		}
	}

	private class StalkPreyGoal extends Goal {
		public StalkPreyGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (SpiritFoxEntity.this.isSleeping()) {
				return false;
			}

			LivingEntity target = SpiritFoxEntity.this.getTarget();
			return target != null
				&& target.isAlive()
				&& SpiritFoxEntity.STALKABLE_PREY.test(target)
				&& SpiritFoxEntity.this.distanceToSqr(target) > 36.0
				&& !SpiritFoxEntity.this.isCrouching()
				&& !SpiritFoxEntity.this.isInterested()
				&& !SpiritFoxEntity.this.jumping;
		}

		@Override
		public void start() {
			SpiritFoxEntity.this.setSitting(false);
			SpiritFoxEntity.this.setFaceplanted(false);
		}

		@Override
		public void stop() {
			LivingEntity target = SpiritFoxEntity.this.getTarget();
			if (target != null && SpiritFoxEntity.isPathClear(SpiritFoxEntity.this, target)) {
				SpiritFoxEntity.this.setIsInterested(true);
				SpiritFoxEntity.this.setIsCrouching(true);
				SpiritFoxEntity.this.getNavigation().stop();
				SpiritFoxEntity.this.getLookControl().setLookAt(target, SpiritFoxEntity.this.getMaxHeadYRot(), SpiritFoxEntity.this.getMaxHeadXRot());
			} else {
				SpiritFoxEntity.this.setIsInterested(false);
				SpiritFoxEntity.this.setIsCrouching(false);
			}
		}

		@Override
		public void tick() {
			LivingEntity target = SpiritFoxEntity.this.getTarget();
			if (target != null) {
				SpiritFoxEntity.this.getLookControl().setLookAt(target, SpiritFoxEntity.this.getMaxHeadYRot(), SpiritFoxEntity.this.getMaxHeadXRot());
				if (SpiritFoxEntity.this.distanceToSqr(target) <= 36.0) {
					SpiritFoxEntity.this.setIsInterested(true);
					SpiritFoxEntity.this.setIsCrouching(true);
					SpiritFoxEntity.this.getNavigation().stop();
				} else {
					SpiritFoxEntity.this.getNavigation().moveTo(target, 1.5);
				}
			}
		}
	}
}
