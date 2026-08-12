package net.nostalgia.mixin.alpha;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.world.entity.player.Player;
import net.nostalgia.world.dimension.ModDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.AttackIndicatorStatus;

@Mixin(Gui.class)
public abstract class AlphaHudElementsMixin {

    private boolean isAlphaMode() {
        Minecraft client = Minecraft.getInstance();
        return client.level != null && client.level.dimension() == ModDimensions.ALPHA_112_01_LEVEL_KEY;
    }

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void hideAlphaFood(GuiGraphics graphics, Player player, int y, int right, CallbackInfo ci) {
        if (isAlphaMode()) {
            ci.cancel();
        }
    }

    @ModifyArg(method = "renderPlayerHealth", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderHearts(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V"), index = 3)
    private int lowerAlphaHearts(int yLineBase) {
        return isAlphaMode() ? yLineBase + 7 : yLineBase;
    }

    @ModifyArg(method = "renderPlayerHealth", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderArmor(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIII)V"), index = 2)
    private int lowerAlphaArmor(int yLineBase) {
        return isAlphaMode() ? yLineBase + 7 : yLineBase;
    }

    @ModifyArg(method = "renderPlayerHealth", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderAirBubbles(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;III)V"), index = 3)
    private int lowerAlphaAirBubbles(int yLineAir) {
        return isAlphaMode() ? yLineAir + 10 + 7 : yLineAir;
    }

    // 1.21.11 renamed Gui#renderHotbar to Gui#renderItemHotbar; the attack
    // indicator option is still read inside it, so the redirect target only
    // needed the new name (confirmed against the javap dump of Gui).
    @Redirect(method = { "renderCrosshair", "renderItemHotbar" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    private Object redirectAttackIndicatorGet(OptionInstance<?> instance) {
        if (isAlphaMode()) {
            Object val = instance.get();
            if (val instanceof AttackIndicatorStatus) {
                return AttackIndicatorStatus.OFF;
            }
            return val;
        }
        return instance.get();
    }

    @Inject(method = "nextContextualInfoState", at = @At("RETURN"), cancellable = true)
    private void nostalgia$disableLocatorInMultiplayer(CallbackInfoReturnable<Enum<?>> cir) {
        Enum<?> value = cir.getReturnValue();
        if (value != null && "LOCATOR".equals(value.name())) {
            Minecraft mc = Minecraft.getInstance();
            if (!mc.isSingleplayer()) {
                boolean canShowExperience = mc.gameMode != null && mc.gameMode.hasExperience();
                Class<? extends Enum> enumClass = value.getClass();
                Enum<?> targetValue = Enum.valueOf(enumClass, canShowExperience ? "EXPERIENCE" : "EMPTY");
                cir.setReturnValue(targetValue);
            }
        }
    }
}
