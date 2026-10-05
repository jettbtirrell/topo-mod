package dev.jett.topomod.companion.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class BunnayRenderState extends LivingEntityRenderState {
	/** Plays the big hop; copied from the entity each frame. */
	public final AnimationState bigHopAnimationState = new AnimationState();
	/** Plays the idle; copied from the entity each frame. */
	public final AnimationState idleAnimationState = new AnimationState();
	/** True while it is dancing to a jukebox. */
	public boolean isDancing;
}
