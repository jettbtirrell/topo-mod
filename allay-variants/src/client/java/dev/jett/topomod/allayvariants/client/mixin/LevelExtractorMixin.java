package dev.jett.topomod.allayvariants.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import dev.jett.topomod.allayvariants.effect.SoulFire;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.extract.LevelExtractor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// The fire that fills the screen when you burn is drawn when you are on soul fire too (in its own blue: see ScreenEffectRendererMixin).
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
	@ModifyExpressionValue(method = "extractPlayerState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isOnFire()Z"))
	private boolean allayVariants$soulFireFillsTheScreen(boolean onFire) {
		LocalPlayer player = Minecraft.getInstance().player;
		return onFire || player != null && SoulFire.isBurning(player);
	}
}
