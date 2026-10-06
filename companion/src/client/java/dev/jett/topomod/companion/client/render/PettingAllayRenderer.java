package dev.jett.topomod.companion.client.render;

import java.util.Map;
import java.util.WeakHashMap;

import dev.jett.topomod.companion.entity.BunnayBreeding;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

import net.minecraft.client.model.animal.allay.AllayModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AllayRenderState;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.animal.allay.Allay;

// Vanilla's allay renderer, copied, with two changes: it uses PettingAllayModel, and it passes the petting animation
// state down to it. This replaces the allay's renderer, so it has to be kept in step with vanilla's AllayRenderer.
public class PettingAllayRenderer extends MobRenderer<Allay, AllayRenderState, AllayModel> {
	/** The petting animation's state for one allay, carried on the render state so the model can play it. */
	public static final RenderStateDataKey<AnimationState> PETTING_STATE = RenderStateDataKey.create(() -> "topo_companion:allay_petting");

	private static final Identifier ALLAY_TEXTURE = Identifier.withDefaultNamespace("textures/entity/allay/allay.png");

	/** Each allay's animation state, kept between frames (weak, so an allay that is gone takes its state with it). */
	private final Map<Allay, AnimationState> pettingStates = new WeakHashMap<>();

	public PettingAllayRenderer(EntityRendererProvider.Context context) {
		super(context, new PettingAllayModel(context.bakeLayer(ModelLayers.ALLAY)), 0.4F);
		this.addLayer(new ItemInHandLayer<>(this));
	}

	@Override
	public Identifier getTextureLocation(AllayRenderState state) {
		return ALLAY_TEXTURE;
	}

	@Override
	public AllayRenderState createRenderState() {
		return new AllayRenderState();
	}

	@Override
	public void extractRenderState(Allay entity, AllayRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, this.itemModelResolver, partialTicks);
		state.isDancing = entity.isDancing();
		state.isSpinning = entity.isSpinning();
		state.spinningProgress = entity.getSpinningProgress(partialTicks);
		state.holdingAnimationProgress = entity.getHoldingItemAnimationProgress(partialTicks);

		AnimationState petting = this.pettingStates.computeIfAbsent(entity, allay -> new AnimationState());
		petting.animateWhen(entity.getAttachedOrElse(BunnayBreeding.PETTING, false), entity.tickCount);
		state.setData(PETTING_STATE, petting);
	}

	@Override
	protected int getBlockLightLevel(Allay entity, BlockPos blockPos) {
		return 15;
	}
}
