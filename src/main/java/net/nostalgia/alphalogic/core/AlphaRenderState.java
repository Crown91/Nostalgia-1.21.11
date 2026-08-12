package net.nostalgia.alphalogic.core;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class AlphaRenderState {
    
    public static int renderDistanceMode = 0;
    
    public static void cycleRenderDistance() {
        renderDistanceMode = (renderDistanceMode + 1) % 4;
        
        String modeName = "";
        switch (renderDistanceMode) {
            case 0: modeName = "Far"; break;
            case 1: modeName = "Normal"; break;
            case 2: modeName = "Short"; break;
            case 3: modeName = "Tiny"; break;
        }
        
        if (Minecraft.getInstance().player != null) {
            // the boolean picks the action bar, which is where the old
            // sendOverlayMessage always put its text
            Minecraft.getInstance().player.displayClientMessage(Component.literal("Render distance: " + modeName), true);
        }
    }
    
    public static float getFogEnd() {
        switch (renderDistanceMode) {
            case 3: return 32.0f;
            case 2: return 64.0f;
            case 1: return 128.0f;
            default: return 256.0f;
        }
    }

    public static float getFogStart() {
        switch (renderDistanceMode) {
            case 3: return 8.0f;
            case 2: return 16.0f;
            case 1: return 32.0f;
            default: return 64.0f;
        }
    }

    public static boolean isFogDense() {
        return renderDistanceMode >= 1;
    }
    
    public static float getCelestialAlpha() {
        float fogEnd = getFogEnd();
        return net.minecraft.util.Mth.clamp((fogEnd - 32.0f) / (256.0f - 32.0f), 0.0f, 1.0f);
    }
}
