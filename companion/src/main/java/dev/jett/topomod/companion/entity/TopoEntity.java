package dev.jett.topomod.companion.entity;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import dev.jett.topomod.companion.CompanionMod;
import dev.jett.topomod.companion.menu.TopoMenu;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

// The topo: a small tameable mouse companion. Smarter behavior and skills will build on this.
public class TopoEntity extends TamableAnimal {
	/** Length of the victory dance in ticks. The renderer uses this to time the animation. */
	public static final int DANCE_LENGTH = 50;
	/** The dances a topo can do; one is picked at random each time. The renderer plays the matching animation. */
	public static final int DANCE_SPIN = 0;
	public static final int DANCE_BACKFLIP = 1;
	public static final int DANCE_MOONWALK = 2;
	/** Relative odds of each dance: 70% the main spin, 20% the moonwalk, 10% the backflip. */
	private static final int DANCE_SPIN_WEIGHT = 70;
	private static final int DANCE_MOONWALK_WEIGHT = 20;
	private static final int DANCE_BACKFLIP_WEIGHT = 10;
	private static final int BACKFLIP_LENGTH = 50;
	private static final int MOONWALK_LENGTH = 70;
	/** How fast the moonwalk glides backward, in blocks per tick. */
	private static final double MOONWALK_SPEED = 0.05;

	// Idle animations: little bits of life while the topo is standing around. The renderer plays the matching one.
	public static final int IDLE_NONE = 0;
	public static final int IDLE_LOOK_AROUND = 1;
	public static final int IDLE_GROOM = 2;
	public static final int IDLE_TAIL_CHASE = 3;
	public static final int IDLE_SHAKE = 4;
	public static final int IDLE_HOP = 5;
	/**
	 * Only grooming is enabled; the other idle animations are still in the model but are never picked
	 * (the debug command can still play them). Like the copper golem's idle, one starts a fixed wait after the last
	 * one ended: IDLE_PAUSE_MIN to IDLE_PAUSE_MIN + IDLE_PAUSE_RANGE ticks (10 to 12 seconds) of standing still.
	 */
	private static final int IDLE_PAUSE_MIN = 200;
	private static final int IDLE_PAUSE_RANGE = 40;

	/** Length of an idle animation in ticks. */
	public static int idleLength(int style) {
		return switch (style) {
			case IDLE_LOOK_AROUND -> 50;
			case IDLE_GROOM -> 60;
			case IDLE_TAIL_CHASE -> 36;
			case IDLE_SHAKE -> 20;
			case IDLE_HOP -> 24;
			default -> 0;
		};
	}

	/** Length of a dance in ticks. Each dance has its own pace. */
	public static int danceLength(int style) {
		return switch (style) {
			case DANCE_BACKFLIP -> BACKFLIP_LENGTH;
			case DANCE_MOONWALK -> MOONWALK_LENGTH;
			default -> DANCE_LENGTH;
		};
	}
	/** How long a torch-hit keeps a target burning, in ticks. */
	private static final int TORCH_FIRE_TICKS = 100;
	/** Max health while wild, and once tamed (like wolves, taming also fully heals). */
	private static final double WILD_HEALTH = 10.0;
	private static final double TAME_HEALTH = 40.0;
	/** Extra damage per hit while holding an amethyst shard (on top of the base attack). */
	private static final double SHARD_BONUS_DAMAGE = 3.0;
	private static final Identifier SHARD_DAMAGE_ID = CompanionMod.id("amethyst_shard_damage");

	// Resonance combo (amethyst shard): hits on one target in quick succession climb a musical scale,
	// and the SHATTER_AT-th hit shatters for bonus damage and knockback.
	private static final int SHATTER_AT = 4;
	private static final double SHATTER_BONUS_DAMAGE = 4.0;
	/** Ticks the combo survives between hits. */
	private static final int COMBO_WINDOW = 60;
	/** Semitones above the base pitch for each step of the combo (a major pentatonic scale). */
	private static final int[] COMBO_SCALE = {0, 2, 4, 7, 9};
	/**
	 * Volume of the amethyst sounds. Above 1.0 this does not make them louder, it makes them carry
	 * further: they can be heard (volume x 16) blocks away.
	 */
	private static final float CHIME_VOLUME = 3.0F;
	/** Pitch multipliers for the layered clink: one copy per entry, so more entries means louder. */
	private static final float[] CLINK_DETUNE = {0.97F, 1.0F, 1.03F};

	// Ender pearl: a hit sends the target at most this far from the topo (straight-line distance), in a random direction.
	private static final double PEARL_MAX_DISTANCE = 10.0;
	/** How long the topo keeps a teleported target even when it is out of range, in ticks. */
	private static final int PEARL_KEEP_TARGET_TICKS = 200;
	/** Chance a pearl hit instead launches the target up through the air (if there is room). */
	private static final double PEARL_SKY_CHANCE = 0.10;
	/** An air launch is still PEARL_MAX_DISTANCE away, aimed upward at an angle in this range (degrees above flat). */
	private static final double PEARL_SKY_MIN_ELEVATION = 35.0;
	private static final double PEARL_SKY_MAX_ELEVATION = 70.0;


	private static final EntityDataAccessor<Boolean> DATA_DANCING = SynchedEntityData.defineId(TopoEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> DATA_IDLE_STYLE = SynchedEntityData.defineId(TopoEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_DANCE_STYLE = SynchedEntityData.defineId(TopoEntity.class, EntityDataSerializers.INT);

	/**
	 * Ticks into the current dance. Each side counts this itself (the server ends the dance), so the
	 * client's animation clock never jumps around with network timing.
	 */
	private int danceElapsed;

	/** Same idea for idle animations: each side counts its own elapsed ticks, and the server decides when they end. */
	private int idleElapsed;
	private int idlePause = IDLE_PAUSE_MIN / 2;

	/** The last mob this topo hit, and when to stop caring about it (server only). */
	private @Nullable LivingEntity lastVictim;
	private int lastVictimExpiresAt;

	/** The mob a pearl hit just sent away, and how long the topo keeps hold of it as its target (server only). */
	private @Nullable LivingEntity pearlTarget;
	private int pearlTargetUntil;

	private @Nullable UUID comboTarget;
	private int comboCount;
	private int comboExpiresAt;



	public TopoEntity(EntityType<? extends TopoEntity> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_DANCING, false);
		entityData.define(DATA_DANCE_STYLE, DANCE_SPIN);
		entityData.define(DATA_IDLE_STYLE, IDLE_NONE);
	}

	public static boolean isTorch(ItemStack stack) {
		return stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH);
	}

	public static boolean isAmethystShard(ItemStack stack) {
		return stack.is(Items.AMETHYST_SHARD);
	}

	public static boolean isEnderPearl(ItemStack stack) {
		return stack.is(Items.ENDER_PEARL);
	}

	/** Anything the topo can hold in its paws. */
	public static boolean isHoldable(ItemStack stack) {
		return isTorch(stack) || isAmethystShard(stack) || isEnderPearl(stack);
	}

	// Mobs spawn left-handed 5% of the time (and that is saved), which would put the held item in the left
	// paw. The model and held item layer only handle the right paw, so every topo is right-handed.
	@Override
	public HumanoidArm getMainArm() {
		return HumanoidArm.RIGHT;
	}

	public boolean isHoldingTorch() {
		return isTorch(this.getMainHandItem());
	}

	public boolean isHoldingAmethystShard() {
		return isAmethystShard(this.getMainHandItem());
	}

	public boolean isHoldingEnderPearl() {
		return isEnderPearl(this.getMainHandItem());
	}

	/** Picks a dance at random: the main spin dance most of the time, the moonwalk now and then, the backflip rarely. */
	private int pickDanceStyle() {
		int roll = this.random.nextInt(DANCE_SPIN_WEIGHT + DANCE_MOONWALK_WEIGHT + DANCE_BACKFLIP_WEIGHT);
		if (roll < DANCE_SPIN_WEIGHT) {
			return DANCE_SPIN;
		}
		return roll < DANCE_SPIN_WEIGHT + DANCE_MOONWALK_WEIGHT ? DANCE_MOONWALK : DANCE_BACKFLIP;
	}

	/** Which idle animation is playing (one of the IDLE_ constants), or IDLE_NONE. */
	public int getIdleStyle() {
		return this.entityData.get(DATA_IDLE_STYLE);
	}

	/** Ticks since the idle animation began (with partial ticks), or -1 when none is playing. */
	public float getIdleTime(float partialTick) {
		int style = this.getIdleStyle();
		return style == IDLE_NONE ? -1.0F : Math.min(this.idleElapsed + partialTick, idleLength(style));
	}

	/** Which dance is playing (one of the DANCE_ constants); only meaningful while dancing. */
	public int getDanceStyle() {
		return this.entityData.get(DATA_DANCE_STYLE);
	}

	public boolean isDancing() {
		return this.entityData.get(DATA_DANCING);
	}

	/** Ticks since the dance began (with partial ticks), or -1 when not dancing. Used by the renderer. */
	public float getDanceTime(float partialTick) {
		return this.isDancing() ? Math.min(this.danceElapsed + partialTick, danceLength(this.getDanceStyle())) : -1.0F;
	}

	public void startDance() {
		if (!this.isDancing() && !this.isOrderedToSit()) {
			this.danceElapsed = 0;
			// Set the style first so the client never sees a dance without one.
			this.entityData.set(DATA_DANCE_STYLE, this.pickDanceStyle());
			this.entityData.set(DATA_DANCING, true);
		}
	}

	// DEBUG: used by TopoDebugCommand to preview animations on demand. Remove together with that command.
	/** Starts the given idle animation right now (server side). Returns false if one is already playing. */
	public boolean debugPlayIdle(int style) {
		if (this.getIdleStyle() != IDLE_NONE) {
			return false;
		}
		this.idleElapsed = 0;
		this.entityData.set(DATA_IDLE_STYLE, style);
		return true;
	}

	// DEBUG: see debugPlayIdle. Returns false if a dance is already playing.
	public boolean debugPlayDance(int style) {
		if (this.isDancing()) {
			return false;
		}
		this.danceElapsed = 0;
		this.entityData.set(DATA_DANCE_STYLE, style);
		this.entityData.set(DATA_DANCING, true);
		return true;
	}

	@Override
	public void aiStep() {
		super.aiStep();

		if (!this.level().isClientSide() && this.lastVictim != null) {
			// Dance when something the topo hit has died. This is checked directly, not via kill credit,
			// because credit goes to the player first if they also hit the mob, and burning deaths have none.
			if (!this.lastVictim.isAlive()) {
				this.lastVictim = null;
				this.startDance();
			} else if (this.tickCount > this.lastVictimExpiresAt) {
				this.lastVictim = null;
			}
		}

		// Straight back to work if there is another enemy to fight.
		if (!this.level().isClientSide() && this.isDancing()) {
			LivingEntity target = this.getTarget();
			if (target != null && target.isAlive()) {
				this.entityData.set(DATA_DANCING, false);
			}
		}

		if (this.isDancing()) {
			this.danceElapsed++;
			if (!this.level().isClientSide() && this.danceElapsed >= danceLength(this.getDanceStyle())) {
				this.entityData.set(DATA_DANCING, false);
			}
		} else {
			this.danceElapsed = 0;
		}

		this.tickIdle();
	}

	/** Starts, plays and ends the idle animations. The server decides; the client just counts along. */
	private void tickIdle() {
		int style = this.getIdleStyle();
		if (this.level().isClientSide()) {
			this.idleElapsed = style == IDLE_NONE ? 0 : this.idleElapsed + 1;
			return;
		}

		if (style != IDLE_NONE) {
			this.idleElapsed++;
			if (this.idleElapsed >= idleLength(style) || !this.canIdle()) {
				this.stopIdle();
			}
			return;
		}

		this.idleElapsed = 0;
		if (this.idlePause > 0) {
			this.idlePause--;
		} else if (this.canIdle()) {
			this.startIdle();
		}
	}

	/** Only when the topo is standing about doing nothing: not fighting, dancing, sitting, walking or swimming. */
	private boolean canIdle() {
		LivingEntity target = this.getTarget();
		return !this.isDancing()
			&& !this.isOrderedToSit()
			&& !this.isInSittingPose()
			&& (target == null || !target.isAlive())
			&& this.onGround()
			&& !this.isInWater()
			&& !this.isPassenger()
			&& this.hurtTime == 0
			&& this.getNavigation().isDone()
			&& this.getDeltaMovement().horizontalDistanceSqr() < 4.0E-4;
	}

	private void startIdle() {
		// Grooming needs both paws free to wash its face, so a topo holding something has no idle at all.
		if (!this.getMainHandItem().isEmpty()) {
			return;
		}
		this.idleElapsed = 0;
		this.entityData.set(DATA_IDLE_STYLE, IDLE_GROOM);
	}

	private void stopIdle() {
		this.entityData.set(DATA_IDLE_STYLE, IDLE_NONE);
		this.idleElapsed = 0;
		this.idlePause = IDLE_PAUSE_MIN + this.random.nextInt(IDLE_PAUSE_RANGE);
	}

	// Ignore attempts to clear the target while a pearl-teleported mob is still its target (see onPearlHit).
	@Override
	public void setTarget(@Nullable LivingEntity target) {
		if (target == null
			&& this.pearlTarget != null
			&& this.getTarget() == this.pearlTarget
			&& this.pearlTarget.isAlive()
			&& this.tickCount < this.pearlTargetUntil
			&& this.isHoldingEnderPearl()
			&& !this.isOrderedToSit()) {
			return;
		}
		super.setTarget(target);
	}

	// A burning zombie that hits the topo would set it alight too, so a topo holding a torch ignores fire.
	@Override
	public void setRemainingFireTicks(int remainingTicks) {
		if (remainingTicks > 0 && this.isHoldingTorch()) {
			return;
		}
		super.setRemainingFireTicks(remainingTicks);
	}

	// Called on whoever gets credit for a kill, which also covers mobs that burn to death after a torch hit.
	@Override
	public void awardKillScore(Entity victim, DamageSource killingBlow) {
		super.awardKillScore(victim, killingBlow);
		if (!this.level().isClientSide()) {
			this.startDance();
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean shard = this.isHoldingAmethystShard();

		// Resonance combo: which hit in the chain is this? Every hit on the same target within the window adds one.
		int comboStep = 0;
		boolean shatter = false;
		if (shard && target instanceof LivingEntity) {
			boolean continuing = target.getUUID().equals(this.comboTarget) && this.tickCount <= this.comboExpiresAt;
			comboStep = continuing ? this.comboCount + 1 : 1;
			shatter = comboStep >= SHATTER_AT;
		}

		// The shard has no attack stat of its own, so add its bonus just for this swing.
		double bonus = shard ? SHARD_BONUS_DAMAGE + (shatter ? SHATTER_BONUS_DAMAGE : 0.0) : 0.0;
		AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
		if (bonus > 0.0) {
			attack.addTransientModifier(new AttributeModifier(SHARD_DAMAGE_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
		}
		boolean hit;
		try {
			hit = super.doHurtTarget(level, target);
		} finally {
			if (bonus > 0.0) {
				attack.removeModifier(SHARD_DAMAGE_ID);
			}
		}

		if (hit && target instanceof LivingEntity victim) {
			this.lastVictim = victim;
			// Long enough for a burning mob to die after the last hit.
			this.lastVictimExpiresAt = this.tickCount + 200;

			if (shard) {
				this.onShardHit(level, victim, comboStep, shatter);
			}
			if (this.isHoldingEnderPearl()) {
				this.onPearlHit(level, victim);
			}
		}
		if (hit && this.isHoldingTorch()) {
			target.igniteForTicks(TORCH_FIRE_TICKS);
		}
		return hit;
	}

	/** Chimes up the scale, and shatters on the last step of the combo. */
	private void onShardHit(ServerLevel level, LivingEntity victim, int comboStep, boolean shatter) {
		double x = victim.getX();
		double y = victim.getY() + victim.getBbHeight() / 2.0;
		double z = victim.getZ();

		float pitch = 0.9F * (float) Math.pow(2.0, COMBO_SCALE[Math.min(comboStep, COMBO_SCALE.length) - 1] / 12.0);
		level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, CHIME_VOLUME, Math.min(pitch, 2.0F));
		// A fifth above it makes the chime fuller and easier to hear.
		level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, CHIME_VOLUME, Math.min(pitch * 1.5F, 2.0F));
		// The chime above is a very quiet file, and a volume over 1.0 only widens the range, so to bring the normal
		// hit sounds up toward the burst's loudness the clink is layered a few times (slightly detuned so the copies
		// don't cancel each other), with the resonating ring of the amethyst blocks underneath for a tone.
		for (float detune : CLINK_DETUNE) {
			level.playSound(null, x, y, z, SoundEvents.AMETHYST_CLUSTER_HIT, SoundSource.NEUTRAL, CHIME_VOLUME, pitch * detune);
		}
		level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, CHIME_VOLUME, Math.min(pitch, 2.0F));
		level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, CHIME_VOLUME, Math.min(pitch * 1.5F, 2.0F));
		level.sendParticles(ParticleTypes.WITCH, x, y, z, 6, 0.25, 0.3, 0.25, 0.02);

		if (shatter) {
			this.comboTarget = null;
			this.comboCount = 0;
			level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.NEUTRAL, CHIME_VOLUME, 1.2F);
			level.playSound(null, x, y, z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, CHIME_VOLUME, 1.0F);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()), x, y, z, 30, 0.35, 0.45, 0.35, 0.15);
			victim.knockback(0.9, this.getX() - victim.getX(), this.getZ() - victim.getZ(), this.damageSources().mobAttack(this), 0.0F);
		} else {
			this.comboTarget = victim.getUUID();
			this.comboCount = comboStep;
			this.comboExpiresAt = this.tickCount + COMBO_WINDOW;
		}
	}

	/**
	 * Sends the target up to 10 blocks away in a random direction, or occasionally up into the air, never more than 10 blocks from the topo, then
	 * switches to a closer enemy that is attacking the owner, if there is one.
	 */
	private void onPearlHit(ServerLevel level, LivingEntity victim) {
		if (victim instanceof Player) {
			return;
		}

		LivingEntity owner = this.getOwner();
		Vec3 from = victim.position();
		boolean moved = this.random.nextDouble() < PEARL_SKY_CHANCE && this.teleportIntoAir(level, victim);
		if (!moved) {
			moved = this.teleportAround(victim);
		}
		if (!moved) {
			return;
		}

		level.broadcastEntityEvent(victim, (byte) 46);
		level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.0F);
		level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.0F);
		level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 0.5, from.z, 25, 0.3, 0.5, 0.3, 0.3);

		// Keep this mob as the target while it is sent away or falling out of the sky. Target goals normally give
		// up on anything beyond the 16-block follow range, and a sky launch puts it about 21 blocks away.
		this.pearlTarget = victim;
		this.pearlTargetUntil = this.tickCount + PEARL_KEEP_TARGET_TICKS;

		// Stay on the same target, unless there is an enemy closer to the owner that is going for the owner.
		if (owner != null) {
			double victimDistance = victim.distanceToSqr(owner);
			List<Mob> threats = level.getEntitiesOfClass(
				Mob.class,
				owner.getBoundingBox().inflate(16.0),
				mob -> mob != victim && mob != this && mob instanceof Enemy && mob.isAlive() && mob.getTarget() == owner
					&& mob.distanceToSqr(owner) < victimDistance && this.wantsToAttack(mob, owner)
			);
			threats.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(owner)));
			if (!threats.isEmpty()) {
				this.setTarget(threats.get(0));
			}
		}
	}

	/**
	 * Finds ground up to 10 blocks (straight-line) from the topo, in a random direction, trying shorter if that is
	 * blocked. Minecraft's own ground search can drop a target far down a cliff or into a cave, so a landing spot
	 * that ends up farther than the limit is undone and another is tried.
	 */
	private boolean teleportAround(LivingEntity victim) {
		Vec3 from = victim.position();
		double[] distances = {PEARL_MAX_DISTANCE * 0.95, PEARL_MAX_DISTANCE * 0.65, PEARL_MAX_DISTANCE * 0.35};
		for (double distance : distances) {
			for (int attempt = 0; attempt < 10; attempt++) {
				double angle = this.random.nextDouble() * Math.PI * 2.0;
				// Start above the topo's height so slopes are fine; the target settles down onto the ground.
				if (victim.randomTeleport(
					this.getX() + Math.cos(angle) * distance, this.getY() + 4.0, this.getZ() + Math.sin(angle) * distance, false, state -> false
				)) {
					if (victim.distanceTo(this) <= PEARL_MAX_DISTANCE + 0.25) {
						return true;
					}
					victim.teleportTo(from.x, from.y, from.z);
				}
			}
		}
		return false;
	}

	/** Puts the target 10 blocks away at most, up in the air in a random direction, if there is room to fit it. */
	private boolean teleportIntoAir(ServerLevel level, LivingEntity victim) {
		for (int attempt = 0; attempt < 8; attempt++) {
			double angle = this.random.nextDouble() * Math.PI * 2.0;
			double elevation = Math.toRadians(
				PEARL_SKY_MIN_ELEVATION + this.random.nextDouble() * (PEARL_SKY_MAX_ELEVATION - PEARL_SKY_MIN_ELEVATION)
			);
			double sideways = Math.cos(elevation) * PEARL_MAX_DISTANCE;
			double x = this.getX() + Math.cos(angle) * sideways;
			double y = this.getY() + Math.sin(elevation) * PEARL_MAX_DISTANCE;
			double z = this.getZ() + Math.sin(angle) * sideways;
			if (y + victim.getBbHeight() >= level.getMaxY() || !level.hasChunkAt(BlockPos.containing(x, y, z))) {
				continue;
			}
			AABB box = victim.getDimensions(victim.getPose()).makeBoundingBox(x, y, z);
			if (level.noCollision(box) && !level.containsAnyLiquid(box)) {
				victim.teleportTo(x, y, z);
				if (victim instanceof Mob mob) {
					mob.getNavigation().stop();
				}
				return true;
			}
		}
		return false;
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
		this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		this.goalSelector.addGoal(2, new DanceGoal(this));
		this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, true));
		this.goalSelector.addGoal(4, new TemptGoal(this, 1.0, this::isFood, false));
		this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.1, 8.0F, 2.5F));
		// Same priority as strolling, so it can't be started over by a stroll but the owner walking off (5) interrupts it.
		this.goalSelector.addGoal(6, new IdleGoal(this));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

		// Fight for the owner: defend them, back up their attacks, and retaliate when hurt.
		this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
		this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.WHEAT_SEEDS);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (this.isTame()) {
			if (this.isOwnedBy(player)) {
				// Sneak + right-click opens the equipment screen.
				if (player.isSecondaryUseActive()) {
					if (!this.level().isClientSide()) {
						player.openMenu(new EquipmentMenuProvider());
					}
					return InteractionResult.SUCCESS;
				}

				// Handing it a torch, amethyst shard or ender pearl equips it straight away.
				if (isHoldable(stack) && this.getMainHandItem().isEmpty()) {
					if (!this.level().isClientSide()) {
						this.setItemSlot(EquipmentSlot.MAINHAND, stack.split(1));
						this.setGuaranteedDrop(EquipmentSlot.MAINHAND);
						this.setPersistenceRequired();
					}
					return InteractionResult.SUCCESS;
				}

				if (this.isFood(stack) && this.getHealth() < this.getMaxHealth()) {
					if (!this.level().isClientSide()) {
						this.heal(2.0F);
						stack.consume(1, player);
					}
					return InteractionResult.SUCCESS;
				}

				// Empty-handed (or any non-food) click toggles sitting.
				if (!this.level().isClientSide()) {
					this.setOrderedToSit(!this.isOrderedToSit());
				}
				return InteractionResult.SUCCESS;
			}
		} else if (this.isFood(stack)) {
			if (!this.level().isClientSide()) {
				stack.consume(1, player);
				if (this.random.nextInt(3) == 0) {
					this.tame(player);
					this.setOrderedToSit(true);
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
	public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
		// A tiny mouse picking a fight with a creeper just gets everyone blown up.
		if (target instanceof Creeper) {
			return false;
		}
		// Never attack the owner or another companion belonging to the same owner.
		if (target instanceof TamableAnimal tamable && tamable.isTame() && tamable.getOwner() == owner) {
			return false;
		}
		return target != owner;
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

	/** Opens the equipment menu; the client is told which topo it belongs to via the entity id. */
	private final class EquipmentMenuProvider implements ExtendedMenuProvider<Integer> {
		@Override
		public Component getDisplayName() {
			return TopoEntity.this.getDisplayName();
		}

		@Override
		public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
			return new TopoMenu(containerId, inventory, TopoEntity.this);
		}

		@Override
		public Integer getScreenOpeningData(ServerPlayer player) {
			return TopoEntity.this.getId();
		}
	}

	/**
	 * Stands still and lets the model do its victory dance. The moonwalk is the exception: the topo really does
	 * glide backward, keeping its face toward where it was looking.
	 */
	private static final class DanceGoal extends Goal {
		private final TopoEntity topo;
		private Vec3 backward = Vec3.ZERO;

		DanceGoal(TopoEntity topo) {
			this.topo = topo;
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			net.minecraft.world.entity.LivingEntity target = this.topo.getTarget();
			boolean fighting = target != null && target.isAlive();
			return this.topo.isDancing() && !this.topo.isOrderedToSit() && !fighting;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			this.topo.getNavigation().stop();
			// Minecraft's forward is (-sin yaw, cos yaw), so backward is the opposite.
			double yaw = Math.toRadians(this.topo.yBodyRot);
			this.backward = new Vec3(Math.sin(yaw), 0.0, -Math.cos(yaw));
		}

		@Override
		public void tick() {
			if (this.topo.getDanceStyle() != DANCE_MOONWALK) {
				return;
			}
			// Don't glide off a ledge: there has to be ground a short way behind.
			Vec3 step = this.backward.scale(MOONWALK_SPEED);
			BlockPos below = BlockPos.containing(this.topo.getX() + step.x * 10.0, this.topo.getY() - 0.5, this.topo.getZ() + step.z * 10.0);
			if (!this.topo.level().getBlockState(below).getCollisionShape(this.topo.level(), below).isEmpty()) {
				Vec3 motion = this.topo.getDeltaMovement();
				this.topo.setDeltaMovement(step.x, motion.y, step.z);
			}
		}
	}

	/** Keeps the topo still, and looking where the animation wants, while an idle animation plays. */
	private static final class IdleGoal extends Goal {
		private final TopoEntity topo;

		IdleGoal(TopoEntity topo) {
			this.topo = topo;
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return this.topo.getIdleStyle() != IDLE_NONE;
		}

		@Override
		public boolean canContinueToUse() {
			return this.topo.getIdleStyle() != IDLE_NONE;
		}

		@Override
		public void start() {
			this.topo.getNavigation().stop();
		}
	}
}
