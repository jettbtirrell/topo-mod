package dev.jett.topomod.companion.menu;

import dev.jett.topomod.companion.entity.TopoEntity;
import dev.jett.topomod.companion.registry.ModMenus;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// The topo's equipment screen, laid out like the horse screen: equipment on the left, player inventory below.
public class TopoMenu extends AbstractContainerMenu {
	private static final int TORCH_SLOT = 0;
	private static final int INVENTORY_START = 1;
	private static final int HOTBAR_START = INVENTORY_START + 27;
	private static final int SLOTS_END = HOTBAR_START + 9;

	private final TopoEntity topo;
	private final Container equipment;

	public TopoMenu(int containerId, Inventory playerInventory, TopoEntity topo) {
		super(ModMenus.TOPO, containerId);
		this.topo = topo;
		// A view onto the topo's main hand, so changes in the menu are changes to the topo itself.
		this.equipment = topo.createEquipmentSlotContainer(EquipmentSlot.MAINHAND);
		this.equipment.startOpen(playerInventory.player);

		this.addSlot(new TorchSlot(this.equipment, 0, 8, 18));
		this.addStandardInventorySlots(playerInventory, 8, 84);
	}

	public TopoEntity getTopo() {
		return this.topo;
	}

	@Override
	public boolean stillValid(Player player) {
		return this.topo.isAlive() && this.equipment.stillValid(player) && player.isWithinEntityInteractionRange(this.topo, 4.0);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.equipment.stopOpen(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = this.slots.get(slotIndex);
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();

		if (slotIndex == TORCH_SLOT) {
			if (!this.moveItemStackTo(stack, INVENTORY_START, SLOTS_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (this.slots.get(TORCH_SLOT).mayPlace(stack) && !this.slots.get(TORCH_SLOT).hasItem()) {
			if (!this.moveItemStackTo(stack, TORCH_SLOT, TORCH_SLOT + 1, false)) {
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

	/** Holds one torch; nothing else fits. */
	private static final class TorchSlot extends Slot {
		TorchSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return TopoEntity.isTorch(stack);
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}
	}
}
