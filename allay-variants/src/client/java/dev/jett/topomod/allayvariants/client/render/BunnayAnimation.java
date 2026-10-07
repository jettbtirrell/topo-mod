package dev.jett.topomod.allayvariants.client.render;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.util.Mth;

// Keyframe animations for the bunnay. Values are offsets from the rest pose: rotations in degrees, positions in
// model pixels. Position keyframes are made with linearPos, which takes y down like the model does.
public final class BunnayAnimation {
	// Declared first: HOP is built while the class loads, and it needs this already set.
	private static final AnimationChannel.Interpolation SMOOTH = AnimationChannel.Interpolations.CATMULLROM;

	/**
	 * The hop, done the way the frog's jump is: one pose, held for as long as the bunnay is in the air (the entity starts it
	 * at takeoff and stops it at landing). The body, head, arms and legs are two keyframes with the same values, so they hold
	 * still. There is no crouch and no landing squash: it snaps into the pose when it leaves the ground and out of it when it
	 * touches down. The ears are the exception: they do what the real rabbit's do over its hop (flick forward, swing back, settle
	 * again), taken from the rabbit's own clip, whose length this clip has. It is played at HOP_CLIP_TICKS divided by the ticks
	 * the hop spends in the air, so the ears always finish as it lands.
	 */
	public static final AnimationDefinition HOP = build();
	/** How many ticks the hop clip lasts at normal speed (0.75 seconds, the rabbit's hop). */
	public static final float HOP_CLIP_TICKS = 15.0F;

	/**
	 * The idle, adapted from the rabbit's: a four second clip where it perks up, looks to one side and then the
	 * other (head turned and tilted), with the ears flicking and the tail wagging in reaction, then settles.
	 * BunnayEntity starts it on the same 9 to 11 second timer the rabbit uses.
	 */
	public static final AnimationDefinition IDLE = buildIdle();

	/** Height of a leg in model pixels, the same as in BunnayModel. */
	private static final float LEG_HEIGHT = 2.0F;

	private BunnayAnimation() {
	}

	private static AnimationDefinition build() {
		AnimationDefinition.Builder hop = AnimationDefinition.Builder.withLength(0.75F);

		// The body, head and arms ride up a little with the jump, and the legs have to rise with them or a gap opens up.
		Keyframe[] torso = {linearPos(0.0F, HOP_LIFT, 0.0F), linearPos(0.75F, HOP_LIFT, 0.0F)};
		for (String bone : new String[]{"body", "head", "left_arm", "right_arm"}) {
			hop.addAnimation(bone, new AnimationChannel(AnimationChannel.Targets.POSITION, torso));
		}
		hop.addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION, rot(0.0F, HOP_HEAD_PITCH, 0.0F), rot(0.75F, HOP_HEAD_PITCH, 0.0F)));
		hop.addAnimation("left_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION, armPose(-1.0F)));
		hop.addAnimation("right_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION, armPose(1.0F)));

		// Legs: they are only two pixels tall, so they can't stretch or squash; they just tilt back. The leg's model pivot is at
		// its foot, so to swing about the hip it has to move: back by 2 sin(angle) and up by 2 (1 - cos(angle)) (y is down).
		float angle = HOP_LEG_TILT * Mth.DEG_TO_RAD;
		float back = LEG_HEIGHT * Mth.sin(angle);
		float up = LEG_HEIGHT * (Mth.cos(angle) - 1.0F) + Math.min(HOP_LIFT, 0.0F);
		for (String leg : new String[]{"left_leg", "right_leg"}) {
			hop.addAnimation(leg, new AnimationChannel(AnimationChannel.Targets.ROTATION, rot(0.0F, HOP_LEG_TILT, 0.0F), rot(0.75F, HOP_LEG_TILT, 0.0F)));
			hop.addAnimation(leg, new AnimationChannel(AnimationChannel.Targets.POSITION, linearPos(0.0F, up, back), linearPos(0.75F, up, back)));
		}

		// Ears: the real rabbit's hop (RabbitAnimation.HOP), keyframe for keyframe. A touch forward as it leaves the ground, then
		// streamed back (the left a little further than the right, as in the rabbit), held there, and settling by the landing.
		// They also dip a fraction of a pixel (the rabbit's y offsets; posVec takes y up).
		hop.addAnimation("left_ear", new AnimationChannel(AnimationChannel.Targets.ROTATION,
			rot(0.000F, 0.0F, 0.0F), rot(0.125F, 2.5F, 0.0F), rot(0.375F, -48.5F, 0.0F), rot(0.542F, -41.24F, 0.0F), rot(0.750F, 0.0F, 0.0F)));
		hop.addAnimation("right_ear", new AnimationChannel(AnimationChannel.Targets.ROTATION,
			rot(0.000F, 0.0F, 0.0F), rot(0.125F, 7.5F, 0.0F), rot(0.375F, -31.5F, 0.0F), rot(0.500F, -35.33F, 0.0F), rot(0.750F, 0.0F, 0.0F)));
		hop.addAnimation("left_ear", new AnimationChannel(AnimationChannel.Targets.POSITION,
			earDip(0.000F, 0.0F), earDip(0.208F, -0.2F), earDip(0.375F, -0.3F), earDip(0.750F, 0.0F)));
		hop.addAnimation("right_ear", new AnimationChannel(AnimationChannel.Targets.POSITION,
			earDip(0.000F, 0.0F), earDip(0.208F, -0.3F), earDip(0.375F, -0.23F), earDip(0.750F, 0.0F)));

		return hop.build();
	}

	private static AnimationDefinition buildIdle() {
		AnimationDefinition.Builder idle = AnimationDefinition.Builder.withLength(4.0F);

		// Head: lifts a little, turns and tilts to one side, holds, turns the other way, holds, then nods and settles.
		idle.addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
			rot(0.00F, 0.0F, 0.0F, 0.0F), rot(0.25F, -6.0F, 0.0F, 0.0F), rot(0.33F, -4.0F, 0.0F, 0.0F),
			rot(0.75F, -4.0F, 0.0F, 0.0F), rot(0.92F, -4.0F, 20.5F, -11.0F), rot(1.00F, -4.0F, 11.5F, -6.0F),
			rot(1.75F, -4.0F, 11.5F, -6.0F), rot(1.92F, -6.0F, -28.5F, 16.5F), rot(2.00F, -5.0F, -18.5F, 10.0F),
			rot(2.62F, -5.0F, -18.5F, 10.0F), rot(2.71F, -4.0F, 0.0F, 0.0F), rot(3.25F, -4.0F, 0.0F, 0.0F),
			rot(3.58F, 5.0F, 0.0F, 0.0F), rot(3.83F, -2.0F, 0.0F, 0.0F), rot(4.00F, 0.0F, 0.0F, 0.0F)));

		// Arms: paws come up a little in front, like the rabbit's, and drop at the end.
		Keyframe[] paws = {
			rot(0.00F, 0.0F, 0.0F, 0.0F), rot(0.42F, -25.0F, 0.0F, 0.0F), rot(3.58F, -25.0F, 0.0F, 0.0F),
			rot(3.75F, -8.0F, 0.0F, 0.0F), rot(3.83F, 0.0F, 0.0F, 0.0F), rot(4.00F, 0.0F, 0.0F, 0.0F)
		};
		idle.addAnimation("left_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION, paws));
		idle.addAnimation("right_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION, paws));

		// Tail: raised while it is perked up, with a quick wag at each head turn, then flicks down at the end.
		idle.addAnimation("tail", new AnimationChannel(AnimationChannel.Targets.ROTATION,
			rot(0.00F, 0.0F, 0.0F, 0.0F), rot(0.21F, 25.0F, 0.0F, 0.0F), rot(0.83F, 25.0F, 15.0F, 0.0F),
			rot(0.92F, 25.0F, -12.0F, 0.0F), rot(1.04F, 25.0F, 0.0F, 0.0F), rot(1.62F, 25.0F, -10.0F, 0.0F),
			rot(1.71F, 25.0F, 20.0F, 0.0F), rot(1.79F, 25.0F, -15.0F, 0.0F), rot(1.88F, 25.0F, 0.0F, 0.0F),
			rot(3.58F, 25.0F, 0.0F, 0.0F), rot(3.83F, -15.0F, 0.0F, 0.0F), rot(4.00F, 0.0F, 0.0F, 0.0F)));

		idle.addAnimation("left_ear", new AnimationChannel(AnimationChannel.Targets.ROTATION, earFlicks(1.0F)));
		idle.addAnimation("right_ear", new AnimationChannel(AnimationChannel.Targets.ROTATION, earFlicks(-1.0F)));

		return idle.build();
	}

	/**
	 * The ears for one side (side is 1 for the left ear, -1 for the right, which rolls the opposite way): a flop
	 * forward as it perks up, a small twitch at each head turn, a long flick back, a perk forward, and a settle.
	 * These are quick, so they are linear, not smoothed.
	 */
	private static Keyframe[] earFlicks(float side) {
		return new Keyframe[]{
			flick(0.00F, 0.0F, 0.0F), flick(0.21F, 25.0F, 0.0F), flick(0.33F, 0.0F, 0.0F), flick(0.75F, 0.0F, 0.0F),
			flick(0.92F, -12.0F, 7.5F * side), flick(1.08F, 0.0F, 0.0F), flick(1.67F, 0.0F, 0.0F),
			flick(1.75F, -3.0F, -12.0F * side), flick(1.83F, 0.0F, 0.0F), flick(1.92F, -35.0F, 5.0F * side),
			flick(2.00F, -45.0F, 0.0F), flick(2.17F, -30.0F, 0.0F), flick(2.54F, -30.0F, 0.0F), flick(2.71F, 18.0F, 3.0F * side),
			flick(2.83F, 8.0F, 0.0F), flick(3.62F, 8.0F, 0.0F), flick(3.71F, -18.0F, 0.0F), flick(3.83F, 3.0F, 0.0F),
			flick(4.00F, 0.0F, 0.0F)
		};
	}

	private static Keyframe flick(float time, float x, float z) {
		return new Keyframe(time, KeyframeAnimations.degreeVec(x, 0.0F, z), AnimationChannel.Interpolations.LINEAR);
	}

	/**
	 * How far each arm is turned outward, in degrees, while the arms are up in the air. Up there they point nearly straight
	 * forward, so they are parallel, and a weapon held in each (flipped into a reverse grip) would cross the other's; this
	 * angles them apart. It is a turn about the vertical, since the flare (a turn about the arm's own long axis when it
	 * points forward) does nothing to the direction an arm points at that moment.
	 */
	private static final float HOP_ARM_SPREAD_DEGREES = 15.0F;
	/**
	 * How far each arm is rolled about its own long axis while up in the air, in degrees: positive rolls the top of a held
	 * weapon toward the middle (the old flare, 25, made the two weapons meet at the top), negative rolls it outward. A
	 * small negative roll keeps the tops apart; as a side effect the lower part of each arm angles a little down and in.
	 */
	private static final float HOP_ARM_ROLL_DEGREES = -10.0F;

	/** How far the body, head and arms are lifted in the pose, in pixels (y is down, so up is negative). */
	private static final float HOP_LIFT = -0.5F;
	/** The head's pitch and the legs' tilt back in the pose, in degrees. */
	private static final float HOP_HEAD_PITCH = -10.0F;
	private static final float HOP_LEG_TILT = 30.0F;
	/** How far the arms are raised in the pose, in degrees (negative is forward and up). */
	private static final float HOP_ARM_PITCH = -110.0F;

	/**
	 * The arm pose for one side; side is -1 for the left arm and 1 for the right. The right arm turns outward with a
	 * positive y rotation and the left with a negative one, so the spread is the same number times side, as is the roll.
	 */
	private static Keyframe[] armPose(float side) {
		float spread = HOP_ARM_SPREAD_DEGREES * side;
		float roll = HOP_ARM_ROLL_DEGREES * side;
		return new Keyframe[]{rot(0.0F, HOP_ARM_PITCH, spread, roll), rot(0.5F, HOP_ARM_PITCH, spread, roll)};
	}

	/** An ear's up and down offset, as the rabbit's clip gives it (y up, which posVec turns into the model's y down). */
	private static Keyframe earDip(float time, float y) {
		return new Keyframe(time, KeyframeAnimations.posVec(0.0F, y, 0.0F), SMOOTH);
	}

	/**
	 * A position keyframe. y here is down, as in model space (so a crouch is positive). KeyframeAnimations.posVec
	 * takes y up, as Blockbench does, and negates it, so it is negated here to cancel that.
	 */
	private static Keyframe linearPos(float time, float y, float z) {
		return new Keyframe(time, KeyframeAnimations.posVec(0.0F, -y, z), AnimationChannel.Interpolations.LINEAR);
	}

	private static Keyframe rot(float time, float x, float y) {
		return rot(time, x, y, 0.0F);
	}

	private static Keyframe rot(float time, float x, float y, float z) {
		return new Keyframe(time, KeyframeAnimations.degreeVec(x, y, z), SMOOTH);
	}
}
