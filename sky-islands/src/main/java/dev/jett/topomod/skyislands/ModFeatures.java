package dev.jett.topomod.skyislands;

import dev.jett.topomod.skyislands.worldgen.AngelVillageFeature;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModFeatures {
	private ModFeatures() {
	}

	/** The village is configured and placed from data/sky_islands/worldgen (feature/angel_village.json and placed_feature/angel_villages.json). */
	public static void register() {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, SkyIslandsMod.id("angel_village"), AngelVillageFeature.CODEC);
	}
}
