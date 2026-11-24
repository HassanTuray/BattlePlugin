#version 150

// Minecraft 1.21.8's fog.glsl, with the Seattle Mist added (BattlePlugin). Everything that draws fog (blocks, mobs,
// models, particles, text) goes through apply_fog below, so this one file restyles all of it.
//
// How the server turns the mist on: it tells a player's game they have Blindness (only their game; the server does not
// apply it). Blindness sets the fog in a way nothing else does: it starts at exactly a quarter of where it ends, and
// ends between 5 blocks (full) and the render distance. With n ticks of blindness left (n up to 20) the end is
// lerp(n / 20, render distance, 5), so n / 20 is how thick the server wants the mist: it sends a new n every tick.
// When that pattern shows up, the fog is redrawn as pale white mist instead of blindness's black.
// (Lava also starts its fog at a quarter of its end, but that end is 1 block, so it never looks like the mist.)

layout(std140) uniform Fog {
    vec4 FogColor;
    float FogEnvironmentalStart;
    float FogEnvironmentalEnd;
    float FogRenderDistanceStart;
    float FogRenderDistanceEnd;
    float FogSkyEnd;
    float FogCloudsEnd;
};

// The mist's colour, a pale cool grey-white.
const vec3 MIST_COLOR = vec3(0.80, 0.82, 0.85);
// At full thickness, anything this many blocks away disappears into the mist completely (half as far: half hidden).
// At thickness t it is MIST_FULL_END / t, so the mist thins out quickly as t drops.
const float MIST_FULL_END = 10.0;

bool is_mist(float environmentalStart, float environmentalEnd) {
    return environmentalEnd >= 4.99 && abs(environmentalStart - environmentalEnd * 0.25) < 0.001;
}

// 0 (none) to 1 (full), read back from where blindness put the fog's end (the game hands blindness this same render
// distance). The game counts the effect down a tick between the server's updates, so the server only sends even
// tick counts and this rounds up to tenths: 2k and 2k - 1 ticks both read as k / 10, so the mist does not flicker.
float mist_thickness(float environmentalEnd, float renderDistanceEnd) {
    float ticks = clamp((renderDistanceEnd - environmentalEnd) / max(renderDistanceEnd - 5.0, 1.0), 0.0, 1.0);
    return ceil(ticks * 10.0 - 0.01) / 10.0;
}

float linear_fog_value(float vertexDistance, float fogStart, float fogEnd) {
    if (vertexDistance <= fogStart) {
        return 0.0;
    } else if (vertexDistance >= fogEnd) {
        return 1.0;
    }

    return (vertexDistance - fogStart) / (fogEnd - fogStart);
}

float total_fog_value(float sphericalVertexDistance, float cylindricalVertexDistance, float environmentalStart, float environmantalEnd, float renderDistanceStart, float renderDistanceEnd) {
    return max(linear_fog_value(sphericalVertexDistance, environmentalStart, environmantalEnd), linear_fog_value(cylindricalVertexDistance, renderDistanceStart, renderDistanceEnd));
}

vec4 apply_fog(vec4 inColor, float sphericalVertexDistance, float cylindricalVertexDistance, float environmentalStart, float environmantalEnd, float renderDistanceStart, float renderDistanceEnd, vec4 fogColor) {
    if (is_mist(environmentalStart, environmantalEnd)) {
        float thickness = mist_thickness(environmantalEnd, renderDistanceEnd);
        float mistEnd = min(MIST_FULL_END / max(thickness, 0.02), renderDistanceEnd);
        float mistValue = max(linear_fog_value(sphericalVertexDistance, 0.0, mistEnd),
                              linear_fog_value(cylindricalVertexDistance, renderDistanceStart, renderDistanceEnd));
        return vec4(mix(inColor.rgb, MIST_COLOR, mistValue), inColor.a);
    }
    float fogValue = total_fog_value(sphericalVertexDistance, cylindricalVertexDistance, environmentalStart, environmantalEnd, renderDistanceStart, renderDistanceEnd);
    return vec4(mix(inColor.rgb, fogColor.rgb, fogValue * fogColor.a), inColor.a);
}

float fog_spherical_distance(vec3 pos) {
    return length(pos);
}

float fog_cylindrical_distance(vec3 pos) {
    float distXZ = length(pos.xz);
    float distY = abs(pos.y);
    return max(distXZ, distY);
}
