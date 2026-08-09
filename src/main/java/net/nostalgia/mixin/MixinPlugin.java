package net.nostalgia.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
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
