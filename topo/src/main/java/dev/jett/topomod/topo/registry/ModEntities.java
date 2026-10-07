package dev.jett.topomod.topo.registry;

import dev.jett.topomod.topo.TopoMod;
import dev.jett.topomod.topo.entity.TopoEntity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final ResourceKey<EntityType<?>> TOPO_KEY = ResourceKey.create(Registries.ENTITY_TYPE, TopoMod.id("topo"));

	public static final EntityType<TopoEntity> TOPO = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		TOPO_KEY,
		EntityType.Builder.of(TopoEntity::new, MobCategory.CREATURE)
			.sized(0.5F, 0.85F)
			.eyeHeight(0.7F)
			.clientTrackingRange(8)
			.build(TOPO_KEY)
	);

	private ModEntities() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(TOPO, TopoEntity.createAttributes());
	}
}
