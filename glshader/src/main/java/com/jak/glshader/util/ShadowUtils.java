package com.jak.glshader.util;

/**
 * Shadow cubemap utilities
 */
public class ShadowUtils {
    // Face order: +X, -X, +Y, -Y, +Z, -Z
    /**
     * Convert cubemap face pixel to direction vector
     * @return float[3] {dx, dy, dz} normalized-ish
     */
    public static float[] cubemapPixelToDirection(int face, int x, int y, int resolution) {
        // Map pixel center to [-1,1]
        float u = (2.0f * (x + 0.5f) / resolution) - 1.0f;
        float v = (2.0f * (y + 0.5f) / resolution) - 1.0f;
        float dx = 0, dy = 0, dz = 0;
        switch (face) {
            case 0 -> { dx = 1.0f;  dy = -v; dz = -u; } // +X
            case 1 -> { dx = -1.0f; dy = -v; dz =  u; } // -X
            case 2 -> { dx =  u; dy = 1.0f;  dz =  v; } // +Y
            case 3 -> { dx =  u; dy = -1.0f; dz = -v; } // -Y
            case 4 -> { dx =  u; dy = -v; dz = 1.0f; } // +Z
            case 5 -> { dx = -u; dy = -v; dz = -1.0f; } // -Z
        }
        return new float[]{dx, dy, dz};
    }
}
