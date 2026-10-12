package dev.jett.topomod.skyislands.client;

import dev.jett.topomod.skyislands.ModEntities;
import dev.jett.topomod.skyislands.client.render.BlubModel;
import dev.jett.topomod.skyislands.client.render.BlubRenderer;
import dev.jett.topomod.skyislands.client.render.SkystalkerModel;
import dev.jett.topomod.skyislands.client.render.SkystalkerRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

public class SkyIslandsModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(BlubRenderer.LAYER, BlubModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.BLUB, BlubRenderer::new);
		ModelLayerRegistry.registerModelLayer(SkystalkerRenderer.LAYER, SkystalkerModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.SKYSTALKER, SkystalkerRenderer::new);
	}
}
