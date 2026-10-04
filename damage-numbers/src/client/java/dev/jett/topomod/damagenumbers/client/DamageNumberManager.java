package dev.jett.topomod.damagenumbers.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

// Watches living entities each tick and spawns a popup whenever their health changes.
public final class DamageNumberManager {
	/** How long a popup lives, in ticks. */
	static final int LIFETIME = 30;
	/** Changes smaller than this are ignored (float noise, regen trickles). */
	private static final float MIN_CHANGE = 0.05F;

	static final List<Popup> POPUPS = new ArrayList<>();

	private static final Map<Integer, Float> LAST_HEALTH = new HashMap<>();
	private static final RandomSource RANDOM = RandomSource.create();
	private static ClientLevel trackedLevel;

	private DamageNumberManager() {
	}

	/** A floating number. {@code amount} is positive for damage and negative for healing. */
	static final class Popup {
		final Vec3 origin;
		final float amount;
		int age;

		Popup(Vec3 origin, float amount) {
			this.origin = origin;
			this.amount = amount;
		}
	}

	static void tick(ClientLevel level) {
		if (level != trackedLevel) {
			// New world/dimension: forget everything so we don't show bogus numbers.
			trackedLevel = level;
			LAST_HEALTH.clear();
			POPUPS.clear();
		}

		Map<Integer, Float> seen = new HashMap<>();
		for (Entity entity : level.entitiesForRendering()) {
			if (!(entity instanceof LivingEntity living)) {
				continue;
			}

			// Count absorption (golden hearts) so hits that only eat absorption still show up.
			float total = living.getHealth() + living.getAbsorptionAmount();
			Float previous = LAST_HEALTH.get(entity.getId());
			seen.put(entity.getId(), total);

			if (previous != null && Math.abs(previous - total) >= MIN_CHANGE) {
				spawn(living, previous - total);
			}
		}

		// Replacing the map also drops entries for entities that unloaded or died.
		LAST_HEALTH.clear();
		LAST_HEALTH.putAll(seen);

		for (Iterator<Popup> it = POPUPS.iterator(); it.hasNext();) {
			if (++it.next().age >= LIFETIME) {
				it.remove();
			}
		}
	}

	private static void spawn(LivingEntity entity, float amount) {
		double jitterX = (RANDOM.nextDouble() - 0.5) * entity.getBbWidth() * 0.8;
		double jitterZ = (RANDOM.nextDouble() - 0.5) * entity.getBbWidth() * 0.8;
		Vec3 origin = new Vec3(entity.getX() + jitterX, entity.getY() + entity.getBbHeight() + 0.2, entity.getZ() + jitterZ);
		POPUPS.add(new Popup(origin, amount));
	}
}
