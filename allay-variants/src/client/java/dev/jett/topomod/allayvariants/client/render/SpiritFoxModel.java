package dev.jett.topomod.allayvariants.client.render;

import net.minecraft.client.model.animal.fox.AdultFoxModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.FoxRenderState;
import net.minecraft.util.Mth;

// The spirit fox: the fox's adult model (its poses are the fox's own) with two more tails. The cube layout below is generated from
// allay-variants/art/spirit_fox.bbmodel by `python3 tools/import_bbmodel.py <that file> <this file> <texture> --unrotated-ground`;
// the pose code relies on these part names, so keep them if the model is edited.
public class SpiritFoxModel extends AdultFoxModel {
	/** How fast, and how far, the two side tails sway (radians), so the three do not move as one. */
	private static final float SWAY_SPEED = 0.09F;
	private static final float SWAY = 0.07F;

	private final ModelPart leftTail;
	private final ModelPart rightTail;

	public SpiritFoxModel(ModelPart root) {
		super(root);
		this.leftTail = this.body.getChild("tail_left");
		this.rightTail = this.body.getChild("tail_right");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// BEGIN GENERATED
		PartDefinition head = root.addOrReplaceChild("head",
			CubeListBuilder.create()
				.mirror(false).texOffs(1, 5).addBox(-3.0F, -2.0F, -5.0F, 8.0F, 6.0F, 6.0F),
			PartPose.offset(-1.0F, 16.5F, -3.0F));
		head.addOrReplaceChild("right_ear",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 0).addBox(-3.0F, -4.0F, -4.0F, 2.0F, 2.0F, 1.0F),
			PartPose.offset(0.0F, 0.0F, 0.0F));
		head.addOrReplaceChild("left_ear",
			CubeListBuilder.create()
				.mirror(false).texOffs(6, 0).addBox(3.0F, -4.0F, -4.0F, 2.0F, 2.0F, 1.0F),
			PartPose.offset(0.0F, 0.0F, 0.0F));
		head.addOrReplaceChild("nose",
			CubeListBuilder.create()
				.mirror(false).texOffs(6, 18).addBox(-1.0F, 2.01F, -8.0F, 4.0F, 2.0F, 3.0F),
			PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create()
				.mirror(false).texOffs(24, 15).addBox(-3.0F, 3.999F, -3.5F, 6.0F, 11.0F, 6.0F),
			PartPose.offsetAndRotation(0.0F, 16.0F, -6.0F, 1.5708F, 0.0F, 0.0F));
		body.addOrReplaceChild("tail",
			CubeListBuilder.create()
				.mirror(false).texOffs(30, 0).addBox(2.0F, 0.0F, -1.0F, 4.0F, 9.0F, 5.0F)
				.mirror(false).texOffs(0, 32).addBox(2.0F, 9.0F, -1.0F, 4.0F, 2.0F, 5.0F),
			PartPose.offsetAndRotation(-4.0F, 15.0F, -1.0F, -0.0524F, 0.0F, 0.0F));
		body.addOrReplaceChild("tail_left",
			CubeListBuilder.create()
				.mirror(false).texOffs(18, 32).addBox(-2.0F, 0.0F, -1.0F, 4.0F, 8.0F, 5.0F, new CubeDeformation(0.01F))
				.mirror(false).texOffs(36, 32).addBox(-2.0F, 8.0F, -1.0F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.01F)),
			PartPose.offsetAndRotation(0.0F, 15.0F, -1.0F, -0.0524F, 0.0F, 0.45F));
		body.addOrReplaceChild("tail_right",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 39).addBox(-2.0F, 0.0F, -1.0F, 4.0F, 8.0F, 5.0F, new CubeDeformation(0.01F))
				.mirror(false).texOffs(36, 39).addBox(-2.0F, 8.0F, -1.0F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.01F)),
			PartPose.offsetAndRotation(0.0F, 15.0F, -1.0F, -0.0524F, 0.0F, -0.45F));
		root.addOrReplaceChild("right_hind_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(13, 24).addBox(2.0F, 0.5F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.001F)),
			PartPose.offset(-5.0F, 17.5F, 7.0F));
		root.addOrReplaceChild("left_hind_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(4, 24).addBox(2.0F, 0.5F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.001F)),
			PartPose.offset(-1.0F, 17.5F, 7.0F));
		root.addOrReplaceChild("right_front_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(13, 24).addBox(2.0F, 0.5F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.001F)),
			PartPose.offset(-5.0F, 17.5F, 0.0F));
		root.addOrReplaceChild("left_front_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(4, 24).addBox(2.0F, 0.5F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.001F)),
			PartPose.offset(-1.0F, 17.5F, 0.0F));
		// END GENERATED

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(FoxRenderState state) {
		super.setupAnim(state);
		// Its health shows in how high it carries its tails (the sitting and sleeping poses place them themselves).
		if (state instanceof SpiritFoxRenderState fox && !state.isSitting && !state.isSleeping) {
			this.tail.xRot += fox.tailLift;
		}
		// Whatever the fox's poses do to its tail (sitting, sleeping) the side tails do too, with a little sway of their own.
		this.followTail(this.leftTail, Mth.sin(state.ageInTicks * SWAY_SPEED) * SWAY);
		this.followTail(this.rightTail, Mth.sin(state.ageInTicks * SWAY_SPEED + 2.0F) * -SWAY);
	}

	private void followTail(ModelPart side, float sway) {
		PartPose rest = this.tail.getInitialPose();
		side.xRot += this.tail.xRot - rest.xRot();
		side.y += this.tail.y - rest.y();
		side.z += this.tail.z - rest.z();
		side.zRot += sway;
	}
}
