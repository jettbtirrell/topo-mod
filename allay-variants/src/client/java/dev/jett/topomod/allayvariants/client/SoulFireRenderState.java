package dev.jett.topomod.allayvariants.client;

/** Added to every entity render state (see EntityRenderStateMixin): whether the flames to draw on it are soul fire's. */
public interface SoulFireRenderState {
	boolean allayVariants$isOnSoulFire();

	void allayVariants$setOnSoulFire(boolean onSoulFire);
}
