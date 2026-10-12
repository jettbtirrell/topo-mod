package dev.jett.topomod.skyislands;

import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;

// The Olum tree: each block copies the matching oak block's behaviour, so only the looks and names differ.
public final class ModBlocks {
	public static final Block OLUM_LOG = register("olum_log", RotatedPillarBlock::new, Blocks.OAK_LOG);
	public static final Block OLUM_WOOD = register("olum_wood", RotatedPillarBlock::new, Blocks.OAK_WOOD);
	public static final Block OLUM_PLANKS = register("olum_planks", Block::new, Blocks.OAK_PLANKS);
	public static final Block OLUM_LEAVES = register(
		"olum_leaves", p -> new LeavesBlock(AmbientLeavesBlockSoundPlayer.noAmbientSound(), p), Blocks.BIRCH_LEAVES
	);

	// The dimension's own ground: plain blocks (no spreading or biome tint) so the textures show exactly as painted.
	public static final Block SKY_DIRT = register("sky_dirt", Block::new, Blocks.DIRT);
	public static final Block SKY_GRASS_BLOCK = register("sky_grass_block", Block::new, Blocks.GRASS_BLOCK);
	public static final Block SKY_STONE = register("sky_stone", Block::new, Blocks.STONE);

	private ModBlocks() {
	}

	/** Registers the block and its item; models, textures and loot are JSON under src/main/resources. */
	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, Block copyFrom) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, SkyIslandsMod.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(BlockBehaviour.Properties.ofFullCopy(copyFrom).setId(blockKey)));

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, SkyIslandsMod.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}

	public static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
			entries.accept(OLUM_LOG);
			entries.accept(OLUM_WOOD);
			entries.accept(OLUM_PLANKS);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
			entries.accept(OLUM_LOG);
			entries.accept(OLUM_LEAVES);
			entries.accept(SKY_GRASS_BLOCK);
			entries.accept(SKY_DIRT);
			entries.accept(SKY_STONE);
		});
	}
}
