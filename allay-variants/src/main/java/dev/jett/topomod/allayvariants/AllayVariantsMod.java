package dev.jett.topomod.allayvariants;

import dev.jett.topomod.allayvariants.command.SpiritFoxCommand;
import dev.jett.topomod.allayvariants.entity.BunnayAi;
import dev.jett.topomod.allayvariants.entity.BunnayBreeding;
import dev.jett.topomod.allayvariants.registry.ModAttachments;
import dev.jett.topomod.allayvariants.registry.ModEffects;
import dev.jett.topomod.allayvariants.registry.ModEnchantmentEffects;
import dev.jett.topomod.allayvariants.registry.ModEntities;
import dev.jett.topomod.allayvariants.registry.ModItems;
import dev.jett.topomod.allayvariants.registry.ModLoot;
import dev.jett.topomod.allayvariants.registry.ModMenus;
import dev.jett.topomod.allayvariants.registry.ModSounds;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Common entrypoint: runs on both the client and the dedicated server.
public class AllayVariantsMod implements ModInitializer {
	public static final String MOD_ID = "allay_variants";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		BunnayAi.register(); // before the entities: the Brain registers its memory and sensor types
		ModEnchantmentEffects.register();
		ModAttachments.register();
		ModEffects.register();
		ModEntities.register();
		ModMenus.register();
		ModSounds.register();
		ModItems.register();
		ModLoot.register();
		BunnayBreeding.register();
		SpiritFoxCommand.register(); // DEBUG-TEMP: remove with the command
		LOGGER.info("Allay Variants loaded");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
