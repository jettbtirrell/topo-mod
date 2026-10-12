package dev.jett.topomod.skyislands.client.render;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

// The blub's keyframe animations. Values are offsets from the rest pose: rotations in degrees, positions in model pixels (y up here, as
// Blockbench has it), scales as the size to be (1 is unchanged). BlubEntity starts the idle on the same 9 to 11 second timer as the bunnay's.
public final class BlubAnimation {
	private static final AnimationChannel.Interpolation SMOOTH = AnimationChannel.Interpolations.CATMULLROM;

	private BlubAnimation() {
	}

	/** The times of the clip's keyframes, in seconds, and the body's width at each (its depth scales the same, its height is below). */
	private static final float[] TIMES = {0.00F, 0.20F, 0.40F, 0.60F, 0.80F, 1.00F, 1.20F, 1.40F};
	private static final float[] WIDTH = {1.00F, 1.08F, 0.96F, 1.06F, 0.98F, 1.02F, 1.00F, 1.00F};
	private static final float[] HEIGHT = {1.00F, 0.88F, 1.08F, 0.92F, 1.03F, 0.98F, 1.00F, 1.00F};
	/** How far the body's sides and ends are from its middle, in pixels: the legs move this much of any change in width. */
	private static final float HALF_WIDTH = 5.0F;
	private static final float FRONT_REACH = 4.5F;
	private static final float BACK_REACH = 4.0F;

	// Declared after the numbers above: it is built while the class loads, and needs them already set.
	/**
	 * The idle: a little happy bounce. It squashes down, stretches up, squashes again with a soft landing, gives two small wiggles side to
	 * side, and settles. Kept small so nothing comes apart: the body scales about its base and never leaves the ground, the legs slide in
	 * and out with the body's width so they stay flush with its sides, and the ears only tip a few degrees.
	 */
	public static final AnimationDefinition IDLE = buildIdle();

	private static AnimationDefinition buildIdle() {
		AnimationDefinition.Builder idle = AnimationDefinition.Builder.withLength(1.4F);

		Keyframe[] body = new Keyframe[TIMES.length];
		for (int i = 0; i < TIMES.length; i++) {
			body[i] = scale(TIMES[i], WIDTH[i], HEIGHT[i]);
		}
		idle.addAnimation("body", new AnimationChannel(AnimationChannel.Targets.SCALE, body));
		// The wiggle after it lands.
		idle.addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
			rot(0.00F, 0.0F, 0.0F), rot(0.60F, 0.0F, 0.0F), rot(0.75F, 0.0F, -3.0F), rot(0.90F, 0.0F, 3.0F), rot(1.05F, 0.0F, -2.0F),
			rot(1.20F, 0.0F, 0.0F), rot(1.40F, 0.0F, 0.0F)));

		// Each leg slides with the body's width (the legs are at the sides, and front and back, of the body), so none stand out or sink in.
		idle.addAnimation("front_left_leg", new AnimationChannel(AnimationChannel.Targets.POSITION, legSlide(1.0F, -1.0F, FRONT_REACH)));
		idle.addAnimation("front_right_leg", new AnimationChannel(AnimationChannel.Targets.POSITION, legSlide(-1.0F, -1.0F, FRONT_REACH)));
		idle.addAnimation("back_left_leg", new AnimationChannel(AnimationChannel.Targets.POSITION, legSlide(1.0F, 1.0F, BACK_REACH)));
		idle.addAnimation("back_right_leg", new AnimationChannel(AnimationChannel.Targets.POSITION, legSlide(-1.0F, 1.0F, BACK_REACH)));

		// The ears tip a little behind the body's moves. side is 1 for the left ear and -1 for the right: a positive roll tips the left
		// ear outward and the right ear inward.
		idle.addAnimation("left_ear", new AnimationChannel(AnimationChannel.Targets.ROTATION, ear(1.0F)));
		idle.addAnimation("right_ear", new AnimationChannel(AnimationChannel.Targets.ROTATION, ear(-1.0F)));

		return idle.build();
	}

	private static Keyframe[] ear(float side) {
		return new Keyframe[]{
			rot(0.00F, 0.0F, 0.0F), rot(0.20F, -5.0F, 0.0F), rot(0.40F, 9.0F, 4.0F * side), rot(0.60F, -7.0F, -3.0F * side),
			rot(0.80F, 4.0F, 2.0F * side), rot(1.00F, -2.0F, -1.0F * side), rot(1.30F, 0.0F, 0.0F), rot(1.40F, 0.0F, 0.0F)
		};
	}

	/** side is 1 for a left leg, -1 for a right one; end is -1 for a front leg, 1 for a back one; reach is how far that end is from the middle. */
	private static Keyframe[] legSlide(float side, float end, float reach) {
		Keyframe[] frames = new Keyframe[TIMES.length];
		for (int i = 0; i < TIMES.length; i++) {
			float grow = WIDTH[i] - 1.0F;
			frames[i] = new Keyframe(TIMES[i], KeyframeAnimations.posVec(side * HALF_WIDTH * grow, 0.0F, end * reach * grow), SMOOTH);
		}
		return frames;
	}

	private static Keyframe scale(float time, float x, float y) {
		return new Keyframe(time, KeyframeAnimations.scaleVec(x, y, x), SMOOTH);
	}

	private static Keyframe rot(float time, float x, float z) {
		return new Keyframe(time, KeyframeAnimations.degreeVec(x, 0.0F, z), SMOOTH);
	}
}
