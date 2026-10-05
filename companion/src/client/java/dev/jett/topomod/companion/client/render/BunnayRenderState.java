package dev.jett.topomod.companion.client.render;

import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class BunnayRenderState extends ArmedEntityRenderState {
	/** Plays the big hop; copied from the entity each frame. */
	public final AnimationState bigHopAnimationState = new AnimationState();
	/** Plays the idle; copied from the entity each frame. */
	public final AnimationState idleAnimationState = new AnimationState();
	/** True while it is dancing to a jukebox. */
	public boolean isDancing;
	/** True while it is sitting down (ordered to sit). */
	public boolean isSitting;
	/** How many ticks the current hop is in the air; the model stretches the hop clip to match. */
	public int hopAirTicks = 15;
	/** 0 to 1: how far into the ready stance (weapon raised, closing on a target) it is; eased by the entity. */
	public float readyProgress;
}
