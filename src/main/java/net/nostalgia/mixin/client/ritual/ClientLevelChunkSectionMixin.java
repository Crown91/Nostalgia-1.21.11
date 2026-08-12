package net.nostalgia.mixin.client.ritual;

import net.minecraft.world.level.chunk.LevelChunkSection;
import net.nostalgia.alphalogic.ritual.event.ClientEchoRitualView;
import net.nostalgia.client.events.core.ClientRitualEventRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunkSection.class)
public class ClientLevelChunkSectionMixin {

    // PORT 26.1.2 -> 1.21.11: LevelChunkSection.hasFluid() does not exist here.
    // javap lists exactly one boolean fluid query on the class:
    //   public boolean isRandomlyTickingFluids();
    // alongside isRandomlyTicking() / isRandomlyTickingBlocks() / hasOnlyAir().
    // That is the same tickingFluidCount > 0 check 26.1 exposed as hasFluid(),
    // and it is what the fluid tick pass consults, which is the behaviour this
    // mixin needs: during a ritual transition the section must keep reporting
    // fluid so frozen water is still processed instead of being skipped.
    @Inject(method = "isRandomlyTickingFluids()Z", at = @At("HEAD"), cancellable = true)
    private void onHasFluid(CallbackInfoReturnable<Boolean> cir) {
        ClientEchoRitualView t = ClientRitualEventRegistry.activeTransition();
        if (t != null && !t.isBystander()) {
            cir.setReturnValue(true);
        }
    }
}
