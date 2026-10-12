package dev.jett.topomod.skyislands.client;

import dev.jett.topomod.skyislands.ModEntities;
import dev.jett.topomod.skyislands.client.render.BlubModel;
import dev.jett.topomod.skyislands.client.render.BlubirdModel;
import dev.jett.topomod.skyislands.client.render.BlubirdRenderer;
import dev.jett.topomod.skyislands.client.render.BlubRenderer;
import dev.jett.topomod.skyislands.client.render.PlumecowModel;
import dev.jett.topomod.skyislands.client.render.PlumecowRenderer;
import dev.jett.topomod.skyislands.client.render.PuffsheepModel;
import dev.jett.topomod.skyislands.client.render.PuffsheepRenderer;
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
		ModelLayerRegistry.registerModelLayer(PlumecowRenderer.LAYER, PlumecowModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.PLUMECOW, PlumecowRenderer::new);
		ModelLayerRegistry.registerModelLayer(PuffsheepRenderer.LAYER, PuffsheepModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.PUFFSHEEP, PuffsheepRenderer::new);
		ModelLayerRegistry.registerModelLayer(BlubirdRenderer.LAYER, BlubirdModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.BLUBIRD, BlubirdRenderer::new);
	}
}
