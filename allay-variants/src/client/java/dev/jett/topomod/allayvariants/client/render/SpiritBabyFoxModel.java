package dev.jett.topomod.allayvariants.client.render;

import net.minecraft.client.model.animal.fox.BabyFoxModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.FoxRenderState;
import net.minecraft.util.Mth;

// The baby spirit fox: the baby fox's model (and walk animation) with two more tails. The cube layout below is generated from
// allay-variants/art/spirit_fox_baby.bbmodel by `python3 tools/import_bbmodel.py <that file> <this file> <texture>`;
// the pose code relies on these part names, so keep them if the model is edited.
public class SpiritBabyFoxModel extends BabyFoxModel {
	private static final float SWAY_SPEED = 0.11F;
	private static final float SWAY = 0.09F;

	private final ModelPart leftTail;
	private final ModelPart rightTail;

	public SpiritBabyFoxModel(ModelPart root) {
		super(root);
		this.leftTail = this.body.getChild("tail_left");
		this.rightTail = this.body.getChild("tail_right");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// BEGIN GENERATED
		root.addOrReplaceChild("head",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 0).addBox(-3.0F, -2.125F, -5.125F, 6.0F, 5.0F, 5.0F)
				.mirror(false).texOffs(18, 20).addBox(-1.0F, 0.875F, -7.125F, 2.0F, 2.0F, 2.0F)
				.mirror(false).texOffs(30, 0).addBox(-3.0F, -5.125F, -4.125F, 2.0F, 3.0F, 1.0F)
				.mirror(false).texOffs(36, 0).addBox(1.0F, -5.125F, -4.125F, 2.0F, 3.0F, 1.0F),
			PartPose.offset(0.0F, 18.125F, 0.125F));
		root.addOrReplaceChild("right_hind_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(22, 4).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
			PartPose.offset(-1.5F, 22.0F, 4.0F));
		root.addOrReplaceChild("left_hind_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(22, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
			PartPose.offset(1.5F, 22.0F, 4.0F));
		root.addOrReplaceChild("right_front_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(22, 4).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
			PartPose.offset(-1.5F, 22.0F, 0.0F));
		root.addOrReplaceChild("left_front_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(22, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
			PartPose.offset(1.5F, 22.0F, 0.0F));
		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 10).addBox(-2.5F, -2.0F, -3.0F, 5.0F, 4.0F, 6.0F),
			PartPose.offset(0.0F, 20.0F, 2.0F));
		body.addOrReplaceChild("tail",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 20).addBox(-1.5F, -1.48F, -1.0F, 3.0F, 3.0F, 6.0F)
				.mirror(false).texOffs(42, 0).addBox(-1.5F, -1.48F, 5.0F, 3.0F, 3.0F, 2.0F),
			PartPose.offset(0.0F, -0.5F, 3.0F));
		body.addOrReplaceChild("tail_left",
			CubeListBuilder.create()
				.mirror(false).texOffs(30, 5).addBox(-1.5F, -1.48F, -1.0F, 3.0F, 3.0F, 5.0F, new CubeDeformation(0.01F))
				.mirror(false).texOffs(52, 0).addBox(-1.5F, -1.48F, 4.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.01F)),
			PartPose.offsetAndRotation(0.0F, -0.5F, 3.0F, 0.0F, 0.5F, 0.0F));
		body.addOrReplaceChild("tail_right",
			CubeListBuilder.create()
				.mirror(false).texOffs(46, 5).addBox(-1.5F, -1.48F, -1.0F, 3.0F, 3.0F, 5.0F, new CubeDeformation(0.01F))
				.mirror(false).texOffs(22, 13).addBox(-1.5F, -1.48F, 4.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.01F)),
			PartPose.offsetAndRotation(0.0F, -0.5F, 3.0F, 0.0F, -0.5F, 0.0F));
		// END GENERATED

		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(FoxRenderState state) {
		super.setupAnim(state);
		// Its health shows in how high it carries its tails (the sitting and sleeping poses place them themselves).
		if (state instanceof SpiritFoxRenderState fox && !state.isSitting && !state.isSleeping) {
			this.tail.xRot += fox.tailLift;
		}
		// The baby's tails lie along its back, so they sway sideways (around the up axis).
		this.followTail(this.leftTail, Mth.sin(state.ageInTicks * SWAY_SPEED) * SWAY);
		this.followTail(this.rightTail, Mth.sin(state.ageInTicks * SWAY_SPEED + 2.0F) * -SWAY);
	}

	private void followTail(ModelPart side, float sway) {
		PartPose rest = this.tail.getInitialPose();
		side.xRot += this.tail.xRot - rest.xRot();
		side.y += this.tail.y - rest.y();
		side.z += this.tail.z - rest.z();
		side.yRot += sway;
	}
}
