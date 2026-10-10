package dev.jett.topomod.allayvariants.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.jett.topomod.allayvariants.AllayVariantsMod;
import dev.jett.topomod.allayvariants.entity.SpiritFoxEntity;

import net.minecraft.client.model.animal.fox.FoxModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.FoxHeldItemLayer;
import net.minecraft.client.renderer.entity.state.FoxRenderState;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.resources.Identifier;

// Drawn the way the vanilla fox is (FoxRenderer): the same poses and the item in its mouth, with its own models and textures (a different
// one asleep, as the fox's is), and its tail tips drawn at full brightness on top (SpiritFoxGlowLayer).
public class SpiritFoxRenderer extends AgeableMobRenderer<SpiritFoxEntity, FoxRenderState, FoxModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(AllayVariantsMod.id("spirit_fox"), "main");
	public static final ModelLayerLocation BABY_LAYER = new ModelLayerLocation(AllayVariantsMod.id("spirit_fox"), "baby");

	private static final Identifier TEXTURE = texture("spirit_fox");
	private static final Identifier SLEEP_TEXTURE = texture("spirit_fox_sleep");
	private static final Identifier BABY_TEXTURE = texture("spirit_fox_baby");
	private static final Identifier BABY_SLEEP_TEXTURE = texture("spirit_fox_baby_sleep");

	public SpiritFoxRenderer(EntityRendererProvider.Context context) {
		super(context, new SpiritFoxModel(context.bakeLayer(LAYER)), new SpiritBabyFoxModel(context.bakeLayer(BABY_LAYER)), 0.4F);
		this.addLayer(new SpiritFoxGlowLayer(this));
		this.addLayer(new FoxHeldItemLayer(this));
	}

	private static Identifier texture(String name) {
		return AllayVariantsMod.id("textures/entity/spirit_fox/" + name + ".png");
	}

	@Override
	protected void setupRotations(FoxRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
		super.setupRotations(state, poseStack, bodyRot, entityScale);
		if (state.isPouncing || state.isFaceplanted) {
			poseStack.rotateDegrees(Axis.XP, -state.xRot);
		}
	}

	@Override
	public Identifier getTextureLocation(FoxRenderState state) {
		if (state.isSleeping) {
			return state.isBaby ? BABY_SLEEP_TEXTURE : SLEEP_TEXTURE;
		}
		return state.isBaby ? BABY_TEXTURE : TEXTURE;
	}

	@Override
	public FoxRenderState createRenderState() {
		return new FoxRenderState();
	}

	@Override
	public void extractRenderState(SpiritFoxEntity entity, FoxRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		HoldingEntityRenderState.extractHoldingEntityRenderState(entity, state, this.itemModelResolver);
		state.headRollAngle = entity.getHeadRollAngle(partialTicks);
		state.isCrouching = entity.isCrouching();
		state.crouchAmount = entity.getCrouchAmount(partialTicks);
		state.isSleeping = entity.isSleeping();
		state.isSitting = entity.isSitting();
		state.isFaceplanted = entity.isFaceplanted();
		state.isPouncing = entity.isPouncing();
	}
}
