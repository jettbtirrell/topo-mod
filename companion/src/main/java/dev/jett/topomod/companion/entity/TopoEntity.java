package dev.jett.topomod.companion.entity;

import java.util.EnumSet;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.monster.Enemy;
import dev.jett.topomod.companion.menu.TopoMenu;
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
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

// The topo: a small tameable mouse companion. Smarter behavior and skills will build on this.
public class TopoEntity extends TamableAnimal {
	/** Length of the victory dance in ticks. The renderer uses this to time the animation. */
	public static final int DANCE_LENGTH = 50;
	/** How long a torch-hit keeps a target burning, in ticks. */
	private static final int TORCH_FIRE_TICKS = 100;
	/** Max health while wild, and once tamed (like wolves, taming also fully heals). */
	private static final double WILD_HEALTH = 10.0;
	private static final double TAME_HEALTH = 40.0;

	private static final EntityDataAccessor<Boolean> DATA_DANCING = SynchedEntityData.defineId(TopoEntity.class, EntityDataSerializers.BOOLEAN);

	/**
	 * Ticks into the current dance. Each side counts this itself (the server ends the dance), so the
	 * client's animation clock never jumps around with network timing.
	 */
	private int danceElapsed;

	public TopoEntity(EntityType<? extends TopoEntity> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_DANCING, false);
	}

	public static boolean isTorch(ItemStack stack) {
		return stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH);
	}

	public boolean isHoldingTorch() {
		return isTorch(this.getMainHandItem());
	}

	public boolean isDancing() {
		return this.entityData.get(DATA_DANCING);
	}

	/** Ticks since the dance began (with partial ticks), or -1 when not dancing. Used by the renderer. */
	public float getDanceTime(float partialTick) {
		return this.isDancing() ? Math.min(this.danceElapsed + partialTick, DANCE_LENGTH) : -1.0F;
	}

	public void startDance() {
		if (!this.isDancing() && !this.isOrderedToSit()) {
			this.danceElapsed = 0;
			this.entityData.set(DATA_DANCING, true);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.isDancing()) {
			this.danceElapsed++;
			if (!this.level().isClientSide() && this.danceElapsed >= DANCE_LENGTH) {
				this.entityData.set(DATA_DANCING, false);
			}
		} else {
			this.danceElapsed = 0;
		}
	}

	// A burning zombie that hits the topo would set it alight too, so a topo holding a torch ignores fire.
	@Override
	public void setRemainingFireTicks(int remainingTicks) {
		if (remainingTicks > 0 && this.isHoldingTorch()) {
			return;
		}
		super.setRemainingFireTicks(remainingTicks);
	}

	// Called on whoever gets credit for a kill, which also covers enemies that burn to death after a torch hit.
	@Override
	public void awardKillScore(Entity victim, DamageSource killingBlow) {
		super.awardKillScore(victim, killingBlow);
		if (!this.level().isClientSide() && victim instanceof Enemy) {
			this.startDance();
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && this.isHoldingTorch()) {
			target.igniteForTicks(TORCH_FIRE_TICKS);
		}
		return hit;
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
		this.goalSelector.addGoal(1, new TamableAnimalPanicGoal(1.5));
		this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		this.goalSelector.addGoal(2, new DanceGoal(this));
		this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, true));
		this.goalSelector.addGoal(4, new TemptGoal(this, 1.0, this::isFood, false));
		this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.1, 8.0F, 2.5F));
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

				// Handing it a torch equips it straight away.
				if (isTorch(stack) && this.getMainHandItem().isEmpty()) {
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

	/** Stands still and lets the model do its victory dance. */
	private static final class DanceGoal extends Goal {
		private final TopoEntity topo;

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
		public void start() {
			this.topo.getNavigation().stop();
		}
	}
}
