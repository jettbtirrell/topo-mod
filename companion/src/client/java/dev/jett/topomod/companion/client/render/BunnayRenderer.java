package dev.jett.topomod.companion.client.render;

import dev.jett.topomod.companion.CompanionMod;
import dev.jett.topomod.companion.entity.BunnayEntity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.resources.Identifier;

public class BunnayRenderer extends MobRenderer<BunnayEntity, BunnayRenderState, BunnayModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(CompanionMod.id("bunnay"), "main");
	private static final Identifier TEXTURE = CompanionMod.id("textures/entity/bunnay/bunnay.png");

	public BunnayRenderer(EntityRendererProvider.Context context) {
		super(context, new BunnayModel(context.bakeLayer(LAYER)), 0.3F);
		this.addLayer(new ItemInHandLayer<>(this));
	}

	@Override
	public Identifier getTextureLocation(BunnayRenderState state) {
		return TEXTURE;
	}

	@Override
	public BunnayRenderState createRenderState() {
		return new BunnayRenderState();
	}

	@Override
	public void extractRenderState(BunnayEntity entity, BunnayRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, this.itemModelResolver, partialTicks);
		state.readyProgress = entity.getReadyProgress(partialTicks);
		state.bigHopAnimationState.copyFrom(entity.bigHopAnimationState);
		state.idleAnimationState.copyFrom(entity.idleAnimationState);
		state.isDancing = entity.isDancing();
		state.isSitting = entity.isInSittingPose();
	}
}
