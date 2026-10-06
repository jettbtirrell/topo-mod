package dev.jett.topomod.companion.client.render;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

// The keyframe clip for an allay petting a rabbit. Values are offsets from the allay's usual pose (its hover, its
// flapping and its bob keep going underneath): rotations in degrees, positions in model pixels with y down.
public final class AllayPettingAnimation {
	private static final AnimationChannel.Interpolation SMOOTH = AnimationChannel.Interpolations.CATMULLROM;

	/** The first press, the time between presses, and the number of strokes. BunnayBreeding times its sounds to these. */
	private static final float FIRST_PRESS = 0.95F;
	private static final float STROKE_PERIOD = 0.9F;
	private static final int STROKES = 5;
	/** How long after each press the lift comes. */
	private static final float LIFT_DELAY = 0.45F;
	/** The time it is leaned in by, and the times it eases back out through and finishes at. */
	private static final float LEAN_IN = 0.5F;
	private static final float SETTLE = 5.5F;
	private static final float LENGTH = 6.0F;

	/**
	 * Six seconds, played once the allay has reached the rabbit: it leans in over it (to 0.5s), strokes five times with
	 * its right arm (the presses are at 0.95s and every 0.9s after, which BunnayBreeding times its sounds to), then
	 * straightens back up (the last lift is at 5.0s, and it is upright and still by 6s). It starts and ends in the
	 * normal pose, so it never snaps.
	 */
	public static final AnimationDefinition PETTING = build();

	private AllayPettingAnimation() {
	}

	private static AnimationDefinition build() {
		AnimationDefinition.Builder clip = AnimationDefinition.Builder.withLength(LENGTH);

		// The whole body rocks forward on each press and back on each lift.
		clip.addAnimation("body", strokes(false, new float[]{24, 0, 0}, new float[]{31, 0, 0}, new float[]{21, 0, 0}, new float[]{8, 0, 0}));

		// Closer to the rabbit: it drops and moves forward, and bobs with the strokes. (y, z)
		clip.addAnimation("root", strokes(true, new float[]{1.4F, -0.8F}, new float[]{2.0F, -1.0F}, new float[]{1.0F, -0.8F}, new float[]{0.3F, -0.2F}));

		// The head tips down to watch the rabbit and nods and sways with each stroke.
		clip.addAnimation("head", strokes(false, new float[]{26, 0, 0}, new float[]{34, 0, 7}, new float[]{24, 0, -7}, new float[]{8, 0, 0}));

		// The petting arm swings a long way: it reaches down and forward over the rabbit's back, presses to nearly
		// straight out, then lifts back up, a big sweep so that it is easy to see...
		clip.addAnimation("right_arm", strokes(false, new float[]{-62, 0, -8}, new float[]{-95, 0, -3}, new float[]{-34, 0, -16}, new float[]{-10, 0, -3}));

		// ...and the whole arm slides forward on each press and back on each lift, so the paw travels along the fur.
		clip.addAnimation("right_arm", strokes(true, new float[]{0.0F, 0.0F}, new float[]{0.5F, -1.4F}, new float[]{-0.3F, 0.9F}, new float[]{0.0F, 0.3F}));

		// The other arm comes forward and in, as if steadying itself, easing a little with each stroke.
		clip.addAnimation("left_arm", strokes(false, new float[]{-26, 0, 14}, new float[]{-30, 0, 17}, new float[]{-24, 0, 12}, new float[]{-8, 0, 4}));

		// The wings spread a little wider while it hovers close and low.
		clip.addAnimation("right_wing", new AnimationChannel(AnimationChannel.Targets.ROTATION,
			rot(0.0F, 0, 0, 0), rot(LEAN_IN, 0, -8, 0), rot(FIRST_PRESS + (STROKES - 1) * STROKE_PERIOD + LIFT_DELAY, 0, -8, 0), rot(LENGTH, 0, 0, 0)));
		clip.addAnimation("left_wing", new AnimationChannel(AnimationChannel.Targets.ROTATION,
			rot(0.0F, 0, 0, 0), rot(LEAN_IN, 0, 8, 0), rot(FIRST_PRESS + (STROKES - 1) * STROKE_PERIOD + LIFT_DELAY, 0, 8, 0), rot(LENGTH, 0, 0, 0)));

		return clip.build();
	}

	/**
	 * One channel of the stroking pattern: rest, leaned in, then a press and a lift for each stroke, then eased back to
	 * the settle pose and rest. Rotations take x, y and z in degrees; positions take y (down) and z in pixels.
	 */
	private static AnimationChannel strokes(boolean position, float[] lean, float[] press, float[] lift, float[] settle) {
		java.util.List<Keyframe> frames = new java.util.ArrayList<>();
		frames.add(key(position, 0.0F, new float[position ? 2 : 3]));
		frames.add(key(position, LEAN_IN, lean));
		for (int i = 0; i < STROKES; i++) {
			float press_t = FIRST_PRESS + i * STROKE_PERIOD;
			frames.add(key(position, press_t, press));
			frames.add(key(position, press_t + LIFT_DELAY, lift));
		}
		frames.add(key(position, SETTLE, settle));
		frames.add(key(position, LENGTH, new float[position ? 2 : 3]));
		return new AnimationChannel(position ? AnimationChannel.Targets.POSITION : AnimationChannel.Targets.ROTATION, frames.toArray(new Keyframe[0]));
	}

	private static Keyframe key(boolean position, float time, float[] v) {
		return position ? pos(time, v[0], v[1]) : rot(time, v[0], v[1], v[2]);
	}

	/** A position keyframe; y is down here, as in model space (posVec takes y up, so it is negated to cancel). */
	private static Keyframe pos(float time, float y, float z) {
		return new Keyframe(time, KeyframeAnimations.posVec(0.0F, -y, z), SMOOTH);
	}

	private static Keyframe rot(float time, float x, float y, float z) {
		return new Keyframe(time, KeyframeAnimations.degreeVec(x, y, z), SMOOTH);
	}
}
