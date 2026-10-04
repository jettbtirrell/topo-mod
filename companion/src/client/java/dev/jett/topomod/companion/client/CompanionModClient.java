package dev.jett.topomod.companion.client;

import dev.jett.topomod.companion.client.render.TopoModel;
import dev.jett.topomod.companion.client.render.TopoRenderer;
import dev.jett.topomod.companion.registry.ModEntities;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

// Client-only entrypoint: safe to touch rendering, screens and keybinds here.
public class CompanionModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(TopoRenderer.LAYER, TopoModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.TOPO, TopoRenderer::new);
	}
}
