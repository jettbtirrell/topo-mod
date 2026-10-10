package dev.jett.topomod.allayvariants.item;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * The attack cooldown meter as it was when a hit began. By the time an item is asked for its damage bonus vanilla has
 * already reset the meter (reading it then always gives the freshly reset 0.025), so it is worked back out from the
 * damage the hit has, which vanilla scaled by 0.2 + 0.8 * meter squared.
 */
public final class AttackMeter {
	private AttackMeter() {
	}

	/** {@code scaledDamage} is the damage passed to Item.getAttackDamageBonus. Returns 0 for an empty meter, 1 for a full one. */
	public static float ofHit(Player attacker, float scaledDamage) {
		float attack = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
		if (attack <= 0.0F) {
			return 1.0F;
		}
		float factor = Mth.clamp(scaledDamage / attack, 0.2F, 1.0F);
		return Mth.sqrt((factor - 0.2F) / 0.8F);
	}

	public static boolean isFull(float meter) {
		return meter >= 0.995F;
	}
}
