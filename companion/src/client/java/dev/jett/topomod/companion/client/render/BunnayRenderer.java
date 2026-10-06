package dev.jett.topomod.companion.client.render;

import dev.jett.topomod.companion.CompanionMod;
import dev.jett.topomod.companion.entity.BunnayEntity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class BunnayRenderer extends MobRenderer<BunnayEntity, BunnayRenderState, BunnayModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(CompanionMod.id("bunnay"), "main");
	private static final Identifier TEXTURE = CompanionMod.id("textures/entity/bunnay/bunnay.png");

	public BunnayRenderer(EntityRendererProvider.Context context) {
		super(context, new BunnayModel(context.bakeLayer(LAYER)), 0.3F);
		this.addLayer(new BunnayEyesLayer(this));
		this.addLayer(new BunnayItemLayer(this));
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
		state.eatProgress = entity.getEatProgress(partialTicks);
		// While it eats, the carrot is in its left hand in place of whatever it holds there (the off hand weapon, if any),
		// like a player switching to food; the weapon is back in hand as soon as the meal is done.
		ItemStack eating = entity.getEatingFood();
		if (state.eatProgress > 0.0F && !eating.isEmpty()) {
			this.itemModelResolver.updateForLiving(state.leftHandItemState, eating, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, entity);
		}
		state.bigHopAnimationState.copyFrom(entity.bigHopAnimationState);
		state.idleAnimationState.copyFrom(entity.idleAnimationState);
		state.isDancing = entity.isDancing();
		state.isSitting = entity.isInSittingPose();
	}
}
