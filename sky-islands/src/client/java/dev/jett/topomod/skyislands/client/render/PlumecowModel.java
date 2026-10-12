package dev.jett.topomod.skyislands.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

// Plumecow: the cube layout below is generated from sky-islands/art/plumecow.bbmodel by `python3 tools/import_bbmodel.py plumecow`;
// the animation under it relies on these part names, so keep them if the model is edited.
public class PlumecowModel extends EntityModel<LivingEntityRenderState> {
	private final ModelPart head;
	private final ModelPart frontLeftLeg;
	private final ModelPart frontRightLeg;
	private final ModelPart backLeftLeg;
	private final ModelPart backRightLeg;
	private final ModelPart tail;
	private final ModelPart leftWing;
	private final ModelPart rightWing;

	public PlumecowModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.frontLeftLeg = root.getChild("front_left_leg");
		this.frontRightLeg = root.getChild("front_right_leg");
		this.backLeftLeg = root.getChild("back_left_leg");
		this.backRightLeg = root.getChild("back_right_leg");
		ModelPart body = root.getChild("body");
		this.tail = body.getChild("tail");
		this.leftWing = body.getChild("left_wing");
		this.rightWing = body.getChild("right_wing");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// BEGIN GENERATED
		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 0).addBox(-6.0F, -5.0F, -9.0F, 12.0F, 10.0F, 18.0F)
				.mirror(false).texOffs(60, 0).addBox(-5.0F, -4.0F, -12.0F, 10.0F, 8.0F, 4.0F),
			PartPose.offset(0.0F, 7.0F, 0.0F));
		body.addOrReplaceChild("tail",
			CubeListBuilder.create()
				.mirror(false).texOffs(88, 0).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 7.0F, 2.0F)
				.mirror(false).texOffs(96, 0).addBox(-2.5F, 6.0F, 0.0F, 5.0F, 6.0F, 2.0F),
			PartPose.offsetAndRotation(0.0F, -4.0F, 9.0F, 0.4887F, 0.0F, 0.0F));
		body.addOrReplaceChild("left_wing",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 28).addBox(-0.5F, 0.0F, 0.0F, 1.5F, 8.0F, 8.0F),
			PartPose.offsetAndRotation(6.5F, -5.0F, -3.0F, 0.0F, 0.0F, -0.1222F));
		body.addOrReplaceChild("right_wing",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 28).addBox(-1.0F, 0.0F, 0.0F, 1.5F, 8.0F, 8.0F),
			PartPose.offsetAndRotation(-6.5F, -5.0F, -3.0F, 0.0F, 0.0F, 0.1222F));
		PartDefinition head = root.addOrReplaceChild("head",
			CubeListBuilder.create()
				.mirror(false).texOffs(20, 28).addBox(-4.0F, -5.0F, -6.0F, 8.0F, 8.0F, 6.0F)
				.mirror(false).texOffs(48, 28).addBox(-3.0F, -1.0F, -8.0F, 6.0F, 4.0F, 2.0F),
			PartPose.offset(0.0F, 5.0F, -9.0F));
		head.addOrReplaceChild("left_horn",
			CubeListBuilder.create()
				.mirror(false).texOffs(64, 28).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
			PartPose.offsetAndRotation(4.0F, -5.0F, -3.0F, 0.0F, 0.0F, -0.2094F));
		head.addOrReplaceChild("right_horn",
			CubeListBuilder.create()
				.mirror(false).texOffs(64, 28).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
			PartPose.offsetAndRotation(-4.0F, -5.0F, -3.0F, 0.0F, 0.0F, 0.2094F));
		head.addOrReplaceChild("crest",
			CubeListBuilder.create()
				.mirror(false).texOffs(72, 28).addBox(-1.5F, -6.0F, -1.0F, 3.0F, 6.0F, 2.0F),
			PartPose.offsetAndRotation(0.0F, -5.0F, -2.0F, -0.4887F, 0.0F, 0.0F));
		root.addOrReplaceChild("front_left_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(82, 28).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
			PartPose.offset(4.0F, 12.0F, -6.0F));
		root.addOrReplaceChild("front_right_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(82, 28).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
			PartPose.offset(-4.0F, 12.0F, -6.0F));
		root.addOrReplaceChild("back_left_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(82, 28).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
			PartPose.offset(4.0F, 12.0F, 7.0F));
		root.addOrReplaceChild("back_right_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(82, 28).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
			PartPose.offset(-4.0F, 12.0F, 7.0F));
		// END GENERATED

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state);

		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;

		// The walk of any four-legged animal: opposite corners swing together.
		float swing = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.2F * state.walkAnimationSpeed;
		this.frontLeftLeg.xRot = swing;
		this.backRightLeg.xRot = swing;
		this.frontRightLeg.xRot = -swing;
		this.backLeftLeg.xRot = -swing;

		// The folded wings give a small flutter as it walks, and the plume sways against the stride.
		float flutter = Mth.sin(state.ageInTicks * 0.35F) * 0.06F * (0.4F + state.walkAnimationSpeed);
		this.leftWing.zRot += flutter;
		this.rightWing.zRot -= flutter;
		this.tail.yRot = Mth.cos(state.walkAnimationPos * 0.6662F) * 0.2F * state.walkAnimationSpeed;
	}
}
