package dev.jett.topomod.companion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.joml.Quaternionf;

// Vanilla's item layer, with one addition: arrows are held like tools. The rods, bamboo, stick and bone all use the
// "handheld" item model, which turns the item to point up and forward from the fist. Arrows use the flat "generated"
// model, which would hold them like a card, so for them this applies the difference between the two models' hand
// transforms, which makes an arrow sit exactly the way a handheld item does.
public class BunnayItemLayer extends ItemInHandLayer<BunnayRenderState, BunnayModel> {
	// The third person hand transform of the two models, from the game's own item models (item/generated and item/handheld):
	// translation in pixels, rotation in degrees about x, y and z, and a uniform scale (the right hand's; see below for the left).
	private static final float GENERATED_TRANSLATE_Y = 3.0F;
	private static final float GENERATED_TRANSLATE_Z = 1.0F;
	private static final float GENERATED_SCALE = 0.55F;
	private static final float HANDHELD_TRANSLATE_Y = 4.0F;
	private static final float HANDHELD_TRANSLATE_Z = 0.5F;
	private static final float HANDHELD_ROTATE_Y = -90.0F;
	private static final float HANDHELD_ROTATE_Z = 55.0F;
	private static final float HANDHELD_SCALE = 0.85F;

	public BunnayItemLayer(RenderLayerParent<BunnayRenderState, BunnayModel> parent) {
		super(parent);
	}

	@Override
	protected void submitArmWithItem(BunnayRenderState state, ItemStackRenderState item, ItemStack stack, HumanoidArm arm,
			PoseStack poseStack, SubmitNodeCollector collector, int lightCoords) {
		if (item.isEmpty()) {
			return;
		}

		// What vanilla does for a full-size mob (see ItemInHandLayer.submitArmWithItem).
		poseStack.pushPose();
		this.getParentModel().translateToHand(state, arm, poseStack);
		poseStack.rotateDegrees(Axis.XP, -90.0F);
		poseStack.rotateDegrees(Axis.YP, 180.0F);
		boolean left = arm == HumanoidArm.LEFT;
		poseStack.translate((left ? -1.0F : 1.0F) / 16.0F, 2.0F / 16.0F, -10.0F / 16.0F);

		if (isGeneratedWeapon(stack)) {
			// handheld = T(th) R(rh) S(sh) and generated = T(tg) S(sg), both then centered, so the change that turns the
			// second into the first is: translate th, rotate rh, scale sh / sg, translate -tg. It is the same for both hands:
			// the game flips a left hand item's transform (negating its x translation and its y and z rotation), and a
			// handheld item's left entry is the mirror of its right one, so after the flip it comes out the same as the
			// right one; and a generated item has no left entry, so it uses the right one and flips it. Mirroring this here
			// as well would turn the item the wrong way round in the left hand.
			poseStack.translate(0.0F, HANDHELD_TRANSLATE_Y / 16.0F, HANDHELD_TRANSLATE_Z / 16.0F);
			poseStack.rotate(new Quaternionf().rotationXYZ(0.0F, HANDHELD_ROTATE_Y * (float) (Math.PI / 180.0), HANDHELD_ROTATE_Z * (float) (Math.PI / 180.0)));
			float scale = HANDHELD_SCALE / GENERATED_SCALE;
			poseStack.scale(scale, scale, scale);
			poseStack.translate(0.0F, -GENERATED_TRANSLATE_Y / 16.0F, -GENERATED_TRANSLATE_Z / 16.0F);
		}

		item.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
	}

	/** The held weapons that use the flat "generated" item model, not "handheld". */
	private static boolean isGeneratedWeapon(ItemStack stack) {
		return stack.is(Items.ARROW) || stack.is(Items.TIPPED_ARROW);
	}
}
