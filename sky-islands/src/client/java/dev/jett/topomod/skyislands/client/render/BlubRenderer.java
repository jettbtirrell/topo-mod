package dev.jett.topomod.skyislands.client.render;

import dev.jett.topomod.skyislands.SkyIslandsMod;
import dev.jett.topomod.skyislands.entity.BlubEntity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class BlubRenderer extends MobRenderer<BlubEntity, LivingEntityRenderState, BlubModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(SkyIslandsMod.id("blub"), "main");
	private static final Identifier TEXTURE = SkyIslandsMod.id("textures/entity/blub/blub.png");

	public BlubRenderer(EntityRendererProvider.Context context) {
		super(context, new BlubModel(context.bakeLayer(LAYER)), 0.35F);
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
