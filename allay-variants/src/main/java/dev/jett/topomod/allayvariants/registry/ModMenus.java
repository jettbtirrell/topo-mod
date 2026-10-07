package dev.jett.topomod.allayvariants.registry;

import dev.jett.topomod.allayvariants.AllayVariantsMod;
import dev.jett.topomod.allayvariants.entity.BunnayEntity;
import dev.jett.topomod.allayvariants.menu.BunnayMenu;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
	// The client only needs to know which bunnay to attach the menu to, so we send its entity id.
	public static final MenuType<BunnayMenu> BUNNAY = Registry.register(
		BuiltInRegistries.MENU,
		AllayVariantsMod.id("bunnay"),
		new ExtendedMenuType<BunnayMenu, Integer>((containerId, inventory, entityId) -> {
			Entity entity = inventory.player.level().getEntity(entityId);
			if (!(entity instanceof BunnayEntity bunnay)) {
				throw new IllegalStateException("No bunnay with entity id " + entityId);
			}
			return new BunnayMenu(containerId, inventory, bunnay);
		}, ByteBufCodecs.VAR_INT)
	);

	private ModMenus() {
	}

	public static void register() {
		// Touching this class registers the menu type.
	}
}
