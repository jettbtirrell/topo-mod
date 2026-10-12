package dev.jett.topomod.skyislands.client.render;

import dev.jett.topomod.skyislands.SkyIslandsMod;
import dev.jett.topomod.skyislands.entity.BlubirdEntity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class BlubirdRenderer extends MobRenderer<BlubirdEntity, LivingEntityRenderState, BlubirdModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(SkyIslandsMod.id("blubird"), "main");
	private static final Identifier TEXTURE = SkyIslandsMod.id("textures/entity/blubird/blubird.png");

	public BlubirdRenderer(EntityRendererProvider.Context context) {
		super(context, new BlubirdModel(context.bakeLayer(LAYER)), 0.2F);
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return TEXTURE;
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}
}
