package dev.jett.topomod.companion.client.render;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.util.Mth;

// Keyframe animations for the bunnay. Values are offsets from the rest pose: rotations in degrees, positions in
// model pixels. Position keyframes are made with linearPos, which takes y down like the model does.
public final class BunnayAnimation {
	// Declared first: BIG_HOP is built while the class loads, and it needs this already set.
	private static final AnimationChannel.Interpolation SMOOTH = AnimationChannel.Interpolations.CATMULLROM;

	/**
	 * The big hop: crouch (0 to 0.2s), launch, airborne (0.2 to 0.95s), landing squash and recover (0.95 to 1.3s).
	 * The timings match BunnayEntity: the launch happens at tick 4 (0.2s) and the landing about 15 ticks later, so
	 * keep BIG_HOP_TAKEOFF_TICK, BIG_HOP_LAUNCH_SPEED and BIG_HOP_TICKS there in step with this.
	 */
	public static final AnimationDefinition BIG_HOP = build();

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
		AnimationDefinition.Builder hop = AnimationDefinition.Builder.withLength(1.3F);

		// The body, head and arms drop into the crouch and rise through the jump together, and the legs have to rise
		// with them or a gap opens up. So both are driven from this one table of rows, (time, how far the body is
		// offset in pixels with y down, how far the legs are tilted back in degrees), and both tracks are linear:
		// a smoothed curve can overshoot differently for the body and the legs, which is what made the gap.
		float[][] rows = {
			{0.00F, 0.0F, 0.0F}, {0.17F, 1.6F, 0.0F}, {0.25F, -0.8F, 15.0F}, {0.40F, -0.6F, 30.0F}, {0.60F, -0.4F, 30.0F},
			{0.80F, -0.4F, 20.0F}, {0.90F, -0.4F, -5.0F}, {0.95F, -0.5F, -10.0F}, {1.00F, 1.4F, -8.0F},
			{1.12F, 1.4F, 0.0F}, {1.30F, 0.0F, 0.0F}
		};
		Keyframe[] torso = new Keyframe[rows.length];
		for (int i = 0; i < rows.length; i++) {
			torso[i] = linearPos(rows[i][0], rows[i][1], 0.0F);
		}
		for (String bone : new String[]{"body", "head", "left_arm", "right_arm"}) {
			hop.addAnimation(bone, new AnimationChannel(AnimationChannel.Targets.POSITION, torso));
		}

		// Head: looks down into the crouch, snaps up at takeoff, then dips to look at the landing.
		hop.addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
			rot(0.00F, 0.0F, 0.0F), rot(0.17F, 15.0F, 0.0F), rot(0.25F, -20.0F, 0.0F), rot(0.60F, -10.0F, 0.0F),
			rot(0.90F, 10.0F, 0.0F), rot(1.00F, 18.0F, 0.0F), rot(1.12F, 8.0F, 0.0F), rot(1.30F, 0.0F, 0.0F)));

		// Arms: wind back in the crouch, whip up and forward at takeoff, out for balance in the air (the extra
		// flare is negative z for the left arm and positive for the right), then absorb the landing.
		hop.addAnimation("left_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION, armSwing(-1.0F)));
		hop.addAnimation("right_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION, armSwing(1.0F)));

		// Legs: they are only two pixels tall, so they can't stretch or squash; they just tilt back for the jump and
		// reach forward a little to land, never further than they swing when walking.
		Keyframe[] legRotation = new Keyframe[rows.length];
		Keyframe[] legPosition = new Keyframe[rows.length];
		for (int i = 0; i < rows.length; i++) {
			float angle = rows[i][2] * Mth.DEG_TO_RAD;
			// The leg's model pivot is at its foot, so to swing about the hip it has to move: back by 2 sin(angle) and
			// up by 2 (1 - cos(angle)) (y is down). It also rides up with the body, but doesn't follow it down into the
			// crouch, since the feet stay on the ground.
			float back = LEG_HEIGHT * Mth.sin(angle);
			float up = LEG_HEIGHT * (Mth.cos(angle) - 1.0F);
			legRotation[i] = rot(rows[i][0], rows[i][2], 0.0F);
			legPosition[i] = linearPos(rows[i][0], up + Math.min(rows[i][1], 0.0F), back);
		}
		for (String leg : new String[]{"left_leg", "right_leg"}) {
			hop.addAnimation(leg, new AnimationChannel(AnimationChannel.Targets.ROTATION, legRotation));
			hop.addAnimation(leg, new AnimationChannel(AnimationChannel.Targets.POSITION, legPosition));
		}

		// Ears stream back going up, flutter on the way down, then flop forward on impact and bounce back.
		Keyframe[] ears = {
			rot(0.00F, 0.0F, 0.0F), rot(0.17F, 10.0F, 0.0F), rot(0.25F, -35.0F, 0.0F), rot(0.60F, -25.0F, 0.0F),
			rot(0.90F, -40.0F, 0.0F), rot(1.00F, 30.0F, 0.0F), rot(1.12F, 15.0F, 0.0F), rot(1.22F, -8.0F, 0.0F),
			rot(1.30F, 0.0F, 0.0F)
		};
		hop.addAnimation("left_ear", new AnimationChannel(AnimationChannel.Targets.ROTATION, ears));
		hop.addAnimation("right_ear", new AnimationChannel(AnimationChannel.Targets.ROTATION, ears));

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

	/**
	 * The arm keyframes for one side; side is -1 for the left arm and 1 for the right. The right arm turns outward with a
	 * positive y rotation and the left with a negative one, so the spread is the same number times side, as is the flare.
	 */
	private static Keyframe[] armSwing(float side) {
		float spread = HOP_ARM_SPREAD_DEGREES * side;
		float roll = HOP_ARM_ROLL_DEGREES * side;
		return new Keyframe[]{
			rot(0.00F, 0.0F, 0.0F, 0.0F), rot(0.17F, 40.0F, 0.0F, 0.0F), rot(0.25F, -95.0F, spread, roll),
			rot(0.60F, -110.0F, spread, roll), rot(0.90F, -70.0F, spread, roll), rot(1.00F, 25.0F, 0.0F, 0.0F),
			rot(1.15F, 10.0F, 0.0F, 0.0F), rot(1.30F, 0.0F, 0.0F, 0.0F)
		};
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
