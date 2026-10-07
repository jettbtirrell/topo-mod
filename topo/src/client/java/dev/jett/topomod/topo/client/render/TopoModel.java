package dev.jett.topomod.topo.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.jett.topomod.topo.entity.TopoEntity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;

import org.joml.Quaternionf;
import org.joml.Vector3f;

// Upright mouse. The cube layout below is generated from topo/art/topo.bbmodel (python3 tools/import_bbmodel.py topo);
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
	/** Right arm pose when holding an item in one paw. */
	private static final float ONE_PAW_PITCH = -1.175F;   // forward and up (about 67 degrees)
	private static final float ONE_PAW_OUTWARD = 0.725F;  // away from the body (about 41 degrees)

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
			switch (state.danceStyle) {
				case TopoEntity.DANCE_BACKFLIP -> this.animateBackflip(state.danceTime, state.holdStyle);
				case TopoEntity.DANCE_MOONWALK -> this.animateMoonwalk(state.danceTime, state.holdStyle);
				default -> this.animateDance(state.danceTime, state.holdStyle);
			}
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
			if (state.holdStyle == TopoRenderState.HoldStyle.TWO_PAWS) {
				this.poseArmsHolding();
			} else {
				this.leftArm.xRot = -0.8F;
				if (state.holdStyle == TopoRenderState.HoldStyle.ONE_PAW) {
					this.poseRightArmHolding();
				} else {
					this.rightArm.xRot = -0.8F;
				}
			}
			this.addSubtleLife(state, false);
		} else {
			float swing = Mth.cos(state.walkAnimationPos * 0.8F) * 0.9F * state.walkAnimationSpeed;
			swingLeg(this.leftLeg, swing);
			swingLeg(this.rightLeg, -swing);
			if (state.holdStyle == TopoRenderState.HoldStyle.TWO_PAWS) {
				this.poseArmsHolding();
			} else {
				this.leftArm.xRot = -swing * 0.7F;
				if (state.holdStyle == TopoRenderState.HoldStyle.ONE_PAW) {
					this.poseRightArmHolding();
				} else {
					this.rightArm.xRot = swing * 0.7F;
				}
			}

			// Always-on gentle motion while standing still, then the occasional bigger idle on top.
			if (state.walkAnimationSpeed < 0.05F) {
				this.addSubtleLife(state, true);
			}
			if (state.idleTime >= 0.0F && state.walkAnimationSpeed < 0.05F) {
				this.animateIdle(state.idleTime, state.idleStyle, state.holdStyle);
			}
		}

		this.applyAttackSwing(state);
	}

	/**
	 * The always-on motion that keeps a topo from looking frozen: slow breathing, a head that drifts a little,
	 * arms that sway, and ears that very occasionally flick. Every motion uses two slow waves at unrelated speeds, so
	 * it never visibly repeats. Standing adds the breathing and arm sway; sitting is just the head, ears and tail.
	 */
	private void addSubtleLife(TopoRenderState state, boolean standing) {
		float t = state.ageInTicks + state.phase;

		// Head: a lazy drift in all three directions.
		this.head.yRot += Mth.sin(t * 0.05F) * 0.06F + Mth.sin(t * 0.031F + 1.7F) * 0.04F;
		this.head.xRot += Mth.sin(t * 0.07F + 0.6F) * 0.025F;
		this.head.zRot += Mth.sin(t * 0.04F + 2.3F) * 0.035F;

		// Ears: an occasional quick flick of one ear, each on its own slow timer.
		// The high power makes each flick a short pulse, and the slow rates put 1.5 to 2 minutes between flicks.
		float flickLeft = (float) Math.pow(Math.max(0.0F, Mth.sin(t * 0.0035F)), 400.0);
		float flickRight = (float) Math.pow(Math.max(0.0F, Mth.sin(t * 0.0027F + 2.0F)), 400.0);
		this.leftEar.zRot += flickLeft * Mth.sin(t * 1.3F) * 0.35F;
		this.rightEar.zRot -= flickRight * Mth.sin(t * 1.1F) * 0.35F;

		// Tail: a slower second sway on top of the basic one.
		this.tail.yRot += Mth.sin(t * 0.033F + 0.9F) * 0.15F;

		if (!standing) {
			return;
		}

		// Breathing: the chest rises a hair, and the head and shoulders ride up with it.
		float breath = Mth.sin(t * 0.13F) * 0.5F + 0.5F;
		this.body.yScale = 1.0F + 0.03F * breath;
		this.head.y -= 0.15F * breath;
		this.leftArm.y -= 0.1F * breath;
		this.rightArm.y -= 0.1F * breath;

		// Arms: a soft sway, smaller where they are holding something so the item stays put in the paw.
		float sway = state.holdStyle == TopoRenderState.HoldStyle.NONE ? 1.0F : 0.35F;
		this.leftArm.xRot += Mth.sin(t * 0.09F + 0.4F) * 0.07F * sway;
		this.rightArm.xRot += Mth.sin(t * 0.09F + 2.9F) * 0.07F * sway;
		this.leftArm.zRot += (Mth.sin(t * 0.06F + 1.1F) * 0.04F) * sway;
		this.rightArm.zRot -= (Mth.sin(t * 0.06F + 4.0F) * 0.04F) * sway;
	}

	/** Plays the current idle animation. Arms are only touched when they are free (not holding anything). */
	private void animateIdle(float t, int style, TopoRenderState.HoldStyle holdStyle) {
		float length = TopoEntity.idleLength(style);
		boolean armsFree = holdStyle == TopoRenderState.HoldStyle.NONE;
		// Ease in and out so each animation blends with the standing pose instead of snapping.
		float amp = Mth.clamp(Math.min(t, length - t) / 5.0F, 0.0F, 1.0F);

		switch (style) {
			case TopoEntity.IDLE_LOOK_AROUND -> {
				// Pans its head from side to side, nose bobbing as it sniffs, ears perked.
				this.head.yRot += Mth.sin(t * 0.28F) * 0.85F * amp;
				this.head.xRot += (-0.12F + Mth.sin(t * 1.6F) * 0.07F) * amp;
				this.leftEar.zRot += 0.15F * amp;
				this.rightEar.zRot -= 0.15F * amp;
			}
			case TopoEntity.IDLE_GROOM -> {
				// Washes its face: both paws up by the chin, rubbing, head tipped down.
				if (armsFree) {
					float rub = Mth.sin(t * 0.9F) * 0.25F;
					this.leftArm.xRot = Mth.lerp(amp, this.leftArm.xRot, -1.7F + rub);
					this.rightArm.xRot = Mth.lerp(amp, this.rightArm.xRot, -1.7F - rub);
					this.leftArm.zRot = Mth.lerp(amp, this.leftArm.zRot, 0.5F);
					this.rightArm.zRot = Mth.lerp(amp, this.rightArm.zRot, -0.5F);
				}
				this.head.xRot += 0.2F * amp;
				this.leftEar.zRot += 0.2F * amp;
				this.rightEar.zRot -= 0.2F * amp;
			}
			case TopoEntity.IDLE_TAIL_CHASE -> {
				// Spins around twice after its own tail, head turned back toward it.
				float u = t / length;
				float turn = u * u * (3.0F - 2.0F * u);
				this.root.yRot = Mth.TWO_PI * 2.0F * turn;
				this.root.y -= Math.abs(Mth.sin(t * 0.5F)) * 1.0F * amp;
				this.head.yRot += 0.9F * amp;
				this.head.xRot += 0.2F * amp;
				this.tail.yRot = Mth.lerp(amp, this.tail.yRot, Mth.sin(t * 1.1F) * 0.8F);
			}
			case TopoEntity.IDLE_SHAKE -> {
				// A quick full-body shake, like shaking off water.
				float quick = Mth.clamp(Math.min(t, length - t) / 3.0F, 0.0F, 1.0F);
				this.root.yRot = Mth.sin(t * 2.4F) * 0.4F * quick;
				this.head.zRot += Mth.sin(t * 2.0F) * 0.15F * quick;
				this.leftEar.zRot += Mth.sin(t * 2.8F) * 0.2F * quick;
				this.rightEar.zRot += Mth.sin(t * 2.8F) * 0.2F * quick;
				this.tail.yRot = Mth.lerp(quick, this.tail.yRot, Mth.sin(t * 2.6F) * 0.9F);
			}
			case TopoEntity.IDLE_HOP -> {
				// Two happy hops in place.
				float hop = Math.abs(Mth.sin(t * Mth.PI / 12.0F));
				this.root.y -= hop * 3.0F;
				if (armsFree) {
					this.leftArm.zRot = -1.1F * hop;
					this.rightArm.zRot = 1.1F * hop;
				}
			}
			default -> {
			}
		}
	}

	/** Holding something out in just the right paw: forward and a little away from the body. */
	private void poseRightArmHolding() {
		this.rightArm.xRot = ONE_PAW_PITCH;
		this.rightArm.zRot = ONE_PAW_OUTWARD;
	}

	/**
	 * Swings the attacking arm the way the player's model swings a sword: the body twists into the blow while the
	 * arm whips up and over and back, driven by the attack progress. With a torch in both paws, both swing.
	 */
	private void applyAttackSwing(TopoRenderState state) {
		float swing = state.swingAnimation;
		if (swing <= 0.0F || state.currentSwing == null) {
			return;
		}

		float twist = Mth.sin(Mth.sqrt(swing) * Mth.TWO_PI) * 0.2F;
		this.body.yRot = twist;
		// Keep the shoulders on the twisting body (the player model does the same, with a wider body).
		this.rightArm.z = Mth.sin(twist) * 2.5F;
		this.rightArm.x = -Mth.cos(twist) * 2.5F;
		this.leftArm.z = -Mth.sin(twist) * 2.5F;
		this.leftArm.x = Mth.cos(twist) * 2.5F;
		this.rightArm.yRot += twist;
		this.leftArm.yRot += twist;
		this.leftArm.xRot += twist;

		float whip = Mth.sin(Ease.outQuart(swing) * Mth.PI);
		float lift = Mth.sin(swing * Mth.PI) * -(this.head.xRot - 0.7F) * 0.75F;
		this.rightArm.xRot -= whip * 1.2F + lift;
		this.rightArm.yRot += twist * 2.0F;
		this.rightArm.zRot += Mth.sin(swing * Mth.PI) * -0.4F;
		if (state.holdStyle == TopoRenderState.HoldStyle.TWO_PAWS) {
			this.leftArm.xRot -= whip * 1.2F + lift;
			this.leftArm.yRot += twist * 2.0F;
			this.leftArm.zRot += Mth.sin(swing * Mth.PI) * 0.4F;
		}
	}

	private void poseArmsHolding() {
		this.leftArm.xRot = HOLD_PITCH;
		this.rightArm.xRot = HOLD_PITCH;
		this.leftArm.zRot = HOLD_INWARD;
		this.rightArm.zRot = -HOLD_INWARD;
	}

	/** Height of the middle of the body above the ground, in pixels; the backflip somersaults around this point. */
	private static final float FLIP_PIVOT_Y = 17.0F;
	private static final int FLIP_COUNT = 2;
	/** Tick the first flip leaves the ground, how long each flip lasts, and the gap from one flip's start to the next. */
	private static final float FLIP_FIRST_START = 3.0F;
	private static final float FLIP_DURATION = 14.0F;
	private static final float FLIP_SPACING = 16.0F;
	private static final float FLIP_HEIGHT = 7.0F;

	/**
	 * Victory dance, take two: two standing backflips in a row, then happy hops with one arm out, waving. The
	 * ears and tail flap throughout. An arm holding an item keeps holding it.
	 */
	private void animateBackflip(float t, TopoRenderState.HoldStyle holdStyle) {
		float length = TopoEntity.danceLength(TopoEntity.DANCE_BACKFLIP);
		float amp = Mth.clamp(Math.min(t, length - t) / 6.0F, 0.0F, 1.0F);
		float flipsEnd = FLIP_FIRST_START + (FLIP_COUNT - 1) * FLIP_SPACING + FLIP_DURATION;

		float angle = 0.0F;
		float rise = 0.0F;
		// 0 on the ground, up to 1 mid-flip; the head and legs tuck by this much, so they ease in and out.
		float tuck = 0.0F;
		for (int i = 0; i < FLIP_COUNT; i++) {
			float start = FLIP_FIRST_START + i * FLIP_SPACING;
			if (t > start && t < start + FLIP_DURATION) {
				float air = (t - start) / FLIP_DURATION;
				// Slow at the start and end of the turn, fast in the middle, finishing exactly one full turn.
				float turn = air * air * (3.0F - 2.0F * air);
				// Negative X rotation takes the head up and back, so this is a backflip.
				angle = -Mth.TWO_PI * turn;
				rise = FLIP_HEIGHT * Mth.sin(air * Mth.PI);
				tuck = Mth.sin(air * Mth.PI);
			}
		}
		boolean finale = t >= flipsEnd;
		if (finale) {
			// Happy little hops.
			rise = Math.abs(Mth.sin((t - flipsEnd) * 0.65F)) * 2.5F * amp;
		}

		// Rotating the root turns the whole model about the top of the grid, not about its middle, so shift it to
		// keep the somersault centered on the body: position = rotated position + (pivot - rotated pivot).
		this.root.xRot = angle;
		this.root.y += FLIP_PIVOT_Y * (1.0F - Mth.cos(angle)) - rise;
		this.root.z -= FLIP_PIVOT_Y * Mth.sin(angle);

		// Everything eases in and out with amp, so the dance starts and ends smoothly from the standing pose.
		float finaleBlend = Mth.clamp((t - flipsEnd) / 4.0F, 0.0F, 1.0F);
		float restLeft = this.leftArm.zRot;
		float restRight = this.rightArm.zRot;
		if (holdStyle == TopoRenderState.HoldStyle.TWO_PAWS) {
			this.poseArmsHolding();
		} else {
			float flipArm = -1.25F + Mth.sin(t * 0.7F) * 0.3F;
			// In the finale one arm is out to the side, wagging; the other goes back to rest (or holds its item).
			float wagArm = -1.3F + Mth.sin(t * 1.3F) * 0.55F;
			this.leftArm.zRot = Mth.lerp(amp, restLeft, Mth.lerp(finaleBlend, flipArm, wagArm));
			if (holdStyle == TopoRenderState.HoldStyle.ONE_PAW) {
				this.poseRightArmHolding();
			} else {
				this.rightArm.zRot = Mth.lerp(amp, restRight, Mth.lerp(finaleBlend, -flipArm, restRight));
			}
		}

		this.head.xRot += -0.3F * tuck;
		this.leftEar.zRot += Mth.sin(t * 0.55F) * 0.2F * amp;
		this.rightEar.zRot += Mth.sin(t * 0.55F) * 0.2F * amp;
		this.tail.yRot = Mth.lerp(amp, this.tail.yRot, Mth.sin(t * 0.5F) * 0.6F);

		// Legs are tucked up while in the air.
		float legs = -0.8F * tuck;
		swingLeg(this.leftLeg, legs);
		swingLeg(this.rightLeg, legs);
	}

	/**
	 * Victory dance, take three: the moonwalk. The legs shuffle as if walking forward while the topo (really)
	 * glides backward, with a bobbing head, one arm swinging and the other held out to the side.
	 */
	private void animateMoonwalk(float t, TopoRenderState.HoldStyle holdStyle) {
		float length = TopoEntity.danceLength(TopoEntity.DANCE_MOONWALK);
		float amp = Mth.clamp(Math.min(t, length - t) / 6.0F, 0.0F, 1.0F);
		float phase = t * 0.5F;

		// One foot is lifted and swings forward while the other stays flat, then they swap.
		float left = Mth.sin(phase);
		float right = Mth.sin(phase + Mth.PI);
		swingLeg(this.leftLeg, left * 0.45F * amp);
		swingLeg(this.rightLeg, right * 0.45F * amp);
		this.leftLeg.y -= Math.max(0.0F, Mth.cos(phase)) * 0.8F * amp;
		this.rightLeg.y -= Math.max(0.0F, -Mth.cos(phase)) * 0.8F * amp;

		// A cool, smooth little bob.
		this.root.y -= Math.abs(Mth.sin(phase)) * 0.7F * amp;
		this.head.xRot = -0.15F + Mth.sin(phase * 2.0F) * 0.05F * amp;
		this.head.zRot = Mth.sin(phase) * 0.08F * amp;

		if (holdStyle == TopoRenderState.HoldStyle.TWO_PAWS) {
			this.poseArmsHolding();
		} else {
			// Left arm swings opposite the legs; right arm is held out to the side with a gentle wave.
			this.leftArm.xRot = -0.6F + Mth.sin(phase + Mth.PI) * 0.6F * amp;
			if (holdStyle == TopoRenderState.HoldStyle.ONE_PAW) {
				this.poseRightArmHolding();
			} else {
				this.rightArm.zRot = 1.35F + Mth.sin(phase * 2.0F) * 0.2F * amp;
			}
		}

		this.leftEar.zRot += Mth.sin(t * 0.4F) * 0.12F * amp;
		this.rightEar.zRot += Mth.sin(t * 0.4F + 1.0F) * 0.12F * amp;
		this.tail.yRot = Mth.sin(phase) * 0.4F;
	}

	/**
	 * Victory dance: spins twice while hopping, with ears and tail flapping. The arms stick out to the
	 * sides and wiggle up and down. An arm holding an item stays in front holding it instead.
	 */
	private void animateDance(float t, TopoRenderState.HoldStyle holdStyle) {
		float length = TopoEntity.DANCE_LENGTH;
		// Ease the motion in and out so it doesn't snap at the start or end.
		float amp = Mth.clamp(Math.min(t, length - t) / 6.0F, 0.0F, 1.0F);

		// Whole number of turns, so the model ends facing the way it started.
		this.root.yRot = Mth.TWO_PI * 2.0F * (t / length);
		this.root.y -= Math.abs(Mth.sin(t * 0.35F)) * 2.5F * amp;

		if (holdStyle == TopoRenderState.HoldStyle.TWO_PAWS) {
			this.poseArmsHolding();
		} else {
			// -PI/2 is straight out to the side. Centered a little below that with a smaller swing upward,
			// so the shoulders never lift an arm into the (wide) head sitting just above them.
			float flap = -1.35F + Mth.sin(t * 0.7F) * 0.5F * amp;
			this.leftArm.zRot = flap;
			if (holdStyle == TopoRenderState.HoldStyle.ONE_PAW) {
				this.poseRightArmHolding();
			} else {
				this.rightArm.zRot = -flap;
			}
		}

		this.head.zRot = Mth.sin(t * 0.4F) * 0.12F * amp;
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

	/** Moves the pose stack to the right paw's grip point, for items held in one paw. */
	public void translateToRightPaw(PoseStack poseStack) {
		this.root.translateAndRotate(poseStack);
		Vector3f tip = armTip(this.rightArm);
		poseStack.translate(tip.x / 16.0F, tip.y / 16.0F, tip.z / 16.0F);
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
