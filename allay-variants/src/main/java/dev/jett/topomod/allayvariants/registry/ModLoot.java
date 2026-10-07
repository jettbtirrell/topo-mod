package dev.jett.topomod.allayvariants.registry;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

// Adds the bunny music disc to the chests of abandoned camps, but only the ones in a dappled forest. Every kind of camp
// shares the same two chest loot tables, so the biome is checked where the chest is.
public final class ModLoot {
	/** Chance that a chest of each kind holds the disc. The secret chests are the rarer ones, so they hold it more often. */
	private static final float COMMON_CHEST_CHANCE = 0.10F;
	private static final float SECRET_CHEST_CHANCE = 0.30F;

	private ModLoot() {
	}

	public static void register() {
		LootTableEvents.MODIFY.register((key, table, source, registries) -> {
			// Only vanilla's own tables, not ones another mod or a data pack changed.
			if (!source.isBuiltin()) {
				return;
			}
			float chance = key.equals(BuiltInLootTables.ABANDONED_CAMP_COMMON_CHEST) ? COMMON_CHEST_CHANCE
				: key.equals(BuiltInLootTables.ABANDONED_CAMP_SECRET_CHEST) ? SECRET_CHEST_CHANCE : 0.0F;
			if (chance <= 0.0F) {
				return;
			}
			Holder<Biome> dappledForest = registries.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.DAPPLED_FOREST);
			table.withPool(LootPool.lootPool()
				.add(LootItem.lootTableItem(ModItems.MUSIC_DISC_BUNNY))
				.when(LocationCheck.checkLocation(LocationPredicate.Builder.inBiome(dappledForest)))
				.when(LootItemRandomChanceCondition.randomChance(chance)));
		});
	}
}
