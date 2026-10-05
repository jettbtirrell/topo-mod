package dev.jett.topomod.companion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

// Draws the held item between the topo's two paws. Vanilla's item layer assumes a 12px humanoid arm,
// so this one has its own offsets for the topo's short arms.
public class TopoHeldItemLayer extends RenderLayer<TopoRenderState, TopoModel> {
	/** How far in front of the paws' midpoint the item sits, in pixels, so it clears the body. */
	private static final float OUT_FROM_BODY = 1.8F;
	/** How far the item leans forward, away from the body, from straight up. */
	private static final float TILT_FORWARD_DEGREES = 45.0F;
	// The same two settings for an item held in one paw.
	private static final float ONE_PAW_OUT_FROM_BODY = 0.0F;
	/**
	 * Extra shift of a one-paw item toward the topo's right (the holding arm's side), in pixels; negative
	 * shifts it the other way. 0 puts the item's anchor exactly on the paw.
	 */
	private static final float ONE_PAW_SHIFT_RIGHT = 0.0F;
	private static final float ONE_PAW_TILT_FORWARD_DEGREES = 80.0F;
	/**
	 * Turns the amethyst shard about its own axis. Its sprite runs along a diagonal, so 45 degrees lines
	 * the long part up with the way the item points (forward). If the tip faces the topo instead of the
	 * viewer try 225; if it points off to a side try 135 or -45.
	 */
	private static final float SHARD_ROLL_DEGREES = 45.0F;

	// Fine adjustment along the item's own axes once it is in place, in pixels (1/16 block).
	private static final float OFFSET_X = 0.0F;
	private static final float OFFSET_Y = 0.0F;
	private static final float OFFSET_Z = 0.0F;

	public TopoHeldItemLayer(RenderLayerParent<TopoRenderState, TopoModel> parent) {
		super(parent);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, TopoRenderState state, float yRot, float xRot) {
		ItemStackRenderState item = state.rightHandItemState;
		if (item.isEmpty()) {
			return;
		}

		poseStack.pushPose();
		boolean onePaw = state.holdStyle == TopoRenderState.HoldStyle.ONE_PAW;
		if (onePaw) {
			this.getParentModel().translateToRightPaw(poseStack);
		} else {
			this.getParentModel().translateToPaws(poseStack);
		}
		// -Z is the front of the model.
		// The model's -X side is the topo's right.
		poseStack.translate(onePaw ? -ONE_PAW_SHIFT_RIGHT / 16.0F : 0.0F, 0.0F, -(onePaw ? ONE_PAW_OUT_FROM_BODY : OUT_FROM_BODY) / 16.0F);
		// Lean the item forward. (-90 would point it straight up in the model's upside-down space.)
		poseStack.rotateDegrees(Axis.XP, (onePaw ? ONE_PAW_TILT_FORWARD_DEGREES : TILT_FORWARD_DEGREES) - 90.0F);
		// The same hand-frame rotations vanilla applies, so the item's own display transform lines up.
		poseStack.rotateDegrees(Axis.XP, -90.0F);
		poseStack.rotateDegrees(Axis.YP, 180.0F);
		if (onePaw) {
			// Turn the item about the way it points, so its flat sides face left and right instead of up and down.
			poseStack.rotateDegrees(Axis.YP, 90.0F);
		}
		if (state.holdingShard) {
			poseStack.rotateDegrees(Axis.ZP, SHARD_ROLL_DEGREES);
		}
		poseStack.translate(OFFSET_X / 16.0F, OFFSET_Y / 16.0F, OFFSET_Z / 16.0F);
		item.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
	}
}
