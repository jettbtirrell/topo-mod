package dev.jett.topomod.companion.entity;

import java.util.List;

import dev.jett.topomod.companion.registry.ModEntities;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.item.ItemStack;

// A bunny and an allay can't breed the normal way (the allay isn't a breedable animal), so this is a special case:
// feeding a rabbit its favorite food with an allay close by produces a bunnay.
public final class BunnayBreeding {
	/** How close an allay has to be to the rabbit, in blocks. */
	private static final double ALLAY_RANGE = 8.0;
	/** Same cooldown a rabbit gets after breeding normally, in ticks. */
	private static final int RABBIT_COOLDOWN = 6000;

	private BunnayBreeding() {
	}

	public static void register() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (!(entity instanceof Rabbit rabbit) || !(level instanceof ServerLevel server)) {
				return InteractionResult.PASS;
			}
			ItemStack stack = player.getItemInHand(hand);
			if (rabbit.isBaby() || rabbit.getAge() != 0 || !rabbit.isFood(stack)) {
				return InteractionResult.PASS;
			}
			List<Allay> allays = server.getEntitiesOfClass(Allay.class, rabbit.getBoundingBox().inflate(ALLAY_RANGE), Allay::isAlive);
			if (allays.isEmpty()) {
				return InteractionResult.PASS;
			}

			BunnayEntity bunnay = ModEntities.BUNNAY.create(server, EntitySpawnReason.BREEDING);
			if (bunnay == null) {
				return InteractionResult.PASS;
			}
			stack.consume(1, player);
			rabbit.setAge(RABBIT_COOLDOWN);
			bunnay.snapTo(rabbit.getX(), rabbit.getY(), rabbit.getZ(), rabbit.getYRot(), 0.0F);
			bunnay.setPersistenceRequired();
			server.addFreshEntity(bunnay);

			for (Entity parent : new Entity[]{rabbit, allays.get(0)}) {
				server.sendParticles(ParticleTypes.HEART, parent.getX(), parent.getY() + parent.getBbHeight(), parent.getZ(), 5, 0.4, 0.3, 0.4, 0.02);
			}
			ExperienceOrb.award(server, rabbit.position(), rabbit.getRandom().nextInt(7) + 1);
			return InteractionResult.SUCCESS;
		});
	}
}
