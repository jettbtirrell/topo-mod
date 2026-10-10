package dev.jett.topomod.allayvariants.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import dev.jett.topomod.allayvariants.item.GreatswordStillness;
import dev.jett.topomod.allayvariants.registry.ModItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// The crosshair draws the cooldown meter whenever it is not full. For the greatsword, which sits empty the whole time
// you move, that is a constant empty bar, so it is hidden while moving unless there is something to hit under the
// crosshair (the same thing the crosshair's own "ready to hit" mark looks at).
@Mixin(Hud.class)
public abstract class HudMixin {
	@ModifyExpressionValue(method = "extractCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F"))
	private float allayVariants$hideIdleGreatswordMeter(float strength) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player != null && player.getMainHandItem().is(ModItems.GREATSWORD) && GreatswordStillness.isMoving(player, player.input.keyPresses)
			&& !(minecraft.crosshairPickEntity instanceof LivingEntity)) {
			return 1.0F; // a full meter draws nothing without a target
		}
		return strength;
	}
}
