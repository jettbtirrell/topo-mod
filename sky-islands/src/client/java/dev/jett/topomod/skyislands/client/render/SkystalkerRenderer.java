package dev.jett.topomod.skyislands.client.render;

import dev.jett.topomod.skyislands.SkyIslandsMod;
import dev.jett.topomod.skyislands.entity.SkystalkerEntity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class SkystalkerRenderer extends MobRenderer<SkystalkerEntity, LivingEntityRenderState, SkystalkerModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(SkyIslandsMod.id("skystalker"), "main");
	private static final Identifier TEXTURE = SkyIslandsMod.id("textures/entity/skystalker/skystalker.png");

	public SkystalkerRenderer(EntityRendererProvider.Context context) {
		super(context, new SkystalkerModel(context.bakeLayer(LAYER)), 0.7F);
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
