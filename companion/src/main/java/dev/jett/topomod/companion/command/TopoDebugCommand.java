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

// DEBUG: /topo bunnay_hop makes the nearest bunnay do its big hop right now, for previewing the animation.
// To remove: delete this file, its register() call in CompanionMod, and debugBigHop and forceBigHop in BunnayEntity.
public final class TopoDebugCommand {
	private static final double SEARCH_RANGE = 32.0;

	private TopoDebugCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(Commands.literal("topo")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("bunnay_hop").executes(TopoDebugCommand::bunnayHop))));
	}

	private static int bunnayHop(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		Vec3 pos = source.getPosition();
		List<BunnayEntity> bunnays = source.getLevel().getEntitiesOfClass(BunnayEntity.class,
			AABB.ofSize(pos, SEARCH_RANGE * 2, SEARCH_RANGE * 2, SEARCH_RANGE * 2));
		BunnayEntity bunnay = bunnays.stream().min(Comparator.comparingDouble(b -> b.distanceToSqr(pos))).orElse(null);
		if (bunnay == null) {
			source.sendFailure(Component.literal("No bunnay within " + (int) SEARCH_RANGE + " blocks."));
			return 0;
		}
		bunnay.debugBigHop();
		source.sendSuccess(() -> Component.literal("Bunnay will big hop."), false);
		return 1;
	}
}
