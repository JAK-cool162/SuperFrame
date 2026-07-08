#version 150

// SuperFrame - FrameGen Blend
// Simple temporal interpolation between prev and curr frames
// Works on all GL 3.2+ hardware, ARM64/x86_64 agnostic

uniform sampler2D PrevSampler;
uniform sampler2D CurrSampler;
uniform float BlendFactor; // 0.0 = prev, 1.0 = curr, 0.5 = midpoint

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 prevCol = texture(PrevSampler, texCoord);
    vec4 currCol = texture(CurrSampler, texCoord);
    // simple lerp, with optional ghosting reduction
    vec4 blended = mix(prevCol, currCol, BlendFactor);
    fragColor = blended;
}
