package dev.jett.topomod.allayvariants.registry;

import dev.jett.topomod.allayvariants.AllayVariantsMod;
import dev.jett.topomod.allayvariants.effect.SoulFireEffect;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class ModEffects {
	/** The blue flame of soul fire: see SoulFireEffect. Its icon is textures/mob_effect/soul_fire.png. */
	public static final Holder<MobEffect> SOUL_FIRE = Registry.registerForHolder(
		BuiltInRegistries.MOB_EFFECT,
		AllayVariantsMod.id("soul_fire"),
		new SoulFireEffect(MobEffectCategory.HARMFUL, 0x3FD8E8)
	);

	private ModEffects() {
	}

	public static void register() {
		// Touching the class registers the effect.
	}
}
