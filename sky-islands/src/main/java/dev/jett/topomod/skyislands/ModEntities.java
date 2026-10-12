package dev.jett.topomod.skyislands;

import dev.jett.topomod.skyislands.entity.AngelEntity;
import dev.jett.topomod.skyislands.entity.BlubEntity;
import dev.jett.topomod.skyislands.entity.BlubirdEntity;
import dev.jett.topomod.skyislands.entity.PlumecowEntity;
import dev.jett.topomod.skyislands.entity.PuffsheepEntity;
import dev.jett.topomod.skyislands.entity.SkystalkerEntity;
import dev.jett.topomod.skyislands.entity.StormcloudEntity;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.LevelAccessor;
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

	public static final ResourceKey<EntityType<?>> PLUMECOW_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SkyIslandsMod.id("plumecow"));

	public static final EntityType<PlumecowEntity> PLUMECOW = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		PLUMECOW_KEY,
		EntityType.Builder.of(PlumecowEntity::new, MobCategory.CREATURE)
			.sized(1.0F, 1.5F)
			.eyeHeight(1.3F)
			.clientTrackingRange(8)
			.build(PLUMECOW_KEY)
	);

	public static final ResourceKey<Item> PLUMECOW_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, SkyIslandsMod.id("plumecow_spawn_egg"));

	public static final Item PLUMECOW_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		PLUMECOW_SPAWN_EGG_KEY,
		new SpawnEggItem(new Item.Properties().setId(PLUMECOW_SPAWN_EGG_KEY).spawnEgg(PLUMECOW))
	);

	public static final ResourceKey<EntityType<?>> PUFFSHEEP_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SkyIslandsMod.id("puffsheep"));

	public static final EntityType<PuffsheepEntity> PUFFSHEEP = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		PUFFSHEEP_KEY,
		EntityType.Builder.of(PuffsheepEntity::new, MobCategory.CREATURE)
			.sized(1.0F, 1.4F)
			.eyeHeight(1.2F)
			.clientTrackingRange(8)
			.build(PUFFSHEEP_KEY)
	);

	public static final ResourceKey<Item> PUFFSHEEP_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, SkyIslandsMod.id("puffsheep_spawn_egg"));

	public static final Item PUFFSHEEP_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		PUFFSHEEP_SPAWN_EGG_KEY,
		new SpawnEggItem(new Item.Properties().setId(PUFFSHEEP_SPAWN_EGG_KEY).spawnEgg(PUFFSHEEP))
	);

	public static final ResourceKey<EntityType<?>> BLUBIRD_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SkyIslandsMod.id("blubird"));

	public static final EntityType<BlubirdEntity> BLUBIRD = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		BLUBIRD_KEY,
		EntityType.Builder.of(BlubirdEntity::new, MobCategory.AMBIENT)
			.sized(0.5F, 0.5F)
			.eyeHeight(0.35F)
			.clientTrackingRange(8)
			.build(BLUBIRD_KEY)
	);

	public static final ResourceKey<Item> BLUBIRD_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, SkyIslandsMod.id("blubird_spawn_egg"));

	public static final Item BLUBIRD_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		BLUBIRD_SPAWN_EGG_KEY,
		new SpawnEggItem(new Item.Properties().setId(BLUBIRD_SPAWN_EGG_KEY).spawnEgg(BLUBIRD))
	);

	public static final ResourceKey<EntityType<?>> ANGEL_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SkyIslandsMod.id("angel"));

	public static final EntityType<AngelEntity> ANGEL = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		ANGEL_KEY,
		EntityType.Builder.of(AngelEntity::new, MobCategory.CREATURE)
			.sized(0.5F, 1.4F)
			.eyeHeight(1.25F)
			.clientTrackingRange(8)
			.build(ANGEL_KEY)
	);

	public static final ResourceKey<Item> ANGEL_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, SkyIslandsMod.id("angel_spawn_egg"));

	public static final Item ANGEL_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		ANGEL_SPAWN_EGG_KEY,
		new SpawnEggItem(new Item.Properties().setId(ANGEL_SPAWN_EGG_KEY).spawnEgg(ANGEL))
	);

	public static final ResourceKey<EntityType<?>> STORMCLOUD_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SkyIslandsMod.id("stormcloud"));

	/** The hitbox is only the core (0.6 blocks); the cloud around it is a damage zone, see StormcloudEntity. */
	public static final EntityType<StormcloudEntity> STORMCLOUD = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		STORMCLOUD_KEY,
		EntityType.Builder.of(StormcloudEntity::new, MobCategory.MONSTER)
			.sized(0.6F, 0.6F)
			.eyeHeight(0.3F)
			.clientTrackingRange(10)
			.build(STORMCLOUD_KEY)
	);

	public static final ResourceKey<Item> STORMCLOUD_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, SkyIslandsMod.id("stormcloud_spawn_egg"));

	public static final Item STORMCLOUD_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		STORMCLOUD_SPAWN_EGG_KEY,
		new SpawnEggItem(new Item.Properties().setId(STORMCLOUD_SPAWN_EGG_KEY).spawnEgg(STORMCLOUD))
	);

	private ModEntities() {
	}

	/** It spawns through the forest biome's spawn list (data/sky_islands/worldgen/biome/forest.json), not through code. */
	public static void register() {
		FabricDefaultAttributeRegistry.register(BLUB, BlubEntity.createAttributes());
		SpawnPlacements.register(BLUB, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlubEntity::checkSpawnRules);
		FabricDefaultAttributeRegistry.register(PLUMECOW, PlumecowEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(PUFFSHEEP, PuffsheepEntity.createAttributes());
		SpawnPlacements.register(PLUMECOW, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::spawnsOnGrass);
		SpawnPlacements.register(PUFFSHEEP, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::spawnsOnGrass);
		FabricDefaultAttributeRegistry.register(BLUBIRD, BlubirdEntity.createAttributes());
		SpawnPlacements.register(BLUBIRD, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING, BlubirdEntity::checkSpawnRules);
		FabricDefaultAttributeRegistry.register(ANGEL, AngelEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(STORMCLOUD, StormcloudEntity.createAttributes());
		SpawnPlacements.register(STORMCLOUD, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING, StormcloudEntity::checkSpawnRules);
		FabricDefaultAttributeRegistry.register(SKYSTALKER, SkystalkerEntity.createAttributes());
		// Any light: the dimension is bright all day, and the vanilla monster rule would wait for dark.
		SpawnPlacements.register(SKYSTALKER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkAnyLightMonsterSpawnRules);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> {
			entries.accept(BLUB_SPAWN_EGG);
			entries.accept(SKYSTALKER_SPAWN_EGG);
			entries.accept(PLUMECOW_SPAWN_EGG);
			entries.accept(PUFFSHEEP_SPAWN_EGG);
			entries.accept(BLUBIRD_SPAWN_EGG);
			entries.accept(ANGEL_SPAWN_EGG);
			entries.accept(STORMCLOUD_SPAWN_EGG);
		});
	}

	/** The grazers spawn on grass and dirt, in any light (the dimension is bright all day). */
	private static <T extends Mob> boolean spawnsOnGrass(EntityType<T> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON);
	}
}
