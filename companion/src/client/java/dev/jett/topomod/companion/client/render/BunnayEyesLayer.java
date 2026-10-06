package dev.jett.topomod.companion.client.render;

import dev.jett.topomod.companion.CompanionMod;

import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

// The bunnay's eyes glow: bunnay_eyes.png is just the eye pixels, drawn again over the model at full brightness no
// matter the light, the way a spider's eyes are.
public class BunnayEyesLayer extends EyesLayer<BunnayRenderState, BunnayModel> {
	private static final RenderType BUNNAY_EYES = RenderTypes.eyes(CompanionMod.id("textures/entity/bunnay/bunnay_eyes.png"));

	public BunnayEyesLayer(RenderLayerParent<BunnayRenderState, BunnayModel> parent) {
		super(parent);
	}

	@Override
	public RenderType renderType() {
		return BUNNAY_EYES;
	}
}
