package dev.jett.topomod.companion.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

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

	@Override
	public void setupAnim(TopoRenderState state) {
		super.setupAnim(state);

		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;

		// Ear twitch and tail sway are always on, so it feels alive when standing still.
		this.leftEar.zRot += Mth.sin(state.ageInTicks * 0.13F) * 0.06F;
		this.rightEar.zRot -= Mth.sin(state.ageInTicks * 0.11F + 1.0F) * 0.06F;
		this.tail.yRot = Mth.sin(state.ageInTicks * 0.1F) * 0.3F;

		if (state.sitting) {
			// Plop down: the body, head and arms drop 2px so the bottom rests on the ground.
			for (ModelPart part : new ModelPart[]{this.body, this.head, this.leftArm, this.rightArm}) {
				part.y += 2.0F;
			}
			// Arms reach forward and the legs stick straight out in front, lying flat on the ground
			// (raised half a pixel and moved forward so the feet show in front of the body).
			this.leftArm.xRot = -0.8F;
			this.rightArm.xRot = -0.8F;
			for (ModelPart leg : new ModelPart[]{this.leftLeg, this.rightLeg}) {
				leg.xRot = -Mth.HALF_PI;
				leg.y -= 0.5F;
				leg.z -= 3.5F;
			}
			this.tail.xRot = 0.5F;
			this.tail.y -= 0.5F;
		} else {
			float swing = Mth.cos(state.walkAnimationPos * 0.8F) * 0.9F * state.walkAnimationSpeed;
			swingLeg(this.leftLeg, swing);
			swingLeg(this.rightLeg, -swing);
			this.leftArm.xRot = -swing * 0.7F;
			this.rightArm.xRot = swing * 0.7F;
		}
	}

	/** Rotates a leg about its hip (top) even though its model pivot is at the foot. */
	private static void swingLeg(ModelPart leg, float angle) {
		leg.xRot = angle;
		leg.y += LEG_HEIGHT * (Mth.cos(angle) - 1.0F);
		leg.z += LEG_HEIGHT * Mth.sin(angle);
	}
}
