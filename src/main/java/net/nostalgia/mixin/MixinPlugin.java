package net.nostalgia.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {

    /**
     * Debug kill switch. Pass a comma separated list of mixin names as a JVM
     * argument and those mixins are skipped entirely:
     *
     *   -Dnostalgia.disableMixins=client.RenderPipelinesMixin,rd132211.RdFullBrightLightmapMixin
     *
     * Matching is a suffix/substring match on the fully qualified mixin class
     * name, so both "RdFullBrightLightmapMixin" and the full package path work.
     * The special value "render" is not magic, it simply matches every mixin
     * whose name contains it.
     *
     * This exists so a single jar can be used to bisect a rendering problem
     * instead of rebuilding once per experiment. It is inert when the property
     * is absent.
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
        if (!disabled.isEmpty()) {
            System.out.println("[nostalgia] mixin kill switch active for: " + disabled);
        }
        return disabled;
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!DISABLED.isEmpty()) {
            String lower = mixinClassName.toLowerCase(Locale.ROOT);
            for (String token : DISABLED) {
                if (lower.contains(token)) {
                    System.out.println("[nostalgia] skipping mixin " + mixinClassName + " (disabled by " + token + ")");
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
