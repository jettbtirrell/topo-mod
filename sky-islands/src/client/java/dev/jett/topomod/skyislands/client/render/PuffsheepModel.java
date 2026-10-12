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

// Puffsheep: the cube layout below is generated from sky-islands/art/puffsheep.bbmodel by `python3 tools/import_bbmodel.py puffsheep`;
// the animation under it relies on these part names, so keep them if the model is edited.
public class PuffsheepModel extends EntityModel<LivingEntityRenderState> {
	private final ModelPart head;
	private final ModelPart frontLeftLeg;
	private final ModelPart frontRightLeg;
	private final ModelPart backLeftLeg;
	private final ModelPart backRightLeg;
	private final ModelPart tail;

	public PuffsheepModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.frontLeftLeg = root.getChild("front_left_leg");
		this.frontRightLeg = root.getChild("front_right_leg");
		this.backLeftLeg = root.getChild("back_left_leg");
		this.backRightLeg = root.getChild("back_right_leg");
		this.tail = root.getChild("body").getChild("tail");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// BEGIN GENERATED
		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 0).addBox(-4.0F, -3.0F, -8.0F, 8.0F, 8.0F, 16.0F)
				.mirror(false).texOffs(48, 0).addBox(-6.0F, -6.5F, -9.5F, 12.0F, 13.0F, 19.0F)
				.mirror(false).texOffs(110, 0).addBox(-4.0F, -9.0F, -6.0F, 4.0F, 2.5F, 4.0F)
				.mirror(false).texOffs(110, 0).addBox(0.0F, -9.0F, 0.0F, 4.0F, 2.5F, 4.0F)
				.mirror(false).texOffs(110, 0).addBox(-3.0F, -9.0F, 5.0F, 4.0F, 2.5F, 4.0F),
			PartPose.offset(0.0F, 7.0F, 0.0F));
		body.addOrReplaceChild("tail",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 32).addBox(-1.5F, -8.0F, 0.0F, 3.0F, 8.0F, 2.0F),
			PartPose.offsetAndRotation(0.0F, -5.0F, 9.5F, -0.4363F, 0.0F, 0.0F));
		PartDefinition head = root.addOrReplaceChild("head",
			CubeListBuilder.create()
				.mirror(false).texOffs(10, 32).addBox(-3.0F, -4.0F, -5.5F, 6.0F, 6.0F, 6.0F)
				.mirror(false).texOffs(34, 32).addBox(-3.5F, -6.0F, -4.5F, 7.0F, 3.0F, 5.0F),
			PartPose.offset(0.0F, 5.0F, -9.5F));
		head.addOrReplaceChild("left_ear",
			CubeListBuilder.create()
				.mirror(false).texOffs(58, 32).addBox(0.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F),
			PartPose.offsetAndRotation(3.0F, -3.0F, -0.5F, 0.0F, 0.0F, -0.384F));
		head.addOrReplaceChild("right_ear",
			CubeListBuilder.create()
				.mirror(false).texOffs(58, 32).addBox(-4.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F),
			PartPose.offsetAndRotation(-3.0F, -3.0F, -0.5F, 0.0F, 0.0F, 0.384F));
		head.addOrReplaceChild("sheep_crest",
			CubeListBuilder.create()
				.mirror(false).texOffs(68, 32).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
			PartPose.offsetAndRotation(0.0F, -6.0F, -1.5F, -0.5236F, 0.0F, 0.0F));
		root.addOrReplaceChild("front_left_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(76, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 3.0F),
			PartPose.offset(3.5F, 12.0F, -5.0F));
		root.addOrReplaceChild("front_right_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(76, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 3.0F),
			PartPose.offset(-3.5F, 12.0F, -5.0F));
		root.addOrReplaceChild("back_left_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(76, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 3.0F),
			PartPose.offset(3.5F, 12.0F, 6.0F));
		root.addOrReplaceChild("back_right_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(76, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 3.0F),
			PartPose.offset(-3.5F, 12.0F, 6.0F));
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

		// The plume bobs a little, more when it walks.
		this.tail.xRot += Mth.sin(state.ageInTicks * 0.1F) * 0.05F + Mth.cos(state.walkAnimationPos * 0.6662F * 2.0F) * 0.12F * state.walkAnimationSpeed;
	}
}
