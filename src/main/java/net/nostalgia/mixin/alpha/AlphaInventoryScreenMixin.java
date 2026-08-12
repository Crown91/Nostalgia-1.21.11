package net.nostalgia.mixin.alpha;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.nostalgia.world.dimension.ModDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class AlphaInventoryScreenMixin {

    // 26.1 drew the container texture in renderBackground; in 1.21.11 the
    // screen-specific background is renderBg(GuiGraphics, float, int, int)
    // (partialTick comes first now). Injecting at TAIL keeps the alpha patch
    // painted over the offhand slot exactly as before.
    @Inject(method = "renderBg", at = @At("TAIL"))
    private void hideOffhandVisuals(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.level.dimension() == ModDimensions.ALPHA_112_01_LEVEL_KEY) {
            AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) (Object) this;
            int leftPos = accessor.getLeftPos();
            int topPos = accessor.getTopPos();
            graphics.fill(leftPos + 76, topPos + 61, leftPos + 76 + 18, topPos + 61 + 18, 0xFFC6C6C6);
        }
    }
}
