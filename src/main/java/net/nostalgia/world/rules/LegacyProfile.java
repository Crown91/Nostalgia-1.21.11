package net.nostalgia.world.rules;

import net.minecraft.resources.ResourceLocation;

public interface LegacyProfile {
  boolean hasWeather();
  boolean isEternalSnow();
  Integer flatSkyColor();
  boolean disableSunriseSunsetGradient();
  boolean classicStars();
  boolean classicClouds();
  ResourceLocation cloudTexture();
}
