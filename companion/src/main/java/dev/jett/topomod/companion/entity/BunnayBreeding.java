package dev.jett.topomod.companion.entity;

import com.mojang.serialization.Codec;

import dev.jett.topomod.companion.CompanionMod;
import dev.jett.topomod.companion.registry.ModEntities;
import dev.jett.topomod.companion.registry.ModItems;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;

// How a bunnay comes about: an allay dancing to the bunny music disc splits into a bunnay when it is given an amethyst
// shard, the way it splits into another allay for any other disc: the same hearts and chime, the new one on the allay,
// and the same wait between splits.
public final class BunnayBreeding {
	/** The game time before which an allay will not split into a bunnay again. */
	private static final AttachmentType<Long> NEXT_SPLIT = AttachmentRegistry.create(
		CompanionMod.id("next_bunnay_split"),
		builder -> builder.persistent(Codec.LONG)
	);

	/** Same wait as an allay has between splits, in ticks. */
	private static final int COOLDOWN_TICKS = 6000;

	private BunnayBreeding() {
	}

	public static void register() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (!(entity instanceof Allay allay) || !(level instanceof ServerLevel server) || !allay.isDancing()) {
				return InteractionResult.PASS;
			}
			ItemStack stack = player.getItemInHand(hand);
			if (!stack.is(ItemTags.DUPLICATES_ALLAYS) || !isHearingBunnayDisc(server, allay)) {
				return InteractionResult.PASS;
			}
			if (server.getGameTime() < allay.getAttachedOrElse(NEXT_SPLIT, 0L)) {
				// Too soon after the last split. Refuse, so the game does not split it into an allay instead.
				return InteractionResult.FAIL;
			}
			makeBunnay(server, player, stack, allay);
			return InteractionResult.SUCCESS;
		});
	}

	/**
	 * Whether a jukebox playing the bunny disc is close enough that the allay can hear it: the allay does not say which
	 * jukebox it is dancing to, so this looks for one within the distance at which it would have stopped dancing.
	 */
	private static boolean isHearingBunnayDisc(ServerLevel level, Allay allay) {
		int radius = GameEvent.JUKEBOX_PLAY.value().notificationRadius();
		BlockPos center = allay.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
			if (pos.closerToCenterThan(allay.position(), radius)
				&& level.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox
				&& jukebox.getTheItem().is(ModItems.MUSIC_DISC_BUNNY)
				&& jukebox.getSongPlayer().isPlaying()) {
				return true;
			}
		}
		return false;
	}

	private static void makeBunnay(ServerLevel level, Player player, ItemStack shard, Allay allay) {
		BunnayEntity bunnay = ModEntities.BUNNAY.create(level, EntitySpawnReason.BREEDING);
		if (bunnay == null) {
			return;
		}
		shard.consume(1, player);
		allay.setAttached(NEXT_SPLIT, level.getGameTime() + COOLDOWN_TICKS);

		bunnay.snapTo(allay.position());
		bunnay.setPersistenceRequired();
		level.addFreshEntity(bunnay);

		// Exactly what an allay does when it splits: three hearts on it and one amethyst chime.
		level.broadcastEntityEvent(allay, (byte) 18);
		level.playSound(null, allay, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 2.0F, 1.0F);
	}
}
