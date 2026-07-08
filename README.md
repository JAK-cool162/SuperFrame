# SuperFrame

Upscaling, performance tweaks, and experimental frame generation for Minecraft (Fabric).

- **Works on any architecture**: ARM64, x86_64, etc. – pure Java / OpenGL, no native DLSS/FSR2 runtime required.
- **Upscaling**: Nearest / Bilinear / FSR 1.0 (EASU + RCAS) / CAS Sharpen
- **Frame Generation** (experimental, software): Blend 2x / Flow Lite – interpolates between last two world frames, UI is never frame-generated
- **Performance Tweaks**: Dynamic Resolution, particle culling, fast fog, entity shadow toggle
- **Sodium / Iris compatible**
- **In-game only**: World render target is scaled, HUD / GUI stays at native resolution
- **Configurable via**: ModMenu + Cloth Config screen, and in-game keybinds

---

## Minecraft Version

Target: **Minecraft 1.21.1**, Fabric Loader ≥0.16.14, Java 21

- Fabric API: `0.116.6+1.21.1`
- Cloth Config: `15.0.140`
- ModMenu: `11.0.3`

### Porting to 1.21.5+ / 1.21.11

The repo includes FSR 1.0 shaders (`assets/superframe/shaders/core/easu.fsh`, `rcas.fsh`) ported from [RenderScale](https://github.com/Zolo101/RenderScale) (MIT).

For 1.21.5+ (new RenderPipeline / blit pipeline):
1. bump `minecraft_version` in `gradle.properties` to `1.21.5` / `1.21.8` / `1.21.11`
2. update Fabric API / Cloth Config / ModMenu versions accordingly
3. enable the `Upscaler.blit()` RenderPipeline path in `Upscaler.java` (commented out)
4. replace the `src.blitToScreen()` call in `SuperFrame.upscaleToTarget()` with `Upscaler.blit(...)`

All the shader assets are already included.

---

## Features

### Upscaling
Render the 3D world at a custom scale (25% – 400%), then upscale to native window resolution.
UI / HUD is rendered afterwards at native resolution, so text stays crisp.

Upscalers:
- Nearest – fastest, pixelated
- Bilinear – smooth
- FSR 1.0 – AMD FidelityFX Super Resolution EASU + RCAS (1.21.5+)
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

## License

MIT – see LICENSE
