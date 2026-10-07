package dev.jett.topomod.companion.client.render;

import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class BunnayRenderState extends ArmedEntityRenderState {
	/** Plays the hop; copied from the entity each frame. */
	public final AnimationState hopAnimationState = new AnimationState();
	/** How many ticks the current hop spends in the air; the hop clip is played at the speed that makes it fit. */
	public float hopAirTicks = 15.0F;
	/** Plays the idle; copied from the entity each frame. */
	public final AnimationState idleAnimationState = new AnimationState();
	/** True while it is dancing to a jukebox. */
	public boolean isDancing;
	/** True while it is sitting down (ordered to sit). */
	public boolean isSitting;
	/** 0 to 1: how far into the ready stance (weapon raised, closing on a target) it is; eased by the entity. */
	public float readyProgress;
	/** 0 to 1: how far into the eating pose (a carrot raised to its mouth) it is; eased by the entity. */
	public float eatProgress;
}
