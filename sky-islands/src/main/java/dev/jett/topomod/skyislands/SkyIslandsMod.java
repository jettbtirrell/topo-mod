package dev.jett.topomod.skyislands;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

public class SkyIslandsMod implements ModInitializer {
	public static final String MOD_ID = "sky_islands";

	@Override
	public void onInitialize() {
		ModBlocks.register();
		ModEntities.register();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
