package dev.jett.topomod.allayvariants.registry;

import dev.jett.topomod.allayvariants.AllayVariantsMod;
import dev.jett.topomod.allayvariants.enchantment.BackdraftEffect;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModEnchantmentEffects {
	/** Used in an enchantment's data file as "allay_variants:backdraft"; see BackdraftEffect. */
	public static final DataComponentType<BackdraftEffect> BACKDRAFT = Registry.register(
		BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE,
		AllayVariantsMod.id("backdraft"),
		DataComponentType.<BackdraftEffect>builder().persistent(BackdraftEffect.CODEC).build()
	);

	private ModEnchantmentEffects() {
	}

	public static void register() {
	}
}
