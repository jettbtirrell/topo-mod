package dev.jett.topomod.companion.client.screen;

import dev.jett.topomod.companion.menu.TopoMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// Reuses the horse screen's background (equipment on the left, a live preview, inventory below).
public class TopoScreen extends AbstractContainerScreen<TopoMenu> {
	private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/horse.png");
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

	private float xMouse;
	private float yMouse;

	public TopoScreen(TopoMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int left = (this.width - this.imageWidth) / 2;
		int top = (this.height - this.imageHeight) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, left, top, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
		// Frame for the torch slot (the horse texture only draws its own saddle and armor frames).
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, left + 7, top + 17, 18, 18);
		InventoryScreen.extractEntityInInventoryFollowsMouse(
			graphics, left + 26, top + 18, left + 78, top + 70, 30, 0.25F, this.xMouse, this.yMouse, this.menu.getTopo()
		);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		this.xMouse = mouseX;
		this.yMouse = mouseY;
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}
}
