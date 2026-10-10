package dev.jett.topomod.allayvariants.item;

/**
 * The greatsword's charge rules: how long you have held the use key, which level that is, and what a release at that
 * level does. Kept apart from GreatswordItem so they can be tuned or swapped without touching the item.
 */
public final class GreatswordCharge {
	/** Ticks of holding needed to reach levels 1, 2 and 3 (0.5, 1.0 and 1.5 seconds). */
	private static final int[] LEVEL_TICKS = {10, 20, 30};
	/** Held for longer than this (2 seconds) and the swing falls back to level 1. */
	private static final int OVERCHARGE_TICKS = 40;
	/** Damage of a release at level 0 (let go right away) up to level 3 (steeper than Monster Hunter's 81:106:135:168, so standing still to charge pays). */
	private static final float[] DAMAGE = {5.0F, 9.0F, 14.0F, 20.0F};
	private static final int OVERCHARGED_LEVEL = 1;

	public static final int MAX_LEVEL = LEVEL_TICKS.length;

	private GreatswordCharge() {
	}

	public static int level(int chargeTicks) {
		if (chargeTicks > OVERCHARGE_TICKS) {
			return OVERCHARGED_LEVEL;
		}
		int level = 0;
		while (level < MAX_LEVEL && chargeTicks >= LEVEL_TICKS[level]) {
			level++;
		}
		return level;
	}

	public static float damage(int level) {
		return DAMAGE[level];
	}
}
