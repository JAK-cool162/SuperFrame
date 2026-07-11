# GLShader

Chunk light cache, shadow cubemaps, cascaded shadows, heightmap sun occlusion, and cheap GI for Minecraft Fabric.

Designed to feed into **SuperFrame** for FSR upscaling + FrameGen output.

- **Target:** Minecraft 1.21.11, Java 21
- **Dependencies:** Fabric API, Cloth Config, ModMenu (all required)
- **Optional / Compatible:** Sodium, Iris, SuperFrame
  - If Iris shader pack is active, GLShader disables itself automatically
  - SuperFrame integration: static world layer → SuperFrame render target, scale change invalidates light cache + shadow cubemaps

---

## Systems (implemented in order)

### 1. CHUNK LIGHT CACHE SYSTEM ✅
- Access `LevelChunk` data via Mixin (`LevelChunkMixin`)
- Pre-calculate light zones per chunk
- Store as lightweight lightmap per chunk (`ChunkLightCache`)
- Invalidate cache only on block change event (`setBlockState` mixin)
- API: `LightCacheManager.getOrCreate(chunkPos, chunk)`

Files:
- `light/LightCacheManager.java`
- `light/ChunkLightCache.java`
- `mixin/LevelChunkMixin.java`

### 2. PER LIGHT SOURCE SHADOW CUBEMAP ✅
- Each `BlockPos` light source owns a 360° shadow cubemap
- Store as a depth map per light (6 faces)
- Cache it — only recalculate when a block changes within that light's range
- Register a block change listener via Mixin on `LevelChunk.setBlockState()`
- On change: find all light sources within range, mark their cubemap dirty
- Only dirty cubemaps get recalculated next frame – max N per frame (configurable)
- Do NOT recalculate every frame

Files:
- `light/ShadowCubemap.java` – 6-face depth map, CPU raymarch, sampleShadow()
- `light/LightSource.java` – owns cubemap, lightLevel, dirty flag
- `light/LightSourceManager.java` – ConcurrentHashMap<BlockPos, LightSource>, chunk spatial index, markDirtyInRange(), tick()
- `util/ShadowUtils.java` – cubemapPixelToDirection()
- `mixin/LevelChunkMixin.java` – block change hook, light source scan on chunk load

API:
- `LightSourceManager.getLightSource(pos)`
- `LightSource.getShadowCubemap().sampleShadow(worldPos)` → 0.0-1.0 shadow factor

### 3. CASCADED SHADOW RINGS ✅
- Divide world around player into 4 rings, rings move with player position
  - Ring 1: 0-16 blocks = full resolution, updates on any block change
  - Ring 2: 16-48 blocks = 50% resolution, updates every 2-3 block changes
  - Ring 3: 48-96 blocks = 25% resolution, updates lazily, low priority
  - Ring 4: 96+ blocks = chunk blob only (8x8), minimal calculation, barely ever updates
- Farther rings update LESS frequently than closer rings
- Hook into existing dirty-flag system from Step 2
- Ring-aware block change threshold: R1=1, R2=2, R3=5, R4=20 changes
- Ring-aware update intervals: R1=1 tick, R2=3 ticks, R3=10 ticks, R4=60 ticks
- Prioritized update scheduler – Ring1 first, then Ring2, etc., closest first within ring
- Cubemap resolution scales with ring – auto-adjusts when light moves between rings
- All ring radii, resolution scales, update intervals, and block change thresholds are configurable via Cloth Config

Files:
- `shadow/ShadowRing.java` – RING_1..RING_4 enum, boundaries, resolutionScale, updateIntervalTicks, blockChangeThreshold
- `shadow/CascadedShadowManager.java` – player position tracking, ring assignment, update frequency scheduler, `getRingForPos()`, `shouldUpdateThisTick()`, `shouldMarkDirtyOnBlockChange()`, `getResolutionForLight()`
- `light/LightSource.java` – extended with: `currentRing`, `blockChangeCounter`, `lastShadowUpdateTick`, `updateRing()`, `shouldUpdateThisTick()`, `onNearbyBlockChange()`
- `light/LightSourceManager.java` – ring-aware `markDirtyInRange()`, prioritized `tick()` – sorts dirty lights by ring priority + distance, respects maxUpdatesPerFrame budget
- `config/GlShaderConfig.java` – `Shadows.cascaded` – ring radii, resolution scales, update intervals, block change thresholds – all configurable in ModMenu

### 4. STATIC WORLD vs ENTITY LAYER (planned)
- Static world renders at 25% res → handed to SuperFrame for FSR upscaling
- Entity layer renders at 75% res → bounding box shadows only → temporal blending
- Composite both layers before SuperFrame output

### 5. HEIGHTMAP SUN OCCLUSION (planned)
- Use chunk heightmap to block sunlight below surface
- Prevents light leaking through terrain/caves
- Cave openings create natural light shafts via heightmap gaps

### 6. FAKE GLOBAL ILLUMINATION (planned)
- Color bleeding from neighbor block colors
- Ambient occlusion derived from heightmap
- Light bounce approximation between chunks

---

## SuperFrame integration
- `compat/SuperFrameCompat.java` – reflection-based, optional runtime dep
- Register scale change listener → invalidate light cache + mark all shadow cubemaps dirty
- Static world layer is designed to render into SuperFrame's scaled target
- Cascaded shadow rings automatically adjust cubemap resolution when SuperFrame render scale changes

Install both mods:
- `superframe-0.1.0.jar`
- `glshader-0.1.0.jar`

---

## Building
```
cd glshader
./gradlew build
```
Output: `build/libs/glshader-0.1.0.jar`

For first build, copy the gradle wrapper from the parent SuperFrame project:
```
cp ../gradlew ../gradlew.bat ../gradle -r .
```

---

## License
MIT
