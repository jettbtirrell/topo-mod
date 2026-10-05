package dev.jett.topomod.companion.registry;

import dev.jett.topomod.companion.CompanionMod;
import dev.jett.topomod.companion.entity.TopoEntity;
import dev.jett.topomod.companion.menu.TopoMenu;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
	// The client only needs to know which topo to attach the menu to, so we send its entity id.
	public static final MenuType<TopoMenu> TOPO = Registry.register(
		BuiltInRegistries.MENU,
		CompanionMod.id("topo"),
		new ExtendedMenuType<TopoMenu, Integer>((containerId, inventory, entityId) -> {
			Entity entity = inventory.player.level().getEntity(entityId);
			if (!(entity instanceof TopoEntity topo)) {
				throw new IllegalStateException("No topo with entity id " + entityId);
			}
			return new TopoMenu(containerId, inventory, topo);
		}, ByteBufCodecs.VAR_INT)
	);

	private ModMenus() {
	}

	public static void register() {
		// Touching this class registers the menu type.
	}
}
