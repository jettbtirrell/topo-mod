package dev.jett.topomod.allayvariants.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import dev.jett.topomod.allayvariants.client.SoulFireRenderState;
import dev.jett.topomod.allayvariants.effect.SoulFire;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// A mob on soul fire has its flames drawn in soul fire's blue (see FlameFeatureRendererMixin), and, like a mob on fire, is drawn at full brightness.
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void allayVariants$noteSoulFire(Entity entity, EntityRenderState state, float partialTicks, CallbackInfo ci) {
		((SoulFireRenderState) state).allayVariants$setOnSoulFire(entity instanceof LivingEntity living && SoulFire.isBurning(living));
	}

	@ModifyReturnValue(method = "getBlockLightLevel", at = @At("RETURN"))
	private int allayVariants$soulFireIsBright(int original, Entity entity, BlockPos pos) {
		return entity instanceof LivingEntity living && SoulFire.isBurning(living) ? 15 : original;
	}
}
