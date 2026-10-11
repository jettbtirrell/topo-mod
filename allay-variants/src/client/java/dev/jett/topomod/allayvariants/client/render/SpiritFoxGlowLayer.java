package dev.jett.topomod.allayvariants.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.jett.topomod.allayvariants.AllayVariantsMod;

import net.minecraft.client.model.animal.fox.FoxModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.FoxRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

// The tail tips glow: a copy of the model drawn again with a texture that has only the tips on it, at full brightness (the way a
// spider's eyes are), so they stay lit in the dark. Asleep or awake, they glow.
public class SpiritFoxGlowLayer extends RenderLayer<FoxRenderState, FoxModel> {
	private static final Identifier GLOW = AllayVariantsMod.id("textures/entity/spirit_fox/spirit_fox_glow.png");
	private static final Identifier BABY_GLOW = AllayVariantsMod.id("textures/entity/spirit_fox/spirit_fox_baby_glow.png");

	public SpiritFoxGlowLayer(RenderLayerParent<FoxRenderState, FoxModel> renderer) {
		super(renderer);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, FoxRenderState state, float yRot, float xRot) {
		submitNodeCollector.order(1).submitModel(this.getParentModel(), state, poseStack, RenderTypes.eyes(state.isBaby ? BABY_GLOW : GLOW), lightCoords,
			OverlayTexture.NO_OVERLAY, state.outlineColor);
	}
}
