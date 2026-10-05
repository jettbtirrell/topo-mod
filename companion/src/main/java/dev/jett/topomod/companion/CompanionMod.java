package dev.jett.topomod.companion;

import dev.jett.topomod.companion.registry.ModEntities;
import dev.jett.topomod.companion.registry.ModItems;
import dev.jett.topomod.companion.registry.ModMenus;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Common entrypoint: runs on both the client and the dedicated server.
public class CompanionMod implements ModInitializer {
	public static final String MOD_ID = "topo_companion";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntities.register();
		ModMenus.register();
		ModItems.register();
		LOGGER.info("Topo Companion loaded");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
