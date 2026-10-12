package dev.jett.topomod.skyislands.client.render;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

// Blub: the cube layout below is generated from sky-islands/art/blub.bbmodel by `python3 tools/import_bbmodel.py blub`;
// the animation under it relies on these part names (the ears are children of the body), so keep them if the model is edited.
public class BlubModel extends EntityModel<BlubRenderState> {
	private final ModelPart body;
	private final ModelPart frontLeftLeg;
	private final ModelPart frontRightLeg;
	private final ModelPart backLeftLeg;
	private final ModelPart backRightLeg;
	private final ModelPart leftEar;
	private final ModelPart rightEar;
	private final KeyframeAnimation idleAnimation;

	public BlubModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.frontLeftLeg = root.getChild("front_left_leg");
		this.frontRightLeg = root.getChild("front_right_leg");
		this.backLeftLeg = root.getChild("back_left_leg");
		this.backRightLeg = root.getChild("back_right_leg");
		this.leftEar = this.body.getChild("left_ear");
		this.rightEar = this.body.getChild("right_ear");
		this.idleAnimation = BlubAnimation.IDLE.bake(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// BEGIN GENERATED
		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 0).addBox(-5.0F, -7.0F, -5.0F, 10.0F, 7.0F, 9.0F),
			PartPose.offset(0.0F, 22.0F, 0.0F));
		body.addOrReplaceChild("left_ear",
			CubeListBuilder.create()
				.mirror(false).texOffs(39, 0).addBox(-1.5F, -5.0F, 1.5F, 3.0F, 4.0F, 1.0F),
			PartPose.offset(2.5F, -6.0F, 0.5F));
		body.addOrReplaceChild("right_ear",
			CubeListBuilder.create()
				.mirror(false).texOffs(39, 0).addBox(-1.5F, -5.0F, 1.5F, 3.0F, 4.0F, 1.0F),
			PartPose.offset(-2.5F, -6.0F, 0.5F));
		root.addOrReplaceChild("front_left_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(40, 10).addBox(0.0F, 0.0F, -2.5F, 2.0F, 2.0F, 2.0F),
			PartPose.offset(3.0F, 22.0F, -2.0F));
		root.addOrReplaceChild("front_right_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(40, 10).addBox(-2.0F, 0.0F, -2.5F, 2.0F, 2.0F, 2.0F),
			PartPose.offset(-3.0F, 22.0F, -2.0F));
		root.addOrReplaceChild("back_left_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(40, 10).addBox(0.0F, 0.0F, 0.0F, 2.0F, 2.0F, 2.0F),
			PartPose.offset(3.0F, 22.0F, 2.0F));
		root.addOrReplaceChild("back_right_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(40, 10).addBox(-2.0F, 0.0F, 0.0F, 2.0F, 2.0F, 2.0F),
			PartPose.offset(-3.0F, 22.0F, 2.0F));
		// END GENERATED

		return LayerDefinition.create(mesh, 64, 64);
	}

	/** Radians of the walk cycle per unit of walkAnimationPos: one full left-right tilt (two steps) every 6 units of walking. */
	private static final float CYCLE_RATE = 1.0F;
	/** How far the body leans to each side at a full walk, in radians (about 5 degrees). */
	private static final float TILT = 0.09F;
	/** How much wider and shorter it gets on each step: 1 + this and 1 - this at the most (with 1 - 1.2 x this in height). */
	private static final float SQUISH = 0.08F;
	/** How far each leg swings forward or back at a full walk, in radians (about 35 degrees from the hip). */
	private static final float LEG_SWING = 0.6F;
	/** Walking speed at which the full squish is reached, in walkAnimationSpeed units. */
	private static final float FULL_SPEED = 0.3F;

	@Override
	public void setupAnim(BlubRenderState state) {
		super.setupAnim(state);

		// The face is on the north side of the cubes in Blockbench, which is the side the game treats as the back, so the whole model
		// is turned around. (The leg pattern below looks the same turned around, so it needs no change.)
		this.root.yRot = Mth.PI;

		// Sitting: a squat little loaf with its ears drooped, breathing slowly; the walk and the idle do not apply.
		if (state.isSitting) {
			this.animateSit(state);
			return;
		}
		this.body.y = BODY_Y;

		// Standing still, everything is at rest; walking fades the effect in.
		float amount = Math.min(state.walkAnimationSpeed / FULL_SPEED, 1.0F);
		float phase = state.walkAnimationPos * CYCLE_RATE;

		// Once per step (twice per tilt cycle) it squashes down and out, then stretches up again, like a slime landing and pushing off.
		float squash = Mth.cos(phase * 2.0F) * amount;
		float wide = 1.0F + SQUISH * squash;
		float tall = 1.0F - SQUISH * 1.2F * squash;

		// Each step it leans over to the other side. The ears are children of the body, so they squish and tilt with it, in sync.
		this.body.zRot = Mth.sin(phase) * TILT * amount;
		this.body.xScale = wide;
		this.body.zScale = wide;
		this.body.yScale = tall;

		// The feet stay planted but spread a little with the body.
		for (ModelPart leg : new ModelPart[] {this.frontLeftLeg, this.frontRightLeg, this.backLeftLeg, this.backRightLeg}) {
			leg.xScale = wide;
			leg.zScale = wide;
		}

		// The two legs on a side swing in opposite directions, and the two sides do the opposite of each other:
		// while the left legs swing toward each other (the back one forward, the front one back) the right legs swing apart
		// (the back one back, the front one forward), and every step they swap. A negative xRot swings a leg forward.
		float swing = Mth.sin(phase) * LEG_SWING * amount;
		this.frontLeftLeg.xRot = swing;
		this.backLeftLeg.xRot = -swing;
		this.frontRightLeg.xRot = -swing;
		this.backRightLeg.xRot = swing;

		// The keyframed idle goes on last: it is an offset from the pose above.
		this.idleAnimation.apply(state.idleAnimationState, state.ageInTicks);
	}

	/** The body's height in the rest pose (the same 22 as in the layer above). */
	private static final float BODY_Y = 22.0F;
	/** How far the body sinks when sitting, in pixels: the legs are tucked under it. */
	private static final float SIT_DROP = 1.5F;
	/** How far each ear droops outward when sitting, in radians (about 28 degrees); a positive roll tips the left ear outward. */
	private static final float EAR_DROOP = 0.5F;

	private void animateSit(BlubRenderState state) {
		float breath = Mth.sin(state.ageInTicks * 0.12F) * 0.015F;
		this.body.y = BODY_Y + SIT_DROP;
		this.body.zRot = 0.0F;
		this.body.xScale = 1.07F;
		this.body.zScale = 1.07F;
		this.body.yScale = 0.88F + breath;
		this.leftEar.zRot = EAR_DROOP;
		this.rightEar.zRot = -EAR_DROOP;
		for (ModelPart leg : new ModelPart[] {this.frontLeftLeg, this.frontRightLeg, this.backLeftLeg, this.backRightLeg}) {
			leg.xRot = 0.0F;
			leg.xScale = 1.07F;
			leg.zScale = 1.07F;
		}
		// The front paws poke out ahead a little (a negative xRot swings a leg forward).
		this.frontLeftLeg.xRot = -0.5F;
		this.frontRightLeg.xRot = -0.5F;
	}
}
