package dev.jett.topomod.companion.menu;

import dev.jett.topomod.companion.CompanionMod;
import dev.jett.topomod.companion.entity.BunnayEntity;
import dev.jett.topomod.companion.registry.ModMenus;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// The bunnay's equipment screen, laid out like the horse screen: two weapon slots (its main hand and off hand, which it
// swings in turn) and a food slot (the carrots it eats) on the left, player inventory below. Each empty slot shows an
// outline of what goes in it, like the armor slots in the player's inventory.
public class BunnayMenu extends AbstractContainerMenu {
	/** The outlines shown in an empty slot; the sprites are in this mod's gui sprites. */
	private static final Identifier ROD_ICON = CompanionMod.id("container/slot/rod");
	private static final Identifier CARROT_ICON = CompanionMod.id("container/slot/carrot");

	private static final int MAIN_WEAPON_SLOT = 0;
	private static final int OFF_WEAPON_SLOT = 1;
	private static final int FOOD_SLOT = 2;
	private static final int INVENTORY_START = 3;
	private static final int HOTBAR_START = INVENTORY_START + 27;
	private static final int SLOTS_END = HOTBAR_START + 9;

	private final BunnayEntity bunnay;
	private final Container mainHand;
	private final Container offHand;

	public BunnayMenu(int containerId, Inventory playerInventory, BunnayEntity bunnay) {
		super(ModMenus.BUNNAY, containerId);
		this.bunnay = bunnay;
		// Views onto the bunnay's hands, so changes in the menu are changes to the bunnay itself.
		this.mainHand = bunnay.createEquipmentSlotContainer(EquipmentSlot.MAINHAND);
		this.offHand = bunnay.createEquipmentSlotContainer(EquipmentSlot.OFFHAND);
		this.mainHand.startOpen(playerInventory.player);
		this.offHand.startOpen(playerInventory.player);

		this.addSlot(new WeaponSlot(this.mainHand, 0, 8, 18));
		this.addSlot(new WeaponSlot(this.offHand, 0, 8, 36));
		this.addSlot(new FoodSlot(bunnay.getFoodContainer(), 0, 8, 54));
		this.addStandardInventorySlots(playerInventory, 8, 84);
	}

	public BunnayEntity getBunnay() {
		return this.bunnay;
	}

	@Override
	public boolean stillValid(Player player) {
		return this.bunnay.isAlive() && this.mainHand.stillValid(player) && player.isWithinEntityInteractionRange(this.bunnay, 4.0);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.mainHand.stopOpen(player);
		this.offHand.stopOpen(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = this.slots.get(slotIndex);
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();

		if (slotIndex < INVENTORY_START) {
			// Out of the bunnay and into the player's inventory.
			if (!this.moveItemStackTo(stack, INVENTORY_START, SLOTS_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (this.slots.get(MAIN_WEAPON_SLOT).mayPlace(stack)) {
			// A weapon goes to the main hand, or the off hand if that is taken.
			if (!this.moveItemStackTo(stack, MAIN_WEAPON_SLOT, OFF_WEAPON_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (this.slots.get(FOOD_SLOT).mayPlace(stack)) {
			if (!this.moveItemStackTo(stack, FOOD_SLOT, FOOD_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (slotIndex < HOTBAR_START) {
			if (!this.moveItemStackTo(stack, HOTBAR_START, SLOTS_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false)) {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return original;
	}

	/** Holds one weapon (a bamboo, a rod, a stick or a bone), or a stack of arrows; nothing else fits. Shows a rod outline when empty. */
	private static final class WeaponSlot extends Slot {
		WeaponSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return BunnayEntity.isHoldable(stack);
		}

		// A weapon is one item, but a hand can hold a stack of arrows (each hit with one uses it up).
		@Override
		public int getMaxStackSize() {
			return 64;
		}

		@Override
		public int getMaxStackSize(ItemStack stack) {
			return BunnayEntity.isArrow(stack) ? stack.getMaxStackSize() : 1;
		}

		@Override
		public Identifier getNoItemIcon() {
			return ROD_ICON;
		}
	}

	/** Holds a stack of carrots or golden carrots (not mixed); nothing else fits. Shows a carrot outline when empty. */
	private static final class FoodSlot extends Slot {
		FoodSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return BunnayEntity.isCarrot(stack);
		}

		@Override
		public Identifier getNoItemIcon() {
			return CARROT_ICON;
		}
	}
}
