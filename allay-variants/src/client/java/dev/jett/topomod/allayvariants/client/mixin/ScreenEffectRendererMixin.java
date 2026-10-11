package dev.jett.topomod.allayvariants.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import dev.jett.topomod.allayvariants.effect.SoulFire;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

// The fire on the screen is the fire texture's second frame; on soul fire it is soul fire's second frame instead, the same size and place.
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
	@Unique
	private static final SpriteId SOUL_FIRE_1 = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("soul_fire_1");

	@WrapOperation(method = "submit", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/resources/model/sprite/SpriteGetter;get(Lnet/minecraft/client/resources/model/sprite/SpriteId;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"))
	private TextureAtlasSprite allayVariants$soulFireOnTheScreen(SpriteGetter sprites, SpriteId id, Operation<TextureAtlasSprite> original) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (id == ModelBakery.FIRE_1 && player != null && SoulFire.isBurning(player)) {
			return original.call(sprites, SOUL_FIRE_1);
		}
		return original.call(sprites, id);
	}
}
