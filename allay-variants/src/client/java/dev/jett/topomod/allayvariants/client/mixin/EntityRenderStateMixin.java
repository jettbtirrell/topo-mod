package dev.jett.topomod.allayvariants.client.mixin;

import dev.jett.topomod.allayvariants.client.SoulFireRenderState;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements SoulFireRenderState {
	@Unique
	private boolean allayVariants$onSoulFire;

	@Override
	public boolean allayVariants$isOnSoulFire() {
		return this.allayVariants$onSoulFire;
	}

	@Override
	public void allayVariants$setOnSoulFire(boolean onSoulFire) {
		this.allayVariants$onSoulFire = onSoulFire;
	}
}
