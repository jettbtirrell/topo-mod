package dev.jett.topomod.skyislands.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

// Angel: the cube layout below is generated from sky-islands/art/angel.bbmodel by `python3 tools/import_bbmodel.py angel`;
// the animation under it relies on these part names (the wings are children of the body), so keep them if the model is edited.
public class AngelModel extends EntityModel<ArmedEntityRenderState> implements ArmedModel<ArmedEntityRenderState> {
	private final ModelPart head;
	private final ModelPart leftArm;
	private final ModelPart rightArm;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;
	private final ModelPart leftWing;
	private final ModelPart rightWing;

	public AngelModel(ModelPart root) {
		super(root);
		ModelPart body = root.getChild("body");
		this.head = root.getChild("head");
		this.leftArm = root.getChild("left_arm");
		this.rightArm = root.getChild("right_arm");
		this.leftLeg = root.getChild("left_leg");
		this.rightLeg = root.getChild("right_leg");
		this.leftWing = body.getChild("left_wing");
		this.rightWing = body.getChild("right_wing");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// BEGIN GENERATED
		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 0).addBox(-3.0F, -7.0F, -2.0F, 6.0F, 7.0F, 4.0F)
				.mirror(false).texOffs(20, 0).addBox(-3.5F, 0.0F, -2.5F, 7.0F, 5.0F, 5.0F),
			PartPose.offset(0.0F, 16.0F, 0.0F));
		body.addOrReplaceChild("left_wing",
			CubeListBuilder.create()
				.mirror(false).texOffs(44, 0).addBox(0.5F, -4.0F, 0.0F, 7.0F, 8.0F, 1.0F)
				.mirror(false).texOffs(0, 11).addBox(2.5F, 4.0F, 0.0F, 4.0F, 3.0F, 1.0F),
			PartPose.offsetAndRotation(1.5F, -6.0F, 2.0F, 0.0F, -0.4363F, 0.0F));
		body.addOrReplaceChild("right_wing",
			CubeListBuilder.create()
				.mirror(false).texOffs(44, 0).addBox(-7.5F, -4.0F, 0.0F, 7.0F, 8.0F, 1.0F)
				.mirror(false).texOffs(0, 11).addBox(-6.5F, 4.0F, 0.0F, 4.0F, 3.0F, 1.0F),
			PartPose.offsetAndRotation(-1.5F, -6.0F, 2.0F, 0.0F, 0.4363F, 0.0F));
		root.addOrReplaceChild("head",
			CubeListBuilder.create()
				.mirror(false).texOffs(10, 11).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F)
				.mirror(false).texOffs(34, 11).addBox(-3.5F, -7.0F, -3.5F, 7.0F, 4.0F, 7.0F)
				.mirror(false).texOffs(0, 23).addBox(-3.0F, -6.0F, 3.0F, 6.0F, 6.5F, 1.0F)
				.mirror(false).texOffs(14, 23).addBox(-3.0F, -8.5F, -3.5F, 6.0F, 1.0F, 1.0F)
				.mirror(false).texOffs(14, 23).addBox(-3.0F, -8.5F, 2.5F, 6.0F, 1.0F, 1.0F)
				.mirror(false).texOffs(28, 23).addBox(2.5F, -8.5F, -2.5F, 1.0F, 1.0F, 5.0F)
				.mirror(false).texOffs(28, 23).addBox(-3.5F, -8.5F, -2.5F, 1.0F, 1.0F, 5.0F),
			PartPose.offset(0.0F, 9.0F, 0.0F));
		root.addOrReplaceChild("left_arm",
			CubeListBuilder.create()
				.mirror(false).texOffs(40, 23).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 6.0F, 2.0F),
			PartPose.offset(4.0F, 10.0F, 0.0F));
		root.addOrReplaceChild("right_arm",
			CubeListBuilder.create()
				.mirror(false).texOffs(40, 23).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 6.0F, 2.0F),
			PartPose.offset(-4.0F, 10.0F, 0.0F));
		root.addOrReplaceChild("left_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(48, 23).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
			PartPose.offset(2.0F, 18.0F, 0.0F));
		root.addOrReplaceChild("right_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(48, 23).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
			PartPose.offset(-2.0F, 18.0F, 0.0F));
		// END GENERATED

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(ArmedEntityRenderState state) {
		super.setupAnim(state);

		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;

		// The player's walk: legs and arms swing against each other.
		float swing = Mth.cos(state.walkAnimationPos * 0.6662F) * state.walkAnimationSpeed;
		this.rightLeg.xRot = swing * 1.4F;
		this.leftLeg.xRot = -swing * 1.4F;
		this.rightArm.xRot = -swing;
		this.leftArm.xRot = swing;
		// The arms hang a little out from the body, and sway gently.
		float sway = Mth.sin(state.ageInTicks * 0.09F) * 0.05F;
		this.leftArm.zRot = -0.08F - sway;
		this.rightArm.zRot = 0.08F + sway;

		// An archer holds its bow out in front of it, ready to shoot: the bow arm straight ahead, the other arm drawn in across it.
		if (!state.rightHandItemState.isEmpty()) {
			this.rightArm.xRot = -1.45F;
			this.rightArm.zRot = 0.0F;
			this.leftArm.xRot = -1.3F;
			this.leftArm.yRot = 0.5F;
			this.leftArm.zRot = 0.0F;
		}

		// The wings fold back at rest and breathe slowly; walking makes them beat quicker.
		float rate = 0.12F + state.walkAnimationSpeed * 0.5F;
		float flap = Mth.sin(state.ageInTicks * rate) * (0.1F + state.walkAnimationSpeed * 0.35F);
		this.leftWing.yRot += flap;
		this.rightWing.yRot -= flap;

		// The halo floats a little: the whole head bobs by half a pixel.
		this.head.y += Mth.sin(state.ageInTicks * 0.1F) * 0.5F;
	}

	/** Size of a held item compared to a player's: the angel is a small one, so its bow is too. */
	private static final float ITEM_SCALE = 0.7F;
	/** How far down the arm the hand is, in pixels from the shoulder pivot; the held item sits here. */
	private static final float HAND_DOWN = 5.0F;

	/**
	 * Moves the pose stack to a hand for vanilla's ItemInHandLayer, which then turns the stack the way it does for a player and moves
	 * the item 10px down and 2px forward of where this leaves it (and 1px to the side), all at this scale; that is backed out here,
	 * so the item ends up centred on the hand (the same way the bunnay does it).
	 */
	@Override
	public void translateToHand(ArmedEntityRenderState state, HumanoidArm arm, PoseStack poseStack) {
		boolean right = arm == HumanoidArm.RIGHT;
		float side = right ? 1.0F : -1.0F;
		this.root.translateAndRotate(poseStack);
		(right ? this.rightArm : this.leftArm).translateAndRotate(poseStack);
		poseStack.translate(0.0F, HAND_DOWN / 16.0F, 0.0F);
		poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
		poseStack.translate(side / 16.0F, -10.0F / 16.0F, 2.0F / 16.0F);
	}
}
