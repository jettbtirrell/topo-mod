package dev.jett.topomod.companion.command;

import java.util.Comparator;
import java.util.List;

import com.mojang.brigadier.context.CommandContext;

import dev.jett.topomod.companion.entity.BunnayEntity;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// DEBUG: /topo bunnay_hop makes the nearest bunnay do a normal hop, and /topo bunnay_quick_hop a flee-style one (almost no
// crouch, keeps its momentum), each away from where you are standing, so the two can be compared.
// To remove: delete this file, its register() call in CompanionMod, and debugHop in BunnayEntity.
public final class TopoDebugCommand {
	private static final double SEARCH_RANGE = 32.0;

	private TopoDebugCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(Commands.literal("topo")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("bunnay_hop").executes(context -> hop(context, false)))
				.then(Commands.literal("bunnay_quick_hop").executes(context -> hop(context, true)))));
	}

	private static int hop(CommandContext<CommandSourceStack> context, boolean quick) {
		CommandSourceStack source = context.getSource();
		Vec3 pos = source.getPosition();
		List<BunnayEntity> bunnays = source.getLevel().getEntitiesOfClass(BunnayEntity.class,
			AABB.ofSize(pos, SEARCH_RANGE * 2, SEARCH_RANGE * 2, SEARCH_RANGE * 2));
		BunnayEntity bunnay = bunnays.stream().min(Comparator.comparingDouble(b -> b.distanceToSqr(pos))).orElse(null);
		if (bunnay == null) {
			source.sendFailure(Component.literal("No bunnay within " + (int) SEARCH_RANGE + " blocks."));
			return 0;
		}
		if (!bunnay.debugHop(pos, quick)) {
			source.sendFailure(Component.literal("That bunnay cannot hop right now (it has to be standing on the ground, with somewhere to land)."));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("Bunnay will do a " + (quick ? "quick (flee)" : "normal") + " hop away from you."), false);
		return 1;
	}
}
