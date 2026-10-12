package dev.jett.topomod.skyislands.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class BlubRenderState extends LivingEntityRenderState {
	/** Plays the idle; copied from the entity each frame. */
	public final AnimationState idleAnimationState = new AnimationState();
	/** True while it is sitting (a tame one that was told to sit). */
	public boolean isSitting;
}
