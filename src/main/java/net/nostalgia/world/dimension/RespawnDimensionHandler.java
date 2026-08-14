package net.nostalgia.world.dimension;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.nostalgia.command.TeleportCommand;

/**
 * Keeps players inside the mod dimension they died in instead of sending them back to the
 * overworld spawn. Vanilla dimensions keep their normal respawn behaviour.
 */
public class RespawnDimensionHandler implements ModInitializer {

    @Override
    public void onInitialize() {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // alive == true means the player returned through the end portal without dying.
            if (alive) {
                return;
            }

            ResourceKey<Level> deathDimension = oldPlayer.level().dimension();
            if (!isNostalgiaDimension(deathDimension)) {
                return;
            }
            if (newPlayer.level().dimension().equals(deathDimension)) {
                return;
            }
            if (!(newPlayer.level() instanceof ServerLevel respawnLevel)) {
                return;
            }

            MinecraftServer server = respawnLevel.getServer();
            if (server == null) {
                return;
            }
            ServerLevel deathLevel = server.getLevel(deathDimension);
            if (deathLevel == null) {
                return;
            }

            BlockPos respawnPos = TeleportCommand.findSafeSpawn(deathLevel, oldPlayer.getBlockX(), oldPlayer.getBlockZ());
            newPlayer.teleportTo(deathLevel,
                    respawnPos.getX() + 0.5,
                    respawnPos.getY(),
                    respawnPos.getZ() + 0.5,
                    java.util.Collections.emptySet(),
                    newPlayer.getYRot(),
                    newPlayer.getXRot(),
                    true);
        });
    }

    private static boolean isNostalgiaDimension(ResourceKey<Level> dimension) {
        return dimension.equals(ModDimensions.ALPHA_112_01_LEVEL_KEY)
                || dimension.equals(ModDimensions.RD_132211_LEVEL_KEY);
    }
}
