package dev.jett.topomod.companion.client.render;

import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;

public class TopoRenderState extends ArmedEntityRenderState {
	public boolean sitting;
	public enum HoldStyle {
		NONE,
		/** Both paws wrapped around the item, like the torch. */
		TWO_PAWS,
		/** Held out in the right paw only, like the amethyst shard. */
		ONE_PAW
	}

	public HoldStyle holdStyle = HoldStyle.NONE;
	/** True for the amethyst shard, which needs turning so its tip points forward. */
	public boolean holdingShard;
	/** Ticks since the victory dance started (with partial ticks), or -1 when not dancing. */
	public float danceTime = -1.0F;
}
