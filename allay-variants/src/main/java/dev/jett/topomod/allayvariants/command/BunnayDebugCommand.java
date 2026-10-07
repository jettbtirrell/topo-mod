package dev.jett.topomod.allayvariants.command;

import java.util.Comparator;
import java.util.List;

import com.mojang.brigadier.context.CommandContext;

import dev.jett.topomod.allayvariants.entity.BunnayEntity;
import dev.jett.topomod.allayvariants.entity.BunnayHopTest;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// DEBUG: /bunnay hop makes the nearest bunnay hop away from where you are standing, to preview the hop.
// To remove: delete this file, its register() call in AllayVariantsMod, and debugHop in BunnayEntity.
public final class BunnayDebugCommand {
	private static final double SEARCH_RANGE = 32.0;

	private BunnayDebugCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(Commands.literal("bunnay")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("hop").executes(BunnayDebugCommand::hop))
				.then(Commands.literal("hoptest")
					.executes(context -> BunnayHopTest.start(context.getSource()) ? 1 : 0)
					.then(Commands.literal("stop").executes(context -> {
						BunnayHopTest.stop(context.getSource());
						return 1;
					})))));
	}

	private static int hop(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		Vec3 pos = source.getPosition();
		List<BunnayEntity> bunnays = source.getLevel().getEntitiesOfClass(BunnayEntity.class,
			AABB.ofSize(pos, SEARCH_RANGE * 2, SEARCH_RANGE * 2, SEARCH_RANGE * 2));
		BunnayEntity bunnay = bunnays.stream().min(Comparator.comparingDouble(b -> b.distanceToSqr(pos))).orElse(null);
		if (bunnay == null) {
			source.sendFailure(Component.literal("No bunnay within " + (int) SEARCH_RANGE + " blocks."));
			return 0;
		}
		if (!bunnay.debugHop(pos)) {
			source.sendFailure(Component.literal("That bunnay cannot hop right now (it has to be standing on the ground, with somewhere to land)."));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("Bunnay will hop away from you."), false);
		return 1;
	}
}
