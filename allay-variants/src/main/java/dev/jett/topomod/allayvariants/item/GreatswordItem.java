package dev.jett.topomod.allayvariants.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;

import java.util.List;

/**
 * A weapon with no tiers, like the mace and the trident. It hits for 30 at a full meter, and the meter takes 2 seconds
 * to fill but only does so while you stand still (see GreatswordStillness). A hit on a part-filled meter does much less
 * than vanilla's share of the damage.
 */
public class GreatswordItem extends Item {
	/** The 1 that bare hands do is added to this, for 30. */
	private static final int DEFAULT_ATTACK_DAMAGE = 29;
	/** 4 + this = 0.5 hits a second, so 2 seconds to fill the meter. */
	private static final float DEFAULT_ATTACK_SPEED = -3.5F;

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

	@Override
	public float getAttackDamageBonus(Entity target, float damage, DamageSource damageSource) {
		return GreatswordStillness.underchargedDamage(damage, damageSource);
	}

	@Override
	public void inventoryTick(ItemStack itemStack, ServerLevel level, Entity owner, EquipmentSlot slot) {
		if (slot == EquipmentSlot.MAINHAND && owner instanceof ServerPlayer player) {
			GreatswordStillness.tick(player, player.getLastClientInput());
		}
	}
}
