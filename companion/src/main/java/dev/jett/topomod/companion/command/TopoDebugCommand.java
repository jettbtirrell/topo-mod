package dev.jett.topomod.companion.command;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import dev.jett.topomod.companion.entity.TopoEntity;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// DEBUG: /topo idle <name> and /topo dance <name> play an animation on the nearest topo, for previewing.
// To remove: delete this file, its register() call in CompanionMod, and the debugPlay* methods in TopoEntity.
public final class TopoDebugCommand {
	private static final double SEARCH_RANGE = 32.0;

	private static final Map<String, Integer> IDLES = Map.of(
		"look_around", TopoEntity.IDLE_LOOK_AROUND,
		"groom", TopoEntity.IDLE_GROOM,
		"tail_chase", TopoEntity.IDLE_TAIL_CHASE,
		"shake", TopoEntity.IDLE_SHAKE,
		"hop", TopoEntity.IDLE_HOP);
	private static final Map<String, Integer> DANCES = Map.of(
		"spin", TopoEntity.DANCE_SPIN,
		"backflip", TopoEntity.DANCE_BACKFLIP,
		"moonwalk", TopoEntity.DANCE_MOONWALK);

	private TopoDebugCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(Commands.literal("topo")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(branch("idle", IDLES, false))
				.then(branch("dance", DANCES, true))));
	}

	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> branch(
			String name, Map<String, Integer> styles, boolean dance) {
		return Commands.literal(name).then(Commands.argument("name", StringArgumentType.word())
			.suggests((context, builder) -> SharedSuggestionProvider.suggest(styles.keySet(), builder))
			.executes(context -> play(context, styles, dance)));
	}

	private static int play(CommandContext<CommandSourceStack> context, Map<String, Integer> styles, boolean dance) {
		CommandSourceStack source = context.getSource();
		String name = StringArgumentType.getString(context, "name");
		Integer style = styles.get(name);
		if (style == null) {
			source.sendFailure(Component.literal("Unknown animation '" + name + "'. Options: " + String.join(", ", styles.keySet())));
			return 0;
		}

		Vec3 pos = source.getPosition();
		List<TopoEntity> topos = source.getLevel().getEntitiesOfClass(TopoEntity.class,
			AABB.ofSize(pos, SEARCH_RANGE * 2, SEARCH_RANGE * 2, SEARCH_RANGE * 2));
		TopoEntity topo = topos.stream().min(Comparator.comparingDouble(t -> t.distanceToSqr(pos))).orElse(null);
		if (topo == null) {
			source.sendFailure(Component.literal("No topo within " + (int) SEARCH_RANGE + " blocks."));
			return 0;
		}

		boolean started = dance ? topo.debugPlayDance(style) : topo.debugPlayIdle(style);
		if (!started) {
			source.sendFailure(Component.literal("That topo is already playing one; try again in a moment."));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("Playing " + name + "."), false);
		return 1;
	}
}
