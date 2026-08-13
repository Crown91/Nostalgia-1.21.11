package net.nostalgia.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * TEMPORARY diagnostic for the "black world" investigation.
 *
 * Block light works, sky light does not: F3 reports 15 sky / 15 block, a
 * lightning flash briefly turns the whole lightmap (and therefore inventory
 * item icons) white, yet daylight terrain, entities and block entities render
 * almost black. That points at the values written into the lightmap rather
 * than at the light engine or at Sodium.
 *
 * Once per second this dumps every number that can plausibly scale the sky
 * component down: the environment probe values, the primitive state of any
 * lightmap-like object reachable from Minecraft, and the ritual overlay flags.
 *
 * Everything is done reflectively and wrapped in Throwable handlers so the
 * dump can never crash or slow down the frame loop. Delete this class and its
 * entry in nostalgia.mixins.json once the cause is fixed.
 */
@Mixin(GameRenderer.class)
public class LightDiagnosticsMixin {

    @Unique
    private static final Logger nostalgia$DIAG = LoggerFactory.getLogger("nostalgia/lightdiag");

    @Unique
    private static long nostalgia$lastDump = 0L;

    @Inject(method = "renderLevel", at = @At("HEAD"), require = 0)
    private void nostalgia$dumpLightingState(DeltaTracker deltaTracker, CallbackInfo ci) {
        long now = System.currentTimeMillis();
        if (now - nostalgia$lastDump < 1000L) {
            return;
        }
        nostalgia$lastDump = now;

        StringBuilder sb = new StringBuilder("LIGHTDIAG");
        Minecraft mc = Minecraft.getInstance();

        try {
            if (mc.level == null) {
                nostalgia$DIAG.info("LIGHTDIAG level=null");
                return;
            }
            sb.append(" dim=").append(String.valueOf(mc.level.dimension()));
            sb.append(" gameTime=").append(mc.level.getGameTime() % 24000L);
        } catch (Throwable t) {
            sb.append(" [level:").append(t).append("]");
        }

        try {
            Camera camera = mc.gameRenderer.getMainCamera();
            sb.append(" SKY_LIGHT_FACTOR=")
                    .append(camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, 1.0F));
            sb.append(" BLOCK_LIGHT_TINT=")
                    .append(camera.attributeProbe().getValue(EnvironmentAttributes.BLOCK_LIGHT_TINT, 1.0F));
        } catch (Throwable t) {
            sb.append(" [probe:").append(t).append("]");
        }

        // Walk Minecraft, its renderers and their fields looking for anything
        // lightmap-shaped, then print that object's primitive state.
        try {
            nostalgia$scan(sb, mc, "mc", 0);
        } catch (Throwable t) {
            sb.append(" [scan:").append(t).append("]");
        }

        nostalgia$staticField(sb, "net.nostalgia.client.events.echo.RitualVisualManager", "isTransitioning");
        nostalgia$staticField(sb, "net.nostalgia.client.render.PortalSkyRenderer", "active");
        nostalgia$staticField(sb, "net.nostalgia.client.render.GlassBreakRenderer", "active");
        nostalgia$staticCall(sb, "net.nostalgia.client.events.echo.RitualVisualManager", "getWhiteoutAlpha");

        nostalgia$DIAG.info(sb.toString());
    }

    /**
     * Depth-limited walk over object fields. Anything whose type name mentions
     * light gets its primitive fields printed.
     */
    @Unique
    private static void nostalgia$scan(StringBuilder sb, Object owner, String path, int depth) {
        if (owner == null || depth > 2) {
            return;
        }
        for (Field field : owner.getClass().getDeclaredFields()) {
            Object value;
            try {
                field.setAccessible(true);
                value = field.get(owner);
            } catch (Throwable ignored) {
                continue;
            }
            if (value == null || value == owner) {
                continue;
            }
            String typeName = value.getClass().getSimpleName();
            String lower = typeName.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("light")) {
                nostalgia$dumpPrimitives(sb, value, path + "." + field.getName() + "(" + typeName + ")");
            } else if (depth < 2
                    && (lower.contains("renderer") || lower.contains("renderstate") || lower.contains("level"))) {
                nostalgia$scan(sb, value, path + "." + field.getName(), depth + 1);
            }
        }
    }

    @Unique
    private static void nostalgia$dumpPrimitives(StringBuilder sb, Object target, String label) {
        sb.append(' ').append(label).append("{");
        boolean first = true;
        for (Field field : target.getClass().getDeclaredFields()) {
            Class<?> type = field.getType();
            if (!type.isPrimitive()) {
                continue;
            }
            try {
                field.setAccessible(true);
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(field.getName()).append('=').append(field.get(target));
            } catch (Throwable ignored) {
                // keep going, a single inaccessible field must not stop the dump
            }
        }
        sb.append('}');
    }

    @Unique
    private static void nostalgia$staticField(StringBuilder sb, String className, String fieldName) {
        try {
            Class<?> clazz = Class.forName(className);
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            sb.append(' ').append(clazz.getSimpleName()).append('.').append(fieldName)
                    .append('=').append(field.get(null));
        } catch (Throwable ignored) {
            // the class may not exist in this build, that is fine
        }
    }

    @Unique
    private static void nostalgia$staticCall(StringBuilder sb, String className, String methodName) {
        try {
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getDeclaredMethod(methodName);
            method.setAccessible(true);
            sb.append(' ').append(clazz.getSimpleName()).append('.').append(methodName)
                    .append("()=").append(method.invoke(null));
        } catch (Throwable ignored) {
            // ditto
        }
    }
}
