package dev.jett.topomod.allayvariants.command;

import com.mojang.brigadier.context.CommandContext;

import dev.jett.topomod.allayvariants.entity.SpiritFoxEntity;
import dev.jett.topomod.allayvariants.registry.ModEntities;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;

// DEBUG-TEMP: a spirit fox is only ever tamed by breeding, so this is the way to get a tame one to test with:
// /spiritfox tame        a tame adult, owned by you, where you stand
// /spiritfox tame baby   a tame baby
// To remove: delete this file and its register() call in AllayVariantsMod.
public final class SpiritFoxCommand {
	private SpiritFoxCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(Commands.literal("spiritfox")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("tame")
					.executes(context -> spawnTame(context, false))
					.then(Commands.literal("baby").executes(context -> spawnTame(context, true))))));
	}

	private static int spawnTame(CommandContext<CommandSourceStack> context, boolean baby) {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("Only a player can own a spirit fox."));
			return 0;
		}
		ServerLevel level = source.getLevel();
		SpiritFoxEntity fox = ModEntities.SPIRIT_FOX.create(level, EntitySpawnReason.COMMAND);
		if (fox == null) {
			source.sendFailure(Component.literal("Could not create a spirit fox."));
			return 0;
		}
		fox.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
		fox.tame(player);
		if (baby) {
			fox.setAge(-24000);
		}
		level.addFreshEntity(fox);
		source.sendSuccess(() -> Component.literal("Spawned a tame " + (baby ? "baby " : "") + "spirit fox for you."), false);
		return 1;
	}
}
