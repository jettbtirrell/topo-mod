package dev.jett.topomod.allayvariants.registry;

import dev.jett.topomod.allayvariants.AllayVariantsMod;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.network.codec.ByteBufCodecs;

public final class ModAttachments {
	/**
	 * The game time until which a mob is on soul fire. A mob's effects are only sent to the client for a player (not for other mobs), so
	 * the client could not tell which mobs to draw flames on; this is sent to everyone who can see the mob instead (see SoulFire).
	 */
	public static final AttachmentType<Long> SOUL_FIRE_UNTIL = AttachmentRegistry.create(
		AllayVariantsMod.id("soul_fire_until"),
		builder -> builder.syncWith(ByteBufCodecs.VAR_LONG, AttachmentSyncPredicate.all())
	);

	private ModAttachments() {
	}

	public static void register() {
		// Touching the class registers the attachment.
	}
}
