package dev.jett.topomod.allayvariants.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;

import java.util.List;

/**
 * The blaze's answer to the mace: the mace rewards the height you set up, this rewards the fire you set up. Light the
 * target first (fire charge, flint and steel, fire aspect, lava), then hit it to use up its burning for extra damage;
 * see Cauterize. Only a hit on a full cooldown does it, so swinging quickly gets nothing extra. The Backdraft
 * enchantment spreads the fire to the creatures around the target. Fire-immune creatures and ones put out by water
 * give nothing to use up.
 */
public class ClubItem extends Item {
	private static final int DEFAULT_ATTACK_DAMAGE = 5;
	private static final float DEFAULT_ATTACK_SPEED = -3.2F;

	public ClubItem(Item.Properties properties) {
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
		return Cauterize.bonusDamage(target, damage, damageSource);
	}

	@Override
	public void postHurtEnemy(ItemStack itemStack, LivingEntity target, LivingEntity attacker) {
		if (attacker instanceof Player player && attacker.level() instanceof ServerLevel level) {
			Cauterize.consume(level, player, target, itemStack);
		}
	}
}
