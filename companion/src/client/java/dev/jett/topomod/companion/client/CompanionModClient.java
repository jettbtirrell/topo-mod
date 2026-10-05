package dev.jett.topomod.companion.client;

import dev.jett.topomod.companion.client.render.TopoModel;
import dev.jett.topomod.companion.client.screen.TopoScreen;
import dev.jett.topomod.companion.client.render.TopoRenderer;
import dev.jett.topomod.companion.registry.ModEntities;
import dev.jett.topomod.companion.registry.ModMenus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import net.minecraft.client.gui.screens.MenuScreens;

// Client-only entrypoint: safe to touch rendering, screens and keybinds here.
public class CompanionModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(TopoRenderer.LAYER, TopoModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.TOPO, TopoRenderer::new);
		MenuScreens.register(ModMenus.TOPO, TopoScreen::new);
	}
}
