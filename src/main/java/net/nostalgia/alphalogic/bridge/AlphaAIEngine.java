package net.nostalgia.alphalogic.bridge;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class AlphaAIEngine {

    /** Ticks between two melee hits of the same mob, matching the vanilla melee goal. */
    private static final int ATTACK_COOLDOWN_TICKS = 20;
    /** Drop bookkeeping for mobs that have not attacked for this many ticks. */
    private static final long STALE_AFTER_TICKS = 600L;

    private static final java.util.Map<java.util.UUID, Long> LAST_ATTACK_TICK = new java.util.HashMap<>();

    public static void tickActivity(Mob mob) {
        boolean isMonster = mob instanceof net.minecraft.world.entity.monster.Monster;
        boolean isSkeleton = mob instanceof net.minecraft.world.entity.monster.skeleton.Skeleton;
        boolean isGhastOrSlime = mob instanceof net.minecraft.world.entity.monster.Ghast || mob instanceof net.minecraft.world.entity.monster.Slime;

        if (isGhastOrSlime) return;

        if (isMonster) {
            Player target = mob.level().getNearestPlayer(mob, 16.0D);
            if (target != null) {
                
                if (!isSkeleton) {
                    mob.getNavigation().moveTo(target, 1.0D);
                }
                mob.setTarget(target);
                
                if (mob.distanceToSqr(target) < 4.0D) {
                    if (!(mob instanceof net.minecraft.world.entity.monster.Creeper) && !isSkeleton) {
                        if (mob.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                            tryMeleeAttack(mob, sl, target);
                        }
                    }
                }
            } else {
                if (!isSkeleton) wanderAimlessly(mob);
            }
        } else {
            wanderAimlessly(mob);
        }
    }

    /**
     * Performs one melee hit with a cooldown and the regular arm swing. Without this the
     * engine hit the target on every single tick, which read as instant, animation-less damage.
     */
    private static void tryMeleeAttack(Mob mob, net.minecraft.server.level.ServerLevel level, Player target) {
        long now = level.getGameTime();
        Long last = LAST_ATTACK_TICK.get(mob.getUUID());
        if (last != null && now - last < ATTACK_COOLDOWN_TICKS) {
            return;
        }

        LAST_ATTACK_TICK.put(mob.getUUID(), now);
        if (LAST_ATTACK_TICK.size() > 512) {
            LAST_ATTACK_TICK.entrySet().removeIf(entry -> now - entry.getValue() > STALE_AFTER_TICKS);
        }

        mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        mob.doHurtTarget(level, target);
    }

    private static void wanderAimlessly(Mob mob) {
        if (mob.getRandom().nextInt(60) == 0) {
            double rx = mob.getX() + (mob.getRandom().nextDouble() * 10.0D - 5.0D);
            double rz = mob.getZ() + (mob.getRandom().nextDouble() * 10.0D - 5.0D);
            double ry = mob.getY();

            mob.getNavigation().moveTo(rx, ry, rz, 0.8D);
        }
    }
}
