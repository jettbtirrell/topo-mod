package dev.jett.topomod.allayvariants.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.jett.topomod.allayvariants.client.SoulFireRenderState;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

// The flames on a burning mob are two frames of the fire texture (fire_0, fire_1) drawn in layers up its body. For a mob on soul fire the
// very same flames are drawn with the soul fire block's frames, so the flame looks exactly the same, in soul fire's blue.
@Mixin(FlameFeatureRenderer.class)
public abstract class FlameFeatureRendererMixin {
	@Unique
	private static final SpriteId SOUL_FIRE_0 = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("soul_fire_0");
	@Unique
	private static final SpriteId SOUL_FIRE_1 = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("soul_fire_1");

	@WrapOperation(method = "buildGroup", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/feature/FlameFeatureRenderer;prepare(Lnet/minecraft/client/renderer/feature/FlameFeatureRenderer$Submit;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"))
	private void allayVariants$drawSoulFlames(FlameFeatureRenderer self, FlameFeatureRenderer.Submit submit, VertexConsumer builder, TextureAtlasSprite fire0,
		TextureAtlasSprite fire1, Operation<Void> original, @Local(argsOnly = true) FeatureFrameContext context) {
		if (((SoulFireRenderState) submit.entityRenderState()).allayVariants$isOnSoulFire()) {
			original.call(self, submit, builder, context.atlasManager().get(SOUL_FIRE_0), context.atlasManager().get(SOUL_FIRE_1));
		} else {
			original.call(self, submit, builder, fire0, fire1);
		}
	}
}
