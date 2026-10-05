package dev.jett.topomod.companion.client.render;

import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;

public class TopoRenderState extends ArmedEntityRenderState {
	public boolean sitting;
	/** True when the topo is holding something (a torch) in its paws. */
	public boolean holdingItem;
	/** Ticks since the victory dance started (with partial ticks), or -1 when not dancing. */
	public float danceTime = -1.0F;
}
