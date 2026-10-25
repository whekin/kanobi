package com.kanobi.app.fx

/**
 * AGSL sources (Android 13+). AGSL has no derivatives, so line widths come from analytic
 * gradients instead of fwidth().
 */
internal object Agsl {
    private const val NOISE = """
float hash(float2 p) {
    float3 p3 = fract(float3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(float2 x) {
    float2 i = floor(x);
    float2 f = fract(x);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + float2(1.0, 0.0)), u.x),
               mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x), u.y);
}
"""

    /**
     * Topographic map that drifts slowly, lit from a direction that follows the tilt. With the
     * phone on its edge it also draws the real horizon, with level strata lines below it.
     */
    const val TERRAIN = """
uniform float2 uSize;
uniform float uDensity;
uniform float uTime;
uniform float uTilt;
uniform float2 uFocus;
uniform float3 uGlow;
uniform float uPulse;
uniform float3 uAccent;
uniform float3 uBase;
uniform float uHorizon;
uniform float2 uUp;
uniform float uClear;
""" + NOISE + """
// Value noise with analytic derivatives: x = height, yz = gradient.
float3 noised(float2 x) {
    float2 i = floor(x);
    float2 f = fract(x);
    float2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    float2 du = 30.0 * f * f * (f * (f - 2.0) + 1.0);
    float a = hash(i);
    float b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0));
    float d = hash(i + float2(1.0, 1.0));
    float k1 = b - a;
    float k2 = c - a;
    float k4 = a - b - c + d;
    return float3(a + k1 * u.x + k2 * u.y + k4 * u.x * u.y,
                  du * float2(k1 + k4 * u.y, k2 + k4 * u.x));
}

float3 terrain(float2 p) {
    float h = 0.0;
    float2 g = float2(0.0);
    float amp = 0.5;
    float2x2 m = float2x2(1.6, -1.2, 1.2, 1.6);
    float2x2 j = float2x2(1.0, 0.0, 0.0, 1.0);
    for (int i = 0; i < 4; i++) {
        float3 n = noised(p);
        h += amp * n.x;
        g += amp * (n.yz * j);
        amp *= 0.5;
        p = m * p;
        j = m * j;
    }
    return float3(h, g);
}

half4 main(float2 fragCoord) {
    float2 dp = fragCoord / uDensity;
    float2 fromFocus = (fragCoord - uFocus) / uDensity;
    float dist = length(fromFocus);
    float pixel = 1.0 / uDensity;

    // Lock pulse: a ring that pushes the contours outwards and fades.
    float ring = 0.0;
    if (uPulse >= 0.0 && uPulse < 1.6) {
        float fade = 1.0 - uPulse / 1.6;
        float k = (dist - uPulse * 620.0) / 26.0;
        ring = exp(-k * k) * fade * fade;
        dp += fromFocus / max(dist, 1.0) * ring * 22.0;
    }

    float scale = 1.0 / 210.0;
    float3 t = terrain(dp * scale + float2(4.2 + uTime * 0.006, 1.3 - uTime * 0.004));
    float2 grad = t.yz * scale;

    // Contours: distance to the nearest level, converted to dp through the gradient.
    float levels = 24.0;
    float band = t.x * levels + uTime * 0.04;
    float f = fract(band);
    float slope = max(length(grad) * levels, 0.0001);
    float distDp = min(f, 1.0 - f) / slope;
    float major = 1.0 - step(0.5, mod(floor(band + 0.5), 5.0));
    float width = mix(0.3, 0.65, major);
    float line = (1.0 - smoothstep(width - pixel, width + pixel, distDp)) * smoothstep(1.5, 5.0, 1.0 / slope);

    // Hillshade from the same gradient; the light swings with the phone's tilt.
    float az = radians(uTilt) * 1.5 + 2.3;
    float3 light = normalize(float3(cos(az), sin(az), 0.8));
    float3 normal = normalize(float3(-grad * 260.0, 1.0));
    float shade = clamp(dot(normal, light), 0.0, 1.0);

    float near = exp(-dist * dist / 70000.0);
    float3 col = uBase * (0.35 + 1.6 * shade) + uAccent * 0.012 * t.x;
    float3 ink = uAccent * mix(0.07, 0.15, major) * (1.0 + 2.0 * near * length(uGlow));
    col = mix(col, ink, line * mix(0.5, 0.85, major));
    col += uGlow * near * 0.08;
    col += uAccent * ring * 0.32;

    // The real horizon through the dial: ordered strata below it, a lime line on it.
    if (uHorizon > 0.002) {
        float above = dot(fromFocus, uUp);
        float along = dot(fromFocus, float2(-uUp.y, uUp.x));
        float depth = max(-above, 0.0);
        float y = depth + noise(float2(along * 0.012, depth * 0.02)) * 8.0 * smoothstep(0.0, 80.0, depth);
        float g = fract(y / 11.0);
        float strong = 1.0 - step(0.5, mod(floor(y / 11.0 + 0.5), 4.0));
        float thickness = mix(0.3, 0.6, strong);
        float strata = 1.0 - smoothstep(thickness - pixel, thickness + pixel, min(g, 1.0 - g) * 11.0);
        float3 ground = uBase * 0.55 + uAccent * strata * mix(0.07, 0.15, strong) * exp(-depth / 320.0);
        col = mix(col, ground, smoothstep(-pixel, pixel, -above) * uHorizon);
        float core = 1.0 - smoothstep(0.6, 0.6 + 2.0 * pixel, abs(above));
        float gap = smoothstep(uClear * 0.9, uClear + 12.0, length(fromFocus));
        col += uAccent * (core * 0.8 + exp(-abs(above) / 9.0) * 0.16) * uHorizon * gap;
    }

    float2 q = fragCoord / uSize;
    col *= 0.6 + 0.4 * pow(16.0 * q.x * q.y * (1.0 - q.x) * (1.0 - q.y), 0.25);
    float frame = mod(floor(uTime * 24.0), 61.0);
    col += (hash(fragCoord + frame * 17.0) - 0.5) * 0.016;
    return half4(half3(max(col, float3(0.0))), 1.0);
}
"""
}
