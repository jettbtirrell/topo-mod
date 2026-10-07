package dev.jett.topomod.allayvariants.registry;

import dev.jett.topomod.allayvariants.AllayVariantsMod;
import dev.jett.topomod.allayvariants.entity.BunnayEntity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final ResourceKey<EntityType<?>> BUNNAY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, AllayVariantsMod.id("bunnay"));

	public static final EntityType<BunnayEntity> BUNNAY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		BUNNAY_KEY,
		EntityType.Builder.of(BunnayEntity::new, MobCategory.CREATURE)
			.sized(0.5F, 0.875F)
			.eyeHeight(0.7F)
			.clientTrackingRange(8)
			.build(BUNNAY_KEY)
	);

	private ModEntities() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(BUNNAY, BunnayEntity.createAttributes());
	}
}
