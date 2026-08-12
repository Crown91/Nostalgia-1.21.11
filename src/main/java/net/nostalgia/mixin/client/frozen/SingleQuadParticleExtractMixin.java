package net.nostalgia.mixin.client.frozen;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.world.level.ChunkPos;
import net.nostalgia.alphalogic.ritual.TickRateManagerAccess;
import net.nostalgia.client.events.core.ClientFreezeRegions;
import net.nostalgia.mixin.client.ritual.ParticleAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SingleQuadParticle.class)
public abstract class SingleQuadParticleExtractMixin implements ParticleAccessor {

    // PORT 26.1.2 -> 1.21.11: the descriptor pointed at
    //   net/minecraft/client/renderer/state/level/QuadParticleRenderState
    // but in 1.21.11 the class lives one package up, without the 'level'
    // segment. Verified against javap:
    //   public void extract(net.minecraft.client.renderer.state.QuadParticleRenderState,
    //                       net.minecraft.client.Camera, float)
    // A wrong descriptor is invisible to javac (the import resolved fine, only
    // the string was stale), so this injector silently never applied.
    @ModifyVariable(
            method = "extract(Lnet/minecraft/client/renderer/state/QuadParticleRenderState;Lnet/minecraft/client/Camera;F)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float nostalgia$stabilizePartial(float partialTickTime) {
        TickRateManagerAccess access = ClientFreezeRegions.access();
        if (access == null || !access.nostalgia$hasRegions()) return partialTickTime;
        net.minecraft.client.multiplayer.ClientLevel level = this.nostalgia$getLevel();
        if (level == null) return partialTickTime;
        double x = this.nostalgia$getX();
        double z = this.nostalgia$getZ();
        long chunkKey = ChunkPos.asLong(((int) Math.floor(x)) >> 4, ((int) Math.floor(z)) >> 4);
        if (access.nostalgia$isChunkFrozen(level.dimension(), chunkKey)) {
            return 1.0F;
        }
        return partialTickTime;
    }
}
