package dev.jett.topomod.companion.client.render;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

// Bunnay: the cube layout below is generated from companion/art/bunnay.bbmodel by
// `python3 companion/art/import_bbmodel.py companion/art/bunnay.bbmodel <this file> <texture>`;
// the animation under it relies on these part names, so keep them if the model is edited.
public class BunnayModel extends EntityModel<BunnayRenderState> {
	private final ModelPart head;
	private final ModelPart leftArm;
	private final ModelPart rightArm;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;
	private final KeyframeAnimation bigHopAnimation;
	private final KeyframeAnimation idleAnimation;

	public BunnayModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.leftArm = root.getChild("left_arm");
		this.rightArm = root.getChild("right_arm");
		this.leftLeg = root.getChild("left_leg");
		this.rightLeg = root.getChild("right_leg");
		this.bigHopAnimation = BunnayAnimation.BIG_HOP.bake(root);
		this.idleAnimation = BunnayAnimation.IDLE.bake(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// BEGIN GENERATED
		PartDefinition head = root.addOrReplaceChild("head",
			CubeListBuilder.create()
				.mirror(false).texOffs(2, 0).addBox(-2.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F),
			PartPose.offset(0.0F, 19.0F, 0.0F));
		head.addOrReplaceChild("left_ear",
			CubeListBuilder.create()
				.mirror(false).texOffs(26, 0).addBox(-0.8F, -4.5F, -0.5F, 2.0F, 4.0F, 1.0F),
			PartPose.offset(1.3F, -4.5F, 0.0F));
		head.addOrReplaceChild("right_ear",
			CubeListBuilder.create()
				.mirror(true).texOffs(26, 0).addBox(-0.2F, -4.5F, -0.5F, 2.0F, 4.0F, 1.0F),
			PartPose.offset(-2.3F, -4.5F, 0.0F));
		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create()
				.mirror(false).texOffs(26, 13).addBox(-1.5F, -5.0F, -1.0F, 3.0F, 3.0F, 2.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F));
		body.addOrReplaceChild("tail",
			CubeListBuilder.create()
				.mirror(false).texOffs(1, 36).addBox(-1.0F, -1.8F, -0.1F, 2.0F, 2.0F, 2.0F),
			PartPose.offset(0.0F, -1.5F, 0.5F));
		root.addOrReplaceChild("left_arm",
			CubeListBuilder.create()
				.mirror(false).texOffs(23, 23).addBox(-1.0F, -0.5F, -1.0F, 1.0F, 4.0F, 2.0F),
			PartPose.offset(2.5F, 19.5F, 0.0F));
		root.addOrReplaceChild("right_arm",
			CubeListBuilder.create()
				.mirror(true).texOffs(23, 23).addBox(0.0F, -0.5F, -1.0F, 1.0F, 4.0F, 2.0F),
			PartPose.offset(-2.5F, 19.5F, 0.0F));
		root.addOrReplaceChild("left_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(1, 42).addBox(-0.75F, -2.0F, -1.0F, 1.0F, 1.0F, 2.0F)
				.mirror(false).texOffs(12, 43).addBox(-0.75F, -1.0F, -1.0F, 1.0F, 1.0F, 2.0F),
			PartPose.offset(1.25F, 24.0F, 0.0F));
		root.addOrReplaceChild("right_leg",
			CubeListBuilder.create()
				.mirror(true).texOffs(1, 42).addBox(-0.25F, -2.0F, -1.0F, 1.0F, 1.0F, 2.0F)
				.mirror(true).texOffs(12, 43).addBox(-0.25F, -1.0F, -1.0F, 1.0F, 1.0F, 2.0F),
			PartPose.offset(-1.25F, 24.0F, 0.0F));
		// END GENERATED

		return LayerDefinition.create(mesh, 64, 64);
	}

	// Height of a leg in model pixels. The legs pivot at the ground, so swinging them has to be
	// compensated to look like it pivots at the hip.
	private static final float LEG_HEIGHT = 2.0F;
	/** How far the arms flare out from the body when at rest, in radians (about 25 degrees, like the allay). */
	private static final float ARM_FLARE = 0.43633232F;

	@Override
	public void setupAnim(BunnayRenderState state) {
		super.setupAnim(state);

		// While the idle plays, the head follows the clip instead of where the bunnay is looking (like the rabbit).
		// A dancing bunnay's head is set by the dance below.
		if (!state.idleAnimationState.isStarted() && !state.isDancing) {
			this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
			this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
		}

		// The player's walk (HumanoidModel): legs swing 1.4 radians and arms 1.0 radian at full speed, each arm
		// opposite to the leg on its own side.
		float step = Mth.cos(state.walkAnimationPos * 0.6662F) * state.walkAnimationSpeed;
		swingLeg(this.rightLeg, step * 1.4F);
		swingLeg(this.leftLeg, -step * 1.4F);

		this.animateArms(state);
		this.rightArm.xRot = -step;
		this.leftArm.xRot = step;

		if (state.isDancing) {
			this.animateDance(state);
		}

		// Keyframed clips go on last: they are offsets from the pose above.
		this.idleAnimation.apply(state.idleAnimationState, state.ageInTicks);
		this.bigHopAnimation.apply(state.bigHopAnimationState, state.ageInTicks);
	}

	/**
	 * The arms move like the allay's: they hang slightly out from the body, and while standing still they sway
	 * in and out on a slow two-second cycle. Walking holds them steady at the flared pose. This is the allay's own
	 * math (AllayModel.setupAnim) without its holding-an-item parts, which a bunnay will get its own version of.
	 * The arms' forward and back swing while walking is set separately, in setupAnim.
	 */
	private void animateArms(BunnayRenderState state) {
		float cycle = state.ageInTicks * 9.0F * Mth.DEG_TO_RAD;
		float moving = Math.min(state.walkAnimationSpeed / 0.3F, 1.0F);
		float still = 1.0F - moving;

		float flare = ARM_FLARE - Mth.sin(cycle) * Mth.PI * 0.075F * still;
		this.leftArm.zRot = -flare;
		this.rightArm.zRot = flare;
	}

	/** Height of the ground in model space; the dance sways the whole model about the feet. */
	private static final float GROUND_Y = 24.0F;

	/**
	 * The allay's jukebox dance (AllayModel.setupAnim): one wave, cos(8 degrees per tick), rolls the whole body 16
	 * degrees from side to side and turns the head 30 degrees and tilts it 14, all in step. The allay rolls about its
	 * middle; a bunnay stands on the ground, so this rolls it about its feet instead, which means shifting the model
	 * to keep the feet in place (position = rotated position + (pivot - rotated pivot)).
	 */
	private void animateDance(BunnayRenderState state) {
		float wave = Mth.cos(state.ageInTicks * 8.0F * Mth.DEG_TO_RAD + state.walkAnimationSpeed);

		float roll = wave * 16.0F * Mth.DEG_TO_RAD;
		this.root.zRot = roll;
		this.root.x += GROUND_Y * Mth.sin(roll);
		this.root.y += GROUND_Y * (1.0F - Mth.cos(roll));

		this.head.yRot = wave * 30.0F * Mth.DEG_TO_RAD;
		this.head.zRot = wave * 14.0F * Mth.DEG_TO_RAD;
	}

	/** Rotates a leg about its hip (top) even though its model pivot is at the foot. */
	private static void swingLeg(ModelPart leg, float angle) {
		leg.xRot = angle;
		leg.y += LEG_HEIGHT * (Mth.cos(angle) - 1.0F);
		leg.z += LEG_HEIGHT * Mth.sin(angle);
	}
}
