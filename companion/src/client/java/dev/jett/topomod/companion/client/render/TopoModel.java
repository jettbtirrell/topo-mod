package dev.jett.topomod.companion.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

// Placeholder mouse model built from boxes. Part names match what the Blockbench model should use,
// so the real model can replace createBodyLayer() without touching the animation.
public class TopoModel extends EntityModel<TopoRenderState> {
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart leftEar;
	private final ModelPart rightEar;
	private final ModelPart tail;
	private final ModelPart leftFrontLeg;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart rightHindLeg;

	public TopoModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = root.getChild("head");
		this.leftEar = this.head.getChild("left_ear");
		this.rightEar = this.head.getChild("right_ear");
		this.tail = this.body.getChild("tail");
		this.leftFrontLeg = root.getChild("left_front_leg");
		this.rightFrontLeg = root.getChild("right_front_leg");
		this.leftHindLeg = root.getChild("left_hind_leg");
		this.rightHindLeg = root.getChild("right_hind_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild(
			"body",
			CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -3.0F, -3.0F, 4.0F, 3.0F, 6.0F),
			PartPose.offset(0.0F, 22.0F, 0.0F)
		);
		body.addOrReplaceChild(
			"tail",
			CubeListBuilder.create().texOffs(0, 18).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 6.0F),
			PartPose.offsetAndRotation(0.0F, -1.5F, 3.0F, -0.4F, 0.0F, 0.0F)
		);

		PartDefinition head = root.addOrReplaceChild(
			"head",
			CubeListBuilder.create()
				.texOffs(20, 0).addBox(-1.5F, -1.5F, -3.0F, 3.0F, 3.0F, 3.0F)
				.texOffs(32, 6).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 1.0F),
			PartPose.offset(0.0F, 20.0F, -3.0F)
		);
		head.addOrReplaceChild(
			"left_ear",
			CubeListBuilder.create().texOffs(20, 12).addBox(-1.0F, -2.0F, -0.25F, 2.0F, 2.0F, 0.5F),
			PartPose.offsetAndRotation(1.4F, -1.0F, -1.0F, 0.0F, 0.0F, 0.3F)
		);
		head.addOrReplaceChild(
			"right_ear",
			CubeListBuilder.create().texOffs(20, 12).mirror().addBox(-1.0F, -2.0F, -0.25F, 2.0F, 2.0F, 0.5F),
			PartPose.offsetAndRotation(-1.4F, -1.0F, -1.0F, 0.0F, 0.0F, -0.3F)
		);

		CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 26).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F);
		root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(1.2F, 22.0F, -2.2F));
		root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-1.2F, 22.0F, -2.2F));
		root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(1.2F, 22.0F, 2.2F));
		root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-1.2F, 22.0F, 2.2F));

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(TopoRenderState state) {
		super.setupAnim(state);

		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;

		// Ear twitch and tail sway are always on, so it feels alive when standing still.
		this.leftEar.zRot = 0.3F + Mth.sin(state.ageInTicks * 0.13F) * 0.05F;
		this.rightEar.zRot = -0.3F - Mth.sin(state.ageInTicks * 0.11F + 1.0F) * 0.05F;
		this.tail.yRot = Mth.sin(state.ageInTicks * 0.1F) * 0.25F;

		if (state.sitting) {
			this.body.xRot = -0.5F;
			this.body.y += 1.0F;
			this.head.y += 1.5F;
			this.head.z += 0.6F;
			this.leftHindLeg.xRot = -1.2F;
			this.rightHindLeg.xRot = -1.2F;
			this.leftFrontLeg.xRot = 0.0F;
			this.rightFrontLeg.xRot = 0.0F;
		} else {
			float swing = Mth.cos(state.walkAnimationPos * 1.2F) * 1.2F * state.walkAnimationSpeed;
			this.leftFrontLeg.xRot = swing;
			this.rightFrontLeg.xRot = -swing;
			this.leftHindLeg.xRot = -swing;
			this.rightHindLeg.xRot = swing;
		}
	}
}
