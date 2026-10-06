package dev.jett.topomod.companion.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.jett.topomod.companion.CompanionMod;
import dev.jett.topomod.companion.registry.ModEntities;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

// How a bunnay comes about: an allay near a rabbit now and then flies over and pets it. While it is petting, giving
// the allay an amethyst shard (the thing that makes allays split) makes a bunnay appear, with the same hearts and
// chime as an allay splitting, plus a burst of amethyst.
public final class BunnayBreeding {
	/**
	 * True on an allay while it is petting a rabbit. It is synced to the players who can see the allay, so the client
	 * can play the petting animation; it is not saved.
	 */
	public static final AttachmentType<Boolean> PETTING = AttachmentRegistry.create(
		CompanionMod.id("allay_petting"),
		builder -> builder.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all())
	);

	/** How often, in ticks, loaded allays near players are checked for starting to pet. */
	private static final int SCAN_INTERVAL = 40;
	/** How far from a player an allay is looked at, and how far from the allay a rabbit has to be to be petted, in blocks. */
	private static final double SCAN_RADIUS = 48.0;
	private static final double RABBIT_RANGE = 6.0;
	/** An allay with a rabbit in range starts petting with a 1-in-this chance each check (so within about 5 seconds). */
	private static final int START_ODDS = 4;
	/**
	 * A session has two parts. First the allay flies over to the rabbit (and gives up if it has not got there after
	 * APPROACH_TIMEOUT ticks). Only once it has arrived does the petting start: PET_TICKS long, a few ticks more than the
	 * petting animation (a clip 6 seconds, or 120 ticks, long that starts at that moment), so the clip finishes before the
	 * allay goes back to what it was doing.
	 */
	private static final int APPROACH_TIMEOUT = 160;
	private static final int PET_TICKS = 122;
	/** The wait before an allay pets again after it stops, in ticks (30 to 60 seconds). */
	private static final int COOLDOWN_MIN = 600;
	private static final int COOLDOWN_RANGE = 600;
	/**
	 * Where the allay stops to pet, relative to the rabbit. Its arms are only about a quarter of a block long, so it
	 * cannot pet from above: it sits low, to one side (the side it came from), and reaches over the rabbit's back.
	 * It has arrived when it is within ARRIVED_DISTANCE of that spot.
	 */
	private static final double HOVER_HEIGHT = 0.1;
	private static final double SIDE_OFFSET = 0.45;
	private static final double ARRIVED_DISTANCE = 0.6;
	/** The ticks into the petting at which each stroke of the clip presses down (0.95 seconds in, then every 0.9). */
	private static final int[] STROKE_TICKS = {19, 37, 55, 73, 91};
	/** Same cooldown a rabbit gets after breeding normally, in ticks. */
	private static final int RABBIT_COOLDOWN = 6000;

	/** One allay petting one rabbit. */
	private static final class Session {
		final Allay allay;
		final Rabbit rabbit;
		/** Which side of the rabbit the allay settles on, fixed when it sets off so it does not circle around. */
		final Vec3 side;
		/** False while it flies over, true once it has arrived and is petting. */
		boolean petting;
		/** Ticks spent in the current part. */
		int ticks;

		Session(Allay allay, Rabbit rabbit, Vec3 side) {
			this.allay = allay;
			this.rabbit = rabbit;
			this.side = side;
		}
	}

	/** Sessions by allay, and the game time before which an allay will not start another. */
	private static final Map<UUID, Session> SESSIONS = new HashMap<>();
	private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

	private BunnayBreeding() {
	}

	public static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			tickSessions(level);
			if (level.getGameTime() % SCAN_INTERVAL == 0) {
				scanForPetting(level);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			SESSIONS.clear();
			COOLDOWNS.clear();
		});

		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (!(entity instanceof Allay allay)) {
				return InteractionResult.PASS;
			}
			ItemStack stack = player.getItemInHand(hand);
			if (!stack.is(ItemTags.DUPLICATES_ALLAYS)) {
				return InteractionResult.PASS;
			}
			if (!(level instanceof ServerLevel server)) {
				// The client knows an allay is petting from the synced flag; claim the click so it doesn't also
				// guess that the allay takes the shard in its hand.
				return allay.getAttachedOrElse(PETTING, false) ? InteractionResult.SUCCESS : InteractionResult.PASS;
			}
			Session session = SESSIONS.get(allay.getUUID());
			if (session == null || !session.petting) {
				return InteractionResult.PASS;
			}
			makeBunnay(server, player, stack, session);
			return InteractionResult.SUCCESS;
		});
	}

	// ---- starting and running a petting session ----

	private static void scanForPetting(ServerLevel level) {
		RandomSource random = level.getRandom();
		for (ServerPlayer player : level.players()) {
			List<Allay> allays = level.getEntitiesOfClass(Allay.class, player.getBoundingBox().inflate(SCAN_RADIUS), a -> canStartPetting(level, a));
			for (Allay allay : allays) {
				List<Rabbit> rabbits = level.getEntitiesOfClass(Rabbit.class, allay.getBoundingBox().inflate(RABBIT_RANGE), BunnayBreeding::canBePetted);
				if (rabbits.isEmpty() || random.nextInt(START_ODDS) != 0) {
					continue;
				}
				Rabbit rabbit = rabbits.stream().min((a, b) -> Double.compare(a.distanceToSqr(allay), b.distanceToSqr(allay))).get();
				startPetting(level, allay, rabbit);
			}
		}
	}

	private static boolean canStartPetting(ServerLevel level, Allay allay) {
		return allay.isAlive()
			&& !allay.isDancing()
			&& allay.getMainHandItem().isEmpty()
			&& !SESSIONS.containsKey(allay.getUUID())
			&& COOLDOWNS.getOrDefault(allay.getUUID(), 0L) <= level.getGameTime();
	}

	/** An adult rabbit that is not on its breeding cooldown and is not already being petted. */
	private static boolean canBePetted(Rabbit rabbit) {
		if (!rabbit.isAlive() || rabbit.isBaby() || rabbit.getAge() != 0) {
			return false;
		}
		return SESSIONS.values().stream().noneMatch(session -> session.rabbit == rabbit);
	}

	private static void startPetting(ServerLevel level, Allay allay, Rabbit rabbit) {
		Vec3 away = allay.position().subtract(rabbit.position()).multiply(1.0, 0.0, 1.0);
		if (away.lengthSqr() < 1.0E-4) {
			away = new Vec3(1.0, 0.0, 0.0);
		}
		SESSIONS.put(allay.getUUID(), new Session(allay, rabbit, away.normalize().scale(SIDE_OFFSET)));
		// A happy chirp as it sets off; the petting animation starts when it gets there.
		level.playSound(null, allay.getX(), allay.getY(), allay.getZ(), SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundSource.NEUTRAL, 1.0F, 1.3F);
	}

	private static void tickSessions(ServerLevel level) {
		for (Session session : new ArrayList<>(SESSIONS.values())) {
			if (session.allay.level() != level) {
				continue;
			}
			Allay allay = session.allay;
			Rabbit rabbit = session.rabbit;
			boolean stillValid = allay.isAlive() && rabbit.isAlive() && !allay.isDancing() && allay.getMainHandItem().isEmpty()
				&& rabbit.level() == level && allay.distanceToSqr(rabbit) < 16.0 * 16.0;
			int limit = session.petting ? PET_TICKS : APPROACH_TIMEOUT;
			if (!stillValid || session.ticks >= limit) {
				endPetting(level, session);
				continue;
			}
			session.ticks++;

			// Fly to its spot beside the rabbit and look at the rabbit.
			Vec3 spot = rabbit.position().add(session.side).add(0.0, HOVER_HEIGHT, 0.0);
			allay.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot, 1.0F, 1));
			allay.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(rabbit, true));

			// The rabbit stays put for the whole visit: no wandering off, and a short slowness (refreshed every tick, so
			// it is gone moments after the visit, even if the world is saved in the middle of one) stops its hops.
			rabbit.getNavigation().stop();
			rabbit.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 5, 6, false, false));
			rabbit.getLookControl().setLookAt(allay);

			if (!session.petting) {
				if (allay.position().distanceToSqr(spot) < ARRIVED_DISTANCE * ARRIVED_DISTANCE) {
					// It has arrived: now the petting animation starts, on the client, from this tick.
					session.petting = true;
					session.ticks = 0;
					allay.setAttached(PETTING, true);
				}
				continue;
			}

			for (int stroke : STROKE_TICKS) {
				if (session.ticks == stroke) {
					// A soft chirp on each stroke. No hearts yet: those are for when it gets the amethyst.
					level.playSound(null, allay.getX(), allay.getY(), allay.getZ(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.NEUTRAL, 0.8F, 1.5F);
				}
			}
		}
	}

	private static void endPetting(ServerLevel level, Session session) {
		SESSIONS.remove(session.allay.getUUID());
		session.allay.removeAttached(PETTING);
		session.allay.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		session.allay.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		COOLDOWNS.put(session.allay.getUUID(), level.getGameTime() + COOLDOWN_MIN + level.getRandom().nextInt(COOLDOWN_RANGE));
	}

	// ---- the amethyst shard ----

	private static void makeBunnay(ServerLevel level, Player player, ItemStack shard, Session session) {
		Allay allay = session.allay;
		Rabbit rabbit = session.rabbit;
		BunnayEntity bunnay = ModEntities.BUNNAY.create(level, EntitySpawnReason.BREEDING);
		if (bunnay == null) {
			return;
		}
		shard.consume(1, player);
		endPetting(level, session);
		rabbit.setAge(RABBIT_COOLDOWN);

		bunnay.snapTo(rabbit.getX(), rabbit.getY(), rabbit.getZ(), rabbit.getYRot(), 0.0F);
		bunnay.setPersistenceRequired();
		level.addFreshEntity(bunnay);

		// What an allay does when it splits: three hearts on it and a loud amethyst chime. The rest is ours: a
		// second chime a fifth above, the ring of the amethyst blocks, and a burst of crystal where the bunnay appears.
		level.broadcastEntityEvent(allay, (byte) 18);
		level.playSound(null, allay.getX(), allay.getY(), allay.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 2.0F, 1.0F);
		level.playSound(null, allay.getX(), allay.getY(), allay.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 2.0F, 1.5F);
		level.playSound(null, bunnay.getX(), bunnay.getY(), bunnay.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 2.0F, 1.2F);
		level.playSound(null, bunnay.getX(), bunnay.getY(), bunnay.getZ(), SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.NEUTRAL, 2.0F, 1.2F);
		level.sendParticles(
			new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
			bunnay.getX(), bunnay.getY() + 0.5, bunnay.getZ(), 30, 0.35, 0.4, 0.35, 0.15
		);
		level.sendParticles(ParticleTypes.END_ROD, bunnay.getX(), bunnay.getY() + 0.5, bunnay.getZ(), 20, 0.3, 0.4, 0.3, 0.05);
		level.sendParticles(ParticleTypes.HEART, rabbit.getX(), rabbit.getY() + 0.6, rabbit.getZ(), 5, 0.4, 0.3, 0.4, 0.02);
		ExperienceOrb.award(level, rabbit.position(), rabbit.getRandom().nextInt(7) + 1);
	}
}
