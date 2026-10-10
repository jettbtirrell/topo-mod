package dev.jett.topomod.allayvariants.registry;

import dev.jett.topomod.allayvariants.AllayVariantsMod;
import dev.jett.topomod.allayvariants.entity.BunnayEntity;
import dev.jett.topomod.allayvariants.entity.SpiritFoxEntity;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntities {
	public static final ResourceKey<EntityType<?>> BUNNAY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, AllayVariantsMod.id("bunnay"));

	public static final EntityType<BunnayEntity> BUNNAY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		BUNNAY_KEY,
		EntityType.Builder.of(BunnayEntity::new, MobCategory.CREATURE)
			.sized(0.5F, 0.5F)
			.eyeHeight(0.4F)
			.clientTrackingRange(8)
			.build(BUNNAY_KEY)
	);

	public static final ResourceKey<EntityType<?>> SPIRIT_FOX_KEY = ResourceKey.create(Registries.ENTITY_TYPE, AllayVariantsMod.id("spirit_fox"));

	/** The fox's size (0.6 by 0.7); see SpiritFoxEntity. */
	public static final EntityType<SpiritFoxEntity> SPIRIT_FOX = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		SPIRIT_FOX_KEY,
		EntityType.Builder.of(SpiritFoxEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 0.7F)
			.eyeHeight(0.4F)
			.clientTrackingRange(8)
			.build(SPIRIT_FOX_KEY)
	);

	/** How often a wild group turns up among the cherry grove's spawns (the fox's is 8 and its groups are 2 to 4). */
	private static final int SPAWN_WEIGHT = 5;

	private ModEntities() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(BUNNAY, BunnayEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SPIRIT_FOX, SpiritFoxEntity.createAttributes());
		SpawnPlacements.register(SPIRIT_FOX, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SpiritFoxEntity::checkSpawnRules);
		// Wild ones turn up at night in cherry groves (the night check is in SpiritFoxEntity.checkSpawnRules).
		BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.CHERRY_GROVE), MobCategory.CREATURE, SPIRIT_FOX, SPAWN_WEIGHT, 1, 2);
	}
}
