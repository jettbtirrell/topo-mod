package dev.jett.topomod.skyislands.client.render;

import dev.jett.topomod.skyislands.SkyIslandsMod;
import dev.jett.topomod.skyislands.entity.PuffsheepEntity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class PuffsheepRenderer extends MobRenderer<PuffsheepEntity, LivingEntityRenderState, PuffsheepModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(SkyIslandsMod.id("puffsheep"), "main");
	private static final Identifier TEXTURE = SkyIslandsMod.id("textures/entity/puffsheep/puffsheep.png");

	public PuffsheepRenderer(EntityRendererProvider.Context context) {
		super(context, new PuffsheepModel(context.bakeLayer(LAYER)), 0.5F);
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
