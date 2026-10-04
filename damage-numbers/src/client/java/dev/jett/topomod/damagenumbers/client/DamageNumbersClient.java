package dev.jett.topomod.damagenumbers.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Purely client-side: damage is inferred from entity health changes, so no server support is needed.
public class DamageNumbersClient implements ClientModInitializer {
	public static final String MOD_ID = "topo_damage_numbers";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_LEVEL_TICK.register(DamageNumberManager::tick);
		LevelRenderEvents.COLLECT_SUBMITS.register(DamageNumberRenderer::render);
		LOGGER.info("Topo Damage Numbers loaded");
	}
}
