package dev.jett.topomod.damagenumbers;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Common entrypoint: runs on both the client and the dedicated server.
public class DamageNumbersMod implements ModInitializer {
	public static final String MOD_ID = "topo_damage_numbers";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Topo Damage Numbers loaded");
	}
}
