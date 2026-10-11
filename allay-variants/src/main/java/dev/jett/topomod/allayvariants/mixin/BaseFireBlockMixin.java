package dev.jett.topomod.allayvariants.mixin;

import dev.jett.topomod.allayvariants.effect.SoulFire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// A living thing standing in a soul fire block is put on soul fire (see SoulFireEffect) instead of the plain fire the block lights, so
// the blue flame is the same whether a block or a spirit fox lit it. Anything that is not a living thing (a dropped item) burns as before.
@Mixin(BaseFireBlock.class)
public abstract class BaseFireBlockMixin {
	@Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
	private void allayVariants$soulFireBurnsWithSoulFire(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise,
		CallbackInfo ci) {
		if (state.is(Blocks.SOUL_FIRE) && entity instanceof LivingEntity living) {
			effectApplier.apply(InsideBlockEffectType.CLEAR_FREEZE);
			SoulFire.ignite(living, SoulFire.BLOCK_TICKS);
			ci.cancel();
		}
	}
}
