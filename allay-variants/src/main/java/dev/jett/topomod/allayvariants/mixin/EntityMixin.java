package dev.jett.topomod.allayvariants.mixin;

import dev.jett.topomod.allayvariants.effect.SoulFire;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// The flames are drawn on whatever is "on fire" (see displayFireAnimation); a mob on soul fire is not on fire to the game, so it is told
// to show them too. The flames themselves are made blue on the client (see FlameFeatureRendererMixin).
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "displayFireAnimation", at = @At("RETURN"), cancellable = true)
	private void allayVariants$showSoulFire(CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && (Object) this instanceof LivingEntity living && !living.isSpectator() && SoulFire.isBurning(living)) {
			cir.setReturnValue(true);
		}
	}
}
