package dev.jett.topomod.topo;

import dev.jett.topomod.topo.registry.ModEntities;
import dev.jett.topomod.topo.registry.ModItems;
import dev.jett.topomod.topo.registry.ModMenus;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Common entrypoint: runs on both the client and the dedicated server.
public class TopoMod implements ModInitializer {
	public static final String MOD_ID = "topo";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntities.register();
		ModMenus.register();
		ModItems.register();
		LOGGER.info("Topo loaded");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
