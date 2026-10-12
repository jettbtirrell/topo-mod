package dev.jett.topomod.skyislands.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

// Stormcloud: the cube layout below is generated from sky-islands/art/stormcloud.bbmodel by `python3 tools/import_bbmodel.py stormcloud`
// (this model is not snapped to the ground, so the core stays in the middle of the hitbox);
// the animation under it relies on these part names, so keep them if the model is edited.
// It is drawn translucent: the cloud puffs have half-transparent pixels in the texture and the core is opaque, and the core is listed first
// so it is drawn before the puffs in front of it.
public class StormcloudModel extends EntityModel<LivingEntityRenderState> {
	/** How many small cubes make up the cloud: the bones puff_0 to puff_49 in the model. */
	private static final int PUFF_COUNT = 50;

	private final ModelPart core;
	private final ModelPart cloud;
	private final ModelPart[] puffs = new ModelPart[PUFF_COUNT];

	public StormcloudModel(ModelPart root) {
		super(root, RenderTypes::entityTranslucent);
		this.core = root.getChild("core");
		this.cloud = root.getChild("cloud");
		for (int i = 0; i < PUFF_COUNT; i++) {
			this.puffs[i] = this.cloud.getChild("puff_" + i);
		}
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// BEGIN GENERATED
		root.addOrReplaceChild("core",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(0.0F, 19.25F, 0.0F));
		PartDefinition cloud = root.addOrReplaceChild("cloud",
			CubeListBuilder.create(),
			PartPose.offset(0.0F, 19.25F, 0.0F));
		cloud.addOrReplaceChild("puff_0",
			CubeListBuilder.create()
				.mirror(false).texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(7.0F, -13.0F, 3.0F));
		cloud.addOrReplaceChild("puff_1",
			CubeListBuilder.create()
				.mirror(false).texOffs(64, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(-8.5F, -10.0F, 3.5F));
		cloud.addOrReplaceChild("puff_2",
			CubeListBuilder.create()
				.mirror(false).texOffs(96, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(6.5F, -3.5F, -12.0F));
		cloud.addOrReplaceChild("puff_3",
			CubeListBuilder.create()
				.mirror(false).texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(4.5F, 0.0F, 12.0F));
		cloud.addOrReplaceChild("puff_4",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 16).addBox(-5.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F),
			PartPose.offset(-12.5F, 3.0F, -5.0F));
		cloud.addOrReplaceChild("puff_5",
			CubeListBuilder.create()
				.mirror(false).texOffs(40, 16).addBox(-5.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F),
			PartPose.offset(11.0F, 4.0F, -5.0F));
		cloud.addOrReplaceChild("puff_6",
			CubeListBuilder.create()
				.mirror(false).texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(-7.5F, 10.5F, 10.5F));
		cloud.addOrReplaceChild("puff_7",
			CubeListBuilder.create()
				.mirror(false).texOffs(64, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(0.0F, 10.5F, -8.0F));
		cloud.addOrReplaceChild("puff_8",
			CubeListBuilder.create()
				.mirror(false).texOffs(80, 16).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
			PartPose.offset(3.0F, -23.5F, 7.5F));
		cloud.addOrReplaceChild("puff_9",
			CubeListBuilder.create()
				.mirror(false).texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(-14.0F, -20.5F, -6.0F));
		cloud.addOrReplaceChild("puff_10",
			CubeListBuilder.create()
				.mirror(false).texOffs(104, 16).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
			PartPose.offset(14.5F, -15.5F, -7.5F));
		cloud.addOrReplaceChild("puff_11",
			CubeListBuilder.create()
				.mirror(false).texOffs(80, 16).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
			PartPose.offset(-9.0F, -12.5F, 18.5F));
		cloud.addOrReplaceChild("puff_12",
			CubeListBuilder.create()
				.mirror(false).texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(-5.5F, -11.0F, -21.5F));
		cloud.addOrReplaceChild("puff_13",
			CubeListBuilder.create()
				.mirror(false).texOffs(64, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(20.0F, -7.0F, 10.5F));
		cloud.addOrReplaceChild("puff_14",
			CubeListBuilder.create()
				.mirror(false).texOffs(80, 16).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
			PartPose.offset(-22.0F, -3.0F, 3.5F));
		cloud.addOrReplaceChild("puff_15",
			CubeListBuilder.create()
				.mirror(false).texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(15.0F, -1.0F, -19.0F));
		cloud.addOrReplaceChild("puff_16",
			CubeListBuilder.create()
				.mirror(false).texOffs(104, 16).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
			PartPose.offset(2.0F, 1.0F, 21.0F));
		cloud.addOrReplaceChild("puff_17",
			CubeListBuilder.create()
				.mirror(false).texOffs(80, 16).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
			PartPose.offset(-16.5F, 4.0F, -13.0F));
		cloud.addOrReplaceChild("puff_18",
			CubeListBuilder.create()
				.mirror(false).texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(23.0F, 5.5F, 1.0F));
		cloud.addOrReplaceChild("puff_19",
			CubeListBuilder.create()
				.mirror(false).texOffs(64, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(-14.5F, 8.5F, 13.5F));
		cloud.addOrReplaceChild("puff_20",
			CubeListBuilder.create()
				.mirror(false).texOffs(96, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(-0.5F, 12.0F, -18.0F));
		cloud.addOrReplaceChild("puff_21",
			CubeListBuilder.create()
				.mirror(false).texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(8.5F, 14.0F, 11.5F));
		cloud.addOrReplaceChild("puff_22",
			CubeListBuilder.create()
				.mirror(false).texOffs(64, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(-13.0F, 19.0F, -3.5F));
		cloud.addOrReplaceChild("puff_23",
			CubeListBuilder.create()
				.mirror(false).texOffs(96, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(8.5F, 21.5F, -5.5F));
		cloud.addOrReplaceChild("puff_24",
			CubeListBuilder.create()
				.mirror(false).texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(-4.5F, -31.0F, 8.0F));
		cloud.addOrReplaceChild("puff_25",
			CubeListBuilder.create()
				.mirror(false).texOffs(64, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(-2.5F, -26.0F, -15.5F));
		cloud.addOrReplaceChild("puff_26",
			CubeListBuilder.create()
				.mirror(false).texOffs(96, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(14.0F, -26.0F, 8.5F));
		cloud.addOrReplaceChild("puff_27",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 36).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
			PartPose.offset(-21.0F, -22.5F, 3.0F));
		cloud.addOrReplaceChild("puff_28",
			CubeListBuilder.create()
				.mirror(false).texOffs(104, 16).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
			PartPose.offset(14.5F, -19.5F, -20.5F));
		cloud.addOrReplaceChild("puff_29",
			CubeListBuilder.create()
				.mirror(false).texOffs(96, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(4.0F, -19.0F, 23.5F));
		cloud.addOrReplaceChild("puff_30",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 36).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
			PartPose.offset(-21.0F, -13.5F, -17.5F));
		cloud.addOrReplaceChild("puff_31",
			CubeListBuilder.create()
				.mirror(false).texOffs(64, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(28.0F, -15.0F, 0.0F));
		cloud.addOrReplaceChild("puff_32",
			CubeListBuilder.create()
				.mirror(false).texOffs(96, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(-18.5F, -11.5F, 20.0F));
		cloud.addOrReplaceChild("puff_33",
			CubeListBuilder.create()
				.mirror(false).texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(0.5F, -8.0F, -31.0F));
		cloud.addOrReplaceChild("puff_34",
			CubeListBuilder.create()
				.mirror(false).texOffs(64, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(21.0F, -6.0F, 22.0F));
		cloud.addOrReplaceChild("puff_35",
			CubeListBuilder.create()
				.mirror(false).texOffs(96, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(-29.0F, -2.5F, -4.0F));
		cloud.addOrReplaceChild("puff_36",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 36).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
			PartPose.offset(23.5F, -2.5F, -18.5F));
		cloud.addOrReplaceChild("puff_37",
			CubeListBuilder.create()
				.mirror(false).texOffs(64, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(-5.5F, 1.5F, 31.0F));
		cloud.addOrReplaceChild("puff_38",
			CubeListBuilder.create()
				.mirror(false).texOffs(96, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(-14.0F, 4.5F, -24.5F));
		cloud.addOrReplaceChild("puff_39",
			CubeListBuilder.create()
				.mirror(false).texOffs(0, 36).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
			PartPose.offset(31.0F, 6.5F, 7.5F));
		cloud.addOrReplaceChild("puff_40",
			CubeListBuilder.create()
				.mirror(false).texOffs(64, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(-27.0F, 7.5F, 12.0F));
		cloud.addOrReplaceChild("puff_41",
			CubeListBuilder.create()
				.mirror(false).texOffs(96, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(11.0F, 9.5F, -26.5F));
		cloud.addOrReplaceChild("puff_42",
			CubeListBuilder.create()
				.mirror(false).texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(9.0F, 12.5F, 27.5F));
		cloud.addOrReplaceChild("puff_43",
			CubeListBuilder.create()
				.mirror(false).texOffs(64, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(-24.5F, 14.0F, -11.0F));
		cloud.addOrReplaceChild("puff_44",
			CubeListBuilder.create()
				.mirror(false).texOffs(80, 16).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
			PartPose.offset(26.5F, 16.0F, -6.5F));
		cloud.addOrReplaceChild("puff_45",
			CubeListBuilder.create()
				.mirror(false).texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(-13.5F, 21.0F, 21.5F));
		cloud.addOrReplaceChild("puff_46",
			CubeListBuilder.create()
				.mirror(false).texOffs(104, 16).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
			PartPose.offset(-3.0F, 22.0F, -20.0F));
		cloud.addOrReplaceChild("puff_47",
			CubeListBuilder.create()
				.mirror(false).texOffs(96, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(15.0F, 23.5F, 11.0F));
		cloud.addOrReplaceChild("puff_48",
			CubeListBuilder.create()
				.mirror(false).texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(-15.0F, 27.0F, 3.0F));
		cloud.addOrReplaceChild("puff_49",
			CubeListBuilder.create()
				.mirror(false).texOffs(64, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(6.5F, 31.5F, -7.5F));
		// END GENERATED

		return LayerDefinition.create(mesh, 128, 128);
	}

	/** How far each cube wanders from its place, in pixels. */
	private static final float DRIFT = 2.2F;

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state);

		// The core turns steadily; the whole cloud swirls slowly the other way.
		this.core.yRot = state.ageInTicks * 0.08F;
		this.core.xRot = Mth.sin(state.ageInTicks * 0.05F) * 0.3F;
		this.cloud.yRot = -state.ageInTicks * 0.015F;

		// Every cube drifts about its place, turns on the spot and swells and shrinks a little (all the same in each direction, so each
		// stays a cube), each out of step with the others so the ball seems to boil.
		float t = state.ageInTicks;
		for (int i = 0; i < PUFF_COUNT; i++) {
			ModelPart puff = this.puffs[i];
			float phase = i * 2.4F;
			puff.x += Mth.sin(t * 0.06F + phase) * DRIFT;
			puff.y += Mth.cos(t * 0.05F + phase * 1.3F) * DRIFT;
			puff.z += Mth.sin(t * 0.055F + phase * 0.7F) * DRIFT;
			puff.yRot = t * 0.03F * (i % 2 == 0 ? 1.0F : -1.0F) + phase;
			puff.xRot = Mth.sin(t * 0.04F + phase) * 0.4F;
			float swell = 1.0F + Mth.sin(t * 0.08F + phase) * 0.12F;
			puff.xScale = swell;
			puff.yScale = swell;
			puff.zScale = swell;
		}
	}
}
