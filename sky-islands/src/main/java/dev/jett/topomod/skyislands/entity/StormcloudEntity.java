package dev.jett.topomod.skyislands.entity;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A storm cloud, a ball of small drifting cubes around a small glowing core. The cloud is only a zone (it has no hitbox you can hit): anyone who is inside it is hurt, and
 * only the little core in its middle can be attacked, so to kill it you have to push into the cloud. It drifts after the nearest player.
 */
public class StormcloudEntity extends Monster {
	/** How far the cloud reaches from the core in every direction, in blocks (the model's puffs reach 36 px = 2.25 blocks). */
	public static final float CLOUD_RADIUS = 2.25F;
	/** A player inside the cloud is hurt this much every this many ticks. */
	private static final float DAMAGE = 1.5F;
	private static final int DAMAGE_INTERVAL = 10;

	public StormcloudEntity(EntityType<? extends StormcloudEntity> type, Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl<>(this, 20, true);
	}

	/** A box around the cloud, to find who might be inside it; the cloud itself is a ball of this radius (see isInCloud). */
	public AABB cloudBox() {
		return AABB.ofSize(this.getBoundingBox().getCenter(), CLOUD_RADIUS * 2.0, CLOUD_RADIUS * 2.0, CLOUD_RADIUS * 2.0);
	}

	/** Whether any part of the entity is within the ball the cloud fills. */
	public boolean isInCloud(Entity entity) {
		return entity.getBoundingBox().distanceToSqr(this.getBoundingBox().getCenter()) <= (double)(CLOUD_RADIUS * CLOUD_RADIUS);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
		navigation.setCanOpenDoors(false);
		navigation.setCanFloat(true);
		return navigation;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new DriftToTargetGoal(this));
		this.goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 0.8));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.MOVEMENT_SPEED, 0.12)
			.add(Attributes.FLYING_SPEED, 0.14)
			.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	/** Spawns in open air with sky above it, in any light (the dimension is bright all day). */
	public static boolean checkSpawnRules(EntityType<StormcloudEntity> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return level.getBlockState(pos).isAir() && level.canSeeSky(pos);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.tickCount % DAMAGE_INTERVAL == 0) {
			for (Player player : level.getEntitiesOfClass(Player.class, this.cloudBox(), EntitySelector.NO_CREATIVE_OR_SPECTATOR)) {
				if (this.isInCloud(player)) {
					player.hurtServer(level, level.damageSources().magic(), DAMAGE);
				}
			}
		}
		// Wisps of cloud drift off it, and now and then a spark.
		if (this.tickCount % 3 == 0) {
			Vec3 centre = this.getBoundingBox().getCenter();
			level.sendParticles(ParticleTypes.CLOUD, centre.x, centre.y, centre.z, 4, CLOUD_RADIUS * 0.45, CLOUD_RADIUS * 0.45, CLOUD_RADIUS * 0.45, 0.01);
			if (this.random.nextInt(6) == 0) {
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centre.x, centre.y, centre.z, 3, CLOUD_RADIUS * 0.3, CLOUD_RADIUS * 0.3, CLOUD_RADIUS * 0.3, 0.2);
			}
		}
	}

	@Override
	protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.BREEZE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BREEZE_DEATH;
	}

	/** Floats toward its target, aiming a little above its feet so the target ends up in the middle of the cloud. */
	private static class DriftToTargetGoal extends Goal {
		private final StormcloudEntity cloud;

		DriftToTargetGoal(StormcloudEntity cloud) {
			this.cloud = cloud;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = this.cloud.getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public void tick() {
			LivingEntity target = this.cloud.getTarget();
			if (target != null && this.cloud.tickCount % 8 == 0) {
				this.cloud.getNavigation().moveTo(target.getX(), target.getY() + 1.0, target.getZ(), 1.0);
			}
		}

		@Override
		public void stop() {
			this.cloud.getNavigation().stop();
		}
	}
}
