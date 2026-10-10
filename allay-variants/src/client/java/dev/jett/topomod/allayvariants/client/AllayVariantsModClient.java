package dev.jett.topomod.allayvariants.client;

import dev.jett.topomod.allayvariants.client.render.BunnayModel;
import dev.jett.topomod.allayvariants.client.render.BunnayRenderer;
import dev.jett.topomod.allayvariants.client.render.SpiritBabyFoxModel;
import dev.jett.topomod.allayvariants.client.render.SpiritFoxModel;
import dev.jett.topomod.allayvariants.client.render.SpiritFoxRenderer;
import dev.jett.topomod.allayvariants.client.screen.BunnayScreen;
import dev.jett.topomod.allayvariants.item.GreatswordStillness;
import dev.jett.topomod.allayvariants.registry.ModEntities;
import dev.jett.topomod.allayvariants.registry.ModItems;
import dev.jett.topomod.allayvariants.registry.ModMenus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import net.minecraft.client.gui.screens.MenuScreens;

// Client-only entrypoint: safe to touch rendering, screens and keybinds here.
public class AllayVariantsModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(BunnayRenderer.LAYER, BunnayModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.BUNNAY, BunnayRenderer::new);
		ModelLayerRegistry.registerModelLayer(SpiritFoxRenderer.LAYER, SpiritFoxModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(SpiritFoxRenderer.BABY_LAYER, SpiritBabyFoxModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.SPIRIT_FOX, SpiritFoxRenderer::new);
		MenuScreens.register(ModMenus.BUNNAY, BunnayScreen::new);
		// The client keeps its own cooldown meter (the crosshair shows it), so the greatsword holds it empty here too.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null && client.player.getMainHandItem().is(ModItems.GREATSWORD)) {
				GreatswordStillness.tick(client.player, client.player.input.keyPresses);
			}
		});
	}
}
