package dev.jett.topomod.allayvariants.registry;

import dev.jett.topomod.allayvariants.AllayVariantsMod;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;

public final class ModItems {
	public static final ResourceKey<Item> BUNNAY_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, AllayVariantsMod.id("bunnay_spawn_egg"));

	public static final Item BUNNAY_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		BUNNAY_SPAWN_EGG_KEY,
		new SpawnEggItem(new Item.Properties().setId(BUNNAY_SPAWN_EGG_KEY).spawnEgg(ModEntities.BUNNAY))
	);

	/** The song the bunny disc plays: data/allay_variants/jukebox_song/bunny.json. */
	public static final ResourceKey<JukeboxSong> BUNNY_SONG = ResourceKey.create(Registries.JUKEBOX_SONG, AllayVariantsMod.id("bunny"));

	public static final ResourceKey<Item> MUSIC_DISC_BUNNY_KEY = ResourceKey.create(Registries.ITEM, AllayVariantsMod.id("music_disc_bunny"));

	/** An allay dancing to this disc splits into a bunnay, not another allay (see BunnayBreeding). */
	public static final Item MUSIC_DISC_BUNNY = Registry.register(
		BuiltInRegistries.ITEM,
		MUSIC_DISC_BUNNY_KEY,
		new Item(new Item.Properties().setId(MUSIC_DISC_BUNNY_KEY).stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(BUNNY_SONG))
	);

	private ModItems() {
	}

	public static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(MUSIC_DISC_BUNNY));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> entries.accept(BUNNAY_SPAWN_EGG));
	}
}
