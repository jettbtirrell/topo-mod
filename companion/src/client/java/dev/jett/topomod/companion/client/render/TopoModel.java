package dev.jett.topomod.companion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.jett.topomod.companion.entity.TopoEntity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import org.joml.Quaternionf;
import org.joml.Vector3f;

// Upright mouse. The cube layout below is generated from companion/art/topo.bbmodel's layout;
// the animation under it relies on these part names, so keep them if the model is edited.
public class TopoModel extends EntityModel<TopoRenderState> {
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart leftEar;
	private final ModelPart rightEar;
	private final ModelPart tail;
	private final ModelPart leftArm;
	private final ModelPart rightArm;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;

	public TopoModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = root.getChild("head");
		this.leftEar = this.head.getChild("left_ear");
		this.rightEar = this.head.getChild("right_ear");
		this.tail = this.body.getChild("tail");
		this.leftArm = root.getChild("left_arm");
		this.rightArm = root.getChild("right_arm");
		this.leftLeg = root.getChild("left_leg");
		this.rightLeg = root.getChild("right_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// BEGIN GENERATED
		PartDefinition head = root.addOrReplaceChild("head",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 0).addBox(-3.5F, -5.0F, -2.5F, 7.0F, 5.0F, 5.0F)
				.mirror(false).texOffs(40, 0).addBox(-1.5F, -1.75F, -3.0F, 3.0F, 1.0F, 1.0F),
			PartPose.offset(0.0F, 19.0F, 0.0F));
		head.addOrReplaceChild("left_ear",
			CubeListBuilder.create()
				.mirror(false).texOffs(24, 0).addBox(-1.5F, -4.0F, -0.5F, 4.0F, 4.0F, 1.0F),
			PartPose.offsetAndRotation(2.3F, -4.5F, 0.0F, 0.0F, 0.0F, 0.3F));
		head.addOrReplaceChild("right_ear",
			CubeListBuilder.create()
				.mirror(true).texOffs(24, 0).addBox(-2.5F, -4.0F, -0.5F, 4.0F, 4.0F, 1.0F),
			PartPose.offsetAndRotation(-2.3F, -4.5F, 0.0F, 0.0F, 0.0F, -0.3F));
		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create()
				.mirror(false).texOffs(24, 12).addBox(-2.0F, -5.0F, -1.5F, 4.0F, 3.0F, 3.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F));
		body.addOrReplaceChild("tail",
			CubeListBuilder.create()
				.mirror(false).texOffs(2, 36).addBox(-0.5F, -1.0F, 0.0F, 1.0F, 1.0F, 2.0F)
				.mirror(false).texOffs(13, 35).addBox(-0.5F, -1.0F, 2.0F, 1.0F, 1.0F, 1.0F),
			PartPose.offsetAndRotation(0.0F, -1.5F, 1.5F, 0.6F, 0.0F, 0.0F));
		PartDefinition left_arm = root.addOrReplaceChild("left_arm",
			CubeListBuilder.create(),
			PartPose.offset(2.5F, 19.5F, 0.0F));
		left_arm.addOrReplaceChild("left_arm_cube_r1",
			CubeListBuilder.create()
				.mirror(false).texOffs(24, 24).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
			PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0873F));
		PartDefinition right_arm = root.addOrReplaceChild("right_arm",
			CubeListBuilder.create(),
			PartPose.offset(-2.5F, 19.5F, 0.0F));
		right_arm.addOrReplaceChild("right_arm_cube_r1",
			CubeListBuilder.create()
				.mirror(true).texOffs(24, 24).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
			PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0873F));
		root.addOrReplaceChild("left_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(2, 43).addBox(-1.0F, -2.0F, -0.5F, 1.0F, 1.0F, 1.0F)
				.mirror(false).texOffs(12, 43).addBox(-1.0F, -1.0F, -1.5F, 1.0F, 1.0F, 2.0F),
			PartPose.offset(1.25F, 24.0F, 0.0F));
		root.addOrReplaceChild("right_leg",
			CubeListBuilder.create()
				.mirror(true).texOffs(2, 43).addBox(0.0F, -2.0F, -0.5F, 1.0F, 1.0F, 1.0F)
				.mirror(true).texOffs(12, 43).addBox(0.0F, -1.0F, -1.5F, 1.0F, 1.0F, 2.0F),
			PartPose.offset(-1.25F, 24.0F, 0.0F));
		// END GENERATED

		return LayerDefinition.create(mesh, 64, 64);
	}

	// Height of a leg in model pixels. The legs pivot at the ground, so swinging them has to be
	// compensated to look like it pivots at the hip.
	private static final float LEG_HEIGHT = 2.0F;
	/** How the arms are posed to hold an item between both paws: forward and pulled in together. */
	private static final float HOLD_PITCH = -0.85F;
	private static final float HOLD_INWARD = 0.75F;

	@Override
	public void setupAnim(TopoRenderState state) {
		super.setupAnim(state);

		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;

		// Ear twitch and tail sway are always on, so it feels alive when standing still.
		this.leftEar.zRot += Mth.sin(state.ageInTicks * 0.13F) * 0.06F;
		this.rightEar.zRot -= Mth.sin(state.ageInTicks * 0.11F + 1.0F) * 0.06F;
		this.tail.yRot = Mth.sin(state.ageInTicks * 0.1F) * 0.3F;

		boolean dancing = state.danceTime >= 0.0F && !state.sitting;
		if (dancing) {
			this.animateDance(state.danceTime);
		} else if (state.sitting) {
			// Plop down: the body, head and arms drop 2px so the bottom rests on the ground.
			for (ModelPart part : new ModelPart[]{this.body, this.head, this.leftArm, this.rightArm}) {
				part.y += 2.0F;
			}
			// Legs stick straight out in front, lying flat on the ground (raised half a pixel and moved
			// forward so the feet show in front of the body).
			for (ModelPart leg : new ModelPart[]{this.leftLeg, this.rightLeg}) {
				leg.xRot = -Mth.HALF_PI;
				leg.y -= 0.5F;
				leg.z -= 3.5F;
			}
			this.tail.xRot = 0.5F;
			this.tail.y -= 0.5F;
			if (state.holdingItem) {
				this.poseArmsHolding();
			} else {
				this.leftArm.xRot = -0.8F;
				this.rightArm.xRot = -0.8F;
			}
		} else {
			float swing = Mth.cos(state.walkAnimationPos * 0.8F) * 0.9F * state.walkAnimationSpeed;
			swingLeg(this.leftLeg, swing);
			swingLeg(this.rightLeg, -swing);
			if (state.holdingItem) {
				this.poseArmsHolding();
			} else {
				this.leftArm.xRot = -swing * 0.7F;
				this.rightArm.xRot = swing * 0.7F;
			}
		}
	}

	private void poseArmsHolding() {
		this.leftArm.xRot = HOLD_PITCH;
		this.rightArm.xRot = HOLD_PITCH;
		this.leftArm.zRot = HOLD_INWARD;
		this.rightArm.zRot = -HOLD_INWARD;
	}

	/** Victory dance: spins twice while hopping, arms up, ears and tail flapping. */
	private void animateDance(float t) {
		float length = TopoEntity.DANCE_LENGTH;
		// Ease the motion in and out so it doesn't snap at the start or end.
		float amp = Mth.clamp(Math.min(t, length - t) / 6.0F, 0.0F, 1.0F);

		// Whole number of turns, so the model ends facing the way it started.
		this.root.yRot = Mth.TWO_PI * 2.0F * (t / length);
		this.root.y -= Math.abs(Mth.sin(t * 0.35F)) * 2.5F * amp;

		float wave = Mth.sin(t * 0.45F) * 0.3F * amp;
		this.leftArm.xRot = -2.9F + wave;
		this.rightArm.xRot = -2.9F - wave;
		this.leftArm.zRot = -0.3F;
		this.rightArm.zRot = 0.3F;

		this.head.zRot = Mth.sin(t * 0.4F) * 0.18F * amp;
		this.head.xRot = -0.15F + Mth.sin(t * 0.7F) * 0.08F * amp;
		this.leftEar.zRot += Mth.sin(t * 0.55F) * 0.2F * amp;
		this.rightEar.zRot += Mth.sin(t * 0.55F) * 0.2F * amp;
		this.tail.yRot = Mth.sin(t * 0.5F) * 0.6F;

		float step = Mth.sin(t * 0.35F) * 0.6F * amp;
		swingLeg(this.leftLeg, step);
		swingLeg(this.rightLeg, -step);
	}

	/** Distance from the shoulder to where a paw grips, along the arm, in pixels. */
	private static final float ARM_GRIP = 2.5F;

	/**
	 * Moves the pose stack to the point halfway between the two paws, so a held item sits in the middle
	 * of the arms (and follows them when they move). Used by the held item layer.
	 */
	public void translateToPaws(PoseStack poseStack) {
		this.root.translateAndRotate(poseStack);
		Vector3f left = armTip(this.leftArm);
		Vector3f right = armTip(this.rightArm);
		poseStack.translate((left.x + right.x) / 32.0F, (left.y + right.y) / 32.0F, (left.z + right.z) / 32.0F);
	}

	private static Vector3f armTip(ModelPart arm) {
		Vector3f tip = new Vector3f(0.0F, ARM_GRIP, 0.0F);
		tip.rotate(new Quaternionf().rotationZYX(arm.zRot, arm.yRot, arm.xRot));
		return tip.add(arm.x, arm.y, arm.z);
	}

	/** Rotates a leg about its hip (top) even though its model pivot is at the foot. */
	private static void swingLeg(ModelPart leg, float angle) {
		leg.xRot = angle;
		leg.y += LEG_HEIGHT * (Mth.cos(angle) - 1.0F);
		leg.z += LEG_HEIGHT * Mth.sin(angle);
	}
}
