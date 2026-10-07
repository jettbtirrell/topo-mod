package dev.jett.topomod.allayvariants.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.jett.topomod.allayvariants.AllayVariantsMod;
import dev.jett.topomod.allayvariants.registry.ModEntities;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// DEBUG: /bunnay hoptest. Builds a small stone arena in the air above the player, puts one bunnay in it (with its Brain switched
// off) and makes it hop to an exact landing spot, case after case, measuring where it ends up against where the planner aimed.
// A hop is deterministic, so the numbers are exact: where it is when the planned flight time is up ("arrive") should be on the
// landing spot, and where it ends when it touches the ground ("settle") close to it. To remove: delete this file, its register()
// call in AllayVariantsMod, the hoptest command in BunnayDebugCommand, and the hopTestMode / planHopTo / HopRun measuring
// bits in BunnayEntity.
public final class BunnayHopTest {
	/** How close, in blocks, counts as exact. */
	private static final double TOLERANCE = 0.10;
	/**
	 * It keeps 10% of its sideways speed when it lands (like the frog), which carries it on a little and then friction stops it:
	 * a few tenths of a block for the fastest hops. Where it comes to rest must be within this of the aim.
	 */
	private static final double REST_TOLERANCE = 0.30;
	private static final int ARENA_RADIUS = 7;
	private static final int ARENA_HEIGHT = 10;
	private static final int ARENA_LIFT = 24;
	private static final int SETTLE_TICKS = 12;
	private static final int FLIGHT_TIMEOUT = 80;
	/** After each hop it stays put for this long (half a second) so the landing can be seen before the next case is set up. */
	private static final int LINGER_TICKS = 10;

	/** A stone column in the arena, at a cell offset from where the bunnay starts, this many blocks above the floor. */
	private record Column(int dx, int dz, int height) {
	}

	/**
	 * One hop to make: from the start cell to the cell at (dx, dz), the start standing startHeight above the floor and the landing
	 * landHeight above it. Extra columns add walls and ledges. If running, it has walking speed when it launches.
	 */
	private record Case(String name, int dx, int dz, int startHeight, int landHeight, boolean running, List<Column> extra) {
		Case(String name, int dx, int dz, int startHeight, int landHeight) {
			this(name, dx, dz, startHeight, landHeight, false, List.of());
		}

		Case whileRunning() {
			return new Case(this.name + ", running", this.dx, this.dz, this.startHeight, this.landHeight, true, this.extra);
		}
	}

	private static final List<Case> CASES = List.of(
		new Case("flat 2", 2, 0, 0, 0),
		new Case("flat 4", 4, 0, 0, 0),
		new Case("flat 6", 6, 0, 0, 0),
		new Case("diagonal 3,3", 3, 3, 0, 0),
		new Case("diagonal 4,-3", 4, -3, 0, 0),
		new Case("up 1", 3, 0, 0, 1),
		new Case("up 2", 3, 0, 0, 2),
		new Case("up 3", 4, 0, 0, 3),
		new Case("down 1", 3, 0, 1, 0),
		new Case("down 2", 4, 0, 2, 0),
		new Case("flat 4", 4, 0, 0, 0).whileRunning(),
		new Case("up 2", 3, 0, 0, 2).whileRunning(),
		// The ledge it leaves runs right up to the landing: a drop beside the block it took off from.
		new Case("drop 1 past a ledge", 3, 0, 1, 0, false, List.of(new Column(1, 0, 1), new Column(2, 0, 1))),
		// A wall touching the landing block's far side, and one running along the path.
		new Case("flat 4, wall at landing", 4, 0, 0, 0, false, List.of(new Column(5, 0, 3))),
		new Case("flat 4, wall along path", 4, 0, 0, 0, false, List.of(new Column(2, 1, 3), new Column(3, 1, 3))),
		new Case("up 2, wall at landing", 3, 0, 0, 2, false, List.of(new Column(4, 0, 3)))
	);

	private static Run run;

	private BunnayHopTest() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (run != null) {
				run.tick();
			}
		});
	}

	/** Starts the test above the source's position; false (with a message sent) if it cannot. */
	public static boolean start(CommandSourceStack source) {
		if (run != null) {
			source.sendFailure(Component.literal("A hop test is already running (/bunnay hoptest stop cancels it)."));
			return false;
		}
		ServerLevel level = source.getLevel();
		BlockPos origin = BlockPos.containing(source.getPosition()).above(ARENA_LIFT);
		if (origin.getY() + ARENA_HEIGHT > level.getMaxY() || origin.getY() - 1 < level.getMinY()) {
			source.sendFailure(Component.literal("There is no room above you for the test arena."));
			return false;
		}
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-ARENA_RADIUS, -1, -ARENA_RADIUS), origin.offset(ARENA_RADIUS, ARENA_HEIGHT, ARENA_RADIUS))) {
			if (!level.getBlockState(pos).isAir()) {
				source.sendFailure(Component.literal("The space " + ARENA_LIFT + " blocks above you is not clear; try somewhere with open sky."));
				return false;
			}
		}
		Run started = new Run(source, level, origin);
		if (!started.begin()) {
			return false;
		}
		run = started;
		source.sendSuccess(() -> Component.literal("Hop test started: an arena " + ARENA_LIFT + " blocks above you, " + CASES.size()
			+ " hops, about a minute. Watch it from below or fly up."), false);
		return true;
	}

	public static void stop(CommandSourceStack source) {
		if (run == null) {
			source.sendFailure(Component.literal("No hop test is running."));
			return;
		}
		run.finish("Hop test cancelled.");
	}

	private static final class Run {
		private final CommandSourceStack source;
		private final ServerLevel level;
		private final BlockPos origin;
		private final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
		private final List<BlockPos> caseBlocks = new ArrayList<>();
		private final List<String> lines = new ArrayList<>();
		private BunnayEntity bunnay;
		private int caseIndex = -1;
		private int phase = 0; // 0 setting up, 1 settling, 2 flying, 3 lingering after a hop
		private int phaseTicks;
		private int exact;
		/** A hop's result is held while the bunnay rests, so that where it came to rest can go on the same line. */
		private String pendingResult;
		private Vec3 pendingLanding;
		private int rested;
		private int refused;

		Run(CommandSourceStack source, ServerLevel level, BlockPos origin) {
			this.source = source;
			this.level = level;
			this.origin = origin;
		}

		/** Builds the floor and puts the bunnay on it; false (with a message sent) if that fails. */
		boolean begin() {
			for (int dx = -ARENA_RADIUS; dx <= ARENA_RADIUS; dx++) {
				for (int dz = -ARENA_RADIUS; dz <= ARENA_RADIUS; dz++) {
					this.set(this.origin.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
				}
			}
			BunnayEntity entity = ModEntities.BUNNAY.create(this.level, EntitySpawnReason.COMMAND);
			if (entity == null) {
				this.saved.forEach((pos, state) -> this.level.setBlock(pos, state, 3));
				this.source.sendFailure(Component.literal("Could not create a bunnay for the test."));
				return false;
			}
			entity.hopTestMode = true;
			entity.setPersistenceRequired();
			entity.setSilent(true);
			this.bunnay = entity;
			entity.snapTo(this.origin.getX() + 0.5, this.origin.getY(), this.origin.getZ() + 0.5);
			this.level.addFreshEntity(entity);
			return true;
		}

		private void set(BlockPos pos, BlockState state) {
			this.saved.putIfAbsent(pos.immutable(), this.level.getBlockState(pos));
			this.level.setBlock(pos, state, 3);
		}

		private BlockPos cell(int dx, int dz, int heightAboveFloor) {
			return this.origin.offset(dx, heightAboveFloor, dz);
		}

		/** Where the bunnay stands for this cell and height: the middle of the cell, on top of the column. */
		private Vec3 stand(int dx, int dz, int height) {
			return new Vec3(this.origin.getX() + dx + 0.5, this.origin.getY() + height, this.origin.getZ() + dz + 0.5);
		}

		void tick() {
			if (this.bunnay == null || this.bunnay.isRemoved()) {
				this.finish("Hop test stopped: the test bunnay is gone.");
				return;
			}
			this.phaseTicks++;
			if (this.phase == 0) {
				this.nextCase();
			} else if (this.phase == 1) {
				this.settle();
			} else if (this.phase == 3) {
				if (this.phaseTicks >= LINGER_TICKS) {
					if (this.pendingResult != null) {
						double[] rest = error(this.bunnay.position(), this.pendingLanding);
						boolean restOk = Math.hypot(rest[0], rest[1]) <= REST_TOLERANCE;
						if (restOk) {
							this.rested++;
						}
						this.record(CASES.get(this.caseIndex), String.format("%s | rested %.2f across %+.2f up from the aim %s", this.pendingResult, rest[0], rest[1],
							restOk ? "OK" : "MISS"));
						this.pendingResult = null;
						this.phase = 0;
						this.phaseTicks = 0;
					} else {
						this.phase = 0;
						this.phaseTicks = 0;
					}
				}
			} else {
				this.fly();
			}
		}

		private void clearCaseBlocks() {
			for (BlockPos pos : this.caseBlocks) {
				this.set(pos, Blocks.AIR.defaultBlockState());
			}
			this.caseBlocks.clear();
		}

		private void column(int dx, int dz, int height) {
			for (int h = 0; h < height; h++) {
				BlockPos pos = this.cell(dx, dz, h);
				this.set(pos, Blocks.STONE.defaultBlockState());
				this.caseBlocks.add(pos);
			}
		}

		private void nextCase() {
			this.clearCaseBlocks();
			this.caseIndex++;
			if (this.caseIndex >= CASES.size()) {
				this.finish(null);
				return;
			}
			Case c = CASES.get(this.caseIndex);
			this.column(0, 0, c.startHeight());
			this.column(c.dx(), c.dz(), c.landHeight());
			for (Column extra : c.extra()) {
				this.column(extra.dx(), extra.dz(), extra.height());
			}
			Vec3 start = this.stand(0, 0, c.startHeight());
			this.bunnay.snapTo(start.x, start.y, start.z, 270.0F, 0.0F);
			this.bunnay.setDeltaMovement(Vec3.ZERO);
			this.bunnay.setSpeed(0.0F);
			this.bunnay.setZza(0.0F);
			this.phase = 1;
			this.phaseTicks = 0;
		}

		private void settle() {
			if (this.phaseTicks < SETTLE_TICKS) {
				return;
			}
			Case c = CASES.get(this.caseIndex);
			Vec3 landing = this.stand(c.dx(), c.dz(), c.landHeight());
			if (!this.bunnay.onGround()) {
				this.record(c, "could not stand at the start (not on the ground)");
				return;
			}
			BunnayEntity.Hop plan = this.bunnay.planHopTo(landing);
			if (plan == null) {
				this.refused++;
				this.record(c, "REFUSED by the planner: " + this.bunnay.lastSolveNote());
				return;
			}
			if (c.running()) {
				this.bunnay.setSpeed(0.45F);
				this.bunnay.setZza(0.45F);
			}
			this.bunnay.debugHopRun.start(plan);
			this.phase = 2;
			this.phaseTicks = 0;
		}

		private void fly() {
			BunnayEntity.HopRun hop = this.bunnay.debugHopRun;
			if (hop.running && this.phaseTicks < FLIGHT_TIMEOUT) {
				return;
			}
			Case c = CASES.get(this.caseIndex);
			Vec3 landing = this.stand(c.dx(), c.dz(), c.landHeight());
			BunnayEntity.Hop plan = hop.plan();
			if (hop.running) {
				this.record(c, "did not land within " + FLIGHT_TIMEOUT + " ticks");
				return;
			}
			double[] miss = error(hop.endPosition, landing);
			boolean onTime = hop.endTicks == plan.airTicks() + 1;
			boolean exact = Math.hypot(miss[0], miss[1]) <= TOLERANCE && onTime;
			if (exact) {
				this.exact++;
			}
			this.pendingResult = String.format("%2d ticks planned | ended off %.2f across %+.2f up after %d ticks%s | %s", plan.airTicks(), miss[0], miss[1],
				hop.endTicks, onTime ? "" : " (planned " + (plan.airTicks() + 1) + ")", exact ? "OK" : "MISS");
			this.pendingLanding = landing;
			this.phase = 3;
			this.phaseTicks = 0;
		}

		/** How far across (sideways) and how far up an actual position is from where it was aimed; large if it never got there. */
		private static double[] error(Vec3 actual, Vec3 aim) {
			if (actual == null) {
				return new double[] {99.0, 99.0};
			}
			return new double[] {Math.hypot(actual.x - aim.x, actual.z - aim.z), actual.y - aim.y};
		}

		private void record(Case c, String result) {
			String line = String.format("%2d. %-26s %s", this.caseIndex + 1, c.name(), result);
			this.lines.add(line);
			AllayVariantsMod.LOGGER.info("[hoptest] {}", line);
			this.source.sendSuccess(() -> Component.literal(line), false);
			this.phase = 3;
			this.phaseTicks = 0;
			this.pendingResult = null;
		}

		void finish(String problem) {
			if (this.bunnay != null) {
				this.bunnay.discard();
			}
			this.caseBlocks.clear();
			this.saved.forEach((pos, state) -> this.level.setBlock(pos, state, 3));
			run = null;
			String summary = problem != null ? problem : String.format("Hop test done: %d/%d landed within %.2f blocks of the aim on time, %d came to rest within %.2f, %d refused by the planner.",
				this.exact, CASES.size(), TOLERANCE, this.rested, REST_TOLERANCE, this.refused);
			AllayVariantsMod.LOGGER.info("[hoptest] {}", summary);
			this.source.sendSuccess(() -> Component.literal(summary), false);
		}
	}
}
