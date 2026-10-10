package dev.jett.topomod.allayvariants.client.render;

import net.minecraft.client.renderer.entity.state.FoxRenderState;

/** The fox's render state with how high it carries its tails: up when it is well, drooping as it is hurt (the wolf's tail does the same). */
public class SpiritFoxRenderState extends FoxRenderState {
	/** Added to the tails' angle (radians; positive lifts them), 0 for a wild one. */
	public float tailLift;
}
