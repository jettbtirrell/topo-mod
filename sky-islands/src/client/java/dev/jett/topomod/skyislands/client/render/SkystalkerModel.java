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

// Skystalker: the cube layout below is generated from sky-islands/art/skystalker.bbmodel by `python3 tools/import_bbmodel.py skystalker`;
// the animation under it relies on these part names (the head is a child of the neck, the shins of the legs), so keep them if the model is edited.
public class SkystalkerModel extends EntityModel<LivingEntityRenderState> {
	private final ModelPart neck;
	private final ModelPart head;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;
	private final ModelPart leftShin;
	private final ModelPart rightShin;
	private final ModelPart tail;

	public SkystalkerModel(ModelPart root) {
		super(root);
		ModelPart body = root.getChild("body");
		this.neck = body.getChild("neck");
		this.head = this.neck.getChild("head");
		this.tail = body.getChild("tail");
		this.leftLeg = root.getChild("left_leg");
		this.rightLeg = root.getChild("right_leg");
		this.leftShin = this.leftLeg.getChild("left_shin");
		this.rightShin = this.rightLeg.getChild("right_shin");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// BEGIN GENERATED
		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 0).addBox(-6.0F, -6.0F, -7.0F, 12.0F, 12.0F, 16.0F)
				.mirror(false).texOffs(56, 0).addBox(-5.0F, -4.0F, -9.5F, 10.0F, 9.0F, 2.5F),
			PartPose.offset(0.0F, 3.0F, 0.0F));
		body.addOrReplaceChild("tail",
			CubeListBuilder.create()
				.mirror(false).texOffs(82, 0).addBox(-3.0F, -1.0F, 0.0F, 6.0F, 2.0F, 13.0F),
			PartPose.offsetAndRotation(0.0F, -2.0F, 9.0F, 0.2094F, 0.0F, 0.0F));
		body.addOrReplaceChild("tail_left",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 28).addBox(0.0F, -1.0F, 0.0F, 3.0F, 1.5F, 11.0F),
			PartPose.offsetAndRotation(2.0F, -2.0F, 9.0F, 0.0F, 0.2443F, 0.0F));
		body.addOrReplaceChild("tail_right",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 28).addBox(-3.0F, -1.0F, 0.0F, 3.0F, 1.5F, 11.0F),
			PartPose.offsetAndRotation(-2.0F, -2.0F, 9.0F, 0.0F, -0.2443F, 0.0F));
		body.addOrReplaceChild("left_wing",
			CubeListBuilder.create()
				.mirror(false).texOffs(28, 28).addBox(0.0F, 0.0F, -4.0F, 2.0F, 10.0F, 10.0F)
				.mirror(false).texOffs(52, 28).addBox(0.2F, 9.0F, 0.0F, 1.6F, 7.0F, 8.0F),
			PartPose.offsetAndRotation(6.0F, -5.0F, 0.0F, 0.0F, 0.0F, -0.1396F));
		body.addOrReplaceChild("right_wing",
			CubeListBuilder.create()
				.mirror(false).texOffs(28, 28).addBox(-2.0F, 0.0F, -4.0F, 2.0F, 10.0F, 10.0F)
				.mirror(false).texOffs(52, 28).addBox(-1.8F, 9.0F, 0.0F, 1.6F, 7.0F, 8.0F),
			PartPose.offsetAndRotation(-6.0F, -5.0F, 0.0F, 0.0F, 0.0F, 0.1396F));
		PartDefinition neck = body.addOrReplaceChild("neck",
			CubeListBuilder.create()
				.mirror(false).texOffs(72, 28).addBox(-2.5F, -14.0F, -2.5F, 5.0F, 14.0F, 5.0F)
				.mirror(false).texOffs(92, 28).addBox(-4.0F, -5.0F, -3.0F, 8.0F, 6.0F, 6.0F),
			PartPose.offsetAndRotation(0.0F, -4.0F, -6.0F, 0.384F, 0.0F, 0.0F));
		PartDefinition head = neck.addOrReplaceChild("head",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 48).addBox(-4.0F, -6.0F, -5.0F, 8.0F, 7.0F, 8.0F)
				.mirror(false).texOffs(32, 48).addBox(-1.5F, -4.0F, -11.0F, 3.0F, 4.0F, 6.0F)
				.mirror(false).texOffs(50, 48).addBox(-1.0F, 0.0F, -11.0F, 2.0F, 2.0F, 2.0F)
				.mirror(false).texOffs(58, 48).addBox(-1.5F, 0.0F, -9.0F, 3.0F, 2.0F, 4.0F),
			PartPose.offsetAndRotation(0.0F, -13.0F, 0.0F, -0.384F, 0.0F, 0.0F));
		head.addOrReplaceChild("crest_center",
			CubeListBuilder.create()
				.mirror(false).texOffs(72, 48).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
			PartPose.offsetAndRotation(0.0F, -6.0F, 1.0F, -0.6109F, 0.0F, 0.0F));
		head.addOrReplaceChild("crest_left",
			CubeListBuilder.create()
				.mirror(false).texOffs(80, 48).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
			PartPose.offsetAndRotation(2.5F, -6.0F, 1.0F, -0.6109F, 0.0F, 0.0F));
		head.addOrReplaceChild("crest_right",
			CubeListBuilder.create()
				.mirror(false).texOffs(80, 48).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
			PartPose.offsetAndRotation(-2.5F, -6.0F, 1.0F, -0.6109F, 0.0F, 0.0F));
		PartDefinition left_leg = root.addOrReplaceChild("left_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(88, 48).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 9.0F, 6.0F),
			PartPose.offset(4.0F, 5.0F, 0.0F));
		left_leg.addOrReplaceChild("left_shin",
			CubeListBuilder.create()
				.mirror(false).texOffs(108, 48).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F)
				.mirror(false).texOffs(0, 63).addBox(-2.0F, 8.0F, -5.0F, 1.0F, 2.0F, 6.0F)
				.mirror(false).texOffs(14, 63).addBox(-0.5F, 8.0F, -7.0F, 1.0F, 2.0F, 8.0F)
				.mirror(false).texOffs(32, 63).addBox(1.0F, 8.0F, -5.0F, 1.0F, 2.0F, 6.0F)
				.mirror(false).texOffs(46, 63).addBox(-2.0F, 8.5F, -7.0F, 1.0F, 1.5F, 2.0F)
				.mirror(false).texOffs(52, 63).addBox(-0.5F, 8.5F, -9.0F, 1.0F, 1.5F, 2.0F)
				.mirror(false).texOffs(58, 63).addBox(1.0F, 8.5F, -7.0F, 1.0F, 1.5F, 2.0F)
				.mirror(false).texOffs(64, 63).addBox(-0.5F, 8.5F, 1.0F, 1.0F, 1.5F, 2.0F),
			PartPose.offset(0.0F, 9.0F, 2.0F));
		PartDefinition right_leg = root.addOrReplaceChild("right_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(88, 48).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 9.0F, 6.0F),
			PartPose.offset(-4.0F, 5.0F, 0.0F));
		right_leg.addOrReplaceChild("right_shin",
			CubeListBuilder.create()
				.mirror(false).texOffs(108, 48).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F)
				.mirror(false).texOffs(0, 63).addBox(1.0F, 8.0F, -5.0F, 1.0F, 2.0F, 6.0F)
				.mirror(false).texOffs(14, 63).addBox(-0.5F, 8.0F, -7.0F, 1.0F, 2.0F, 8.0F)
				.mirror(false).texOffs(32, 63).addBox(-2.0F, 8.0F, -5.0F, 1.0F, 2.0F, 6.0F)
				.mirror(false).texOffs(46, 63).addBox(1.0F, 8.5F, -7.0F, 1.0F, 1.5F, 2.0F)
				.mirror(false).texOffs(52, 63).addBox(-0.5F, 8.5F, -9.0F, 1.0F, 1.5F, 2.0F)
				.mirror(false).texOffs(58, 63).addBox(-2.0F, 8.5F, -7.0F, 1.0F, 1.5F, 2.0F)
				.mirror(false).texOffs(64, 63).addBox(-0.5F, 8.5F, 1.0F, 1.0F, 1.5F, 2.0F),
			PartPose.offset(0.0F, 9.0F, 2.0F));
		// END GENERATED

		return LayerDefinition.create(mesh, 128, 128);
	}

	/** Radians of the stride per unit of walkAnimationPos; a bird's short legs turn over fast. */
	private static final float STRIDE_RATE = 0.8F;
	/** How far a leg swings forward and back from the hip at a full walk, in radians (about 40 degrees). */
	private static final float LEG_SWING = 0.7F;
	/** How far the shin folds back as the leg comes forward, in radians. */
	private static final float SHIN_BEND = 0.6F;
	/** How far the neck rocks forward and back with each step, in radians: the head stays level while the neck drives it. */
	private static final float NECK_BOB = 0.1F;
	/** Walking speed at which the full stride is reached, in walkAnimationSpeed units. */
	private static final float FULL_SPEED = 0.5F;

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state);

		// The head follows where it is looking, on top of the pose it was modelled in.
		this.head.yRot += state.yRot * Mth.DEG_TO_RAD;
		this.head.xRot += state.xRot * Mth.DEG_TO_RAD;

		float amount = Math.min(state.walkAnimationSpeed / FULL_SPEED, 1.0F);
		float phase = state.walkAnimationPos * STRIDE_RATE;

		// Legs swing opposite to each other. A negative xRot swings a leg forward; the shin folds back while its leg comes forward.
		float swing = Mth.cos(phase) * LEG_SWING * amount;
		this.leftLeg.xRot = swing;
		this.rightLeg.xRot = -swing;
		this.leftShin.xRot = Math.max(-swing, 0.0F) * (SHIN_BEND / LEG_SWING);
		this.rightShin.xRot = Math.max(swing, 0.0F) * (SHIN_BEND / LEG_SWING);

		// The neck rocks once per step, and the tail swings a little against the stride.
		this.neck.xRot += Mth.sin(phase * 2.0F) * NECK_BOB * amount;
		this.tail.yRot = Mth.sin(phase) * 0.12F * amount;
	}
}
