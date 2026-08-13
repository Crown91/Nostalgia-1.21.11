package net.nostalgia.world.dimension;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.core.HolderSet;
import net.nostalgia.NostalgiaMod;

public class ModDimensions {
    /**
     * Sentinel fed to the overridden vanilla lightmap shader to switch it to the
     * Alpha 1.1.2 lighting curve.
     *
     * <p>A shader cannot ask which dimension it is rendering, and 1.21.11 removed
     * BLOCK_LIGHT_TINT, the channel the 26.1 build used to smuggle this flag in.
     * SKY_LIGHT_COLOR is the replacement carrier: it reaches the shader's
     * LightmapInfo.SkyLightColor through the ordinary attribute system, so no
     * mixin is involved, and the Alpha branch outputs greyscale without ever
     * reading the colour, which leaves the channel free to carry a flag.
     *
     * <p>ARGB 0xFF226BFF decodes to 34/255 = 0.13333 and 107/255 = 0.41961, the
     * two magic numbers the shader matches with a 0.001 epsilon. Keep this value
     * and assets/minecraft/shaders/core/lightmap.fsh in sync.
     */
    public static final int ALPHA_LIGHTING_SENTINEL_COLOR = 0xFF226BFF;

    public static final ResourceKey<Level> RD_132211_LEVEL_KEY = ResourceKey.create(Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(NostalgiaMod.MOD_ID, "rd_132211"));
    public static final ResourceKey<DimensionType> RD_132211_DIM_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            Identifier.fromNamespaceAndPath(NostalgiaMod.MOD_ID, "rd_132211"));
    public static final ResourceKey<LevelStem> RD_132211_STEM = ResourceKey.create(Registries.LEVEL_STEM,
            Identifier.fromNamespaceAndPath(NostalgiaMod.MOD_ID, "rd_132211"));

    public static final ResourceKey<Level> ALPHA_112_01_LEVEL_KEY = ResourceKey.create(Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(NostalgiaMod.MOD_ID, "alpha_112_01"));
    public static final ResourceKey<DimensionType> ALPHA_112_01_DIM_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            Identifier.fromNamespaceAndPath(NostalgiaMod.MOD_ID, "alpha_112_01"));
    public static final ResourceKey<LevelStem> ALPHA_112_01_STEM = ResourceKey.create(Registries.LEVEL_STEM,
            Identifier.fromNamespaceAndPath(NostalgiaMod.MOD_ID, "alpha_112_01"));

    public static void bootstrapType(BootstrapContext<DimensionType> context) {
        // PORT 1.21.11: record components are (hasFixedTime, hasSkyLight, hasCeiling,
        // coordinateScale, minY, height, logicalHeight, infiniburn, ambientLight,
        // monsterSettings, skybox, cardinalLightType, attributes, timelines).
        // The 26.1 fourth boolean and the trailing Optional<Holder<WorldClock>> are gone.
        context.register(RD_132211_DIM_TYPE, new DimensionType(
                false, true, false, 1.0, 0, 256, 256,
                BlockTags.INFINIBURN_OVERWORLD, 1.0f,
                new DimensionType.MonsterSettings(ConstantInt.of(0), 0),
                DimensionType.Skybox.OVERWORLD,
                DimensionType.CardinalLightType.DEFAULT,
                net.minecraft.world.attribute.EnvironmentAttributeMap.EMPTY,
                HolderSet.empty()
        ));

        context.register(ALPHA_112_01_DIM_TYPE, new DimensionType(
                false, true, false, 1.0, 0, 128, 128,
                BlockTags.INFINIBURN_OVERWORLD, 0.0f,
                new DimensionType.MonsterSettings(ConstantInt.of(0), 15),
                DimensionType.Skybox.OVERWORLD,
                DimensionType.CardinalLightType.DEFAULT,
                EnvironmentAttributeMap.builder()
                        .set(EnvironmentAttributes.SKY_COLOR, 0xFF88BBFF)
                        .set(EnvironmentAttributes.FOG_COLOR, 0xFFEBEBFF)
                        .set(EnvironmentAttributes.CLOUD_HEIGHT, 108.0f)
                        .set(EnvironmentAttributes.CLOUD_COLOR, 0xFFFFFFFF)
                        // Switches the overridden lightmap shader to the Alpha curve.
                        .set(EnvironmentAttributes.SKY_LIGHT_COLOR, ALPHA_LIGHTING_SENTINEL_COLOR)
                        .build(),
                HolderSet.empty()
        ));
    }

    public static void bootstrapStem(BootstrapContext<LevelStem> context) {
        var biomeRegistry = context.lookup(Registries.BIOME);
        var dimTypes = context.lookup(Registries.DIMENSION_TYPE);

        context.register(RD_132211_STEM, new LevelStem(
                dimTypes.getOrThrow(RD_132211_DIM_TYPE),
                new net.nostalgia.world.gen.RD132211ChunkGenerator(
                        new net.minecraft.world.level.biome.FixedBiomeSource(
                                biomeRegistry.getOrThrow(
                                        ResourceKey.create(Registries.BIOME,
                                                Identifier.fromNamespaceAndPath("nostalgia", "rd_132211")))))));

        context.register(ALPHA_112_01_STEM, new LevelStem(
                dimTypes.getOrThrow(ALPHA_112_01_DIM_TYPE),
                new net.nostalgia.world.gen.AlphaChunkGenerator(
                        new net.minecraft.world.level.biome.FixedBiomeSource(
                                biomeRegistry.getOrThrow(
                                        ResourceKey.create(Registries.BIOME,
                                                Identifier.fromNamespaceAndPath("nostalgia", "alpha")))))));
    }
}
