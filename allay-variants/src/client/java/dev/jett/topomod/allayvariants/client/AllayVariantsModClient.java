package dev.jett.topomod.allayvariants.client;

import dev.jett.topomod.allayvariants.client.render.BunnayModel;
import dev.jett.topomod.allayvariants.client.render.BunnayRenderer;
import dev.jett.topomod.allayvariants.client.screen.BunnayScreen;
import dev.jett.topomod.allayvariants.registry.ModEntities;
import dev.jett.topomod.allayvariants.registry.ModMenus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import net.minecraft.client.gui.screens.MenuScreens;

// Client-only entrypoint: safe to touch rendering, screens and keybinds here.
public class AllayVariantsModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(BunnayRenderer.LAYER, BunnayModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.BUNNAY, BunnayRenderer::new);
		MenuScreens.register(ModMenus.BUNNAY, BunnayScreen::new);
	}
}
