package net.nostalgia.world.rules;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.Nullable;

public interface DimensionRules {
    boolean isMobAllowed(EntityType<?> type);
    @Nullable SoundEvent getRedirectedSound(EntityType<?> type, SoundEvent original);
    @Nullable ResourceLocation getOverriddenTexture(EntityType<?> type, ResourceLocation original);
    default boolean showDamageIndicatorParticles() { return true; }
}
