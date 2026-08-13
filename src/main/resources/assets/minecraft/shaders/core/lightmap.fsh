#version 330

// PORTING NOTE - READ BEFORE TOUCHING THIS FILE.
//
// This overrides a VANILLA shader, so the uniform block below must match the
// std140 layout of the targeted Minecraft version FIELD FOR FIELD, in order.
// GLSL cannot detect a mismatch: it just reads the buffer at its own offsets and
// silently returns whatever happens to sit there.
//
// The 26.1.2 version of this file started with SkyFactor. In 1.21.11 a new
// AmbientLightFactor was inserted in front of it and BlockLightTint was removed,
// so the copied file read SkyFactor out of AmbientLightFactor - which is exactly
// 0.0 in the overworld. Result: sky light vanished entirely, block light still
// worked, and the whole world went black while every Java-side value stayed
// correct. Do not copy this block from another version; dump it from the client
// jar of the target version (see .github/workflows/dump-vanilla-shaders.yml).
//
// Layout below verified against the vanilla 1.21.11 client jar.
layout(std140) uniform LightmapInfo {
    float AmbientLightFactor;
    float SkyFactor;
    float BlockFactor;
    float NightVisionFactor;
    float DarknessScale;
    float DarkenWorldFactor;
    float BrightnessFactor;
    vec3 SkyLightColor;
    vec3 AmbientColor;
} lightmapInfo;

in vec2 texCoord;

out vec4 fragColor;

// The Alpha branch is selected by a sentinel value in SkyLightColor rather than
// by any kind of switch, because a shader cannot ask which dimension it is in.
// SkyLightColor is fed from the EnvironmentAttributes.SKY_LIGHT_COLOR attribute,
// which a dimension type can set declaratively - no mixin required - and the
// Alpha branch below produces greyscale output that never reads the colour, so
// the channel is free to carry a flag there.
//
// The sentinel is ARGB 0xFF226BFF: 34/255 = 0.13333, 107/255 = 0.41961. Those are
// the same two magic numbers the 26.1.2 build smuggled through BlockLightTint as
// ARGB.color(34, 107, 255), a field that no longer exists in 1.21.11.
const float ALPHA_SENTINEL_R = 0.1337;
const float ALPHA_SENTINEL_G = 0.420;
const float ALPHA_SENTINEL_EPSILON = 0.001;

float get_brightness(float level) {
    return level / (4.0 - 3.0 * level);
}

vec3 notGamma(vec3 color) {
    float maxComponent = max(max(color.x, color.y), color.z);
    float maxInverted = 1.0f - maxComponent;
    float maxScaled = 1.0f - maxInverted * maxInverted * maxInverted * maxInverted;
    return color * (maxScaled / maxComponent);
}

bool isAlphaLighting() {
    return abs(lightmapInfo.SkyLightColor.r - ALPHA_SENTINEL_R) < ALPHA_SENTINEL_EPSILON
        && abs(lightmapInfo.SkyLightColor.g - ALPHA_SENTINEL_G) < ALPHA_SENTINEL_EPSILON;
}

void main() {
    // ---- Nostalgia: Alpha 1.1.2 lighting curve ----
    if (isAlphaLighting()) {
        float block_level = floor(texCoord.x * 16.0) / 15.0;
        float sky_level = floor(texCoord.y * 16.0) / 15.0;

        // Alpha had no smooth day/night lightmap ramp: the sky level itself was
        // stepped down as the sun set, and block and sky light simply took the
        // maximum instead of being blended.
        float effective_skylevel = max(0.0, sky_level - (1.0 - lightmapInfo.SkyFactor));
        float effective_level = max(block_level, effective_skylevel);

        float f3 = 1.0 - effective_level;
        float alphaBrightness = (1.0 - f3) / (f3 * 3.0 + 1.0) * 0.95 + 0.05;

        alphaBrightness = max(alphaBrightness, lightmapInfo.NightVisionFactor);

        fragColor = vec4(alphaBrightness, alphaBrightness, alphaBrightness, 1.0);
        return;
    }

    // ---- everything below is vanilla 1.21.11, unmodified ----
    float block_brightness = get_brightness(floor(texCoord.x * 16) / 15) * lightmapInfo.BlockFactor;
    float sky_brightness = get_brightness(floor(texCoord.y * 16) / 15) * lightmapInfo.SkyFactor;

    // cubic nonsense, dips to yellowish in the middle, white when fully saturated
    vec3 color = vec3(
        block_brightness,
        block_brightness * ((block_brightness * 0.6 + 0.4) * 0.6 + 0.4),
        block_brightness * (block_brightness * block_brightness * 0.6 + 0.4)
    );

    color = mix(color, lightmapInfo.AmbientColor, lightmapInfo.AmbientLightFactor);

    color += lightmapInfo.SkyLightColor * sky_brightness;
    color = mix(color, vec3(0.75), 0.04);

    if (lightmapInfo.AmbientLightFactor == 0.0f) {
        vec3 darkened_color = color * vec3(0.7, 0.6, 0.6);
        color = mix(color, darkened_color, lightmapInfo.DarkenWorldFactor);
    }

    if (lightmapInfo.NightVisionFactor > 0.0) {
        // scale up uniformly until 1.0 is hit by one of the colors
        float max_component = max(color.r, max(color.g, color.b));
        if (max_component < 1.0) {
            vec3 bright_color = color / max_component;
            color = mix(color, bright_color, lightmapInfo.NightVisionFactor);
        }
    }

    if (lightmapInfo.AmbientLightFactor == 0.0f) {
        color = color - vec3(lightmapInfo.DarknessScale);
    }

    color = clamp(color, 0.0, 1.0);

    vec3 notGamma = notGamma(color);
    color = mix(color, notGamma, lightmapInfo.BrightnessFactor);
    color = mix(color, vec3(0.75), 0.04);

    fragColor = vec4(color, 1.0);
}
