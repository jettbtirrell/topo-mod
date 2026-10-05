package dev.jett.topomod.companion.client.render;

import dev.jett.topomod.companion.CompanionMod;
import dev.jett.topomod.companion.entity.TopoEntity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class TopoRenderer extends MobRenderer<TopoEntity, TopoRenderState, TopoModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(CompanionMod.id("topo"), "main");
	private static final Identifier TEXTURE = CompanionMod.id("textures/entity/topo/topo.png");

	public TopoRenderer(EntityRendererProvider.Context context) {
		super(context, new TopoModel(context.bakeLayer(LAYER)), 0.3F);
	}

	@Override
	public Identifier getTextureLocation(TopoRenderState state) {
		return TEXTURE;
	}

	@Override
	public TopoRenderState createRenderState() {
		return new TopoRenderState();
	}

	@Override
	public void extractRenderState(TopoEntity entity, TopoRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.sitting = entity.isInSittingPose();
	}
}
