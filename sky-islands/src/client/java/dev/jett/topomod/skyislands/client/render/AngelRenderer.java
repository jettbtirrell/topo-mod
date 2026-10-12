package dev.jett.topomod.skyislands.client.render;

import dev.jett.topomod.skyislands.SkyIslandsMod;
import dev.jett.topomod.skyislands.entity.AngelEntity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.resources.Identifier;

public class AngelRenderer extends MobRenderer<AngelEntity, ArmedEntityRenderState, AngelModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(SkyIslandsMod.id("angel"), "main");
	private static final Identifier TEXTURE = SkyIslandsMod.id("textures/entity/angel/angel.png");

	public AngelRenderer(EntityRendererProvider.Context context) {
		super(context, new AngelModel(context.bakeLayer(LAYER)), 0.3F);
		this.addLayer(new ItemInHandLayer<>(this));
	}

	@Override
	public Identifier getTextureLocation(ArmedEntityRenderState state) {
		return TEXTURE;
	}

	@Override
	public ArmedEntityRenderState createRenderState() {
		return new ArmedEntityRenderState();
	}

	@Override
	public void extractRenderState(AngelEntity entity, ArmedEntityRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, this.itemModelResolver, partialTicks);
	}
}
