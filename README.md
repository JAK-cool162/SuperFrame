# SuperFrame + GLShader

**SuperFrame** – Upscaling, performance tweaks, and experimental frame generation for Minecraft (Fabric).

**GLShader** – Chunk light cache, shadow cubemaps, cascaded shadows, heightmap sun occlusion, and cheap GI. Designed to feed into SuperFrame for FSR upscaling + FrameGen. See [`glshader/`](./glshader/).

---

- **Works on any architecture**: ARM64, x86_64, etc. – pure Java / OpenGL, no native DLSS/FSR2 runtime required.
- **Upscaling**: Nearest / Bilinear / FSR 1.0 (EASU + RCAS) / CAS Sharpen
- **Frame Generation** (experimental, software): Blend 2x / Flow Lite – interpolates between last two world frames, UI is never frame-generated
- **Performance Tweaks**: Dynamic Resolution, particle culling, fast fog, entity shadow toggle
- **Sodium / Iris compatible**
- **In-game only**: World render target is scaled, HUD / GUI stays at native resolution
- **Configurable via**: ModMenu + Cloth Config screen, and in-game keybinds

---

## Minecraft Version

Target: **Minecraft 1.21.11**, Fabric Loader ≥0.18.2, Java 21

- Fabric API: `0.139.4+1.21.11`
- Cloth Config: `21.11.150+fabric`
- ModMenu: `14.0.0`
- Sodium: `0.6.13+mc1.21.11` (optional)

### Rendering pipeline

FSR 1.0 EASU+RCAS shaders are included (`assets/superframe/shaders/core/easu.fsh`, `rcas.fsh`).

- **1.21.1 – 1.21.4**: uses vanilla `blitToScreen` (Nearest / Bilinear)
- **1.21.5+ / 1.21.11**: enable the `Upscaler.blit()` RenderPipeline path in `Upscaler.java` for full FSR 1.0
  1. `SuperFrame.upscaleToTarget()` → call `Upscaler.blit(...)` instead of `src.blitToScreen()`
  2. Pipelines are already defined in `Upscaler.java` (commented)

All shader assets are included.

---

## Features

### Upscaling
Render the 3D world at a custom scale (25% – 400%), then upscale to native window resolution.
UI / HUD is rendered afterwards at native resolution, so text stays crisp.

Upscalers:
- Nearest – fastest, pixelated
- Bilinear – smooth
- FSR 1.0 – AMD FidelityFX Super Resolution EASU + RCAS (1.21.11 – enable RenderPipeline path in Upscaler.java)
- CAS – Contrast Adaptive Sharpen

### Frame Generation
Pure-GPU, architecture-agnostic interpolator.

- **OFF** – vanilla
- **Blend 2x** – temporal blend of previous/current frame, ~2× perceived smoothness
- **Flow Lite** – luma-difference masked blend, reduces ghosting

This is NOT DLSS Frame Generation / FSR Frame Generation (those need motion vectors and vendor runtimes). SuperFrame FG is a software optical-flow-lite interpolator designed to run on **any GPU, ARM64 or x86_64**, and to be compatible with Sodium/Iris.

FrameGen only touches the world render target – GUI is never interpolated.

Future: Window.swapBuffers mixin for true 2× present rate.

### Performance Tweaks
- Dynamic Resolution – auto-lower scale to hit target FPS
- Reduce Particles
- Fast Fog
- No Entity Shadows

### Keybinds
- `O` – Toggle SuperFrame
- `=` / `-` – Increase / Decrease render scale
- `P` – Cycle FrameGen mode

All rebindable in Controls.

### Compatibility
- **Sodium**: Fully compatible. Optional Sodium options page hook in `compat.sodium`.
- **Iris / Oculus**: Shader pack reload is triggered on scale change. Optional per-shader-pack scale override in config (`irisShaderScaleOverride`).

If you hit issues, open an issue with your `latest.log`.

---

## Building

```bash
./gradlew build
```

Output: `build/libs/superframe-0.1.0.jar`

---

## Credits

- Upscaling / render target swap technique based on **RenderScale** by Zelo101 – MIT License – https://github.com/Zolo101/RenderScale
- FSR 1.0 shaders – AMD FidelityFX, ported via RenderScale
- Fabric, Cloth Config, ModMenu, Sodium, Iris teams

SuperFrame adds: Frame Generation module, performance tweak suite, keybind HUD, architecture-agnostic design, extended config.

---

## GLShader companion mod

`glshader/` – Minecraft 1.21.11 / Java 21 – separate Fabric mod

Implements:

1. **Chunk Light Cache System** ✅ – LevelChunk mixin, pre-calculated light zones, invalidates on block change
2. **Per-Light Shadow Cubemap** ✅ – each BlockPos light owns a 360° shadow cubemap, 6-face depth map, cached, only recalculates when a block changes within light range, NOT every frame – max N updates/frame configurable
3. Cascaded Shadow Rings (planned)
4. Static World vs Entity Layer – 25% / 75% split → SuperFrame FSR (planned)
5. Heightmap Sun Occlusion (planned)
6. Fake Global Illumination (planned)

- Auto-disables if Iris shader pack is active
- SuperFrame scale change → invalidate light cache + shadow cubemaps
- Compatible: ModMenu, Cloth Config, Fabric API, Sodium, Iris, SuperFrame

Build:
```
cd glshader
./gradlew build
```
Output: `build/libs/glshader-0.1.0.jar`

See [glshader/README.md](./glshader/README.md) for full API docs.

---

## License

MIT – see LICENSE
