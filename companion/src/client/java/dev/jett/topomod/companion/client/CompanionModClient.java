package dev.jett.topomod.companion.client;

import dev.jett.topomod.companion.client.render.BunnayModel;
import dev.jett.topomod.companion.client.render.BunnayRenderer;
import dev.jett.topomod.companion.client.render.PettingAllayRenderer;
import dev.jett.topomod.companion.client.render.TopoModel;
import dev.jett.topomod.companion.client.screen.BunnayScreen;
import dev.jett.topomod.companion.client.screen.TopoScreen;
import dev.jett.topomod.companion.client.render.TopoRenderer;
import dev.jett.topomod.companion.registry.ModEntities;
import dev.jett.topomod.companion.registry.ModMenus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.entity.EntityTypes;

// Client-only entrypoint: safe to touch rendering, screens and keybinds here.
public class CompanionModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(TopoRenderer.LAYER, TopoModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.TOPO, TopoRenderer::new);
		// Replaces the allay's own renderer, to play the petting animation on it (see PettingAllayRenderer).
		EntityRendererRegistry.register(EntityTypes.ALLAY, PettingAllayRenderer::new);
		ModelLayerRegistry.registerModelLayer(BunnayRenderer.LAYER, BunnayModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.BUNNAY, BunnayRenderer::new);
		MenuScreens.register(ModMenus.TOPO, TopoScreen::new);
		MenuScreens.register(ModMenus.BUNNAY, BunnayScreen::new);
	}
}
