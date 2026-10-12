package dev.jett.topomod.skyislands.client.render;

import dev.jett.topomod.skyislands.SkyIslandsMod;
import dev.jett.topomod.skyislands.entity.StormcloudEntity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

public class StormcloudRenderer extends MobRenderer<StormcloudEntity, LivingEntityRenderState, StormcloudModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(SkyIslandsMod.id("stormcloud"), "main");
	private static final Identifier TEXTURE = SkyIslandsMod.id("textures/entity/stormcloud/stormcloud.png");

	public StormcloudRenderer(EntityRendererProvider.Context context) {
		super(context, new StormcloudModel(context.bakeLayer(LAYER)), 0.0F);
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return TEXTURE;
	}

	// Lit from within, like a charged core: it is drawn at full block light, so it glows in the shade of its own cloud.
	@Override
	protected int getBlockLightLevel(StormcloudEntity entity, BlockPos pos) {
		return 15;
	}

	/** The hitbox is only the core, but the cloud around it reaches much further: without this it would vanish as soon as the core left the screen. */
	@Override
	protected AABB getBoundingBoxForCulling(StormcloudEntity entity, float partialTicks) {
		return entity.getBoundingBox().inflate(StormcloudEntity.CLOUD_RADIUS);
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}
}
