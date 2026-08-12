package net.nostalgia.mixin.client;

import net.minecraft.client.renderer.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CloudRenderer.class)
public class CloudRendererCancelMixin {
    // PORT 26.1.2 -> 1.21.11: CloudRenderer.render lost its 'int range'
    // parameter (the cloud distance is now derived internally by
    // getSizeForCloudDistance). Verified against javap:
    //   public void render(int, CloudStatus, float, Vec3, long, float)
    // The descriptor is spelled out so a future signature change fails the
    // build instead of silently not applying.
    @Inject(
            method = "render(ILnet/minecraft/client/CloudStatus;FLnet/minecraft/world/phys/Vec3;JF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void nost$hideCloudsDuringWhiteout(int color, net.minecraft.client.CloudStatus cloudStatus, float bottomY, net.minecraft.world.phys.Vec3 cameraPosition, long gameTime, float partialTicks, CallbackInfo ci) {
        if (net.nostalgia.client.events.core.ClientRitualEventRegistry.activeTransition() != null) {
            ci.cancel();
        }
    }
}
