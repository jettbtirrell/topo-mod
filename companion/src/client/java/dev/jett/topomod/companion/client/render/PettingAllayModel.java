package dev.jett.topomod.companion.client.render;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.animal.allay.AllayModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AllayRenderState;
import net.minecraft.world.entity.AnimationState;

// The vanilla allay model with our petting clip layered on top of whatever it is already doing (hovering, flapping).
public class PettingAllayModel extends AllayModel {
	private final KeyframeAnimation pettingAnimation;

	public PettingAllayModel(ModelPart layerRoot) {
		super(layerRoot);
		this.pettingAnimation = AllayPettingAnimation.PETTING.bake(layerRoot);
	}

	@Override
	public void setupAnim(AllayRenderState state) {
		super.setupAnim(state);
		AnimationState petting = state.getData(PettingAllayRenderer.PETTING_STATE);
		if (petting != null) {
			this.pettingAnimation.apply(petting, state.ageInTicks);
		}
	}
}
