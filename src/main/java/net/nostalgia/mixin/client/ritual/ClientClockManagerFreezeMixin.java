package net.nostalgia.mixin.client.ritual;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.nostalgia.alphalogic.ritual.event.ClientEchoRitualView;
import net.nostalgia.client.events.core.ClientFreezeRegions;
import net.nostalgia.client.events.core.ClientRitualEventRegistry;
import net.nostalgia.client.events.core.ClientZoneTime;
import net.nostalgia.client.events.core.ZoneTimeBridge;
import net.nostalgia.client.events.echo.RitualVisualManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rewinds the client-side day time during a ritual transition and freezes it inside timestop
 * zones.
 *
 * <p>26.1 hooked {@code ClientClockManager.getTotalTicks(Holder<WorldClock>)}. That class does not
 * exist in 1.21.11. Hooking {@code Timeline.getTotalTicks(Level)} was not enough either: it merely
 * delegates to {@link Level#getDayTime()}, and the environment attribute system samples time
 * through its own {@code LongSupplier} (see {@code AttributeTrackSampler.dayTimeGetter}) which
 * bypasses {@code Timeline} entirely. That is why only the sun visibly moved while sky colour,
 * light and fog stayed put.
 *
 * <p>{@link Level#getDayTime()} is the one shared choke point - {@code ClientLevel} does not
 * override it - so a single hook here drives the sun, the moon, the sky gradient, the fog and the
 * light level together, reproducing the original morning -> day -> evening -> night sweep.
 */
@Mixin(Level.class)
public abstract class ClientClockManagerFreezeMixin {

        @Inject(method = "getDayTime()J", at = @At("RETURN"), cancellable = true)
        private void nostalgia$rewindClientTimeline(CallbackInfoReturnable<Long> cir) {
                Level self = (Level) (Object) this;
                if (!self.isClientSide()) {
                        return;
                }

                Minecraft mc = Minecraft.getInstance();
                if (mc == null || mc.level != self) {
                        return;
                }

                long real = cir.getReturnValueJ();
                ZoneTimeBridge.lastRealClockTicks = real;
                ZoneTimeBridge.hasClockReal = true;

                ClientEchoRitualView transition = ClientRitualEventRegistry.activeTransition();
                if (transition != null && !transition.isBystander()) {
                        cir.setReturnValue(RitualVisualManager.calculateInertialTime(real));
                        return;
                }

                if (ClientFreezeRegions.hasRegions() || ClientZoneTime.isActive()) {
                        cir.setReturnValue(ClientZoneTime.getEffectiveClockTicks(real));
                }
        }
}
