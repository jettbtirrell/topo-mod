package dev.jett.topomod.allayvariants.item;

import dev.jett.topomod.allayvariants.AllayVariantsMod;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * A weapon with no tiers, like the mace and the trident. Hit it like the mace for 6 damage on the same 1.67 second
 * attack cooldown, or hold the use key to charge a stronger swing that lands when the key is let go. The charge levels and what
 * they do are in GreatswordCharge; the shockwave of a full charge is in GreatswordSlam.
 */
public class GreatswordItem extends Item {
	private static final int DEFAULT_ATTACK_DAMAGE = 5;
	private static final float DEFAULT_ATTACK_SPEED = -3.4F;
	/** How long it can be held (like the trident). Past GreatswordCharge's overcharge point the charge only goes to waste. */
	private static final int USE_DURATION = 72000;
	/** Follow-through after a release: how long before the next charge can start. The attack cooldown only slows left clicks. */
	private static final int FOLLOW_THROUGH_TICKS = 12;
	/** Takes all of the jump away while charging, like the use effects take the walking. */
	private static final Identifier CHARGING_ID = AllayVariantsMod.id("greatsword_charging");
	private static final AttributeModifier CHARGING_NO_JUMP = new AttributeModifier(CHARGING_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

	public GreatswordItem(Item.Properties properties) {
		super(properties);
	}

	public static ItemAttributeModifiers createAttributes() {
		return ItemAttributeModifiers.builder()
			.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, DEFAULT_ATTACK_DAMAGE, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
			.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, DEFAULT_ATTACK_SPEED, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
			.build();
	}

	public static Tool createToolProperties() {
		return new Tool(List.of(), 1.0F, 2, false);
	}

	/** Planted while charging: no walking and no sprinting. */
	public static UseEffects createUseEffects() {
		return new UseEffects(false, true, 0.0F);
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack itemStack) {
		return ItemUseAnimation.BLOCK;
	}

	@Override
	public int getUseDuration(ItemStack itemStack, LivingEntity entity) {
		return USE_DURATION;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player.getCooldowns().isOnCooldown(player.getItemInHand(hand))) {
			return InteractionResult.FAIL;
		}
		player.startUsingItem(hand);
		if (!level.isClientSide()) {
			player.getAttribute(Attributes.JUMP_STRENGTH).addOrUpdateTransientModifier(CHARGING_NO_JUMP);
		}
		return InteractionResult.CONSUME;
	}

	/** Safety net: if a charge ended some way that skipped releaseUsing, the jump comes back. */
	@Override
	public void inventoryTick(ItemStack itemStack, ServerLevel level, Entity owner, EquipmentSlot slot) {
		if (owner instanceof LivingEntity living && !(living.isUsingItem() && living.getUseItem().is(this))) {
			living.getAttribute(Attributes.JUMP_STRENGTH).removeModifier(CHARGING_ID);
		}
	}

	@Override
	public boolean releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime) {
		if (!(entity instanceof Player player)) {
			return false;
		}
		if (level instanceof ServerLevel serverLevel) {
			player.getAttribute(Attributes.JUMP_STRENGTH).removeModifier(CHARGING_ID);
			int chargeLevel = GreatswordCharge.level(this.getUseDuration(itemStack, entity) - remainingTime);
			float damage = GreatswordCharge.damage(chargeLevel);
			InteractionHand hand = player.getUsedItemHand();
			player.swing(hand, SwingAnimation.DEFAULT, true);
			if (ProjectileUtil.getHitResultOnViewVector(player, target -> !target.isSpectator() && target.isPickable(), player.entityInteractionRange()) instanceof EntityHitResult hit
				&& hit.getEntity() instanceof LivingEntity target && target.isAttackable()) {
				target.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), damage);
				serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.0F);
				if (chargeLevel == GreatswordCharge.MAX_LEVEL) {
					GreatswordSlam.slam(serverLevel, player, target, target.position(), damage);
				}
				itemStack.hurtAndBreak(1, player, hand);
			}
			player.getCooldowns().addCooldown(itemStack, FOLLOW_THROUGH_TICKS);
		}
		player.resetAttackStrengthTicker();
		return true;
	}
}
