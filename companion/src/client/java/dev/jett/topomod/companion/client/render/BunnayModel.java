package dev.jett.topomod.companion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

// Bunnay: the cube layout below is generated from companion/art/bunnay.bbmodel by
// `python3 companion/art/import_bbmodel.py companion/art/bunnay.bbmodel <this file> <texture>`;
// the animation under it relies on these part names, so keep them if the model is edited.
public class BunnayModel extends EntityModel<BunnayRenderState> implements ArmedModel<BunnayRenderState> {
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart leftArm;
	private final ModelPart rightArm;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;
	private final KeyframeAnimation bigHopAnimation;
	private final KeyframeAnimation idleAnimation;

	public BunnayModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.leftArm = root.getChild("left_arm");
		this.rightArm = root.getChild("right_arm");
		this.leftLeg = root.getChild("left_leg");
		this.rightLeg = root.getChild("right_leg");
		this.bigHopAnimation = BunnayAnimation.BIG_HOP.bake(root);
		this.idleAnimation = BunnayAnimation.IDLE.bake(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// BEGIN GENERATED
		PartDefinition head = root.addOrReplaceChild("head",
			CubeListBuilder.create()
				.mirror(false).texOffs(2, 0).addBox(-2.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F),
			PartPose.offset(0.0F, 19.0F, 0.0F));
		head.addOrReplaceChild("left_ear",
			CubeListBuilder.create()
				.mirror(false).texOffs(26, 0).addBox(-0.8F, -4.5F, -0.5F, 2.0F, 4.0F, 1.0F),
			PartPose.offset(1.3F, -4.5F, 0.0F));
		head.addOrReplaceChild("right_ear",
			CubeListBuilder.create()
				.mirror(true).texOffs(26, 0).addBox(-0.2F, -4.5F, -0.5F, 2.0F, 4.0F, 1.0F),
			PartPose.offset(-2.3F, -4.5F, 0.0F));
		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create()
				.mirror(false).texOffs(26, 13).addBox(-1.5F, -5.0F, -1.0F, 3.0F, 3.0F, 2.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F));
		body.addOrReplaceChild("tail",
			CubeListBuilder.create()
				.mirror(false).texOffs(1, 36).addBox(-1.0F, -1.8F, -0.1F, 2.0F, 2.0F, 2.0F),
			PartPose.offset(0.0F, -1.5F, 0.5F));
		root.addOrReplaceChild("left_arm",
			CubeListBuilder.create()
				.mirror(false).texOffs(23, 23).addBox(-1.0F, -0.5F, -1.0F, 1.0F, 4.0F, 2.0F),
			PartPose.offset(2.5F, 19.5F, 0.0F));
		root.addOrReplaceChild("right_arm",
			CubeListBuilder.create()
				.mirror(true).texOffs(23, 23).addBox(0.0F, -0.5F, -1.0F, 1.0F, 4.0F, 2.0F),
			PartPose.offset(-2.5F, 19.5F, 0.0F));
		root.addOrReplaceChild("left_leg",
			CubeListBuilder.create()
				.mirror(false).texOffs(1, 42).addBox(-0.75F, -2.0F, -1.0F, 1.0F, 1.0F, 2.0F)
				.mirror(false).texOffs(12, 43).addBox(-0.75F, -1.0F, -1.0F, 1.0F, 1.0F, 2.0F),
			PartPose.offset(1.25F, 24.0F, 0.0F));
		root.addOrReplaceChild("right_leg",
			CubeListBuilder.create()
				.mirror(true).texOffs(1, 42).addBox(-0.25F, -2.0F, -1.0F, 1.0F, 1.0F, 2.0F)
				.mirror(true).texOffs(12, 43).addBox(-0.25F, -1.0F, -1.0F, 1.0F, 1.0F, 2.0F),
			PartPose.offset(-1.25F, 24.0F, 0.0F));
		// END GENERATED

		return LayerDefinition.create(mesh, 64, 64);
	}

	// Height of a leg in model pixels. The legs pivot at the ground, so swinging them has to be
	// compensated to look like it pivots at the hip.
	private static final float LEG_HEIGHT = 2.0F;
	/** How far the arms flare out from the body when at rest, in radians (about 25 degrees, like the allay). */
	private static final float ARM_FLARE = 0.43633232F;

	@Override
	public void setupAnim(BunnayRenderState state) {
		super.setupAnim(state);

		// While the idle plays, the head follows the clip instead of where the bunnay is looking (like the rabbit).
		// A dancing bunnay's head is set by the dance below.
		if (!state.idleAnimationState.isStarted() && !state.isDancing) {
			this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
			this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
		}

		// The player's walk (HumanoidModel): legs swing 1.4 radians and arms 1.0 radian at full speed, each arm
		// opposite to the leg on its own side.
		float step = Mth.cos(state.walkAnimationPos * 0.6662F) * state.walkAnimationSpeed;
		if (state.isSitting) {
			step = 0.0F;
			this.animateSit();
		} else {
			swingLeg(this.rightLeg, step * 1.4F);
			swingLeg(this.leftLeg, -step * 1.4F);
		}

		this.animateArms(state);
		this.rightArm.xRot = -step;
		this.leftArm.xRot = step;

		if (state.isDancing) {
			this.animateDance(state);
		}

		this.animateCombat(state);
		this.animateEating(state);

		// Keyframed clips go on last: they are offsets from the pose above.
		this.idleAnimation.apply(state.idleAnimationState, state.ageInTicks);
		this.bigHopAnimation.apply(state.bigHopAnimationState, state.ageInTicks);
	}

	/**
	 * The arms move like the allay's: they hang slightly out from the body, and while standing still they sway
	 * in and out on a slow two-second cycle. Walking holds them steady at the flared pose. This is the allay's own
	 * math (AllayModel.setupAnim) without its holding-an-item parts, which a bunnay will get its own version of.
	 * The arms' forward and back swing while walking is set separately, in setupAnim.
	 */
	private void animateArms(BunnayRenderState state) {
		float cycle = state.ageInTicks * 9.0F * Mth.DEG_TO_RAD;
		float moving = Math.min(state.walkAnimationSpeed / 0.3F, 1.0F);
		float still = 1.0F - moving;

		float flare = ARM_FLARE - Mth.sin(cycle) * Mth.PI * 0.075F * still;
		this.leftArm.zRot = -flare;
		this.rightArm.zRot = flare;
	}

	/** Size of the held item compared to a player's; the bunnay's hands are a fraction of the size (the vex and the copper golem use 0.55). */
	private static final float ITEM_SCALE = 0.55F;
	/** How far down the arm the hand is, in pixels from the shoulder pivot; the held item sits here. */
	private static final float HAND_DOWN = 3.0F;
	/** The arm cubes are 1px wide and sit 0.5px toward the body from their pivot, so the hand is that much off the pivot. */
	private static final float HAND_TOWARD_BODY = 0.5F;
	/** Distance from one shoulder pivot to the body's center, in pixels; the swing keeps the shoulders on the twisting body. */
	private static final float SHOULDER_OFFSET = 2.5F;
	/** How far the item flips for a reverse grip, in degrees; positive turns the tip forward and then down. */
	private static final float REVERSE_GRIP_DEGREES = 180.0F;
	// When the flip out and the flip back happen, in seconds into the hop clip: out as the arms whip up, back as they come down.
	private static final float SPIN_OUT_START = 0.17F;
	private static final float SPIN_OUT_END = 0.32F;
	private static final float SPIN_BACK_START = 0.75F;
	private static final float SPIN_BACK_END = 0.97F;

	/**
	 * The weapon arm in the ready stance: raised forward by about 50 degrees (negative is forward and up; 0 hangs down).
	 * It was the vex's full overhead charge pose, but on a head this close to the shoulders that put the held item
	 * through the head, so it is a much slighter raise now. Raise it further for a more dramatic stance, and stop
	 * short of about 100 degrees (-1.75) or the item starts to point at the head.
	 */
	private static final float READY_PITCH = -0.9F;
	/** A little in toward the body; kept small, since further in brings the item toward the head. */
	private static final float READY_YAW = 0.1F;
	/** Where the arm comes down to at the middle of a strike from the ready stance: nearly hanging, just forward of it. */
	private static final float STRIKE_PITCH = -0.1F;

	/**
	 * Moves the pose stack to a hand, following the arm as it moves, for vanilla's ItemInHandLayer (the same hook the
	 * vex, allay and copper golem use). The layer then turns the stack the way it does for a player and moves the item
	 * 10px down and 2px forward of where this leaves it (and 1px to the side), all at this scale, so that is backed out
	 * here, which leaves the item centered on the hand. The item can also be spun about the hand (see itemSpinDegrees).
	 */
	@Override
	public void translateToHand(BunnayRenderState state, HumanoidArm arm, PoseStack poseStack) {
		boolean right = arm == HumanoidArm.RIGHT;
		float side = right ? 1.0F : -1.0F;
		this.root.translateAndRotate(poseStack);
		(right ? this.rightArm : this.leftArm).translateAndRotate(poseStack);

		// To the hand, then spin about it, so the item turns about the fist and not about the shoulder.
		poseStack.translate(side * HAND_TOWARD_BODY / 16.0F, HAND_DOWN / 16.0F, 0.0F);
		if (right) {
			poseStack.rotateDegrees(Axis.XP, itemSpinDegrees(state));
		}
		poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);

		// Back out the layer's own offset (in the scaled space, in pixels): it moves the item 1px out to the side, 10px
		// down and 2px forward once it has turned the stack, so move the opposite way here first.
		poseStack.translate(side / 16.0F, -10.0F / 16.0F, 2.0F / 16.0F);
	}

	/**
	 * How far the held item is turned about the hand, in degrees, during the big hop. The hop whips the arm up and
	 * forward, which points the item up through the head, so the item flips forward into a reverse grip (blade away
	 * from the head) as the arm goes up, stays flipped in the air, then flips back as the arm comes down to land. The
	 * times are seconds into the hop clip and line up with BunnayAnimation.BIG_HOP.
	 */
	private static float itemSpinDegrees(BunnayRenderState state) {
		if (!state.bigHopAnimationState.isStarted()) {
			return 0.0F;
		}
		float t = state.bigHopAnimationState.getTimeInMillis(state.ageInTicks) / 1000.0F;
		if (t <= SPIN_OUT_START || t >= SPIN_BACK_END) {
			return 0.0F;
		}
		if (t < SPIN_OUT_END) {
			return REVERSE_GRIP_DEGREES * smoothstep((t - SPIN_OUT_START) / (SPIN_OUT_END - SPIN_OUT_START));
		}
		if (t <= SPIN_BACK_START) {
			return REVERSE_GRIP_DEGREES;
		}
		return REVERSE_GRIP_DEGREES * (1.0F - smoothstep((t - SPIN_BACK_START) / (SPIN_BACK_END - SPIN_BACK_START)));
	}

	private static float smoothstep(float x) {
		float clamped = Mth.clamp(x, 0.0F, 1.0F);
		return clamped * clamped * (3.0F - 2.0F * clamped);
	}

	/** The off hand in the eating pose: raised forward and turned in, so the carrot in it is at the mouth. */
	private static final float EAT_ARM_PITCH = -1.45F;
	private static final float EAT_ARM_YAW = 0.5F;

	/**
	 * Eating: the left hand (the off hand, which holds the carrot) comes up to the mouth and the head bobs, chewing.
	 * Eased in and out by the entity. It only happens out of combat, so it does not meet the weapon arm's poses.
	 */
	private void animateEating(BunnayRenderState state) {
		float eat = state.eatProgress;
		if (eat <= 0.0F) {
			return;
		}
		this.leftArm.xRot = Mth.lerp(eat, this.leftArm.xRot, EAT_ARM_PITCH);
		this.leftArm.yRot = Mth.lerp(eat, this.leftArm.yRot, EAT_ARM_YAW);
		this.head.xRot += Mth.sin(state.ageInTicks * 1.3F) * 0.1F * eat;
	}

	/**
	 * Everything that only happens when the bunnay is armed. The arm holds the item with the player's pose (tilted
	 * forward a little and swinging half as much), raises into a ready stance while it closes on a target, and
	 * swings when it attacks. An unarmed bunnay is left exactly as the rest of setupAnim made it.
	 */
	private void animateCombat(BunnayRenderState state) {
		if (state.rightHandItemState.isEmpty()) {
			return;
		}

		// The player's "holding an item" arm pose (HumanoidModel.ArmPose.ITEM).
		this.rightArm.xRot = this.rightArm.xRot * 0.5F - 0.31415927F;

		// Ready stance (the vex's charge): the weapon arm comes up over the head as it closes on a target, and eases
		// back down afterward. The other arm keeps swinging as normal.
		float ready = state.readyProgress;
		if (ready > 0.0F) {
			this.rightArm.xRot = Mth.lerp(ready, this.rightArm.xRot, READY_PITCH);
			this.rightArm.yRot = Mth.lerp(ready, this.rightArm.yRot, READY_YAW);
		}

		this.applyAttackSwing(state);
	}

	/**
	 * The player's attack swing (HumanoidModel.setupAttackAnimation): the body twists into the blow while the swinging
	 * arm whips up and over and back, driven by the attack progress. The only change is that the shoulders are 2.5px
	 * from the body's center here, not the player's 5.
	 */
	private void applyAttackSwing(BunnayRenderState state) {
		float swing = state.swingAnimation;
		if (swing <= 0.0F || state.currentSwing == null) {
			return;
		}
		HumanoidArm attackArm = state.currentSwing.hand().asArm(state.mainArm);
		ModelPart arm = attackArm == HumanoidArm.RIGHT ? this.rightArm : this.leftArm;
		ModelPart body = this.root.getChild("body");

		body.yRot = Mth.sin(Mth.sqrt(swing) * Mth.TWO_PI) * 0.2F;
		if (attackArm == HumanoidArm.LEFT) {
			body.yRot *= -1.0F;
		}
		// Keep the shoulders on the twisting body.
		this.rightArm.z = Mth.sin(body.yRot) * SHOULDER_OFFSET;
		this.rightArm.x = -Mth.cos(body.yRot) * SHOULDER_OFFSET;
		this.leftArm.z = -Mth.sin(body.yRot) * SHOULDER_OFFSET;
		this.leftArm.x = Mth.cos(body.yRot) * SHOULDER_OFFSET;
		this.rightArm.yRot += body.yRot;
		this.leftArm.yRot += body.yRot;
		this.leftArm.xRot += body.yRot;

		float whip = Mth.sin(Ease.outQuart(swing) * Mth.PI);
		float lift = Mth.sin(swing * Mth.PI) * -(this.head.xRot - 0.7F) * 0.75F;
		float swungPitch = arm.xRot - (whip * 1.2F + lift);
		// From the ready stance the player's swing would go on up and over, so the arm chops down in front instead and
		// comes back up to the ready position; with no ready stance it is exactly the player's swing.
		float chopPitch = Mth.lerp(whip, arm.xRot, STRIKE_PITCH);
		arm.xRot = Mth.lerp(state.readyProgress, swungPitch, chopPitch);
		arm.yRot += body.yRot * 2.0F;
		arm.zRot += Mth.sin(swing * Mth.PI) * -0.4F;
	}

	/** Sitting: how far the body, head and arms drop (a leg's height, so the bottom rests on the ground). */
	private static final float SIT_DROP = 2.0F;

	/**
	 * Sits down: the body, head and arms drop so the bottom rests on the ground, and the legs lie flat, sticking straight
	 * out in front of the body. A leg pivots at its foot, so turned forward 90 degrees it lies with its top end behind its
	 * foot; it is moved up half its thickness (so it lies on the ground) and forward (so the feet are in front of the body).
	 */
	private void animateSit() {
		for (ModelPart part : new ModelPart[]{this.body, this.head, this.leftArm, this.rightArm}) {
			part.y += SIT_DROP;
		}
		for (ModelPart leg : new ModelPart[]{this.leftLeg, this.rightLeg}) {
			leg.xRot = -Mth.HALF_PI;
			leg.y -= 1.0F;
			leg.z -= 3.0F;
		}
	}

	/** Height of the ground in model space; the dance sways the whole model about the feet. */
	private static final float GROUND_Y = 24.0F;

	/**
	 * The allay's jukebox dance (AllayModel.setupAnim): one wave, cos(8 degrees per tick), rolls the whole body 16
	 * degrees from side to side and turns the head 30 degrees and tilts it 14, all in step. The allay rolls about its
	 * middle; a bunnay stands on the ground, so this rolls it about its feet instead, which means shifting the model
	 * to keep the feet in place (position = rotated position + (pivot - rotated pivot)).
	 */
	private void animateDance(BunnayRenderState state) {
		float wave = Mth.cos(state.ageInTicks * 8.0F * Mth.DEG_TO_RAD + state.walkAnimationSpeed);

		float roll = wave * 16.0F * Mth.DEG_TO_RAD;
		this.root.zRot = roll;
		this.root.x += GROUND_Y * Mth.sin(roll);
		this.root.y += GROUND_Y * (1.0F - Mth.cos(roll));

		this.head.yRot = wave * 30.0F * Mth.DEG_TO_RAD;
		this.head.zRot = wave * 14.0F * Mth.DEG_TO_RAD;
	}

	/** Rotates a leg about its hip (top) even though its model pivot is at the foot. */
	private static void swingLeg(ModelPart leg, float angle) {
		leg.xRot = angle;
		leg.y += LEG_HEIGHT * (Mth.cos(angle) - 1.0F);
		leg.z += LEG_HEIGHT * Mth.sin(angle);
	}
}
