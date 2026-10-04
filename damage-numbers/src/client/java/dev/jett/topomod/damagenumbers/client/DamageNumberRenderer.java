package dev.jett.topomod.damagenumbers.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

// Draws each popup as world-space text that faces the camera, rises and fades out.
public final class DamageNumberRenderer {
	private static final int DAMAGE_COLOR = 0xFF5555;
	private static final int HEAL_COLOR = 0x55FF55;
	/** Blocks risen over a popup's lifetime. */
	private static final float RISE = 0.8F;
	private static final float BASE_SCALE = 0.03F;

	private DamageNumberRenderer() {
	}

	static void render(LevelRenderContext context) {
		if (DamageNumberManager.POPUPS.isEmpty()) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		CameraRenderState camera = context.levelState().cameraRenderState;
		PoseStack poseStack = context.poseStack();

		for (DamageNumberManager.Popup popup : DamageNumberManager.POPUPS) {
			float progress = Math.min((popup.age + partialTick) / DamageNumberManager.LIFETIME, 1.0F);
			// Ease out: fast at first, then slows as it floats up.
			float rise = RISE * (1.0F - (1.0F - progress) * (1.0F - progress));

			// Fade over the last 40% of the lifetime.
			float alpha = progress < 0.6F ? 1.0F : 1.0F - (progress - 0.6F) / 0.4F;
			int color = ARGB.color(Math.max(alpha, 0.05F), popup.amount > 0 ? DAMAGE_COLOR : HEAL_COLOR);

			// Bigger hits are bigger numbers (capped), and popups start with a little pop.
			float size = BASE_SCALE * (1.0F + Math.min(Math.abs(popup.amount), 20.0F) / 20.0F);
			float pop = 1.0F + 0.4F * Math.max(0.0F, 1.0F - progress * 8.0F);

			Vec3 pos = popup.origin.add(0.0, rise, 0.0);
			FormattedCharSequence text = Component.literal(format(popup.amount)).getVisualOrderText();

			poseStack.pushPose();
			poseStack.translate(pos.x - camera.pos.x, pos.y - camera.pos.y, pos.z - camera.pos.z);
			poseStack.rotate(camera.orientation);
			poseStack.scale(size * pop, -size * pop, size * pop);
			context.submitNodeCollector().submitText(
				poseStack,
				-minecraft.font.width(text) / 2.0F,
				0.0F,
				text,
				true,
				Font.DisplayMode.SEE_THROUGH,
				LightCoordsUtil.FULL_BRIGHT,
				color,
				0,
				0
			);
			poseStack.popPose();
		}
	}

	private static String format(float amount) {
		float abs = Math.abs(amount);
		String number = abs == Math.rint(abs) ? Integer.toString((int) abs) : String.format("%.1f", abs);
		return amount < 0 ? "+" + number : number;
	}
}
