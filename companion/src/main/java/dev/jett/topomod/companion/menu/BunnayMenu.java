package dev.jett.topomod.companion.menu;

import dev.jett.topomod.companion.entity.BunnayEntity;
import dev.jett.topomod.companion.registry.ModMenus;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// The bunnay's equipment screen, laid out like the horse screen: a weapon slot (its main hand) and a food slot (its off
// hand, for the carrots it eats) on the left, player inventory below.
public class BunnayMenu extends AbstractContainerMenu {
	private static final int WEAPON_SLOT = 0;
	private static final int FOOD_SLOT = 1;
	private static final int INVENTORY_START = 2;
	private static final int HOTBAR_START = INVENTORY_START + 27;
	private static final int SLOTS_END = HOTBAR_START + 9;

	private final BunnayEntity bunnay;
	private final Container weapon;
	private final Container food;

	public BunnayMenu(int containerId, Inventory playerInventory, BunnayEntity bunnay) {
		super(ModMenus.BUNNAY, containerId);
		this.bunnay = bunnay;
		// Views onto the bunnay's hands, so changes in the menu are changes to the bunnay itself.
		this.weapon = bunnay.createEquipmentSlotContainer(EquipmentSlot.MAINHAND);
		this.food = bunnay.createEquipmentSlotContainer(EquipmentSlot.OFFHAND);
		this.weapon.startOpen(playerInventory.player);
		this.food.startOpen(playerInventory.player);

		this.addSlot(new WeaponSlot(this.weapon, 0, 8, 18));
		this.addSlot(new FoodSlot(this.food, 0, 8, 36));
		this.addStandardInventorySlots(playerInventory, 8, 84);
	}

	public BunnayEntity getBunnay() {
		return this.bunnay;
	}

	@Override
	public boolean stillValid(Player player) {
		return this.bunnay.isAlive() && this.weapon.stillValid(player) && player.isWithinEntityInteractionRange(this.bunnay, 4.0);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.weapon.stopOpen(player);
		this.food.stopOpen(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = this.slots.get(slotIndex);
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();

		if (slotIndex == WEAPON_SLOT || slotIndex == FOOD_SLOT) {
			if (!this.moveItemStackTo(stack, INVENTORY_START, SLOTS_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (this.slots.get(WEAPON_SLOT).mayPlace(stack) && !this.slots.get(WEAPON_SLOT).hasItem()) {
			if (!this.moveItemStackTo(stack, WEAPON_SLOT, WEAPON_SLOT + 1, false)) {
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

	/** Holds one weapon (for now a bamboo); nothing else fits. */
	private static final class WeaponSlot extends Slot {
		WeaponSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return BunnayEntity.isHoldable(stack);
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}
	}

	/** Holds a stack of carrots or golden carrots (not mixed); nothing else fits. */
	private static final class FoodSlot extends Slot {
		FoodSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return BunnayEntity.isCarrot(stack);
		}
	}
}
