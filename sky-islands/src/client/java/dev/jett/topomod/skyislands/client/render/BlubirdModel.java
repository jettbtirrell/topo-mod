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

// Blubird: the cube layout below is generated from sky-islands/art/blubird.bbmodel by `python3 tools/import_bbmodel.py blubird`;
// the animation under it relies on these part names, so keep them if the model is edited.
public class BlubirdModel extends EntityModel<LivingEntityRenderState> {
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart leftWing;
	private final ModelPart rightWing;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;

	public BlubirdModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.leftWing = root.getChild("left_wing");
		this.rightWing = root.getChild("right_wing");
		this.leftLeg = root.getChild("left_leg");
		this.rightLeg = root.getChild("right_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// BEGIN GENERATED
		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 0).addBox(-3.0F, -3.0F, -4.0F, 6.0F, 6.0F, 8.0F),
			PartPose.offset(0.0F, 19.0F, 2.0F));
		body.addOrReplaceChild("tail",
			CubeListBuilder.create()
				.mirror(false).texOffs(28, 0).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 1.0F, 4.0F),
			PartPose.offsetAndRotation(0.0F, -1.0F, 4.0F, -0.1745F, 0.0F, 0.0F));
		root.addOrReplaceChild("head",
			CubeListBuilder.create()
				.mirror(false).texOffs(42, 0).addBox(-3.0F, -4.0F, -4.0F, 6.0F, 5.0F, 5.0F)
				.mirror(false).texOffs(0, 14).addBox(-1.0F, -2.0F, -6.0F, 2.0F, 2.0F, 2.0F)
				.mirror(false).texOffs(8, 14).addBox(1.5F, -7.0F, -2.0F, 1.0F, 3.0F, 1.0F)
				.mirror(false).texOffs(8, 14).addBox(-2.5F, -7.0F, -2.0F, 1.0F, 3.0F, 1.0F),
			PartPose.offset(0.0F, 17.0F, -3.0F));
		root.addOrReplaceChild("left_wing",
			CubeListBuilder.create()
				.mirror(false).texOffs(12, 14).addBox(0.0F, -0.5F, -2.0F, 6.0F, 1.0F, 6.0F),
			PartPose.offset(3.0F, 17.0F, 1.0F));
		root.addOrReplaceChild("right_wing",
			CubeListBuilder.create()
				.mirror(false).texOffs(12, 14).addBox(-6.0F, -0.5F, -2.0F, 6.0F, 1.0F, 6.0F),
			PartPose.offset(-3.0F, 17.0F, 1.0F));
		root.addOrReplaceChild("left_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(36, 14).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
			PartPose.offset(1.0F, 22.0F, 1.5F));
		root.addOrReplaceChild("right_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(36, 14).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
			PartPose.offset(-1.0F, 22.0F, 1.5F));
		// END GENERATED

		return LayerDefinition.create(mesh, 64, 64);
	}

	/** Radians of the wing cycle per tick: a quick flutter, about three beats a second. */
	private static final float FLAP_RATE = 1.0F;
	/** How far each wing swings above and below flat, in radians (about 40 degrees). */
	private static final float FLAP_RANGE = 0.7F;

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state);

		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;

		// It is always in the air: the wings beat, the body bobs against them, and the feet are tucked up behind.
		float flap = Mth.sin(state.ageInTicks * FLAP_RATE);
		this.leftWing.zRot = flap * FLAP_RANGE;
		this.rightWing.zRot = -flap * FLAP_RANGE;
		this.body.y += Mth.cos(state.ageInTicks * FLAP_RATE) * 0.4F;
		this.head.y += Mth.cos(state.ageInTicks * FLAP_RATE) * 0.4F;
		this.leftLeg.xRot = 0.9F;
		this.rightLeg.xRot = 0.9F;
	}
}
