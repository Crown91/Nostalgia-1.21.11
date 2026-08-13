package net.nostalgia.mixin.client.ritual;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.timeline.Timeline;
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

// PORT NOTE 26.1.2 -> 1.21.11:
//
// The original mixin targeted net.minecraft.client.ClientClockManager:
//     @Inject(method = "getTotalTicks", at = @At("RETURN"), cancellable = true)
//     void (Holder<WorldClock> definition, CallbackInfoReturnable<Long> cir)
//
// ClientClockManager, WorldClock and WorldClocks do not exist in 1.21.11 - the
// whole clock system was renamed and moved into net.minecraft.world.timeline.
// Verified against the real 1.21.11 jar (api dump):
//     net.minecraft.world.timeline.Timeline
//       public long getCurrentTicks(Level)
//       public long getTotalTicks(Level)
//
// Timeline is a COMMON class, not a client-only one, so two safety measures are
// used: the mixin is registered in the "client" block of nostalgia.mixins.json
// (so it is never applied on a dedicated server), and every injection is guarded
// by level.isClientSide() plus an identity check against the client level. The
// server's authoritative world time must never be modified here.
//
// This hook is what produces the fast day/night spin during a ritual transition
// (Timelines.DAY / Timelines.MOON sample their tracks through this method), and
// what freezes time inside a beacon freeze region.
@Mixin(Timeline.class)
public abstract class ClientClockManagerFreezeMixin {

    @Inject(
        method = "getTotalTicks(Lnet/minecraft/world/level/Level;)J",
        at = @At("RETURN"),
        cancellable = true
    )
    private void nostalgia$rewindClientTimeline(Level level, CallbackInfoReturnable<Long> cir) {
        // Never touch server-side time.
        if (level == null || !level.isClientSide()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != level) return;

        long real = cir.getReturnValueJ();

        // Publish the untouched value so the zone-time machinery has a real reference.
        ZoneTimeBridge.lastRealClockTicks = real;
        ZoneTimeBridge.hasClockReal = true;

        ClientEchoRitualView transition = ClientRitualEventRegistry.activeTransition();
        if (transition != null && !transition.isBystander()) {
            // Accelerated / inertial visual time while the ritual is running.
            cir.setReturnValue(RitualVisualManager.calculateInertialTime(real));
            return;
        }

        if (ClientFreezeRegions.hasRegions() || ClientZoneTime.isActive()) {
            cir.setReturnValue(ClientZoneTime.getEffectiveClockTicks(real));
        }
    }
}
