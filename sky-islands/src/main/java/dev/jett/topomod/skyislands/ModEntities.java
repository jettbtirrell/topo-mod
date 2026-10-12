package dev.jett.topomod.skyislands;

import dev.jett.topomod.skyislands.entity.BlubEntity;
import dev.jett.topomod.skyislands.entity.SkystalkerEntity;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntities {
	public static final ResourceKey<EntityType<?>> BLUB_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SkyIslandsMod.id("blub"));

	public static final EntityType<BlubEntity> BLUB = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		BLUB_KEY,
		EntityType.Builder.of(BlubEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 0.6F)
			.eyeHeight(0.35F)
			.clientTrackingRange(8)
			.build(BLUB_KEY)
	);

	public static final ResourceKey<Item> BLUB_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, SkyIslandsMod.id("blub_spawn_egg"));

	public static final Item BLUB_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		BLUB_SPAWN_EGG_KEY,
		new SpawnEggItem(new Item.Properties().setId(BLUB_SPAWN_EGG_KEY).spawnEgg(BLUB))
	);

	public static final ResourceKey<EntityType<?>> SKYSTALKER_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SkyIslandsMod.id("skystalker"));

	public static final EntityType<SkystalkerEntity> SKYSTALKER = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		SKYSTALKER_KEY,
		EntityType.Builder.of(SkystalkerEntity::new, MobCategory.MONSTER)
			.sized(0.9F, 2.6F)
			.eyeHeight(2.3F)
			.clientTrackingRange(8)
			.build(SKYSTALKER_KEY)
	);

	public static final ResourceKey<Item> SKYSTALKER_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, SkyIslandsMod.id("skystalker_spawn_egg"));

	public static final Item SKYSTALKER_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		SKYSTALKER_SPAWN_EGG_KEY,
		new SpawnEggItem(new Item.Properties().setId(SKYSTALKER_SPAWN_EGG_KEY).spawnEgg(SKYSTALKER))
	);

	private ModEntities() {
	}

	/** It spawns through the forest biome's spawn list (data/sky_islands/worldgen/biome/forest.json), not through code. */
	public static void register() {
		FabricDefaultAttributeRegistry.register(BLUB, BlubEntity.createAttributes());
		SpawnPlacements.register(BLUB, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlubEntity::checkSpawnRules);
		FabricDefaultAttributeRegistry.register(SKYSTALKER, SkystalkerEntity.createAttributes());
		// Any light: the dimension is bright all day, and the vanilla monster rule would wait for dark.
		SpawnPlacements.register(SKYSTALKER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkAnyLightMonsterSpawnRules);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> {
			entries.accept(BLUB_SPAWN_EGG);
			entries.accept(SKYSTALKER_SPAWN_EGG);
		});
	}
}
