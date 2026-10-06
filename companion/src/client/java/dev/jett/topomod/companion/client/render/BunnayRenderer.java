package dev.jett.topomod.companion.client.render;

import dev.jett.topomod.companion.CompanionMod;
import dev.jett.topomod.companion.entity.BunnayEntity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

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

	// Lit from within like an allay: its whole body (and what it holds) is drawn at full block light, so it glows in the dark.
	@Override
	protected int getBlockLightLevel(BunnayEntity entity, BlockPos pos) {
		return 15;
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
		state.hopAnimationState.copyFrom(entity.hopAnimationState);
		state.idleAnimationState.copyFrom(entity.idleAnimationState);
		state.isDancing = entity.isDancing();
		state.isSitting = entity.isInSittingPose();
	}
}
