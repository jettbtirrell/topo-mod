package dev.jett.topomod.allayvariants.item;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;

/**
 * The greatsword's cooldown meter only fills while you stand still. The meter is vanilla's attack strength, which fills
 * by itself every tick, so this holds it empty whenever the player moves or jumps. Kept apart from GreatswordItem so it
 * can be changed or removed on its own. It runs on both sides, because the client keeps its own meter for the crosshair.
 */
public final class GreatswordStillness {
	/** Share of the full damage that a hit with an empty meter does (vanilla's is 0.2). */
	private static final float MIN_SHARE = 0.05F;
	/** How steeply the damage rises with the meter: 3 keeps a half-full meter at about a sixth (vanilla's is 40%). */
	private static final float EXPONENT = 5.0F;

	private GreatswordStillness() {
	}

	/**
	 * What to add to the damage of a hit (it comes out negative) so that a part-filled meter does much less than
	 * vanilla's share. Vanilla scales the hit by 0.2 + 0.8 * meter squared before the item's bonus is added, so this
	 * rescales that to MIN_SHARE + (1 - MIN_SHARE) * meter to the power of EXPONENT. At a full meter it adds nothing.
	 */
	public static float underchargedDamage(float damage, DamageSource source) {
		if (!(source.getDirectEntity() instanceof Player attacker)) {
			return 0.0F;
		}
		float meter = attacker.getAttackStrengthScale(0.5F);
		if (meter >= 1.0F) {
			return 0.0F;
		}
		float vanillaShare = 0.2F + 0.8F * meter * meter;
		float share = MIN_SHARE + (1.0F - MIN_SHARE) * (float) Math.pow(meter, EXPONENT);
		return damage * (share / vanillaShare - 1.0F);
	}

	/** Call once a tick while the greatsword is held. */
	public static void tick(Player player, Input input) {
		if (isMoving(player, input)) {
			player.resetOnlyAttackStrengthTicker();
		}
	}

	/** Walking, jumping, or in the air for any other reason, like a fall or a knockback. */
	private static boolean isMoving(Player player, Input input) {
		return input.forward() || input.backward() || input.left() || input.right() || input.jump() || !player.onGround();
	}
}
