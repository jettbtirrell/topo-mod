package dev.jett.topomod.allayvariants.registry;

import dev.jett.topomod.allayvariants.AllayVariantsMod;
import dev.jett.topomod.allayvariants.item.ClubItem;
import dev.jett.topomod.allayvariants.item.GreatswordItem;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Weapon;

public final class ModItems {
	public static final ResourceKey<Item> BUNNAY_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, AllayVariantsMod.id("bunnay_spawn_egg"));

	public static final Item BUNNAY_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		BUNNAY_SPAWN_EGG_KEY,
		new SpawnEggItem(new Item.Properties().setId(BUNNAY_SPAWN_EGG_KEY).spawnEgg(ModEntities.BUNNAY))
	);

	public static final ResourceKey<Item> SPIRIT_FOX_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, AllayVariantsMod.id("spirit_fox_spawn_egg"));

	public static final Item SPIRIT_FOX_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		SPIRIT_FOX_SPAWN_EGG_KEY,
		new SpawnEggItem(new Item.Properties().setId(SPIRIT_FOX_SPAWN_EGG_KEY).spawnEgg(ModEntities.SPIRIT_FOX))
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

	public static final ResourceKey<Item> GREATSWORD_KEY = ResourceKey.create(Registries.ITEM, AllayVariantsMod.id("greatsword"));

	/** Set up like the mace: one weapon, no material tiers. */
	public static final Item GREATSWORD = Registry.register(
		BuiltInRegistries.ITEM,
		GREATSWORD_KEY,
		new GreatswordItem(new Item.Properties().setId(GREATSWORD_KEY).rarity(Rarity.RARE).durability(500)
			.component(DataComponents.TOOL, GreatswordItem.createToolProperties())
			.attributes(GreatswordItem.createAttributes())
			.enchantable(15)
			.component(DataComponents.WEAPON, new Weapon(1)))
	);

	public static final ResourceKey<Item> CLUB_KEY = ResourceKey.create(Registries.ITEM, AllayVariantsMod.id("club"));

	/** Set up like the mace, down to the repair material: the blaze rod to the mace's breeze rod. */
	public static final Item CLUB = Registry.register(
		BuiltInRegistries.ITEM,
		CLUB_KEY,
		new ClubItem(new Item.Properties().setId(CLUB_KEY).rarity(Rarity.EPIC).durability(500).fireResistant()
			.component(DataComponents.TOOL, ClubItem.createToolProperties())
			.repairable(Items.BLAZE_ROD)
			.attributes(ClubItem.createAttributes())
			.enchantable(15)
			.component(DataComponents.WEAPON, new Weapon(1)))
	);

	private ModItems() {
	}

	public static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(MUSIC_DISC_BUNNY));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(entries -> {
			entries.accept(GREATSWORD);
			entries.accept(CLUB);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> {
			entries.accept(BUNNAY_SPAWN_EGG);
			entries.accept(SPIRIT_FOX_SPAWN_EGG);
		});
	}
}
