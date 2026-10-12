package dev.jett.topomod.skyislands.worldgen;

import com.mojang.serialization.MapCodec;

import dev.jett.topomod.skyislands.ModBlocks;
import dev.jett.topomod.skyislands.ModEntities;
import dev.jett.topomod.skyislands.entity.AngelEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A small village of angels, built by code: a quartz plaza with a shrine in the middle and four cottages around it, on a patch of
 * grass levelled out of the land. Its whole footprint is within 13 blocks of where it is placed, so it stays inside the chunks
 * the game lets a feature touch. Angels are spawned in at the same time and kept (they never despawn).
 */
public record AngelVillageFeature() implements Feature {
	public static final MapCodec<AngelVillageFeature> CODEC = MapCodec.unit(AngelVillageFeature::new);

	/** How far out the village reaches from its centre, in blocks. */
	private static final int RADIUS = 13;
	/** The land under it may differ this much in height, or the village is not built. */
	private static final int MAX_SLOPE = 5;
	/** Centre of each cottage, as a distance from the plaza centre along one axis. */
	private static final int HOUSE_DISTANCE = 9;

	private static final BlockState GRASS = ModBlocks.SKY_GRASS_BLOCK.defaultBlockState();
	private static final BlockState STONE = ModBlocks.SKY_STONE.defaultBlockState();
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();
	private static final BlockState BRICKS = Blocks.QUARTZ_BRICKS.defaultBlockState();
	private static final BlockState WALL = Blocks.QUARTZ_BLOCK.defaultBlockState();
	private static final BlockState PILLAR = Blocks.QUARTZ_PILLAR.defaultBlockState();
	private static final BlockState ROOF = Blocks.SMOOTH_QUARTZ.defaultBlockState();
	private static final BlockState CHISELED = Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
	private static final BlockState GOLD = Blocks.GOLD_BLOCK.defaultBlockState();
	private static final BlockState LANTERN = Blocks.SEA_LANTERN.defaultBlockState();
	private static final BlockState GLASS = Blocks.GLASS.defaultBlockState();
	private static final BlockState CARPET = Blocks.CARPET.pick(DyeColor.WHITE).defaultBlockState();

	@Override
	public MapCodec<AngelVillageFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		// Is the land here flat enough, and dry? Sample the edge and the middle.
		int lowest = Integer.MAX_VALUE;
		int highest = Integer.MIN_VALUE;
		long total = 0;
		int samples = 0;
		for (int dx = -RADIUS; dx <= RADIUS; dx += RADIUS) {
			for (int dz = -RADIUS; dz <= RADIUS; dz += RADIUS) {
				int x = origin.getX() + dx;
				int z = origin.getZ() + dz;
				int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
				if (!level.getBlockState(new BlockPos(x, top - 1, z)).getFluidState().isEmpty()) {
					return false;
				}
				lowest = Math.min(lowest, top);
				highest = Math.max(highest, top);
				total += top;
				samples++;
			}
		}
		if (highest - lowest > MAX_SLOPE) {
			return false;
		}
		int floorY = Math.round((float)total / samples) - 1; // the y of the ground the village stands on
		BlockPos centre = new BlockPos(origin.getX(), floorY, origin.getZ());

		levelGround(level, centre);
		buildPlaza(level, centre);
		int[][] outward = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
		for (int[] out : outward) {
			buildCottage(level, random, centre, out[0], out[1]);
		}
		spawnAngel(level, random, centre.offset(2, 1, 2));
		spawnAngel(level, random, centre.offset(-2, 1, -2));
		spawnAngel(level, random, centre.offset(2, 1, -2));
		return true;
	}

	/** Cuts the hills and fills the dips so the village sits on one level of grass, a disc of land around the centre. */
	private void levelGround(WorldGenLevel level, BlockPos centre) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dz = -RADIUS; dz <= RADIUS; dz++) {
				if (dx * dx + dz * dz > RADIUS * RADIUS) {
					continue;
				}
				for (int dy = 1; dy <= 10; dy++) {
					pos.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
					if (!level.getBlockState(pos).isAir()) {
						level.setBlock(pos, AIR, 2);
					}
				}
				pos.set(centre.getX() + dx, centre.getY(), centre.getZ() + dz);
				level.setBlock(pos, GRASS, 2);
				// Fill downward through air and water until it meets solid ground.
				for (int dy = 1; dy <= 10; dy++) {
					pos.set(centre.getX() + dx, centre.getY() - dy, centre.getZ() + dz);
					BlockState below = level.getBlockState(pos);
					if (!below.isAir() && below.getFluidState().isEmpty()) {
						break;
					}
					level.setBlock(pos, dy == 1 ? ModBlocks.SKY_DIRT.defaultBlockState() : STONE, 2);
				}
			}
		}
	}

	/** A 9 by 9 floor of quartz bricks, with a shrine in the middle: a gold and chiselled base, a pillar and a glowing top. */
	private void buildPlaza(WorldGenLevel level, BlockPos centre) {
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				level.setBlock(centre.offset(dx, 0, dz), BRICKS, 2);
			}
		}
		// The paths out to the cottages, a little wider than the doors.
		for (int along = 5; along <= 6; along++) {
			for (int across = -1; across <= 1; across++) {
				level.setBlock(centre.offset(across, 0, along), BRICKS, 2);
				level.setBlock(centre.offset(across, 0, -along), BRICKS, 2);
				level.setBlock(centre.offset(along, 0, across), BRICKS, 2);
				level.setBlock(centre.offset(-along, 0, across), BRICKS, 2);
			}
		}
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				level.setBlock(centre.offset(dx, 0, dz), (dx == 0 && dz == 0) ? GOLD : CHISELED, 2);
			}
		}
		for (int dy = 1; dy <= 4; dy++) {
			level.setBlock(centre.offset(0, dy, 0), PILLAR, 2);
		}
		level.setBlock(centre.offset(0, 5, 0), LANTERN, 2);
	}

	/**
	 * A 5 by 5 cottage with its door on the side toward the plaza. (outX, outZ) is the direction from the plaza to the cottage;
	 * local u is across the cottage and v is outward, both measured from the cottage's centre.
	 */
	private void buildCottage(WorldGenLevel level, RandomSource random, BlockPos centre, int outX, int outZ) {
		int rightX = -outZ;
		int rightZ = outX;
		int hx = centre.getX() + outX * HOUSE_DISTANCE;
		int hz = centre.getZ() + outZ * HOUSE_DISTANCE;
		int y = centre.getY();
		for (int u = -2; u <= 2; u++) {
			for (int v = -2; v <= 2; v++) {
				int x = hx + u * rightX + v * outX;
				int z = hz + u * rightZ + v * outZ;
				boolean wall = Math.abs(u) == 2 || Math.abs(v) == 2;
				boolean corner = Math.abs(u) == 2 && Math.abs(v) == 2;
				level.setBlock(new BlockPos(x, y, z), BRICKS, 2);
				for (int dy = 1; dy <= 3; dy++) {
					BlockState state = AIR;
					if (corner) {
						state = PILLAR;
					} else if (wall) {
						boolean door = u == 0 && v == -2 && dy <= 2;
						boolean window = dy == 2 && ((u == 0 && v == 2) || (v == 0 && Math.abs(u) == 2));
						state = door ? AIR : window ? GLASS : WALL;
					}
					level.setBlock(new BlockPos(x, y + dy, z), state, 2);
				}
				// Roof: a stepped pyramid with a gold point.
				int ring = Math.max(Math.abs(u), Math.abs(v));
				level.setBlock(new BlockPos(x, y + 4, z), ROOF, 2);
				if (ring <= 1) {
					level.setBlock(new BlockPos(x, y + 5, z), ROOF, 2);
				}
				if (ring == 0) {
					level.setBlock(new BlockPos(x, y + 6, z), GOLD, 2);
				}
				if (!wall) {
					level.setBlock(new BlockPos(x, y + 1, z), (u == 1 && v == 1) ? LANTERN : CARPET, 2);
				}
			}
		}
		// Two angels live in each cottage.
		spawnAngel(level, random, new BlockPos(hx - rightX, y + 1, hz - rightZ));
		spawnAngel(level, random, new BlockPos(hx + rightX, y + 1, hz + rightZ));
	}

	private void spawnAngel(WorldGenLevel level, RandomSource random, BlockPos pos) {
		AngelEntity angel = ModEntities.ANGEL.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (angel == null) {
			return;
		}
		angel.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
		angel.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
		angel.setPersistenceRequired();
		level.addFreshEntityWithPassengers(angel);
	}
}
