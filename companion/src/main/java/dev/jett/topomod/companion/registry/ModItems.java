package dev.jett.topomod.companion.registry;

import dev.jett.topomod.companion.CompanionMod;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public final class ModItems {
	public static final ResourceKey<Item> TOPO_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, CompanionMod.id("topo_spawn_egg"));

	public static final Item TOPO_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		TOPO_SPAWN_EGG_KEY,
		new SpawnEggItem(new Item.Properties().setId(TOPO_SPAWN_EGG_KEY).spawnEgg(ModEntities.TOPO))
	);

	private ModItems() {
	}

	public static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> entries.accept(TOPO_SPAWN_EGG));
	}
}
