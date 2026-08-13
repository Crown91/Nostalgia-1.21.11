package net.nostalgia.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {

    // System.out is not redirected into latest.log yet while mixin configs are
    // being loaded, so kill switch decisions have to go through SLF4J to be
    // visible in the log file the player sends us.
    private static final Logger LOGGER = LoggerFactory.getLogger("nostalgia/mixinplugin");

    /**
     * Debug kill switch. Pass a comma separated list of mixin names as a JVM
     * argument and those mixins are skipped entirely:
     *
     *   -Dnostalgia.disableMixins=client.RenderPipelinesMixin,rd132211.RdFullBrightLightmapMixin
     *
     * Matching is a case-insensitive substring match on the fully qualified
     * mixin class name, so a bare class name, a package fragment or the full
     * path all work. Passing "net.nostalgia.mixin" disables every mixin this
     * mod owns, which is useful as a baseline.
     *
     * This exists so a single jar can bisect a rendering problem instead of
     * rebuilding once per experiment. It is inert when the property is absent.
     */
    private static final List<String> DISABLED = readDisabledList();

    private static List<String> readDisabledList() {
        List<String> disabled = new ArrayList<>();
        String raw = System.getProperty("nostalgia.disableMixins", "");
        for (String token : raw.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                disabled.add(trimmed.toLowerCase(Locale.ROOT));
            }
        }
        return disabled;
    }

    private static boolean announced = false;

    @Override
    public void onLoad(String mixinPackage) {
        if (DISABLED.isEmpty()) {
            LOGGER.info("KILLSWITCH inactive: -Dnostalgia.disableMixins was not set");
        } else {
            LOGGER.info("KILLSWITCH active with {} token(s): {}", DISABLED.size(), DISABLED);
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!DISABLED.isEmpty()) {
            if (!announced) {
                announced = true;
                LOGGER.info("KILLSWITCH first evaluation, tokens: {}", DISABLED);
            }
            String lower = mixinClassName.toLowerCase(Locale.ROOT);
            for (String token : DISABLED) {
                if (lower.contains(token)) {
                    LOGGER.info("KILLSWITCH skipping {} (matched token '{}')", mixinClassName, token);
                    return false;
                }
            }
        }

        // Punchy is an optional client-side mod; its mixins target classes that
        // simply do not exist when the player has not installed it.
        if (mixinClassName.startsWith("net.nostalgia.mixin.client.punchy.")) {
            return FabricLoader.getInstance().isModLoaded("punchy");
        }

        // Same story for Sodium. These mixins reach into Sodium internals, so
        // without Sodium present the target classes are missing and the game
        // would hard-crash instead of quietly running without them.
        if (mixinClassName.startsWith("net.nostalgia.mixin.client.frozen.sodium.")
                || mixinClassName.equals("net.nostalgia.mixin.alpha.AlphaSodiumWaterMixin")
                || mixinClassName.equals("net.nostalgia.mixin.alpha.AlphaSodiumLightMixin")
                || mixinClassName.equals("net.nostalgia.mixin.rd132211.RdSodiumAOMixin")) {
            return FabricLoader.getInstance().isModLoaded("sodium");
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
