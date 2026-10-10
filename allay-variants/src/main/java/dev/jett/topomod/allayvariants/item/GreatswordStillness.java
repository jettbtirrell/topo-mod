package dev.jett.topomod.allayvariants.item;

import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;

/**
 * The greatsword's cooldown meter only fills while you stand still. The meter is vanilla's attack strength, which fills
 * by itself every tick, so this holds it empty whenever the player moves or jumps. Kept apart from GreatswordItem so it
 * can be changed or removed on its own. It runs on both sides, because the client keeps its own meter for the crosshair.
 */
public final class GreatswordStillness {
	private GreatswordStillness() {
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
