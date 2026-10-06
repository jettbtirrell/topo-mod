package dev.jett.topomod.companion.registry;

import dev.jett.topomod.companion.CompanionMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	/** The bunny music disc's song. The file is assets/topo_companion/sounds/music_disc/bunny.ogg (see sounds.json). */
	public static final SoundEvent MUSIC_DISC_BUNNY = register("music_disc.bunny");

	private ModSounds() {
	}

	private static SoundEvent register(String path) {
		Identifier id = CompanionMod.id(path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void register() {
	}
}
