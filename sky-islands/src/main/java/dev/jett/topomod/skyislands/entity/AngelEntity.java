package dev.jett.topomod.skyislands.entity;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// A small villager of the islands. It leaves you alone until it is hurt, then the whole village comes for you: the anger rules are a
// zombified piglin's (a persistent anger that lasts 20 to 39 seconds, and a call to every angel in earshot). About a third of them carry
// a bow and shoot, from a distance, at whatever they are angry at; the rest bite.

public class AngelEntity extends PathfinderMob implements NeutralMob, RangedAttackMob {
	private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
	private static final UniformInt ALERT_INTERVAL = TimeUtil.rangeOfSeconds(4, 6);
	private static final int ALERT_RANGE_Y = 10;
	/** The chance that a new angel is an archer. */
	private static final float ARCHER_CHANCE = 0.35F;
	private long persistentAngerEndTime;
	private @Nullable EntityReference<LivingEntity> persistentAngerTarget;
	private int ticksUntilNextAlert;

	public AngelEntity(EntityType<? extends AngelEntity> type, Level level) {
		super(type, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		// An angel with a bow shoots, one without bites: the goals that do not fit are not used.
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false) {
			@Override
			public boolean canUse() {
				return !AngelEntity.this.isArcher() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return !AngelEntity.this.isArcher() && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0, 30, 16.0F) {
			@Override
			public boolean canUse() {
				return AngelEntity.this.isArcher() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return AngelEntity.this.isArcher() && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
		this.targetSelector.addGoal(3, new ResetUniversalAngerTargetGoal<>(this, true));
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 16.0)
			.add(Attributes.MOVEMENT_SPEED, 0.25)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	/** An archer is an angel holding a bow. */
	public boolean isArcher() {
		return this.getMainHandItem().is(Items.BOW);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		groupData = super.finalizeSpawn(level, difficulty, reason, groupData);
		if (level.getRandom().nextFloat() < ARCHER_CHANCE) {
			this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
		}
		return groupData;
	}

	@Override
	public void performRangedAttack(LivingEntity target, float power) {
		ItemStack bow = this.getMainHandItem();
		ItemStack ammo = this.getProjectile(bow);
		AbstractArrow arrow = ProjectileUtil.getMobArrow(this, ammo, power, bow);
		double dx = target.getX() - this.getX();
		double dy = target.getY(0.3333333333333333) - arrow.getY();
		double dz = target.getZ() - this.getZ();
		double flat = Math.sqrt(dx * dx + dz * dz);
		if (this.level() instanceof ServerLevel serverLevel) {
			Projectile.spawnProjectileUsingShoot(arrow, serverLevel, ammo, dx, dy + flat * 0.2F, dz, 1.6F, this.rangedAttackUncertainty(serverLevel));
		}
		this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		this.updatePersistentAnger(level, true);
		if (this.getTarget() != null) {
			this.maybeAlertOthers();
		}
		super.customServerAiStep(level);
	}

	private void maybeAlertOthers() {
		if (this.ticksUntilNextAlert > 0) {
			this.ticksUntilNextAlert--;
		} else {
			if (this.getSensing().hasLineOfSight(this.getTarget())) {
				this.alertOthers();
			}
			this.ticksUntilNextAlert = ALERT_INTERVAL.sample(this.random);
		}
	}

	private void alertOthers() {
		double range = this.getAttributeValue(Attributes.FOLLOW_RANGE);
		AABB area = AABB.unitCubeFromLowerCorner(this.position()).inflate(range, ALERT_RANGE_Y, range);
		this.level().getEntitiesOfClass(AngelEntity.class, area, EntitySelector.NO_SPECTATORS).stream()
			.filter(other -> other != this)
			.filter(other -> other.getTarget() == null)
			.filter(other -> !other.isAlliedTo(this.getTarget()))
			.forEach(other -> other.setTarget(this.getTarget()));
	}

	@Override
	public void setTarget(@Nullable LivingEntity target) {
		if (this.getTarget() == null && target != null) {
			this.ticksUntilNextAlert = ALERT_INTERVAL.sample(this.random);
		}
		super.setTarget(target);
	}

	@Override
	public void startPersistentAngerTimer() {
		this.setTimeToRemainAngry(PERSISTENT_ANGER_TIME.sample(this.random));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		this.addPersistentAngerSaveData(output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.readPersistentAngerSaveData(this.level(), input);
	}

	@Override
	public void setPersistentAngerEndTime(long endTime) {
		this.persistentAngerEndTime = endTime;
	}

	@Override
	public long getPersistentAngerEndTime() {
		return this.persistentAngerEndTime;
	}

	@Override
	public void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> target) {
		this.persistentAngerTarget = target;
	}

	@Override
	public @Nullable EntityReference<LivingEntity> getPersistentAngerTarget() {
		return this.persistentAngerTarget;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ALLAY_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ALLAY_DEATH;
	}
}
