#version 150
// SuperFrame - Flow Lite FrameGen
// Cheap optical-flow guided blend – luma difference masking to reduce ghosting
uniform sampler2D PrevSampler;
uniform sampler2D CurrSampler;
uniform float BlendFactor;

in vec2 texCoord;
out vec4 fragColor;

float luma(vec3 c){ return dot(c, vec3(0.299,0.587,0.114)); }

void main(){
    vec4 prevCol = texture(PrevSampler, texCoord);
    vec4 currCol = texture(CurrSampler, texCoord);
    float lp = luma(prevCol.rgb);
    float lc = luma(currCol.rgb);
    float diff = abs(lc - lp);
    // reduce blending where there's high motion (reduces ghosting)
    float motionMask = clamp(diff * 4.0, 0.0, 1.0);
    float bf = mix(BlendFactor, 0.5 + (BlendFactor-0.5)*0.5, motionMask);
    fragColor = mix(prevCol, currCol, bf);
}
