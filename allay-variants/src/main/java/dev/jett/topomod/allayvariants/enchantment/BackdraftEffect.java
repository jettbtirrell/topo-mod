package dev.jett.topomod.allayvariants.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * What the Backdraft enchantment does, read from the enchantment's data file (data/allay_variants/enchantment/
 * backdraft.json) like the effects of vanilla enchantments: when a club uses up a target's burning, every
 * creature within {@code radius} blocks of it is set alight for {@code seconds}. Both grow with the level.
 */
public record BackdraftEffect(LevelBasedValue radius, LevelBasedValue seconds) {
	public static final Codec<BackdraftEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		LevelBasedValue.CODEC.fieldOf("radius").forGetter(BackdraftEffect::radius),
		LevelBasedValue.CODEC.fieldOf("seconds").forGetter(BackdraftEffect::seconds)
	).apply(instance, BackdraftEffect::new));
}
